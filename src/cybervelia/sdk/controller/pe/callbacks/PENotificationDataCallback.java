package cybervelia.sdk.controller.pe.callbacks;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.NotificationValidation;

public interface PENotificationDataCallback {

	void notification_event(BLECharacteristic char_used, NotificationValidation validation);
	/*
	switch(validation)
	{
		case NOTIFICATION_ENABLED:
			System.out.println("Notification Enabled for " + handle);
			break;
		case NOTIF_INDIC_DISABLED:
			System.out.println("Notification/Indication Disabled for " + handle);
			break;
		case INDICATION_ENABLED:
			System.out.println("Indication Enabled for " + handle);
			break;
		case NOT_VALID:
			break;
	}
	*/
	
	
}
