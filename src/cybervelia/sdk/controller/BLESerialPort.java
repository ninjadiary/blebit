package cybervelia.sdk.controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.fazecast.jSerialComm.SerialPort;

public class BLESerialPort {
	private SerialPort port;
	public static int TIMEOUT_READ_BLOCKING = SerialPort.TIMEOUT_READ_BLOCKING;
	public static int NO_PARITY = SerialPort.NO_PARITY;
	
	public BLESerialPort(String comport) {
		port = SerialPort.getCommPort(comport);
	}
	
	public boolean closePort() {
		//System.err.println("Stacktracing closePort():" + getStackTrace());
		return port.closePort();
	}
	
	public boolean openPort() {
		//System.err.println("Stacktracing openPort():" + getStackTrace());
		return port.openPort();
	}
	
	public boolean setBaudRate(int brate) {
		return this.port.setBaudRate(brate);
	}
	
	public boolean clearDTR() {
		return port.clearDTR();
	}
	
	public boolean clearRTS() {
		return port.clearRTS();
	}
	
	public boolean setComPortTimeouts(int new_tout_mode, int new_r_tm, int new_w_tm) {
		return port.setComPortTimeouts(new_tout_mode, new_r_tm, new_w_tm);
	}
	
	public boolean setComPortParameters(int new_brate, int new_databits, int new_stopbits, int parity) {
		return port.setComPortParameters(new_brate, new_databits, new_stopbits, parity);
	}
	
	public InputStream getInputStream() {
		//System.err.println("Stacktracing getInputStream:" + getStackTrace());
		return port.getInputStream();
	}
	
	public OutputStream getOutputStream() {
		//System.err.println("Stacktracing getOutputStream:" + getStackTrace());
		return port.getOutputStream();
	}
	
	private String getStackTrace() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		Exception ex = new Exception("StackTracing...");
		ex.printStackTrace(pw);
		return sw.toString();
	}
}
