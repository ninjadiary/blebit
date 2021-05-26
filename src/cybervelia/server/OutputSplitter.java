package cybervelia.server;

import java.io.IOException;
import java.io.OutputStream;

// Class used mostly for debugging purposes...
public class OutputSplitter extends OutputStream {

	private OutputStream one;
	private OutputStream ours;
	
	public OutputSplitter(OutputStream one, OutputStream two) {
		this.one = one;
		this.ours = two;
	}

	@Override
	public void write(int arg0) throws IOException {
		one.write(arg0);
		ours.write(String.format("%02x", arg0).getBytes());
	}
	
	@Override
	public void write(byte[]arg0) throws IOException {
		one.write(arg0);
		for(int i=0; i<arg0.length; ++i)
			ours.write(String.format("%02x", arg0[i]).getBytes());
	}
	
	@Override
	public void write(byte[]arg0, int offset, int len) throws IOException {
		one.write(arg0, offset, len);
		for(int i=offset; i<len; ++i)
			ours.write(String.format("%02x", arg0[i]).getBytes());
	}
	
	@Override
	public void flush() throws IOException {
		one.flush();
		ours.flush();
	}
	
	@Override
	public void close() throws IOException {
		one.close();
		ours.close();
	}
}
