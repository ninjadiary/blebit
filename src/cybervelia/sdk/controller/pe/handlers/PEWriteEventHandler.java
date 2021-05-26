package cybervelia.sdk.controller.pe.handlers;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.PEWriteEventCallback;

public class PEWriteEventHandler {
	
	private PEBLEDeviceCallbackHandler parent_handler;
	private PEWriteEventCallback user_callback;
	
	public PEWriteEventHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(PEWriteEventCallback callback)
	{
		this.user_callback = callback;
	}
	
	// write-event from device
	public void setWrite(short handle, final byte[] data, int data_len, boolean is_cmd) {
		
		BLECharacteristic characteristic = parent_handler.getCharacteristicByHandle(handle);
		
		if (characteristic == null)
			characteristic = parent_handler.getCharacteristicByCCCDHandle(handle);
				
		parent_handler.pushWrite(handle, data, data_len); // push the value to the characteristic
		
		if(user_callback != null && characteristic != null)
			user_callback.writeEvent(characteristic, data, data_len, is_cmd, handle);
	}
}
