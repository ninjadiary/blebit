package cybervelia.sdk.controller.ce.handlers;

import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEScanCallback;

public class CEScanHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEScanCallback user_callback = null;
	private volatile boolean scan_start_triggered = false;
	private volatile boolean scan_stop_triggered = false;
	public boolean is_scan_in_progress = false;
	
	public CEScanHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(CEScanCallback callback)
	{
		this.user_callback = callback;
	}
	
	// Scan Timeout
	public void scanStopped() {
		if (user_callback != null && is_scan_in_progress)
			user_callback.scanStopped();
		is_scan_in_progress = false;
	}
	
	// Scan -> Start
	public boolean startScan() {
		if(is_scan_in_progress) return false;
		
		is_scan_in_progress = true;
		scan_start_triggered = true;
		return true; // return success
	}
	
	public boolean isScanStartTriggered() {
		if(scan_start_triggered)
		{
			scan_start_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	// Scan -> Stop
	public void stopScan() {
		if (is_scan_in_progress)
		{
			// is_scan_in_progress is cleared by scanStopped(), that is triggered by the device when scan has been stopped
			scan_stop_triggered = true;
		}
	}
	
	public boolean isScanStopTriggered() {
		if(scan_stop_triggered)
		{
			scan_stop_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	public boolean isScanInProgress()
	{
		return this.is_scan_in_progress;
	}
	
	public void clearScanInProgress()
	{
		this.is_scan_in_progress = false;
	}
}
