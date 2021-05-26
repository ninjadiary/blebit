package cybervelia.sdk.controller.pe.callbacks;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.AuthorizedData;

public interface PEReadCallback {
	
	boolean authorizeRead(BLECharacteristic characteristic, final byte[] data, int data_len, AuthorizedData authorized_Data);
	/*
		System.out.print("Requesting Read Authentication - on Handle " + handle + " with a value of ");
		for(int i = 0; i<data_len; ++i)
			System.out.print(String.format("%02X ", data[i]));
		System.out.println();
		
		authorizeRead(true);
	 * */
	
	void readEvent(BLECharacteristic characteristic, final byte[] data, int data_len);
}
