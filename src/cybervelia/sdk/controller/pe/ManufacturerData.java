package cybervelia.sdk.controller.pe;

import java.io.IOException;

public class ManufacturerData {
	protected short company_identifier = 0;
	protected byte[] data;
	protected int size;
	
	public ManufacturerData(final byte[] data, int size, short company_identifier) throws IOException {
		if (size > 29 || size > data.length) throw new IOException("Incorrect Size");
		this.data = new byte[size];
		this.size = size;
		this.company_identifier = company_identifier;
		System.arraycopy(data, 0, this.data, 0, size);
	}

	public byte[] getData() {
		byte[] datacopy = new byte[size];
		System.arraycopy(data, 0, datacopy, 0, size);
		return datacopy;
	}
	
	public short getIdentifier() {
		return company_identifier;
	}
}
