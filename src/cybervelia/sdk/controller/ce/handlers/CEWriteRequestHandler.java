package cybervelia.sdk.controller.ce.handlers;

import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEWriteCallback;

public class CEWriteRequestHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEWriteCallback user_callback = null;
	private CEBondingHandler bond_handler;
	// Write Req Data
	private byte[] write_req_data = new byte[31];
	private int write_req_data_len = 0;
	private int write_req_error = 2;
	private short write_request_handle = 0;
	private Object block_until_write_response = new Object();
	private boolean write_req_pending_auth = false;
	private volatile boolean write_request_triggered = false;
	// Write CMD Req
	private volatile boolean write_request_triggered_cmd = false;
	private byte[] write_req_data_cmd = new byte[31];
	private int write_req_data_len_cmd = 0;
	private short write_request_handle_cmd = 0;
	
	public CEWriteRequestHandler(CEBLEDeviceCallbackHandler parent_handler, CEBondingHandler bond_handler)
	{
		this.parent_handler = parent_handler;
		this.bond_handler = bond_handler;
	}
	
	public void setCallback(CEWriteCallback callback)
	{
		this.user_callback = callback;
	}
	
	// Write CMD Request
	public void writeDataCMD(byte[] data, int offset, int len, short handle)
	{
		if (parent_handler.isDeviceConnected() == false) return;
		System.arraycopy(data, offset, this.write_req_data_cmd, 0, len);
		write_req_data_len_cmd = len;
		write_request_triggered_cmd = true;
		write_request_handle_cmd = handle;
	}
	
	public boolean isWriteRequestCMDTriggered()
	{
		if(write_request_triggered_cmd)
		{
			write_request_triggered_cmd = false;
			return true;
		}
		else
			return false;
	}
	
	public final byte[] getWriteReqDataCMD() {
		return write_req_data_cmd;
	}
	
	public int getWriteReqDataLenCMD() {
		return write_req_data_len_cmd;
	}
	
	public short getWriteRequestHandleCMD() {
		return write_request_handle_cmd;
	}
	
	// Write Request
	public boolean writeData(byte[] data, int offset, int len, short handle, int tm_ms)
	{
		if (parent_handler.isDeviceConnected() == false) return false;
		System.arraycopy(data, offset, this.write_req_data, 0, len);
		write_req_data_len = len;
		write_request_triggered = true;
		write_request_handle = handle;
		write_req_error = 2;
		// Block
		try {
			synchronized(block_until_write_response) 
			{
				block_until_write_response.wait(tm_ms);
			}
		}catch(InterruptedException e) {}
		
		if (write_req_pending_auth)
		{
			write_req_pending_auth = false;
			if (parent_handler.isBonded())
				return writeData(data, offset, len, handle, tm_ms);
			else
				return true;
		}
		
		return write_req_error == 0 ? true : false; // return success on error=0
	}
	
	public boolean isWriteRequestTriggered()
	{
		if(write_request_triggered)
		{
			write_request_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	public final byte[] getWriteReqData() {
		return write_req_data;
	}
	
	public int getWriteReqDataLen() {
		return write_req_data_len;
	}
	
	public short getWriteRequestHandle() {
		return write_request_handle;
	}
	
	public void bondSucceed()
	{
		if (write_req_pending_auth == true)
			synchronized(block_until_write_response) {block_until_write_response.notify();}
	}
	
	public void bondFailed()
	{
		if (write_req_pending_auth == true)
			synchronized(block_until_write_response) {block_until_write_response.notify();}	
	}
	
	public void writeResponseReceived(int error, short handle){
		this.write_req_error = error;
		
		if (error == 1)
		{
			synchronized(bond_handler.block_until_bond)
			{
				bond_handler.setForceRepairing();
				write_req_pending_auth = true;
				bond_handler.triggerBondNowCN();
			}
		}
		else
			synchronized(block_until_write_response) {block_until_write_response.notify();}
		
		if((error == 0 || error > 1) && user_callback != null)
			user_callback.writeResponse(handle);
	}
	
	public void reset()
	{
		this.write_req_error = 2;
		this.write_request_triggered = false;
		this.write_req_pending_auth = false;
		this.write_request_triggered_cmd = false;
		synchronized(block_until_write_response) {block_until_write_response.notify();}
	}
	
}
