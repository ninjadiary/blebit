package cybervelia.sdk.controller.ce;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.types.ConnectionTypesCE;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesPE;
import cybervelia.sdk.controller.BLESerialPort;


public class CEController {
	private HashMap<String, List<BLEService>> services_map;
	
	private BLESerialPort port = null;
	private InputStream in = null;
	private OutputStream out = null;
	private CEEventHandler evt_handler = null;
	private CEBLEDeviceCallbackHandler callbackHandler = null;
	private Thread handler_thread = null;
	private boolean is_initialized;
	private byte[] notification_data_tmp;
	private byte CONNECTION_INITIALIZER[] = {0x10, 'c','y','b','e','r','v','e','l','i','a'};
	private boolean init_correct = false;
	private short firmware_version = 0;
	
	public CEController(String str_port, CEBLEDeviceCallbackHandler callbackHandler) throws IOException 
	{
		// Open Serial Port
		port = new BLESerialPort(str_port);
		//if (!port.openPort())
		//	throw new IOException("Cannot open CEController serial port!!");
		port.setBaudRate(9600);
		port.clearDTR();
		port.clearRTS();
		port.setComPortParameters(9600, 8, 1, BLESerialPort.NO_PARITY);
		if (!port.openPort()) throw new IOException("Serial Port could not be opened");
		else {
			if (port.setComPortTimeouts(BLESerialPort.TIMEOUT_READ_BLOCKING, 0, 0) == false)
			{
				throw new IOException("Cannot configure the serial port");
			}
			
			in = port.getInputStream();
			out = port.getOutputStream();
		}
		
		if (callbackHandler == null) throw new IOException("Null callback handler given");
		this.callbackHandler = callbackHandler;
		this.callbackHandler.setController(this);
		is_initialized = false;
		initializeConnection();
		services_map = new HashMap<String, List<BLEService>>();
		notification_data_tmp = new byte[2];
	}
	
	public static boolean reset(String device_port)
	{
		try {
			InputStream in = null;
			OutputStream out = null;
			BLESerialPort port = new BLESerialPort(device_port);
			port.setBaudRate(9600);
			port.clearDTR();
			port.clearRTS();
			port.setComPortParameters(9600, 8, 1, BLESerialPort.NO_PARITY);
			if (!port.openPort()) throw new IOException("Serial Port could not be opened for reset");
			else {
				if (port.setComPortTimeouts(BLESerialPort.TIMEOUT_READ_BLOCKING, 0, 0) == false)
				{
					throw new IOException("Cannot configure the serial port");
				}
				
				in = port.getInputStream();
				out = port.getOutputStream();
			}
			out.write(ConnectionTypesCE.EVT_RESET);
			port.closePort();
			try {Thread.sleep(1000);}catch(InterruptedException ie) {}
			return true;
		}catch(IOException ex) {
			System.err.println("Unable to reset CE: " + ex.getMessage());
			return false;
		}
	}
	
	private void initializeConnection() throws IOException {
		out.write(ConnectionTypesCE.EVT_RESET);
		try {Thread.sleep(1000);}catch(InterruptedException ie) {}
		out.write(CONNECTION_INITIALIZER);
		firmware_version = getShort();
		
		if (((firmware_version & 0xffff) & 0x8000) > 0)
		{
			firmware_version = (short)(firmware_version  - 0x8000);
			init_correct = false;
		}
		else
		{
			init_correct = true;
			ConnectionTypesCommon.validateVersion(firmware_version);
		}
	}
	
	public InputStream getInputStream() {
		return in;
	}
	
	public OutputStream getOutputStream() {
		return out;
	}
	
	public BLESerialPort getSerialPort() {
		return port;
	}
	
	public short getFirmwareVersion() {
		return firmware_version;
	}
	
	public void switchIO(InputStream in, OutputStream out, BLESerialPort port, short firmwarev) throws IOException {
		this.in = in;
		this.out = out;
		this.port = port;
		this.firmware_version = firmwarev;
		ConnectionTypesCommon.validateVersion(firmware_version);
	}
	
	public boolean isInitializedCorrectly() {
		return init_correct;
	}
	
