package cybervelia.sdk.controller.ce;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.handlers.CEAdvertisementHandler;
import cybervelia.sdk.controller.ce.handlers.CEBondKeysHandler;
import cybervelia.sdk.controller.ce.handlers.CEBondingHandler;
import cybervelia.sdk.controller.ce.handlers.CEConnectionHandler;
import cybervelia.sdk.controller.ce.handlers.CEDiscoveryHandler;
import cybervelia.sdk.controller.ce.handlers.CENotificationEventHandler;
import cybervelia.sdk.controller.ce.handlers.CEReadRequestHandler;
import cybervelia.sdk.controller.ce.handlers.CEScanHandler;
import cybervelia.sdk.controller.ce.handlers.CEWriteRequestHandler;
import cybervelia.sdk.types.ConnectionTypesCE;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesPE;

public class CEEventHandler implements Runnable {
	private CEBLEDeviceCallbackHandler callbackHandler;
	private InputStream in;
	private OutputStream out;
	private byte temp_data[];
	private byte adv_data[];
	private byte adv_address[];
	private boolean request_failed;
	private byte[] own_key;
	private byte[] peer_key;
	
	private CEConnectionHandler con_handler;
	private CEBondingHandler bond_handler;
	private CEBondKeysHandler keys_handler; 
	private CEScanHandler scan_handler;
	private CEAdvertisementHandler adv_handler;
	private CEDiscoveryHandler discovery_handler;
	private CEReadRequestHandler read_handler;
	private CEWriteRequestHandler write_handler;
	private CENotificationEventHandler notification_handler;
	private volatile boolean shutdown_thread = false;
	
	public CEEventHandler(InputStream in, OutputStream out, CEBLEDeviceCallbackHandler handler)
	{
		this.callbackHandler = handler;
		this.in = in;
		this.out = out;
		this.temp_data = new byte[31];
		this.adv_data = new byte[31];
		this.adv_address = new byte[6];
		this.own_key = new byte[32];
		this.peer_key = new byte[32];
		
		con_handler = callbackHandler.getConnectionHandler();
		bond_handler = callbackHandler.getBondHandler();
		keys_handler = callbackHandler.getBondKeysHandler();
		scan_handler = callbackHandler.getScanHandler();
		adv_handler = callbackHandler.getAdvertisementHandler();
		discovery_handler = callbackHandler.getDiscoveryHandler();
		read_handler = callbackHandler.getReadRequestHandler();
		write_handler = callbackHandler.getWriteRequestHandler();
		notification_handler = callbackHandler.getNotificationHandler();
	}
	
