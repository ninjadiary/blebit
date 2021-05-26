package cybervelia.sdk.controller.pe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cybervelia.sdk.types.ConnectionTypesPE;
import cybervelia.sdk.controller.pe.handlers.PEAdvertisingHandler;
import cybervelia.sdk.controller.pe.handlers.PEBondKeysHandler;
import cybervelia.sdk.controller.pe.handlers.PEBondingHandler;
import cybervelia.sdk.controller.pe.handlers.PEConnectionHandler;
import cybervelia.sdk.controller.pe.handlers.PENotificationDataHandler;
import cybervelia.sdk.controller.pe.handlers.PEReadHandler;
import cybervelia.sdk.controller.pe.handlers.PEUpdateValueHandler;
import cybervelia.sdk.controller.pe.handlers.PEWriteEventHandler;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class PEEventHandler implements Runnable {
	
	private InputStream in;
	private OutputStream out;
	private PEBLEDeviceCallbackHandler callbackHandler = null;
	private byte temp_data[];
	private byte[] own_key;
	private byte[] peer_key;
	byte address[];
	
	private PEUpdateValueHandler update_value_handler;
	private PENotificationDataHandler notification_data_handler;
	private PEReadHandler read_handler;
	private PEAdvertisingHandler advertising_handler;
	private PEBondingHandler bonding_handler;
	private PEWriteEventHandler write_handler;
	private PEBondKeysHandler keys_handler;
	private PEConnectionHandler con_handler;
	private volatile boolean shutdown_thread = false;
	public PEEventHandler(InputStream in, OutputStream out, PEBLEDeviceCallbackHandler handler) {
		this.in = in;
		this.out = out;
		this.callbackHandler = handler;
		this.temp_data = new byte[31];
		this.own_key = new byte[32];
		this.peer_key = new byte[32];
		this.address = new byte[6];
		
		update_value_handler = callbackHandler.getUpdateValueHandler();
		notification_data_handler = callbackHandler.getNotificationDataHandler();
		read_handler = callbackHandler.getReadHandler();
		advertising_handler = callbackHandler.getAdvertisingHandler();
		bonding_handler = callbackHandler.getBondingHandler();
		write_handler = callbackHandler.getWriteEventHandler();
		keys_handler = callbackHandler.getBondKeyHandler();
		con_handler = callbackHandler.getConnectionHandler();
	}
	
	@Override
	public void run() {
		boolean device_turn = true;
		boolean send_pin = false;
		int char_value_length = 0;
		short char_handle = 0;
		byte evt;
		byte address_type = 0;
		int size = 0;
		byte characteristic_id = 0;
		
		try {
			while(!shutdown_thread) {
				if (device_turn){	// Device -> To -> User
					evt = getByte();
					switch(evt) {
						
					case ConnectionTypesPE.EVT_PIN_REQUEST:
							System.out.println("PE-Request to enter pin");
							send_pin = true;
							break;
							
						case ConnectionTypesPE.EVT_CLIENT_CONNECTED:
							address_type = getByte();
							readDataIn(address, 6);
							short peer_id = getShort();
							this.con_handler.setDeviceConnected(address_type, address, peer_id);
							break;
							
						case ConnectionTypesPE.EVT_CLIENT_DISCONNECTED:
							// Abort blocking operations - This need to happen before calling callhandler's disconnect method
							if(update_value_handler.isUpdateValueTriggered()) 
								update_value_handler.setUpdateValueDataSent(false);
							
							if(notification_data_handler.isNotificationTriggered())
								notification_data_handler.setNotificationDataSent(false);
							
							this.con_handler.setDeviceDisconnected(in.read());
							break;
							
						case ConnectionTypesPE.EVT_NOTIFICATION_ENABLED:
							this.notification_data_handler.setNotificationEnabled();
							break;
							
						case ConnectionTypesPE.EVT_NOTIFICATION_DISABLED:
							this.notification_data_handler.setNotificationDisabled();
							break;
							
						case ConnectionTypesPE.EVT_INDICATION_ENABLED:
							this.notification_data_handler.setIndicationEnabled();
							break;
							
						case ConnectionTypesPE.EVT_INDICATION_DISABLED:
							this.notification_data_handler.setIndicationDisabled();
							break;
							
						case ConnectionTypesPE.EVT_REPAIRING:
							this.bonding_handler.setRepairingHappened();
							break;
							
						case ConnectionTypesPE.EVT_WRITE:
						{
							char_handle = getShort();
							char_value_length = getByte();
							byte [] write_data = readData((byte)char_value_length);
							boolean is_cmd = (getByte() == 1 ? true : false);
							
							this.write_handler.setWrite(char_handle, write_data, char_value_length, is_cmd); // Receive Value Attribute or CCCD Attribute Handle
							
							byte continuous_write = getByte();
							if (continuous_write == 1)
							{
								char_handle = getShort();
								char_value_length = getByte();
								write_data = readData((byte)char_value_length);
								is_cmd = (getByte() == 1 ? true : false);
								
								this.write_handler.setWrite(char_handle, write_data, char_value_length, is_cmd); // Receive Value Attribute or CCCD Attribute Handle								
							}
						}	
							break;
							
						case ConnectionTypesPE.EVT_READ:
							char_handle = getShort();
							char_value_length = getByte();
							this.read_handler.setRead(char_handle, readData((byte)char_value_length), char_value_length);
							break;
							
						case ConnectionTypesPE.EVT_READ_AUTH_REQ:
							char_handle = getShort();
							size = getShort();		// data length
							this.read_handler.setReadAuth(char_handle, readData((byte)size), size);
							break;
							
						case ConnectionTypesPE.EVT_ENC_LTK_UPDATE:
							
							byte ownkey_len = getByte();
							readDataIn(own_key, ownkey_len);
							
							byte peerkey_len = getByte();
							readDataIn(peer_key, peerkey_len);
							
							this.keys_handler.setLTKKey(own_key, ownkey_len, peer_key, peerkey_len);
							break;
							
						case ConnectionTypesPE.EVT_PIN_STATUS:
							byte pin_status = getByte();
							byte pin_peers_fault = getByte();
							
							bonding_handler.authStatus(pin_status, (pin_peers_fault <= 127 && pin_peers_fault >= 0 ? pin_peers_fault : (128 + (128 + pin_peers_fault)) ));
							break;
							
						case ConnectionTypesPE.EVT_BONDING_STATUS:
							/*
							 Errors:
							 	PM_CONN_SEC_ERROR_PIN_OR_KEY_MISSING = 0x1006 - Encryption failed because the peripheral has lost the LTK for this bond. See also BLE_HCI_STATUS_CODE_PIN_OR_KEY_MISSING and Table 3.7 ("Pairing Failed Reason Codes") in the Bluetooth Core Specification 4.2, section 3.H.3.5.5
							 	PM_CONN_SEC_ERROR_MIC_FAILURE   = 0x103D - Encryption ended with disconnection because of mismatching keys or a stray packet during a procedure. See the SoftDevice GAP Message Sequence Charts on encryption (GAP Message Sequence Charts), the Bluetooth Core Specification 4.2, sections 6.B.5.1.3.1 and 3.H.3.5.5
							 	PM_CONN_SEC_ERROR_DISCONNECT  = 0x1100 - 	Pairing or encryption did not finish before the link disconnected for an unrelated reason. 
							 	PM_CONN_SEC_ERROR_SMP_TIMEOUT = 0x1101 -  	Pairing/bonding could not start because an SMP time-out has already happened on this link. This means that no more pairing or bonding can happen on this link. To be able to pair or bond, the link must be disconnected and then reconnected. See Bluetooth Core Specification 4.2 section 3.H.3.4
							 * */
							byte status = getByte();
							if (status == 1) // succeed
							{
								byte procedure = getByte();
								bonding_handler.bondingSucceed(procedure);
							}
							else
							{
								short error = getShort();
								byte bond_error_src = getByte();
								bonding_handler.bondingFailed(error, bond_error_src);
							}
							break;
							
						case ConnectionTypesPE.EVT_EMPTY_DEVICE:
							break;
					}
					device_turn = false;
					continue;
					
				}
				else	// User -> To -> Device
				{
					if (con_handler.isTerminateTriggered())
					{
						out.write(ConnectionTypesPE.EVT_RESET);
						con_handler.terminated();
						return;
					}
					
					if (send_pin)
					{
						sendPIN();
						send_pin = false;
						
						device_turn = true;
						continue;
					}
					
					if (con_handler.isDisconnectTriggered())
					{
						out.write(ConnectionTypesPE.EVT_DISCONNECT);
						verifySuccess(false);
						
						device_turn = true;
						continue;
					}
					
					// Send Bond Now
					if (bonding_handler.isBondNowTriggered())
					{
						byte force_repairing = (byte) (bonding_handler.getForceRepairing() == true ? 1 : 0);
						out.write(ConnectionTypesPE.EVT_BOND);
						out.write(force_repairing);
						try {verifySuccess(false);}catch(IOException e) {bonding_handler.authStatus(0xff, -1);}
						
						device_turn = true;
						continue;
					}
					
					// Delete Peer Bond
					if(bonding_handler.isDeletePeerBondTriggered())
					{
						boolean error_generated = false;
						out.write(ConnectionTypesPE.EVT_DELETE_PEER_BOND);
						sendShort(bonding_handler.getDeleteBondPeerId());
						try {verifySuccess(false);}catch(IOException e) {bonding_handler.deletePeerBondRiseError(); error_generated = true;}
						if (!error_generated) bonding_handler.deletePeerBondSuccess();
						
						device_turn = true;
						continue;
					}
					
					
					if (read_handler.isAuthorizeReadTriggered())
					{
						if (con_handler.isDeviceConnected())
						{
							out.write(ConnectionTypesPE.EVT_READ_AUTHORIZE);
							if (read_handler.getAuthorizationApproval())
								out.write(1);
							else
								out.write(0);
						}
						
						device_turn = true;
						continue;
					}
					
					if(advertising_handler.isAdvertisingStopTriggered())
					{
						out.write(ConnectionTypesPE.EVT_STOP_ADV);
						
						device_turn = true;
						continue;
					}
					
					if(advertising_handler.isAdvertisingStartTriggered())
					{
						out.write(ConnectionTypesPE.EVT_START_ADV);
						verifySuccess(false);
						
						device_turn = true;
						continue;
					}
					
					if(read_handler.isAuthorizeReadWithDataTriggered())
					{
						if (con_handler.isDeviceConnected())
						{
							out.write(ConnectionTypesPE.EVT_READ_AUTHORIZE_W_DATA);
							out.write(1); // Authorised read
							//System.out.println("Sending Data of size: " + read_handler.getReadAuthrorizationDataSize());
							out.write(read_handler.getReadAuthrorizationDataSize());
							sendData(read_handler.getReadAuthrorizationData(), read_handler.getReadAuthrorizationDataSize());
							verifySuccess(false);
						}
						
						device_turn = true;
						continue;
					}
					
					if(notification_data_handler.isNotificationTriggered())
					{
						byte []data = notification_data_handler.getNotificationData();
						boolean notification_error_generated = false;
						characteristic_id = notification_data_handler.getNotificationCharacteristicId();
						size = notification_data_handler.getNotificationDataSize();
						
						if (con_handler.isDeviceConnected())
						{
							out.write(ConnectionTypesPE.EVT_NOTIFICATION_SEND);
							out.write(characteristic_id);
							out.write((byte) size);
							sendData(data, size);
							try {
								verifySuccess(false);
							}catch(IOException e) {notification_error_generated = true;}
							notification_data_handler.setNotificationDataSent(notification_error_generated);
						}
						
						device_turn = true;
						continue;
					}
					
					if(update_value_handler.isUpdateValueTriggered())
					{
						byte []data = update_value_handler.getUpdateValueData();
						boolean update_value_error_generated = false;
						characteristic_id = update_value_handler.getUpdateValueCharacteristicId();
						size = update_value_handler.getUpdateValueDataSize();
						
						if(con_handler.isDeviceConnected())
						{
							out.write(ConnectionTypesPE.EVT_VALUE_UPDATE);
							out.write(characteristic_id);
							out.write((byte) size);
							sendData(data, size);
							try {
								verifySuccess(false);
							}catch(IOException e) {System.err.println(e.getMessage()); update_value_error_generated = true;}
							update_value_handler.setUpdateValueDataSent(update_value_error_generated);
						}
						
						device_turn = true;
						continue;						
					}
					
					out.write(ConnectionTypesPE.EVT_EMPTY_USER);
					device_turn = true;
					continue;
				}
			}
		}catch(IOException ex) {
			System.err.println("PEEventHandler: " + ex.getMessage());
		}
		System.out.println("PE Event Handler shutting down..");
	}
	
	private final void readDataIn(byte data[], int size) throws IOException {
		in.read(data, 0, size);
	}
	
	private final byte[] readData(byte size) throws IOException {
		int max_size = (size > 31 ? 31 : size);
		in.read(temp_data, 0, max_size);
		if (max_size < size) 
			for(int i = 0; i< size-max_size; ++i) 
				in.read();
		return temp_data;
	}
	
	private void sendData(final byte[] data, int size) throws IOException {
		out.write(data, 0, size);
	}
	
	private byte getByte() throws IOException {
		return (byte) in.read();
	}
	
	private void sendPIN() throws IOException {
		if (con_handler.isDeviceConnected())
		{
			byte[] pin = bonding_handler.replyWithPasskey();
			if (pin.length == 6)
			{
				out.write(ConnectionTypesPE.EVT_PIN_REPLY);
				out.write(pin);
				verifySuccess(false);
			}
			else 
				System.err.println("PIN should have length of 6-digits");
		}
	}
	
	private void verifySuccess(boolean first_code_received) throws IOException {
		StringBuilder error_str = new StringBuilder();
		boolean generate_error = false;
		byte err_code;
		byte err_message_cnt = 0;
		if (first_code_received == false)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesPE.STP_ERROR && err_code != ConnectionTypesPE.EVT_ERROR)
				throw new IOException("Supposed to receive an error message.");
		}
		err_message_cnt = (byte) in.read();
		while(err_message_cnt > 0)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesPE.ERR_SUCCESS)
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
