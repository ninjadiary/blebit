package cybervelia.sdk.controller.pe.handlers;

import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;

public class PEAdvertisingHandler {
	private boolean is_advertising_stop_triggered = false;
	private boolean is_advertising_start_triggered = false;
	private PEBLEDeviceCallbackHandler parent_handler;
	
	public PEAdvertisingHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	
	// Advertising
	public void startAdvertising() {
		is_advertising_start_triggered = true;
	}
	
	public void stopAdvertising() {
		is_advertising_stop_triggered = true;
	}
	
	public boolean isAdvertisingStopTriggered() {
		if (is_advertising_stop_triggered)
		{
			is_advertising_stop_triggered = false;
			return true;
		}
		else return false;
	}
	
	public boolean isAdvertisingStartTriggered() {
		if (is_advertising_start_triggered)
		{
			is_advertising_start_triggered = false;
			return true;
		}
		else return false;
	}
	
	public void reset()
	{
		is_advertising_start_triggered = false;
		is_advertising_stop_triggered = false;
	}
}
