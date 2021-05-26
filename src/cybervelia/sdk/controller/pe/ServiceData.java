package cybervelia.sdk.controller.pe;

import java.io.IOException;

import cybervelia.sdk.controller.BLEService;

public class ServiceData {
	protected byte[] uuid;
	protected byte[] data;
	protected int size = 0;
	
	public ServiceData(final byte []UUIDLow, final byte[] data, int size) throws IOException {
		// UUIDLow can be retrieved by invoking service.getUUIDLow()
		
		this.uuid = new byte[2];
		if (size > 31 || size > data.length) throw new IOException("Given length is bigger than buffer size");
		if (uuid.length != 2) throw new IOException("UUID Length should be 2");
		this.data = new byte[size];
		this.size = size;
		System.arraycopy(UUIDLow, 0, this.uuid, 0, 2);
		System.arraycopy(data, 0, this.data, 0, size);
	}
	
	public byte[] getServiceData() {
		byte[] datacopy = new byte[size];
		System.arraycopy(data, 0, datacopy, 0, size);
		return datacopy;
	}
	
	public byte[] getUUID() {
		byte[] datacopy = new byte[2];
		System.arraycopy(uuid, 0, datacopy, 0, 2);
		return datacopy;
	}
}
