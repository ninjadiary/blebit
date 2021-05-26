package cybervelia.sdk.controller.ce.handlers;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CENotificationEventCallback;

public class CENotificationEventHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CENotificationEventCallback user_callback = null;

	public CENotificationEventHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(CENotificationEventCallback callback)
	{
		this.user_callback = callback;
	}
	
	// Notification/Indication Received
	public void notificationReceived(final byte []data, int data_len, short handle)
	{	
		BLECharacteristic characteristic = parent_handler.getCharacteristicByHandle(handle);
		
		if (characteristic == null)
			characteristic = parent_handler.getCharacteristicByCCCDHandle(handle);
		
		if(user_callback != null && characteristic != null)
			user_callback.notificationReceived(characteristic, data, data_len);
		else if (user_callback != null)
			user_callback.notificationReceivedRaw(handle, data, data_len);
	}
}