	public void sendConnectionParameters(CEConnectionParameters ceparameters) throws IOException 
	{	
		if (is_initialized) throw new IOException("Device already Initialized");
		
		ceparameters.validate();
		
		out.write(ConnectionTypesCE.STP_CONN_PARAMS);
		sendShort(out, ceparameters.getMinConnectionIntervalMs());
		sendShort(out, ceparameters.getMaxConnectionIntervalMs());
		sendShort(out, ceparameters.getSlaveLatency());
		sendShort(out, ceparameters.getConnectionSupervisionLatencyMs());
		
		sendShort(out, ceparameters.getScanInterval());
		sendShort(out, ceparameters.getScanWindow());
		sendShort(out, ceparameters.getScanTimeout());
		
		sendShort(out, ceparameters.getConnectionRequestInterval());
		sendShort(out, ceparameters.getConnectionRequestWindow());
		sendShort(out, ceparameters.getConnectionRequestSupervisionTimeout());
		
		out.write((byte)(ceparameters.scanRequestsEnforced() ? 1:0)); // to use ADV-SCAN while scanning for PEs
		verifySuccess(false);
		System.out.println("Connection Parameters Set Successfully");
	}
	
	public void sendBluetoothDeviceAddress(String address, ConnectionTypesCommon.BITAddressType address_type) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		if (address.length() != 17)
			throw new IOException("Malformed Hardware Address");
		
		out.write(ConnectionTypesCE.STP_SET_ADDRESS);
		
		// send address
		if (address_type == ConnectionTypesCommon.BITAddressType.PUBLIC)
			out.write(0);
		else
			out.write(1);
		
