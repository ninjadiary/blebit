package cybervelia.sdk.controller.ce.handlers;

import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEAdvertisementCallback;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class CEAdvertisementHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEAdvertisementCallback user_callback = null;
	
	public CEAdvertisementHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(CEAdvertisementCallback callback)
	{
		this.user_callback = callback;
	}
	
	public void advertisementPacket(final byte []address, byte address_type, byte scan_adv, byte advertisement_type, byte rssi, byte datalen, byte []data)
	{
		if(user_callback != null)
			user_callback.advertisementPacket(address, address_type, scan_adv, advertisement_type, rssi, datalen, data);
	}
}
