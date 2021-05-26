package cybervelia.sdk.controller.pe.handlers;

import java.io.IOException;
import java.util.HashMap;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.NotificationValidation;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.PENotificationDataCallback;

public class PENotificationDataHandler {
	private boolean is_notification_data_triggered = false;
	private byte[] notification_data = new byte[31];
	private int notification_data_size = 0;
	private byte notification_characteristic_id = 0;
	private Object lock_until_notification_received = new Object();
	private volatile boolean notification_error_generated = false;
	
	private boolean is_indication_enabled = false;
	private boolean is_notification_enabled = false;
	private HashMap<Short, Boolean> notification_status_map;
	
	
	private PEBLEDeviceCallbackHandler parent_handler;
	private PENotificationDataCallback user_callback = null;
	
	public PENotificationDataHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
		notification_status_map = new HashMap<Short, Boolean>();
	}
	
	public void setCallback(PENotificationDataCallback callback)
	{
		this.user_callback = callback;
	}
	
	// Notifications
	public boolean isNotificationTriggered() {
		if (is_notification_data_triggered)
		{
			is_notification_data_triggered = false;
			return true;
		}
		else return false;
	}
	
	public final byte[] getNotificationData() {
		return notification_data;
	}
	
	public int getNotificationDataSize() {
		return notification_data_size;
	}
	
	public void setNotificationDataSent(boolean notification_error_generated) {
		this.notification_error_generated = notification_error_generated;
		parent_handler.pushNotification(notification_characteristic_id, notification_data, notification_data_size);
		synchronized(lock_until_notification_received) {lock_until_notification_received.notify();}
	}
	
	public boolean setNotificationData(byte characteristic_id, final byte[] data, int size) throws IOException {
		if (parent_handler.isDisconnectTriggeredNC()) return false;
		if(!parent_handler.isDeviceConnected()) return false;
		if (size > data.length) throw new IOException("Given size is smaller than size of buffer");
		System.arraycopy(data, 0, notification_data, 0, size);
		notification_data_size = size;
		notification_characteristic_id = characteristic_id;
		is_notification_data_triggered = true;
		try {synchronized(lock_until_notification_received) {lock_until_notification_received.wait();}}catch(InterruptedException e) {}
		return !notification_error_generated;
	}
	
	public byte getNotificationCharacteristicId() {
		return notification_characteristic_id;
	}
	
	public void setNotificationEnabledWithHandle(short handle, NotificationValidation validation)
	{
		BLECharacteristic char_used = parent_handler.getCharacteristicByCCCDHandle(handle);
		if (char_used != null)
		{
			if (validation == NotificationValidation.INDICATION_ENABLED || validation == NotificationValidation.NOTIFICATION_ENABLED)
				notification_status_map.put(handle, true);
			else if (validation == NotificationValidation.NOTIF_INDIC_DISABLED)
				notification_status_map.put(handle, false);
			
			if (user_callback != null)
				user_callback.notification_event(char_used, validation);
		}
	}
	
	public boolean isNotificationAllowed(short handle) {
		if (notification_status_map == null) 
			System.err.println("WARNING: Notification_status_map is NULL");
		
		if (notification_status_map.containsKey(handle) && notification_status_map.get(handle)) return true;
		else return false;
	}
	
	public void setIndicationEnabled() {
		this.is_indication_enabled = true;
	}
	
	public void setIndicationDisabled() {
		this.is_indication_enabled = false;
	}
	

	public void setNotificationEnabled() {
		this.is_notification_enabled = true;
	}
	
	public void setNotificationDisabled() {
		this.is_notification_enabled = false;
	}
	
	
	
	public void reset()
	{
		notification_error_generated = true;
		notification_status_map.clear();
		is_notification_data_triggered = false;
		synchronized(lock_until_notification_received) {lock_until_notification_received.notify();}
	}
}
