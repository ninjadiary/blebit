package cybervelia.sdk.controller.pe.handlers;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.AuthorizedData;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.PEReadCallback;

public class PEReadHandler {
	private byte []read_auth_data = new byte[31];
	private int read_auth_data_size;
	private volatile boolean is_read_auth_triggered = false;
	private volatile boolean is_read_auth_w_data_triggered = false;
	private boolean read_auth_approved_cmd = false;
	private PEBLEDeviceCallbackHandler parent_handler;
	private PEReadCallback user_callback;
	
	public PEReadHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(PEReadCallback callback)
	{
		this.user_callback = callback;
	}
	
	// read-auth
	public void setReadAuth(short handle, final byte[] data, int data_len) {
		BLECharacteristic characteristic = parent_handler.getCharacteristicByHandle(handle);
		AuthorizedData auth_data = new AuthorizedData();
		
		if (user_callback != null && characteristic != null)
		{
			boolean authroized = user_callback.authorizeRead(characteristic, data, data_len, auth_data);
			if (authroized)
			{
				if (auth_data.getAuthorizedDataLength() > 0)
					authorizeReadWithData(auth_data.getAuthorizedData(), auth_data.getAuthorizedDataLength());
				else
					authorizeRead(true);
			}
			else
				authorizeRead(false);
		}
		else
			authorizeRead(true);
	}
	
	public boolean getAuthorizationApproval() {
		return read_auth_approved_cmd;
	}
	
	public boolean isAuthorizeReadTriggered() {
		if (is_read_auth_triggered)
		{
			is_read_auth_triggered = false;
			return true;
		}
		else return false;
	}
	
	public boolean isAuthorizeReadWithDataTriggered() {
		if (is_read_auth_w_data_triggered)
		{
			is_read_auth_w_data_triggered = false;
			return true;
		}
		else return false;
	}
	
	public byte[] getReadAuthrorizationData() {
		return read_auth_data;
	}
	
	public int getReadAuthrorizationDataSize() {
		return read_auth_data_size;
	}
	
	private void authorizeRead(boolean authorization) {
		read_auth_approved_cmd = authorization;
		this.is_read_auth_triggered = true;
	}
	
	private void authorizeReadWithData(final byte[] data, int size) {
		read_auth_approved_cmd = true;
		this.is_read_auth_w_data_triggered = true;
		read_auth_data_size = size;
		System.arraycopy(data, 0, read_auth_data, 0, size);
		/*
		System.out.print("Reply Success to read-auth with updated-data: ");
		for(int i=0; i<size;++i)
			System.out.print(String.format("%02x ", data[i]));
		System.out.println();*/
	}
	
	// read-event- from device
	public void setRead(short handle, final byte[] data, int data_len) {
		BLECharacteristic characteristic = parent_handler.getCharacteristicByHandle(handle);
		if (characteristic == null)
			characteristic = parent_handler.getCharacteristicByCCCDHandle(handle);
		
		if (user_callback != null && characteristic != null)
		{
			user_callback.readEvent(characteristic, data, data_len);
		}
		
	}
	
	public void reset()
	{
		is_read_auth_w_data_triggered = false;
		is_read_auth_triggered = false;
	}
}
