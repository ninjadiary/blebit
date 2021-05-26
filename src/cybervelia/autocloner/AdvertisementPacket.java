package cybervelia.autocloner;

import java.io.UnsupportedEncodingException;

import cybervelia.sdk.types.ConnectionTypesCommon.AddressType;
import cybervelia.server.CryptoHelper;

class AdvertisementPacket {
	private String address;
	private AddressType type;
	private String adv_data = null;
	private String scan_data = null;
	private String raddr_type = "";
	private String device_name = "";
	
	AdvertisementPacket(String address, AddressType addr_type){
		this.address = address;
		this.type = addr_type;
		
		switch(addr_type)
		{
			case PUBLIC_ADDR:
				raddr_type = "Public";
				break;
			case RANDOM_STATIC_ADDR:
				raddr_type = "Random Static";
				break;
			case RANDOM_RESOLVABLE_ADDR:
				raddr_type = "Random Resv.";
				break;
			case RANDOM_NON_RESOLVABLE_ADDR:
				raddr_type = "Random Non.Resv";
				break;
			default:
				raddr_type = "Unknown";
		}
	}
	
	AdvertisementPacket(){} // dump constructor
	
	public void setAdvertisementData(byte[] data, int data_len) {
		adv_data = CryptoHelper.bytesToHex(data, data_len);
		
		if (data_len > 0 && device_name.length() == 0)
			device_name = getDeviceName(data, data_len);
	}
	
	public String getDeviceName() {
		return device_name;
	}
	
	private boolean isValid(byte[] ad, int ad_len) {
	    int position = 0;

	    while (position != ad_len) {
	      int len = ((int)ad[position]) & 0xff;
	      
	      if (position + len >= ad_len || ad_len > 31) {
	    	  return false;
	      }
	      
	      position += len + 1;
	    }

	    return true;
	}
	
	private String getDeviceName(byte[] ad, int ad_len) {
		String device_name = new String();
		int position = 0;
		
		if (!isValid(ad, ad_len))
		{
			System.err.println("Invalid packet detected - cannot get device name");
			return device_name;
		}
		
		// 02010611069ecadc240ee5a9e093f3a3b50700406e - adv
		
		 while (position < ad_len) 
		 {
		      int len = ((int) ad[position]) & 0xff;
		      int type = ((int) ad[position + 1]) & 0xff;
		      
		      // Set Device Name
		      if (type == 0x09 || type == 0x08)
		      {
		    	  //System.err.println("Setting Devname...");
		    	  try {
		    		  device_name = new String(ad,position+2,position+len-1,"UTF-8");
		    	  }catch(UnsupportedEncodingException unex) {
		    		  device_name = new String(ad,position+2,position+len-1);
		    	  }
		    	  break;
		      }
		      position += len + 1;
		 }
		return device_name;
	}
	
	public String getAddress() {
		return address;
	}
	
	public AddressType getAddressType() {
		return type;
	}
	
	public boolean hasScanData() {
		if (this.scan_data != null)
			return true;
		else
			return false;
	}
	
	public String getAdvertisementData() {
		return adv_data;
	}
	
	public String getScanData() {
		return scan_data;
	}
	
	public void setScanData(byte[] data, int data_len) {
		this.scan_data = CryptoHelper.bytesToHex(data, data_len);
		
		if (data_len > 0 && device_name.length() == 0)
			device_name = getDeviceName(data, data_len);
	}
	
	public String toString() {
		StringBuilder output = new StringBuilder();
		output.append(address);
		output.append(" ");
		output.append(raddr_type);
		output.append(" ");
		output.append(adv_data);
		output.append(" ");
		if (scan_data != null)
			output.append(scan_data);
		return output.toString();
	}
}