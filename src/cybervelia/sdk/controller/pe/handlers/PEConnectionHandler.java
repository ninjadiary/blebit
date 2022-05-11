package cybervelia.sdk.controller.pe.handlers;

import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.PEConnectionCallback;
import cybervelia.sdk.types.ConnectionTypesCE;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class PEConnectionHandler {
	private volatile boolean disconnect_triggered = false;
	private volatile boolean is_device_connected = false;
	private String connected_address = null;
	private ConnectionTypesCommon.AddressType connected_address_type = null;
	private int disconnect_reason = 0;
	private PEBLEDeviceCallbackHandler parent_handler;
	private PEConnectionCallback user_callback = null;
	private boolean is_terminate_triggered = false;
	private Object block_until_terminated = new Object();
	
	public PEConnectionHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(PEConnectionCallback callback)
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
	
	public int getDisconnectionReason() {
		return this.disconnect_reason;
	}
	

	public void disconnect() {
		disconnect_triggered = true;
	}
	
	public boolean isDisconnectTriggered() {
		if (disconnect_triggered)
		{
			disconnect_triggered = false;
			parent_handler.untriggerAll();
			reset();
			return true;
		}
		else return false;
	}
	
	public String getClientAddress() {
		return this.connected_address;
	}
	
	public ConnectionTypesCommon.AddressType getClientAddressType(){
		return this.connected_address_type;
	}
	
	public boolean isDisconnectTriggeredNC()
	{
		return this.disconnect_triggered;
	}
	
	public boolean isDeviceConnected() {
		return this.is_device_connected; // this should be only affected by the device and not the user
	}
	
	public void setDeviceConnected(byte address_type_code, final byte[] address, short peer_id) {		
		this.is_device_connected = true;
		this.connected_address = ConnectionTypesCommon.addressToStringFormat(address);
		this.connected_address_type = ConnectionTypesCommon.getAddressTypeFromCode(address_type_code);
		parent_handler.setBondPeerId(peer_id);
		if (user_callback != null)
			user_callback.connected(connected_address_type, connected_address);
	}
	
	// Disconnect Event received
	public void setDeviceDisconnected(int reason) {
		if (user_callback != null)
			user_callback.disconnected(disconnect_reason);
		this.disconnect_reason = reason;
		
		this.is_device_connected = false;
		parent_handler.untriggerAll();
		parent_handler.clearBondedFlag();
	}
	
	public void reset()
	{
		this.disconnect_reason = 0;	
	}
}
