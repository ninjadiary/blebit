package cybervelia.sdk.controller.ce.callbacks;

import cybervelia.sdk.types.ConnectionTypesCommon;

public interface CEAdvertisementCallback {
	public void advertisementPacket(final byte []address, byte address_type, byte scan_adv, byte advertisement_type, byte rssi, byte datalen, byte []data);
	/*System.out.println("Address: " + ConnectionTypesCommon.addressToStringFormat(address));*/
	
	
}