	public void run() {
		boolean device_turn = true;
		byte evt;
		boolean send_pin = false;
		
		try {
			
			while(!shutdown_thread) 
			{
				if (device_turn)	// Device -> To -> User
				{
					evt = getByte();
					switch(evt) {
						case ConnectionTypesCE.EVT_CONN_TIMEOUT:
							System.err.println("EVT_Connection_Timeout");
							con_handler.connectionRequestFinished(true); // finished with error
							break;
							
						case ConnectionTypesCE.EVT_SCAN_TIMEOUT:
							scan_handler.scanStopped();
							break;
							
						case ConnectionTypesCE.EVT_CONNECTED:
							
							byte addr_type = getByte(); // read device address type
							readData(6);				// read device address
							short peer_id = getShort();  // read peer id
							
							callbackHandler.deviceConnectedEvent(temp_data, addr_type, peer_id);
							con_handler.connectionRequestFinished(false); 
							break;
							
						case ConnectionTypesCE.EVT_DISCONNECTED:
							System.err.println("Disconnect event received!");
							byte reason = getByte(); // read disconnection reason
							callbackHandler.deviceDisconnectedEvent(reason);
							break;
							
						case ConnectionTypesCE.EVT_ADV_INFO:
							int count = getByte();
							while(count > 0)
							{
								if (count % 5 == 0)
									out.write(1);
								readDataIn(adv_address, 6); // read address
								byte adr_t = getByte(); // Address Type
								byte sa = getByte(); // Scan or Adv ( 1 = scan, 0 = adv). On scan, ignore adv type
								byte at = getByte(); // Adv Type
								byte rssi = getByte(); // Rssi
								byte datalen = getByte(); // Data Len
								readDataIn(adv_data, datalen); // read data of adv packet
								callbackHandler.advertisementPacket(adv_address, adr_t, sa, at, rssi, datalen, adv_data);
								count = count - 1;
							}
							break;
						
						case ConnectionTypesCE.EVT_DISCOVERY_ITEM:
							int icount = getByte(); // items count
							
							while(icount > 0)
							{
								byte item_type = getByte(); // Get Type of Discovery (Service = 0, Characteristic = 1, Descriptor = 2)
								// Service
								if (item_type == 0)
								{
									byte service_id = getByte();
									short handle = getShort();
									boolean is128bit = getByte() == 1 ? true : false;
									readData((is128bit ? 16 : 2));	// read uuid
									BLEService srv = new BLEService(ConnectionTypesCommon.buildUUIDfromBytes(temp_data, is128bit), String.valueOf(service_id));
									srv.setHandle(handle);
									out.write(1);
									discovery_handler.addService(srv);
									
									//System.out.println("Service: " + srv.getUUID().toString());
								}
								// Characteristic
								else if (item_type == 1)
								{
									byte service_id = getByte();
									byte characteristic_id = getByte();
									byte properties = getByte();
									short handle_value = getShort();
									short handle_cccd = getShort();
									boolean is128bit = getByte() == 1 ? true : false;
									
									readData((is128bit ? 16 : 2));	// read uuid
									BLECharacteristic chr = new BLECharacteristic(ConnectionTypesCommon.buildUUIDfromBytes(temp_data, is128bit), String.valueOf(characteristic_id));
									chr.setValueHandle(handle_value);
									chr.setRawProperties(properties);
									if (chr.isCCCDEnabled())
										chr.setCCCDHandle(handle_cccd);
									out.write(1);
									discovery_handler.addCharacteristic(chr, String.valueOf(service_id));
									
									//System.out.println("Char: " + chr.getUUID().toString());
									
								}
								// Descriptor
								else if (item_type == 2)
								{
									byte characteristic_id = getByte();
									byte descriptor_id = getByte();
									short handle = getShort();
									short uuid = getShort();
									out.write(1);
									discovery_handler.addDescriptor(handle, String.valueOf(characteristic_id), String.valueOf(descriptor_id), uuid);
									
									//System.out.println("Descr: 0x" + String.format("%04x", uuid));
								}
								icount = icount -1;
							}
							break;
							
						case ConnectionTypesCE.EVT_DISCOVERY_DONE:
							boolean discovery_success = getByte() == 1 ? false : true;
							discovery_handler.discoveryDone(discovery_success);
							break;
							
						case ConnectionTypesCE.EVT_WRITE_RESP:
							int wrerror = getByte();
							short wrhandle = 0;
							if (wrerror == 0)
							{
								wrhandle = getShort();
							}
							
							write_handler.writeResponseReceived(wrerror, wrhandle);
							break;
						
						case ConnectionTypesCE.EVT_READ_RESP:
							int rrerror = getByte();
							short rrhandle = 0;
							int data_len = 0;
							if (rrerror == 0)
							{
								rrhandle = getShort();
								data_len = getByte();
								readData(data_len);
							}
							
							read_handler.readResponseReceived(rrerror == 0 ? temp_data : null, data_len, rrerror, rrhandle);
							break;
						
						case ConnectionTypesCE.EVT_NOTIFICATION:
							int notif_count = getByte(); // count
							while(notif_count > 0)
							{
								if (notif_count % 5 == 0)
									out.write(1);
								
								short nhandle = getShort();
								byte notif_data_len = getByte();
								readData(notif_data_len);
								callbackHandler.notificationReceived(temp_data, notif_data_len, nhandle);
								--notif_count;
							}
							break;
						
						case ConnectionTypesCE.EVT_PIN_REQUEST:
							send_pin = true;
							break;
						
						case ConnectionTypesCE.EVT_PIN_STATUS:
							byte pin_status = getByte();
							byte pin_peers_fault = getByte();
							
							bond_handler.pinStatus(pin_status, (pin_peers_fault <= 127 && pin_peers_fault >= 0 ? pin_peers_fault : (128 + (128 + pin_peers_fault)) ));
							break;
						
						case ConnectionTypesCE.EVT_LTK_UPDATE:
							byte ltklen_own = getByte();
							readDataIn(own_key, ltklen_own);
							byte ltklen_peer = getByte();
							readDataIn(peer_key, ltklen_peer);
							keys_handler.setLTKKey(own_key, ltklen_own, peer_key, ltklen_peer);
							break;
							
						case ConnectionTypesCE.EVT_BONDING_STATUS:
							byte status = getByte();
							if (status == 1) // succeed
							{
								byte procedure = getByte();
								callbackHandler.bondingSucceed(procedure);
							}
							else
							{
								short error = getShort();
								byte bond_error_src = getByte();
								callbackHandler.bondingFailed(error, bond_error_src);
							}
							break;
					}
					
					device_turn = false;
					continue;
				}
				else	// User -> To -> Device
				{
					
					if (con_handler.isTerminateTriggered())
					{
						out.write(ConnectionTypesCE.EVT_RESET);
						con_handler.terminated();
						return;
					}
					
					// Send pin
					if (send_pin)
					{
						sendPIN();
						send_pin = false;
						
						device_turn = true;
						continue;
					}
					
					// Request to connect
					if (con_handler.isConnectRequestTriggered())
					{
						request_failed = false;
						out.write(ConnectionTypesCE.EVT_CONNECT_REQ);
						sendData(con_handler.getConnectAddress(), 6);
						out.write(con_handler.getConnectAddressType());
						
						try{verifySuccess(false);}catch(IOException e) {request_failed = true;}
						
						// Scan Stopped by device (connect failed or not does not matter)
						scan_handler.scanStopped();
						
						if (request_failed)
							con_handler.connectionRequestFinished(request_failed); // if failed notify user immediately (because it wont get notified otherwise) - error = false, no error = true
						
						device_turn = true;
						continue;
					}
					
					// Request to cancel connect
					if (con_handler.isConnectCancelRequestTriggered())
					{
						out.write(ConnectionTypesCE.EVT_CONNECT_CANCEL);
						try{verifySuccess(false);}catch(IOException e) {/*Ignore failure*/}
						
						device_turn = true;
						continue;						
					}
					
					// Start Scan
					if (scan_handler.isScanStartTriggered())
					{
						out.write(ConnectionTypesCE.EVT_STARTSTOP_SCAN);
						out.write(1); // Start Scan
						try{verifySuccess(false);}catch(IOException e) {scan_handler.scanStopped();}
						
						device_turn = true;
						continue;		
					}
					
					// Stop Scan
					if (scan_handler.isScanStopTriggered())
					{
						out.write(ConnectionTypesCE.EVT_STARTSTOP_SCAN);
						out.write(0); // Stop Scan
						verifySuccess(false); // let it fail
						
						device_turn = true;
						continue;		
					}
					
					// User Disconnect
					if (con_handler.isDisconnectTriggered())
					{
						System.err.println("User wants to disconnect!");
						out.write(ConnectionTypesCE.EVT_DISCONNECT_NOW);
						out.write(con_handler.getUserDisconnectReason());
						
						device_turn = true;
						continue;	
					}
					
					// Start Discovery
					if (discovery_handler.isDiscoveryTriggered())
					{
						out.write(ConnectionTypesCE.EVT_START_DISCOVERY);
						try {verifySuccess(false);}catch(IOException ex) { System.err.println("Discovery Status Failed"); discovery_handler.discoveryDone(false);}
						
						device_turn = true;
						continue;						
					}
					
					// Send Write Request
					if (write_handler.isWriteRequestTriggered() && callbackHandler.isDeviceConnected())
					{	
						out.write(ConnectionTypesCE.EVT_WRITE_REQ);
						sendShort(write_handler.getWriteRequestHandle());
						byte write_data[] = write_handler.getWriteReqData();
						byte data_len = (byte)write_handler.getWriteReqDataLen();
						out.write(data_len);
						sendData(write_data, data_len);
						try {verifySuccess(false);}catch(IOException e) {write_handler.writeResponseReceived(2, (short)0);}
						
						device_turn = true;
						continue;
					}
					
					// Send Write CMD Request
					if (write_handler.isWriteRequestCMDTriggered() && callbackHandler.isDeviceConnected())
					{	
						out.write(ConnectionTypesCE.EVT_WRITECMD_REQ);
						sendShort(write_handler.getWriteRequestHandle());
						byte write_data[] = write_handler.getWriteReqData();
						byte data_len = (byte)write_handler.getWriteReqDataLen();
						out.write(data_len);
						sendData(write_data, data_len);
						
						device_turn = true;
						continue;
					}
					
					// Send Read Request
					if (read_handler.isReadRequestTriggered() && callbackHandler.isDeviceConnected())
					{
						out.write(ConnectionTypesCE.EVT_READ_REQ);
						sendShort(read_handler.getReadRequestHandle());
						try {verifySuccess(false);}catch(IOException e) {read_handler.readResponseReceived(null, 0, 2, (short)0);}
						
						device_turn = true;
						continue;
					}
					
					// Send Bond Now
					if (bond_handler.isBondNowTriggered())
					{
						byte force_repairing = (byte) (bond_handler.getForceRepairing() == true ? 1 : 0);
						out.write(ConnectionTypesCE.EVT_BOND_NOW);
						out.write(force_repairing);
						try {verifySuccess(false);}catch(IOException e) {bond_handler.pinStatus(0xff, -1);}
						
						device_turn = true;
						continue;
					}
					
					// Delete Peer Bond
					if(bond_handler.isDeletePeerBondTriggered())
					{
						out.write(ConnectionTypesCE.EVT_DELETE_PEER_BOND);
						sendShort(bond_handler.getDeleteBondPeerId());
						try {verifySuccess(false);}catch(IOException e) {bond_handler.deletePeerBondRiseError();}
						
						device_turn = true;
						continue;
					}
					
					// Send Empty Event
					out.write(ConnectionTypesCE.EVT_EMPTY_DEVICE);
					device_turn = true;
					continue;
				}
			}
			
		}catch(IOException ex) {
			System.err.println("CEEventHandler: " + ex.getMessage());
		}
		System.out.println("CE Event Handler shutting down..");
	}
	
