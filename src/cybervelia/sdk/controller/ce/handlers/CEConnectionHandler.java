package cybervelia.sdk.controller.ce.handlers;

import java.util.ArrayList;

import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEConnectionCallback;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class CEConnectionHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEConnectionCallback user_callback = null;
	
	// Triggers
	private volatile boolean connect_request_triggered = false;
	private volatile boolean is_device_connected = false;
	private volatile boolean connect_cancel_request_triggered = false;
	private volatile boolean disconnect_triggered = false;
	private boolean is_terminate_triggered = false;
	private Object block_until_terminated = new Object();
	
	// Trigger Data
	private byte []connect_addr = null; // Address Requested from user to connect (object is coming from the user)
	private ConnectionTypesCommon.AddressType addr_type;
	private boolean request_failed = false;
	private Object block_until_connection_establish_or_fail = new Object();
	private boolean connect_request_in_progress = false;
	private byte user_disconnect_reason = 0; // Disconnection issued by user - reason set by user
	
	private int disconnection_reason = 0; // Dis-connection reason comes from the disconnect event
	private String connected_address = new String(); // Address comes from event when target is connected
	ConnectionTypesCommon.AddressType connected_address_type;
	
	public CEConnectionHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(CEConnectionCallback callback)
	{
		this.user_callback = callback;
	}
	
	
	
	public void terminate()
	{
		is_terminate_triggered = true;
		
		// Block
		try {
			synchronized(block_until_terminated) 
			{
				block_until_terminated.wait();
			}
		}catch(InterruptedException e) {}
	}
	
	public boolean isTerminateTriggered() {
		if (is_terminate_triggered)
		{
			is_terminate_triggered = false;
			return true;
		}
		else return false;
	}
	
	public void terminated()
	{
		synchronized(block_until_terminated) {block_until_terminated.notify();}
	}
	
	
	// Connect Request
	public boolean connectRequest(byte []addr, ConnectionTypesCommon.AddressType type, boolean block)
	{
		if (connect_request_in_progress) return false; // return error
		
		this.connect_request_in_progress = true;
		this.connect_addr = addr;
		this.addr_type = type;
		connect_request_triggered = true;
		this.request_failed = true;
		if (block)
		{
			// Block
			try {
				synchronized(block_until_connection_establish_or_fail) 
				{
					block_until_connection_establish_or_fail.wait();
				}
			}catch(InterruptedException e) {}
			return !this.request_failed;
		}else 
			return true;
	}
	
	public void connectionRequestFinished(boolean error) // Unlocked when Connected, Timeout, or cancel request is sent
	{
		if (error)
			System.err.println("Connection Request Finished with Failure");
		this.connect_request_in_progress = false;
		this.request_failed = error;
		synchronized(block_until_connection_establish_or_fail) {block_until_connection_establish_or_fail.notify();}
	}
	
	public boolean isConnectRequestTriggered() {
		if(connect_request_triggered)
		{
			connect_request_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	public boolean isConnectRequestInProgress()
	{
		return this.connect_request_in_progress;
	}
	
	public byte[] getConnectAddress() {
		return connect_addr;
	}
	
	public byte getConnectAddressType() {
		return ConnectionTypesCommon.getCodeFromAddressType(addr_type);
	}
	
	
	// Cancel Connect Request
	public boolean cancelConnectRequest() {
		if (is_device_connected) return false;
		else {
			connect_cancel_request_triggered = true;
			synchronized(block_until_connection_establish_or_fail) {block_until_connection_establish_or_fail.notify();}
			return true;
		}
	}
	
	public boolean isConnectCancelRequestTriggered() {
		if(connect_cancel_request_triggered)
		{
			connect_cancel_request_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	
	// connected event
	public void deviceConnectedEvent(byte address[], byte addr_type, short peer_id) {
		connected_address = new String();
		connected_address_type = ConnectionTypesCommon.getAddressTypeFromCode(addr_type);
		for(int i = 0; i<6; ++i)
			connected_address = connected_address+String.format("%02x", address[i]) + (i < 5 ? ":" : "");
		this.is_device_connected = true;
		System.out.println("Device Connected - Client Address: " + connected_address + " " + (connected_address_type == ConnectionTypesCommon.AddressType.PUBLIC_ADDR ? "PUBLIC" : "PRIVATE"));
		if(user_callback != null)
			user_callback.connected(connected_address_type, connected_address);
	}
	
	
	// target disconnected event
	// triggered from event handler
	public void deviceDisconnectedEvent(int reason) {
		this.is_device_connected = false;
		this.disconnection_reason = reason;
		this.connected_address = "";
		if(user_callback != null)
			user_callback.disconnected(reason);
	}
	
	public int getDisconnectionReason() {
		return this.disconnection_reason;
	}
	
	// User Disconnect
	public void disconnect(int reason) {
		if (is_device_connected)
		{
			this.disconnect_triggered = true;
			this.user_disconnect_reason = (byte) reason;
		}
	}
	
	public boolean isDisconnectTriggered() {
		if(disconnect_triggered)
		{
			disconnect_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	public byte getUserDisconnectReason() {
		return this.user_disconnect_reason;
	}
	
	public boolean isDeviceConnected() {
		return this.is_device_connected;
	}
	
	public String getClientAddress() {
		return connected_address;
	}
	
	public ConnectionTypesCommon.AddressType getClientAddressType(){
		return connected_address_type;
	}
	
	
}
