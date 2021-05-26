package cybervelia.tools.cli;
import java.io.IOException;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.AdvertisementData;
import cybervelia.sdk.controller.pe.ManufacturerData;


public class Mitm extends BBMFramework {
	
	// Custom AdvData used by device
	static AdvertisementData getAdvertisementData() throws IOException {
		AdvertisementData our_data = new AdvertisementData();
		
		our_data.setTXPower((byte) 0);
		
		byte[] manu_data = {0x34, 0x03, (byte)0xde, 0x2b, 0x0a, 0x74};
		ManufacturerData manu = new ManufacturerData(manu_data, manu_data.length, (short)0x0201);
		our_data.setManufacturerData(manu);
		our_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE | AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
		
		return our_data;
	}
	
	static AdvertisementData getScanData() throws IOException {
		AdvertisementData scand = new AdvertisementData();
		scand.includeDeviceName();
		return scand;
	}
	
	public static int writeCapture(BLECharacteristic characteristic, byte[] data, int data_size, boolean is_cmd, short handle) {
		System.out.print((is_cmd ? "CMD-WRITE " : "WRITE ") + BBMFramework.remote_ce+" >> "+BBMFramework.remote_pe+"\t" + characteristic.getUUID().toString() + ":\t");
		for(int i = 0; i<data_size; ++i)
			System.out.print(String.format("%02X ", data[i]));
		System.out.println();
		return data_size;
	}
	
	public static int readCapture(BLECharacteristic characteristic, byte[] data, int data_len) {
		System.out.print("READ "+remote_ce+" << "+remote_pe+"\t"+ characteristic.getUUID().toString() +":\t");
		for(int i=0; i<data_len; ++i) System.out.print(String.format("%02x ", data[i]));
		System.out.println();
		return data_len;
	}
	
	public static int notificationCapture(BLECharacteristic characteristic, byte[] data, int data_len) {
		System.out.print("N/I "+BBMFramework.remote_pe+" >> "+BBMFramework.remote_ce+"\t" + characteristic.getUUID().toString()+":\t");
		for(int i=0; i<data_len; ++i) System.out.print(String.format("%02x ", data[i]));
		System.out.println();
		return data_len;
	}
	
	public static void advertisement(String raddr, String raddr_type, byte scan_adv, byte advertisement_type, byte rssi, byte[] data, int datalen)
	{
		
	}
	
	public static void targetAdvertisementData(AdvertisementData data) {
		System.out.println("Device Features:");
		System.out.print(printableAdvData(data));
	}
	
	public static void targetScanData(AdvertisementData data) {
		System.out.print(printableAdvData(data));
	}
	

	
}



