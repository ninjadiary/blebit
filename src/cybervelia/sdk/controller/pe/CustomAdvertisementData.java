package cybervelia.sdk.controller.pe;

public class CustomAdvertisementData {
	public byte[] data;
	public int size = 0;
	
	public CustomAdvertisementData(final byte[] data, int len){
		this.data = new byte[31];
		System.arraycopy(data, 0, this.data, 0, len);
		this.size = len;
	}
}
