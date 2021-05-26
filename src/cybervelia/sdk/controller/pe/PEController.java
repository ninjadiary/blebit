package cybervelia.sdk.controller.pe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLESerialPort;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.pe.callbacks.BondKeysCallback;
import cybervelia.sdk.controller.pe.callbacks.BondingCallback;
import cybervelia.sdk.controller.pe.callbacks.PEConnectionCallback;
import cybervelia.sdk.controller.pe.callbacks.PENotificationDataCallback;
import cybervelia.sdk.controller.pe.callbacks.PEReadCallback;
import cybervelia.sdk.controller.pe.callbacks.UpdateValueCallback;
import cybervelia.sdk.controller.pe.callbacks.PEWriteEventCallback;
import cybervelia.sdk.controller.pe.handlers.PEAdvertisingHandler;
import cybervelia.sdk.controller.pe.handlers.PEBondKeysHandler;
import cybervelia.sdk.controller.pe.handlers.PEBondingHandler;
import cybervelia.sdk.controller.pe.handlers.PEConnectionHandler;
import cybervelia.sdk.controller.pe.handlers.PENotificationDataHandler;
import cybervelia.sdk.controller.pe.handlers.PEReadHandler;
import cybervelia.sdk.controller.pe.handlers.PEUpdateValueHandler;
import cybervelia.sdk.controller.pe.handlers.PEWriteEventHandler;
import cybervelia.sdk.types.BLEAttributePermission;
import cybervelia.sdk.types.ConnectionTypesCE;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesPE;
import cybervelia.server.OutputSplitter;

public class PEController {
	private ArrayList<BLEService> peripheral_services;
	private AtomicInteger next_service_id;
	private byte CONNECTION_INITIALIZER[] = {0x10, 'c','y','b','e','r','v','e','l','i','a'};
	private boolean init_correct = false;
	private short firmware_version = 0;
	
	// attribute permission values
	protected final byte PRM_NO_ACCESS 			 		= 0x01;
	protected final byte PRM_OPEN 					 	= 0x02;
	protected final byte PRM_ENCRYPTION 			 	= 0x04;
	protected final byte PRM_ENCRYPTION_MITM 		 	= 0x08;
	
	// Object fields
	private PEBLEDeviceCallbackHandler callback_handler = null;
	private BLESerialPort port = null;
	private InputStream in = null;
	private OutputStream out = null;
	private PEEventHandler evt_handler = null;
	private Thread handler_thread = null;
	private boolean is_initialized;
	private String device_name = null;
	private int advertisement_timeout = 0;
	private ConnectionTypesCommon.BITAddressType direct_address_type;
	private String direct_address = null;
	private byte connection_type;
	private boolean disable_pairing_after_bonding = true;
	private String peripheral_address = null;
	
