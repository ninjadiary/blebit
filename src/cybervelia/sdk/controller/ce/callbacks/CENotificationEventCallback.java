package cybervelia.sdk.controller.ce.callbacks;

import cybervelia.sdk.controller.BLECharacteristic;

// Notifications and Indicates are handled the same, as one of them can be enabled at the same time. So the action is the same
public interface CENotificationEventCallback {
	public void notificationReceived(BLECharacteristic characteristic, final byte[] data, int data_len); // called when notification is received and discovery already happened
	public void notificationReceivedRaw(short handle, final byte[] data, int data_len); // called when notification is received but discovery never happened
}