	private final byte[] readData(int size) throws IOException {
		int max_size = (size > 31 ? 31 : size);
		int r = in.read(temp_data, 0, max_size);
		if (r != size) System.err.println("readData() -> Requested Size and returned size doesn not match");
		return temp_data;
	}
	
	private final void readDataIn(byte data[], int size) throws IOException {
		in.read(data, 0, size);
	}
	
	private void sendData(final byte[] data, int size) throws IOException {
		out.write(data, 0, size);
	}
	
	private byte getByte() throws IOException {
		return (byte) in.read();
	}
	
	private void sendPIN() throws IOException {
		if (callbackHandler.isDeviceConnected())
		{
			byte[] pin = bond_handler.replyWithPasskey();
			if (pin.length == 6)
			{
				out.write(ConnectionTypesCE.EVT_PIN_REPLY);
				out.write(pin,0,6);
				verifySuccess(false);
			}
		}
		else
			System.err.println("PIN should have length of 6-digits");
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
			{
				/*for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				    System.out.println("ErrCode: " + String.valueOf(err_code) + " - " + ste);
				}*/
				throw new IOException("Supposed to receive an error message. byte recv: 0x" + String.format("%02x", err_code));
			}
		}
		err_message_cnt = (byte) in.read();
		while(err_message_cnt > 0)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesCE.ERR_SUCCESS && err_code != ConnectionTypesCE.STP_ERROR)
			{
				generate_error = true;
				error_str.append("CE EH Error received ");
				error_str.append(String.valueOf(err_code));
				error_str.append("\n");
			}
			else if (err_code == ConnectionTypesCE.STP_ERROR)
			{
				generate_error = true;
				error_str.append("CE EH Error\n");
			}
			--err_message_cnt;
		}
		if (generate_error)
			throw new IOException(error_str.toString());
	}
	
	private short getShort() throws IOException {
		return (short)((in.read() + (in.read() << 8)) & 0xffff);
	}
	
	private void sendShort(int value) throws IOException {
		out.write((byte) (value & 0xff));
		out.write((byte) ((value & 0xff00) >> 8));
	}
	
	protected void shutdown() {
		shutdown_thread = true;
	}
}
