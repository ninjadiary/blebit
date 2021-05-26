package cybervelia.sdk.controller.pe;

import java.io.IOException;

public class AuthorizedData {
	private byte[] authorized_data;
	private int authorized_data_len;
	public AuthorizedData()
	{
		authorized_data = new byte[31];
		authorized_data_len = 0;
	}
	
	public final byte[] getAuthorizedData()
	{
		return this.authorized_data;
	}
	
	public int getAuthorizedDataLength()
	{
		return this.authorized_data_len;
	}
	
	public void setAuthorizedData(byte[] authorized_data, int data_len) throws IOException
	{
		if (data_len > 31)
			throw new IOException("Error with authorized data length");
		
		System.arraycopy(authorized_data, 0, this.authorized_data, 0, data_len);
		this.authorized_data_len = data_len;
	}
}
