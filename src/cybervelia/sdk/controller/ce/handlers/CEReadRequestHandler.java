package cybervelia.sdk.controller.ce.handlers;

import java.io.IOException;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEReadCallback;

public class CEReadRequestHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEReadCallback user_callback = null;
	private CEBondingHandler bond_handler;
	// Read Req Data
	private byte[] read_req_data = new byte[31];
	private int read_req_data_len = 0;
	private int read_req_error = 0;
	private short read_request_handle = 0;
	private Object block_until_read_response = new Object();
	private boolean read_req_pending_auth = false;
	private volatile boolean read_request_triggered = false;
	
	public CEReadRequestHandler(CEBLEDeviceCallbackHandler parent_handler, CEBondingHandler bond_handler)
	{
		this.parent_handler = parent_handler;
		this.bond_handler = bond_handler;
	}
	
	public void setCallback(CEReadCallback callback)
	{
		this.user_callback = callback;
	}
	
	
	// Read Request
	public int readData(byte[] data, int offset, int len, short handle, int tm_ms) throws IOException
	{
		int transfer_bytes = 0;
		if (parent_handler.isDeviceConnected() == false) return 0;
		if (data.length < len) throw new IOException("Data length less than expected");
		if (data.length - offset < len) throw new IOException("Data offset is mis-calcualted");
		if (len == 0) return 0;
		
		read_request_triggered = true;
		read_request_handle = handle;
		read_req_error = 2;
		
		// Block
		try {
			synchronized(block_until_read_response) 
			{
				block_until_read_response.wait(tm_ms);
			}
		}catch(InterruptedException e) {}
		
		
		if (read_req_pending_auth)
		{
			read_req_pending_auth = false;
			if (parent_handler.isBonded())
				return readData(data, offset, len, handle, tm_ms);
			else
				return 0;
		}
		
		if (read_req_error == 0)
		{
			transfer_bytes = read_req_data_len > len ? len : read_req_data_len;
			System.arraycopy(this.read_req_data, 0, data, offset, transfer_bytes);
		}
		
		if (len < read_req_data_len) throw new IOException("Length too small for data");
		return (read_req_error != 0 ? 0 : transfer_bytes);
	}
	
	
	public boolean isReadRequestTriggered()
	{
		if(read_request_triggered)
		{
			read_request_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	public short getReadRequestHandle() 
	{
		return read_request_handle;
	}
	
	public void readResponseReceived(byte []data, int data_len, int error, short handle)
	{
		BLECharacteristic characteristic = null;
		if (parent_handler.isDiscoveryInProgress()) return; // this is called during discovery because the device issue some attribute reads. That's why we should ignore such reads responses
		this.read_req_error = error;
		
		if (this.read_req_error == 0)
		{
			System.arraycopy(data, 0, this.read_req_data, 0, data_len);
			read_req_data_len = data_len;
		}
		
		if (this.read_req_error == 1)
		{
			synchronized(bond_handler.block_until_bond)
			{
				bond_handler.clearForceRepairing();
				read_req_pending_auth = true;
				bond_handler.triggerBondNowCN();
			}
		}
		else
			synchronized(block_until_read_response) {block_until_read_response.notify();}
		
		if (error == 0)
			characteristic = parent_handler.getCharacteristicByHandle(handle);
		
		if (error == 0 && characteristic != null && user_callback != null)
			user_callback.readResponse(characteristic, this.read_req_data, data_len, false);
		else if (error == 0 && characteristic == null && user_callback != null)
			user_callback.readResponseRaw(handle, this.read_req_data, data_len, false);
		else if (error != 0 && characteristic != null && user_callback != null)
			user_callback.readResponse(characteristic, this.read_req_data, data_len, true);
		else if (error != 0 && characteristic == null && user_callback != null)
			user_callback.readResponseRaw(handle, this.read_req_data, data_len, true);
	}
	
	public void onBondFailed()
	{
		if (read_req_pending_auth == true)
			synchronized(block_until_read_response) {block_until_read_response.notify();}
	}
	
	public void onBondSucceed()
	{
		if (read_req_pending_auth == true)
			synchronized(block_until_read_response) {block_until_read_response.notify();}
	}
	
	public void reset()
	{
		read_req_error = 2;
		this.read_request_triggered = false;
		this.read_req_pending_auth = false;
		synchronized(block_until_read_response) {block_until_read_response.notify();}
	}
	
}