	public PEController(String str_port, PEBLEDeviceCallbackHandler callbackHandler) throws IOException {
		// Open Serial Port
		port = new BLESerialPort(str_port);
		//if (!port.openPort())
		//	throw new IOException("Cannot open PEController serial port!!");
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
			//out = new myout();
		}
		if (callbackHandler == null) throw new IOException("Null callback handler given");
		this.callback_handler = callbackHandler;
		this.callback_handler.setController(this);
		is_initialized = false;
		peripheral_services = new ArrayList<BLEService>();
		initializeConnection();
		this.next_service_id = new AtomicInteger(1);
	}
	
	/* function reset: Reset device
	 * args: str_port - COM Port (ie COM3 or ttyACM0) 
	 * Return: true if reset was successful 
	 * */
	public static boolean reset(String str_port)
	{
		try {
			InputStream in = null;
			OutputStream out = null;
			BLESerialPort port = new BLESerialPort(str_port);
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
			out.write(ConnectionTypesPE.EVT_RESET);
			port.closePort();
			try {Thread.sleep(1000);}catch(InterruptedException ie) {}
			
			return true;
		}catch(IOException ex) {
			System.err.println("Unable to reset PE: " + ex.getMessage());
			return false;
		}
	}
	
	/* function initializeConnection: Initialize the device and understand the device's type (PE or CE)
	 * args: None
	 * Return: None
	 * */
	private void initializeConnection() throws IOException {
		out.write(ConnectionTypesPE.EVT_RESET);
		try {Thread.sleep(1000);}catch(InterruptedException ie) {}
		out.write(CONNECTION_INITIALIZER);
		firmware_version = getShort();
		
		if (((firmware_version & 0xffff) & 0x8000) == 0)
			init_correct = false;
		else
		{
			firmware_version = (short)(firmware_version  - 0x8000);
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
	
	/* switchIO: Replace the streams of the current controller, along with BLESerialPort object and firmware version
	 * args: 
	 * 	in: New Inputstream
	 * 	out: New OutputStream
	 * 	port: New BLESerialPort
	 * 	firmwarev: New Firmware Version
	 * Return: None */
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
	
	private void processConnected()  {
		if (evt_handler == null) 
			evt_handler = new PEEventHandler(in, out, this.callback_handler);
		
		handler_thread = new Thread(evt_handler);
		handler_thread.start();
	}
	
	public boolean isDeviceConnected() {
		return this.callback_handler.isDeviceConnected();
	}
	
	public void disconnect() {
		if (this.callback_handler.isDeviceConnected())
			callback_handler.disconnect();
	}
	
	public boolean bondNow(boolean force_repairing) throws IOException {
		if (!this.callback_handler.isDeviceConnected()) return false;
		return this.callback_handler.bondNow(force_repairing);
	}
	
	public int getDisconnectionReason()
	{
		return callback_handler.getDisconnectionReason();
	}
	
	public void finishSetup() throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		out.write(ConnectionTypesPE.STP_FINISH_SETUP);
		verifySuccess(false);
		is_initialized = true;
		processConnected();
	}
	
	public void setDirectAddress(String address, ConnectionTypesCommon.BITAddressType type) throws IOException {
		if (address.length() != 17)
			throw new IOException("Malformed Hardware Address");
		
		this.direct_address = address;
		this.direct_address_type = type;
	}
	
	public boolean isForcedRepairingAllowed()
	{
		return this.disable_pairing_after_bonding;
	}
	
	public void sendAdvertisementData(AdvertisementData data) throws IOException {
		if (data.getFlags() == 0)
			throw new IOException("You have to set adv/scan. flags with non-zero value");
		sendAdvData(data, true);
	}
	
	public void sendScanData(AdvertisementData data) throws IOException {
		sendAdvData(data, false);
	}
	
	/* sendAdvData: Send advertisement data to the device
	 * args: 
	 * 	data: AdvertisementData object that holds the advertisement data
	 * 	are_adv_data: Type of Advertisement Data: Advertisement or Scan data */
	private void sendAdvData(AdvertisementData data, boolean are_adv_data) throws IOException{
		if ((data.getFlags() & AdvertisementData.FLAG_LE_LIMITED_DISCOVERABLE_MODE) > 0) 
			if (advertisement_timeout >= 288 || advertisement_timeout == 0) throw new IOException("You may not set more than 180ms advertising timeout when in LE Limited Discoverable Mode");
		
		if (((data.getFlags()  & AdvertisementData.FLAG_LE_LIMITED_DISCOVERABLE_MODE) > 0) && this.connection_type == ConnectionTypesPE.BLE_GAP_ADV_TYPE_ADV_UNDIRECTED)
			throw new IOException("You may not use Limited Discoverable Mode using Undirected Advertising");
		
		if (data.getFlags() == 0 && are_adv_data)
			throw new IOException("You have to set adv/scan. flags with non-zero value");
		
		if (is_initialized) throw new IOException("Device already Initialized");
		if (device_name != null)
			data.setDeviceName(device_name);
		
		// Send adv data to the device:
		data.sendAll(in, out, are_adv_data);
	}
	
	/* sendBLECharacteristic: Send characteristic object to th device */
	private void sendBLECharacteristic(BLECharacteristic chr) throws IOException
	{
		byte value[] = new byte[31];
		if (is_initialized) throw new IOException("Device already Initialized");
		byte r;
		byte properties = chr.getProperties();
		byte char_id = (byte)chr.getCharacteristicId();
		byte srv_id = (byte)chr.getServiceId();
		byte initial_value_length = (byte)chr.getInitialValueLength();
		byte max_value_length = (byte)chr.getMaxValueLength();
		int copied = chr.getLocalValue(value);
		byte hook_on_read = (byte) ((chr.isHookOnReadEnabled() == true) ? 1 : 0);
		BLEAttributePermission cccd_read = chr.getCCCDPermissionRead();
		BLEAttributePermission cccd_write = chr.getCCCDPermissionWrite();
		BLEAttributePermission attr_read = chr.getValuePermissionRead();
		BLEAttributePermission attr_write = chr.getValuePermissionWrite();
		byte is_variable_length = (byte) (chr.isValueLengthVariable() == true ? 1 : 0);
		byte has_cccd = (byte) (chr.isCCCDEnabled() == true ? 1 : 0);
		byte[] uuid = chr.getUUIDBytes();
		
		if (initial_value_length > 31)
			throw new IOException("Maximum Allowed Characteristic Value Length is 31 bytes");
		
		if (max_value_length > 31)
			throw new IOException("Maximum Allowed Characteristic Value Length is 31 bytes");
		
		if (chr.is_128bit_uuid())
			out.write(ConnectionTypesPE.STP_NEW_CHARACTERISTIC_128);
		else
			out.write(ConnectionTypesPE.STP_NEW_CHARACTERISTIC_16);
		
		out.write(char_id);
		out.write(srv_id);
		out.write(initial_value_length);
		out.write(max_value_length);
		out.write(properties);
		out.write(convertToPermission(attr_read));
		out.write(convertToPermission(attr_write));
		out.write(has_cccd);
		if (chr.isCCCDEnabled())
		{
			out.write(convertToPermission(cccd_read));
			out.write(convertToPermission(cccd_write));
		}
		out.write(is_variable_length);
		out.write(hook_on_read);
		out.write(value, 0, copied);
		out.write(uuid);
		r = (byte) in.read();
		if (r == ConnectionTypesPE.STP_NEW_HANDLE_RETURN)
		{
			short handle = getShort(in);
			chr.setValueHandle(handle);
			if (chr.isCCCDEnabled())
				chr.setCCCDHandle(getShort(in));
			verifySuccess(false);
			//System.out.println("BLE Characteristic put successfully - Handle: " + chr.getValueHandle());
		}
		else if (r == ConnectionTypesPE.STP_ERROR)
		{
			verifySuccess(true);
			chr.finalize();
			throw new IOException("Characteristic Handle Not Received - BLECharacteristic not added");
		}
		else
			throw new IOException("Error on RX - BLECharacteristic not added");
	}
	
	private byte convertToPermission(BLEAttributePermission perm) {
		byte encoded_permission = 0;
		switch(perm) {
			case NO_ACCESS:
				encoded_permission = PRM_NO_ACCESS;
				break;
			case OPEN:
				encoded_permission = PRM_OPEN;
				break;
			case ENCRYPTION:
				encoded_permission = PRM_ENCRYPTION;
				break;
			case ENCRYPTION_MITM:
				encoded_permission = PRM_ENCRYPTION_MITM;
				break;
			default:
				encoded_permission = 0;
		}
		return encoded_permission;
	}
	
	/* sendBLEService: Send service to the deevice */
	public boolean sendBLEService(BLEService ble_service) throws IOException {
		byte next_service_id_byte = 0;
		byte r;
		
		if (ble_service.getServiceId() == 0)
		{
			next_service_id_byte = (byte)next_service_id.getAndIncrement();
			ble_service.setServiceId(String.valueOf(next_service_id_byte));
		}
		else
			next_service_id_byte = ble_service.getServiceId();
		
		//System.out.println("------> Service ID: " + String.format("%02x", next_service_id_byte));
		
		if (is_initialized) throw new IOException("Device already Initialized");
		if (ble_service.isFinalized()) throw new IOException("You have already sent this service");
		
		if (ble_service.is_128bit_uuid())
			out.write(ConnectionTypesPE.STP_NEW_SERVICE_128);
		else
			out.write(ConnectionTypesPE.STP_NEW_SERVICE_16);
		out.write(next_service_id_byte);
		
		out.write(ble_service.getUUIDBytes());
		r = (byte)in.read();
		
		if (r == ConnectionTypesPE.STP_NEW_HANDLE_RETURN)
		{
			ble_service.setHandle(getShort(in));
			try{verifySuccess(false);}catch(IOException ioe) {return false;}
			
			addService(ble_service);
			
			//System.out.println("BLE Service put successfully - Handle: " + ble_service.getHandle());
		}
		else if (r == ConnectionTypesPE.STP_ERROR)
		{
			try{verifySuccess(true);}catch(IOException ioe) {return false;}
			throw new IOException("Serivce Handle Not Received - BLEService not added");
		}
		else
			throw new IOException("Error on RX - BLEService not added");
		
		for(BLECharacteristic chr : ble_service.getCharacteristicsAndFinalize())
		{
			sendBLECharacteristic(chr);
		}
		
		return true;
	}
	
	public void startAdvertising() throws IOException 
	{
		if (callback_handler.isDeviceConnected()) throw new IOException("Device do not advertise while connected");
		if (is_initialized == false) throw new IOException("Device is not initialized yet");
		this.callback_handler.startAdvertising();
	}
	
	public int readLTKOwnKey(byte[] data) throws IOException {
		return this.callback_handler.readLTKOwnKey(data);
	}
	
	public int readLTKPeerKey(byte[] data) throws IOException {
		return this.callback_handler.readLTKPeerKey(data);
	}
	
	public boolean isBonded() {
		return callback_handler.isBonded();
	}
	
	public void stopAdvertising() throws IOException {
		if (this.callback_handler.isDeviceConnected()) throw new IOException("Device do not advertise while connected");
		if (is_initialized == false) throw new IOException("Device is not initialized yet");
		this.callback_handler.stopAdvertising();
	}
	
	public boolean sendNotification(short cccd_handle, final byte[] data, int size) throws IOException {
		if (!this.callback_handler.isDeviceConnected()) return false;
		if (!this.callback_handler.isNotificationAllowed(cccd_handle)) return false;
		// if (size < data.length) throw new IOException("sendNotification: Given length smaller than size of buffer");
		return callback_handler.setNotification(cccd_handle, data, size);
	}
	
	public boolean updateValue(short handle, final byte[] data, int size) throws IOException {
		if (!this.callback_handler.isDeviceConnected()) return false;
		// if (size < data.length) throw new IOException("updateValue: Given length smaller than size of buffer");
		return callback_handler.setUpdateValue(handle, data, size);
	}
	
	public final int getCharacteristicValue(short handle, byte[] data) throws IOException {
		return callback_handler.getCharacteristicValue(handle, data);
	}
	
	public void sendAdvIntervalTU(int value) throws IOException {
		// Default is 300 TU (187.5 ms)
		if (value < 0x20 || value > 0x4000) throw new IOException("Interval should be positive and between range 0x0020 and 0x4000 TU");
		out.write(ConnectionTypesPE.STP_ADV_INTERVAL);
		sendInt(out, value);	// In Time unit of 0.625 ms
	}
	
	public void deviceDisableRepairingAfterBonding() throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		this.disable_pairing_after_bonding = false;
		out.write(ConnectionTypesPE.STP_DISABLE);
		out.write(ConnectionTypesCommon.DISABLE_ALLOW_REPAIRING);
		verifySuccess(false);
		System.out.println("Disabling Re-Pairing Success");
	}
	
	public void bondConnect(boolean force_repairing) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		System.out.println("Bond on Connect - Unverified");
		out.write(ConnectionTypesPE.STP_BOND_ON_CONNECT);
		byte force_repairing_b = (byte) (force_repairing ? 1 : 0);
		out.write(force_repairing_b);
		// device does not return any errors
	}
	
	public short getPeerId() throws IOException
	{
		if (!this.callback_handler.isDeviceConnected()) throw new IOException("Device is not connected");
		return this.callback_handler.getPeerId();
	}
	
	public boolean deletePeerBond(short peer_id)
	{
		// you may delete bonds only when you are disconnected and no advertising/scan is on place - otherwise it will fail
		if (this.callback_handler.isDeviceConnected()) return false;
		this.callback_handler.deletePeerBond(peer_id);
		return true;
	}
	
	public void eraseBonds() throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		
		out.write(ConnectionTypesPE.STP_ERASE_BONDS);
		// device does not return any errors
	}
	
	public void sendBluetoothDeviceAddress(String address, ConnectionTypesCommon.BITAddressType address_type) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		if (address.length() != 17)
			throw new IOException("Malformed Hardware Address");
		
		out.write(ConnectionTypesPE.STP_SET_ADDRESS);
		
		// send address
		if (address_type == ConnectionTypesCommon.BITAddressType.PUBLIC)
			out.write(0);
		else
			out.write(1);
		
		out.write(hwaddr_to_byte_array(address));
		verifySuccess(false);
		peripheral_address = address;
		System.out.println("Set Address Successfully");
	}
	
	public void disableAdvertisingChannels(int channels) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		if ((channels & (ConnectionTypesPE.ADV_CH_37 | ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39)) == 0)
			throw new IOException("Unknown Advertising Channels Set");
		channels = channels & (ConnectionTypesPE.ADV_CH_37 | ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39);
		
		out.write(ConnectionTypesPE.STP_CHANNELS_DISABLE);
		out.write((byte)channels);
		verifySuccess(false);
	}
	
	public void configurePairing(ConnectionTypesCommon.PairingMethods p_method) throws IOException {
		configurePairing(p_method, null);
	}
	
	public void configurePairing(ConnectionTypesCommon.PairingMethods p_method, String static_pin) throws IOException
	{
		if (is_initialized) throw new IOException("Device already Initialized");
		byte sel_method = 0;
		if (p_method != ConnectionTypesCommon.PairingMethods.NO_IO && static_pin != null && static_pin.length() != 6) throw new IOException("PIN Length should be 6 digits");
		if (p_method == ConnectionTypesCommon.PairingMethods.NO_IO && static_pin != null) throw new IOException("Cannot set static key with NO/IO method");
		if (p_method == ConnectionTypesCommon.PairingMethods.DISPLAY && static_pin == null) throw new IOException("You should set a static key along with display-only pairing method");
		out.write(ConnectionTypesPE.STP_CON_PAIRING);
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
	
	public void setAppearanceValue(short value) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		out.write(ConnectionTypesPE.STP_DEV_APPEARANCE);
		sendShort(out, value);
		verifySuccess(false);
		
	}
	
	public void sendDeviceName(String devname) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		if(devname.length() > 31)
			throw new IOException("Device Name should be shorter");
		
		out.write(ConnectionTypesPE.STP_DEV_NAME);
		out.write((byte) devname.length());
		for(int i = 0; i < devname.length(); ++i)
			out.write(devname.charAt(i));
		
		verifySuccess(false);
		this.device_name = devname;
		
	}
	
	public void setFirmwareRevision(String value) throws IOException {
		if (value.length() == 0 || value.length() > 31)
			throw new IOException("Firmware revision value should be greater than 0 and less or equal to 31");
		
		out.write(ConnectionTypesPE.STP_DEVICE_INFO);
		out.write(ConnectionTypesPE.DEV_INFO_SUB_FW_INFO);
		out.write((byte)value.length());
		out.write(value.getBytes());
		verifySuccess(false);
	}
	
	public void setSoftwareRevision(String value) throws IOException {
		if (value.length() == 0 || value.length() > 31)
			throw new IOException("Software Revision value should be greater than 0 and less or equal to 31");
		
		out.write(ConnectionTypesPE.STP_DEVICE_INFO);
		out.write(ConnectionTypesPE.DEV_INFO_SUB_SW_INFO);
		out.write(value.length());
		out.write(value.getBytes());
		verifySuccess(false);
	}
	
	public void setHardwareRevision(String value) throws IOException {
		if (value.length() == 0 || value.length() > 31)
			throw new IOException("Hardware Revision value should be greater than 0 and less or equal to 31");
		
		out.write(ConnectionTypesPE.STP_DEVICE_INFO);
		out.write(ConnectionTypesPE.DEV_INFO_SUB_HW_INFO);
		out.write(value.length());
		out.write(value.getBytes());
		verifySuccess(false);
	}
	
	public void setModelName(String value) throws IOException {
		if (value.length() == 0 || value.length() > 31)
			throw new IOException("Model name value should be greater than 0 and less or equal to 31");
		
		out.write(ConnectionTypesPE.STP_DEVICE_INFO);
		out.write(ConnectionTypesPE.DEV_INFO_SUB_MODEL_INFO);
		out.write(value.length());
		out.write(value.getBytes());
		verifySuccess(false);
	}
	
	public void setManufacturerName(String value) throws IOException {
		if (value.length() == 0 || value.length() > 31)
			throw new IOException("Manufacturer name value should be greater than 0 and less or equal to 31");
		
		out.write(ConnectionTypesPE.STP_DEVICE_INFO);
		out.write(ConnectionTypesPE.DEV_INFO_SUB_MANUF_NAME_INFO);
		out.write(value.length());
		out.write(value.getBytes());
		verifySuccess(false);
	}
	
	public void sendConnectionParameters(PEConnectionParameters pe_connection_parameters) throws IOException {
		if (is_initialized) throw new IOException("Device already Initialized");
		
		
		int first_conn_params_update_delay = pe_connection_parameters.getFirstConnectionParametersUpdateDelay();
		int next_conn_params_update_delay = pe_connection_parameters.getNextConnectionParametersUpdateDelay();
		int max_conn_params_update_count = pe_connection_parameters.getMaxConnectionParametersUpdateCounter();
		int min_conn_interval_ms = pe_connection_parameters.getMinConnectionIntervalMS();
		int max_conn_interval_ms = pe_connection_parameters.getMaxConnectionIntervalMS();
		int slave_latency_evts = pe_connection_parameters.getSlaveLatencyMS();
		int con_sup_latency_ms = pe_connection_parameters.getConnectionSupervisionTimeoutMS();
		int advertisement_timeout = pe_connection_parameters.getAdvertisementTimeout();
		byte connection_type = pe_connection_parameters.getConnectionType();
		
		min_conn_interval_ms = (((min_conn_interval_ms) * 1000) / (1250));
		max_conn_interval_ms = (((max_conn_interval_ms) * 1000) / (1250));
		con_sup_latency_ms = con_sup_latency_ms / 10;
		
		
		if (connection_type == ConnectionTypesPE.BLE_GAP_ADV_TYPE_ADV_DIRECT_IND && direct_address == null)
			throw new IOException("Direct advertisement used, but direct address not set");
		
		if (!pe_connection_parameters.validate())
			throw new IOException("PE - Connection Sup Value error Connection Sup Value error ((con_sup_ms ["+String.valueOf(pe_connection_parameters.getConnectionSupervisionTimeoutMS())+"] ) > ((1 + slave_latency_evts ["+String.valueOf(pe_connection_parameters.getSlaveLatencyMS())+")] * max_conn_interval_ms ["+String.valueOf(pe_connection_parameters.getMaxConnectionIntervalMS())+"] * 2))");
		
		out.write(ConnectionTypesPE.STP_CON_PARAMS);
		sendShort(out, first_conn_params_update_delay);
		sendShort(out, next_conn_params_update_delay);
		sendShort(out, max_conn_params_update_count);
		sendShort(out, min_conn_interval_ms);
		sendShort(out, max_conn_interval_ms);
		sendShort(out, slave_latency_evts);
		sendShort(out, con_sup_latency_ms);
		sendInt(out, advertisement_timeout); // advertisement timeout in 0.625 ms units
		out.write(connection_type);
		if (connection_type == ConnectionTypesPE.BLE_GAP_ADV_TYPE_ADV_DIRECT_IND)
		{
			if (direct_address_type == ConnectionTypesCommon.BITAddressType.PUBLIC)
				out.write(0);
			else
				out.write(1);
			
			out.write(hwaddr_to_byte_array(direct_address));
		}
		System.out.println("Waiting for device verification");
		verifySuccess(false);
		System.out.println("Connection Parameters Set Successfully");
		this.advertisement_timeout = advertisement_timeout;
		this.connection_type = connection_type;
	}
	
	boolean hasService(String service_id) {
		byte service_id_val = (byte) Integer.valueOf(service_id).intValue();
		for(BLEService service : peripheral_services)
		{
			if (service.getServiceId() == service_id_val)
				return true;
		}
		return false;
	}
	
	BLEService getService(String service_id) {
		byte service_id_val = (byte)Integer.valueOf(service_id).intValue();
		for(BLEService service : peripheral_services)
		{
			if (service.getServiceId() == service_id_val)
				return service;
		}
		return null;
	}
	
	// PRIVATE METHODS
	
	private void addService(BLEService service) 
	{
		if (!peripheral_services.contains(service))
		{
			peripheral_services.add(service);
			if (service.getServiceId() == 0)
				service.setServiceId(String.valueOf(next_service_id.getAndIncrement()));
		}
	}
	
	protected BLECharacteristic getCharacteristicByCCCDHandle(short handle) {
		BLECharacteristic chr = null;
		
		for (BLEService service : peripheral_services)
		{
			chr = service.getCharacteristicByCCCDHandle(handle);
			if (chr != null) break;
		}
		return chr;
	}
	
	protected BLECharacteristic getCharacteristicByHandle(short handle) {
		BLECharacteristic chr = null;
		for (BLEService service : peripheral_services)
		{
			chr = service.getCharacteristicByHandle(handle);
			if (chr != null) break;
		}
		return chr;
	}
	
	protected BLECharacteristic getCharacteristicById(String characteristic_id) {
		BLECharacteristic chr = null;
		for (BLEService service : peripheral_services)
		{
			chr = service.getCharacteristicById(characteristic_id);
			if (chr != null) break;
		}
		return chr;
	}
	
	private void verifySuccess(boolean first_code_received) throws IOException {
		StringBuilder error_str = new StringBuilder();
		boolean generate_error = false;
		byte err_code;
		byte err_message_cnt = 0;
		if (first_code_received == false)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesPE.STP_ERROR)
				throw new IOException("Supposed to receive an error message");
		}
		err_message_cnt = (byte) in.read();
		while(err_message_cnt > 0)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesPE.ERR_SUCCESS)
			{
				generate_error = true;
				error_str.append("PE-Controller Error received ");
				error_str.append(String.valueOf(err_code));
				error_str.append("\n");
			}
			--err_message_cnt;
		}
		if (generate_error)
			throw new IOException(error_str.toString());
	}
	
	/** User Callback Installs **/
	
	public void installValueUpdateCallback(UpdateValueCallback callback)
	{
		this.callback_handler.installValueUpdateCallback(callback);
	}
	
	public void installNotificationDataCallback(PENotificationDataCallback callback)
	{
		this.callback_handler.installNotificationDataCallback(callback);
	}
	
	public void installReadCallback(PEReadCallback callback)
	{
		this.installReadCallback(callback);
	}
	
	public void installBondingCallback(BondingCallback callback)
	{
		this.installBondingCallback(callback);
	}
	
	public void installWriteCallback(PEWriteEventCallback callback)
	{
		this.installWriteCallback(callback);
	}
	
	public void installBondKeysCallback(BondKeysCallback callback)
	{
		this.installBondKeysCallback(callback);
	}
	
	public void installConnectionCallback(PEConnectionCallback callback)
	{
		this.installConnectionCallback(callback);
	}
	
	private short getShort() throws IOException {
		return (short)((in.read() + (in.read() << 8)) & 0xffff);
	}
	
	public void terminate()
	{
		try {
			
			if (evt_handler != null)
			{
				evt_handler.shutdown();
				try{handler_thread.join(1100);}catch(InterruptedException iex) {handler_thread.interrupt();}
			}
			
			out.write(ConnectionTypesPE.EVT_RESET);
			//try {Thread.sleep(1000);}catch(InterruptedException iex) {}
			
			if (!port.closePort())
				System.err.println("Failed to close PEController's serial port");
			
		}catch(IOException x) {System.err.println("PE.terminate(): " + x.getMessage());}
	}
	
	private void sendInt(OutputStream out, int value) throws IOException {
		out.write((byte) (value & 0xff));
		out.write((byte) ((value & 0xff00) >> 8));
		out.write((byte) ((value & 0xff0000) >> 16));
		out.write((byte) ((value & 0xff000000) >> 24));
	}
	
	private void sendShort(OutputStream out, int value) throws IOException {
		out.write((byte) (value & 0xff));
		out.write((byte) ((value & 0xff00) >> 8));
	}
	
	private short getShort(InputStream in) throws IOException {
		return (short)((in.read() + (in.read() << 8)) & 0xffff);
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

class myout extends OutputStream {

	@Override
	public void write(int arg0) throws IOException {
		System.out.print(String.format("%02x", arg0));		
	}
	
	public void write(byte []arg) {
		for(int i=0; i<arg.length;++i) System.out.print(String.format("%02x", arg[i]));
	}
	
	public void write(byte []arg, int offset, int len) {
		for(int i=offset; i<len;++i) System.out.print(String.format("%02x", arg[i]));
	}
	
}