		out.write(hwaddr_to_byte_array(address));
		verifySuccess(false);
		System.out.println("Set Address Successfully");
	}
	
	public void deviceDisableRepairingAfterBonding() throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		out.write(ConnectionTypesCE.STP_DISABLE_REPAIRING);
		// device does not return any errors
		System.out.println("Disabling Re-Pairing Success");
	}
	
	public void eraseBonds() throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		
		out.write(ConnectionTypesCE.STP_ERASE_BONDS);
		// device does not return any errors
	}
	
	public boolean deletePeerBond(short peer_id)
	{
		// you may delete bonds only when you are disconnected and no advertising/scan is on place - otherwise it will fail
		if (callbackHandler.isDeviceConnected()) return false;
		return callbackHandler.deletePeerBond(peer_id);
	}
	
	public void bondOnConnect(boolean force_repairing) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		out.write(ConnectionTypesCE.EVT_BOND_ON_CONNECT);
		out.write(force_repairing == true ? 1 : 0);
	}
	
	public void finishSetup() throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		out.write(ConnectionTypesCE.STP_FINISH);
		verifySuccess(false);
		is_initialized = true;
		processConnected();
	}
	
	public int readLTKOwnKey(byte[] data) throws IOException {
		return callbackHandler.readLTKOwnKey(data);
	}
	
	public int readLTKPeerKey(byte[] data) throws IOException {
		return callbackHandler.readLTKPeerKey(data);
	}
	
	public boolean isBonded() {
		return callbackHandler.isBonded();
	}
	
	private void processConnected()  {
		if (evt_handler == null) 
			evt_handler = new CEEventHandler(in, out, this.callbackHandler);
		
		handler_thread = new Thread(evt_handler);
		handler_thread.start();
	}
	
	public void configurePairing(ConnectionTypesCommon.PairingMethods p_method, String static_pin) throws IOException
	{
		if (is_initialized) throw new IOException("Device already Initialized");
		byte sel_method = 0;
		if (p_method != ConnectionTypesCommon.PairingMethods.NO_IO && static_pin != null && static_pin.length() != 6) throw new IOException("PIN Length should be 6 digits");
		if (p_method == ConnectionTypesCommon.PairingMethods.NO_IO && static_pin != null) throw new IOException("Cannot set static key with NO/IO method");
		if (p_method == ConnectionTypesCommon.PairingMethods.DISPLAY && static_pin == null) throw new IOException("You should set a static key along with display-only pairing method");
		out.write(ConnectionTypesCE.STP_PAIRING_INFO);
		switch(p_method) {
			case NO_IO:
				sel_method = ConnectionTypesCommon.PAIRING_METHOD_NO_IO;
				break;
			case KEYBOARD:
				sel_method = ConnectionTypesCommon.PAIRING_METHOD_KEYBOARD;
				break;
			case DISPLAY:
				sel_method = ConnectionTypesCommon.PAIRING_METHOD_DISPLAY;
				break;
			case KEY_DISP:
				sel_method = ConnectionTypesCommon.PAIRING_METHOD_KEY_DISP;
				break;
			default:
				sel_method = 0; // device will return error
		}
		out.write(sel_method);
		
		if (static_pin != null)
		{
			out.write(6);
			for(int i = 0; i<6; ++i)
				out.write(static_pin.charAt(i));
		}
		else
			out.write(0);
		
		verifySuccess(false);
	}
	
	public boolean connectNow(String address, ConnectionTypesCommon.AddressType type) throws IOException
	{
		if (!is_initialized) throw new IOException("Device have to be Initialized first");
		byte mac_addr[] = hwaddr_to_byte_array(address);
		return this.callbackHandler.connect(mac_addr, type, true);
	}
	
	public boolean connect(String address, ConnectionTypesCommon.AddressType type) throws IOException
	{
		if (!is_initialized) throw new IOException("Device have to be Initialized first");
		byte mac_addr[] = hwaddr_to_byte_array(address);
		return this.callbackHandler.connect(mac_addr, type, false);
	}
	
	public boolean isConnectRequestInProgress() {
		return this.callbackHandler.isConnectRequestInProgress();
	}
	
	public boolean cancelConnectRequest() throws IOException {
		if (!is_initialized) throw new IOException("Device have to be Initialized first");
		return this.callbackHandler.cancelConnectRequest();
	}
	
	public boolean startScan() {
		if (isDeviceConnected())
			return false;
		
		return this.callbackHandler.startScan();
	}
	
	public void stopScan() {
		this.callbackHandler.stopScan();
	}
	
	public boolean isDeviceConnected() {
		return this.callbackHandler.isDeviceConnected();
	}
	
	public String getConnectedDeviceAddress() {
		return this.callbackHandler.getClientAddress();
	}
	
	public ConnectionTypesCommon.AddressType getConnectedDeviceAddressType() {
		return this.callbackHandler.getClientAddressType();
	}
	
	public int getDisconnectionReason() {
		return this.callbackHandler.getDisconnectionReason();
	}
	
	public void disconnect(int reason) {
		this.callbackHandler.disconnect(reason);
	}
	
	public List<BLEService> getDiscoveredServices()
	{
		if (!this.callbackHandler.isDeviceConnected()) return null;
		return services_map.get(this.getClientAddress());
	}
	
	public boolean startDiscovery(boolean block) {
		if (!this.callbackHandler.isDeviceConnected()) return false;
		if(services_map.get(this.callbackHandler.getClientAddress()) != null) return true;
		return this.callbackHandler.startDiscovery(block);
	}
	
	public boolean isDiscoveryInProgress() {
		return this.callbackHandler.isDiscoveryInProgress();
	}
	
	public boolean getDiscoverySuccess() {
		return this.callbackHandler.getDiscoverySuccess();
	}
	
	public ConnectionTypesCommon.AddressType getClientAddressType() {
		return this.callbackHandler.getClientAddressType();
	}
	
	public String getClientAddress() {
		return this.callbackHandler.getClientAddress();
	}
	
	protected void addServicesOnDiscoveryComplete(List<BLEService> services)
	{
		if (!services_map.containsKey(this.callbackHandler.getClientAddress()))
			services_map.put(this.callbackHandler.getClientAddress(), services);
	}
	
	public void clearCachedServices(String client_address)
	{
		services_map.remove(client_address);
	}
	
	public boolean writeData(byte []data, int offset, int len, short handle) throws IOException
	{
		if (offset + len > data.length) throw new IOException("Write data length needs to be higher than provided length parameter");
		if (len > 31) throw new IOException("Length provided is greater than maximum allowed data length");
		return callbackHandler.writeData(data, offset, len, handle, 0);
	}
	
	public boolean writeData(byte []data, int offset, int len, short handle, int tm_ms) throws IOException
	{
		return callbackHandler.writeData(data, offset, len, handle, tm_ms);
	}
	
	public void writeDataCMD(byte []data, int offset, int len, short handle) throws IOException
	{
		if (offset + len > data.length) throw new IOException("Write data length needs to be higher than provided length parameter");
		if (len > 31) throw new IOException("Length provided is greater than maximum allowed data length");
		callbackHandler.writeDataCMD(data, offset, len, handle);
	}
	
	public boolean enableNotifications(short cccd_handle)
	{
		notification_data_tmp[1] = 0;
		notification_data_tmp[0] = 1;
		
		return callbackHandler.writeData(notification_data_tmp, 0, 2, cccd_handle, 0);
	}
	
	public boolean disableNotifications(short cccd_handle)
	{
		notification_data_tmp[0] = 0;
		return callbackHandler.writeData(notification_data_tmp, 0, 2, cccd_handle, 0);
	}
	
	public boolean enableIndications(short cccd_handle)
	{
		notification_data_tmp[1] = 0;
		notification_data_tmp[0] = 2;
		
		return callbackHandler.writeData(notification_data_tmp, 0, 2, cccd_handle, 0);
	}
	
	public boolean disableIndications(short cccd_handle)
	{
		notification_data_tmp[0] = 0;
		return callbackHandler.writeData(notification_data_tmp, 0, 2, cccd_handle, 0);
	}
	
	public int readData(byte[] data, int offset, int len, short handle) throws IOException
	{
		return callbackHandler.readData(data, offset, len, handle, 0);
	}
	
	public int readData(byte[] data, int offset, int len, short handle, int tm_ms) throws IOException
	{
		return callbackHandler.readData(data, offset, len, handle, tm_ms);
	}
	
	public boolean bondNow(boolean force_repairing)
	{
		return callbackHandler.bondNow(force_repairing);
	}
	
	public BLECharacteristic getCharacteristicByHandle(short handle, String client_address)
	{
		BLECharacteristic characteristic = null;
		if (services_map.containsKey(client_address)) {
			for(BLEService service : services_map.get(client_address))
			{
				characteristic = service.getCharacteristicByHandle(handle);
				if (characteristic != null) break;
			}
		}
		return characteristic;
	}
	
	public BLECharacteristic getCharacteristicByCCCDHandle(short handle, String client_address)
	{
		BLECharacteristic characteristic = null;
		if (services_map.containsKey(client_address)) {
			for(BLEService service : services_map.get(client_address))
			{
				characteristic = service.getCharacteristicByCCCDHandle(handle);
				if (characteristic != null) break;
			}
		}
		return characteristic;
	}
	
	private short getShort() throws IOException {
		return (short)((in.read() + (in.read() << 8)) & 0xffff);
	}
	
	public void terminate()
	{
		try {
			//try {Thread.sleep(1000);}catch(InterruptedException iex) {}
			
			if (evt_handler != null)
			{
				evt_handler.shutdown();
				try{handler_thread.join(1100);}catch(InterruptedException iex) {handler_thread.interrupt();}
			}
			
			out.write(ConnectionTypesCE.EVT_RESET);
			
			if (!port.closePort())
				System.err.println("Failed to close CEController's serial port");
			
		}catch(IOException x) {System.err.println("CE.terminate(): " + x.getMessage());}
	}
	
	// Helper Functions
	
	private void sendShort(OutputStream out, int value) throws IOException {
		out.write((byte) (value & 0xff));
		out.write((byte) ((value & 0xff00) >> 8));
	}
	
	private void verifySuccess(boolean first_code_received) throws IOException {
		StringBuilder error_str = new StringBuilder();
		boolean generate_error = false;
		byte err_code;
		byte err_message_cnt = 0;
		if (first_code_received == false)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesCE.STP_ERROR)
				throw new IOException("Supposed to receive an error message");
		}
		err_message_cnt = (byte) in.read();
		while(err_message_cnt > 0)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesCE.STP_ERROR && err_code != ConnectionTypesCE.ERR_SUCCESS)
			{
				generate_error = true;
				error_str.append("Error received ");
				error_str.append(String.valueOf(err_code));
				error_str.append("\n");
			}
			--err_message_cnt;
		}
		if (generate_error)
			throw new IOException(error_str.toString());
	}
	
	
	private byte[] hwaddr_to_byte_array(String addr)
	{
		byte byteArray[] = new byte[6];
		for (int i=0, j=0; i<addr.length() && i < 17; i+=3)
		{
		  byteArray[j++] = hex2byte(addr.charAt(i), addr.charAt(i+1));
		}
		return byteArray;
	}
	
	private int hex2num(char c)
	{
		if (c >= '0' && c <= '9')
			return c - '0';
		if (c >= 'a' && c <= 'f')
			return c - 'a' + 10;
		if (c >= 'A' && c <= 'F')
			return c - 'A' + 10;
		return -1;
	}
	
	private byte hex2byte(char ch1, char ch2)
	{
		int a, b;
		a = hex2num(ch1);
		if (a < 0)
			return -1;
		b = hex2num(ch2);
		if (b < 0)
			return -1;
		return (byte) ((a << 4) | b);
	}
}
