package cybervelia.sdk.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.ArrayList;

import com.fazecast.jSerialComm.SerialPort;

import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.PEController;
import cybervelia.server.CryptoHelper;

public class BLEHelper {
	
	public static void wait(int timeoutms) {
		try {Thread.sleep(timeoutms);}catch(InterruptedException iex) {}
	}
	
	public static String validateBLEAddress(String addr) {
		addr = addr.trim();
		if (addr.split(":").length != 6)
		{
			return null;
		}
		
		for(String addr_part : addr.split(":")) {
			if (addr_part.length() != 2)
				return null;
			if (!CryptoHelper.isValidHex(addr_part))
			{
				System.err.println("invalid hex " + addr_part);
				return null;
			}
		}
		
		return addr;
	}
	
	public static String generateRandomDeviceAddress() {
		StringBuffer address = new StringBuffer();
		byte[] random_bytes = new byte[6];
		SecureRandom random = new SecureRandom();
		random.nextBytes(random_bytes);
		for(int i = 0; i<6; ++i) {
			String hex = String.format("%02x", random_bytes[i]);
			if (i>0)
				address.append(":");
			address.append(hex);
		}
		return address.toString();
	}
	
	// Get ce controller
	public static CEController getCentralController(CEBLEDeviceCallbackHandler handler) {
		
		// Create the Central Object
		CEController ce = null;
		
		String[] ports = findPorts(false, null);
		
		for(String port : ports)
		{
			try {
				ce = new CEController(port, handler);
			}catch(IOException ioex) {ce = null; continue;}
			
			if (!ce.isInitializedCorrectly()) {
				ce.terminate();
				ce = null;
			}else
				break;			
		}
		
		if (ce == null) {
			return null;
		}
		
		return ce;
		
	}
	
	// Get PE controller
	public static PEController getPeripheralController(PEBLEDeviceCallbackHandler handler) {
		
		PEController pe = null;
		
		String[] ports = findPorts(false, null);

		for(String port : ports)
		{
			try {
				pe = new PEController(port, handler);
			}catch(IOException ioex) {continue;}
			
			if (!pe.isInitializedCorrectly()) {
				pe.terminate();
				pe = null;
			}else
				break;
		}
		
		if (pe == null) {
			return null;
		}
		
		return pe;		
	}
	
	/* Identify BLE:Bit devices */
	private static String[] findPorts(boolean debugging, String iface) {
		ArrayList<String> portsFound = new ArrayList<String>();
		
		SerialPort[] sp = SerialPort.getCommPorts();
		for(SerialPort s : sp)
		{
			if (!debugging && (s.getDescriptivePortName().toLowerCase().contains("cp210x") && System.getProperty("os.name").startsWith("Windows")))
				portsFound.add(s.getSystemPortName());
			else if (!debugging && (s.getDescriptivePortName().toLowerCase().contains("cp210x") && System.getProperty("os.name").startsWith("Linux")))
				portsFound.add(s.getSystemPortName());
			else if (debugging  && s.getDescriptivePortName().contains(iface))
				portsFound.add(s.getSystemPortName());
		}
		
		String[] ports = new String[portsFound.size()];
		for(int i = 0; i<portsFound.size(); ++i)
		{
			ports[i] = portsFound.get(i);
		}
		
		return ports;
	}
	
	/* Identify the correct device (ce or pe ) and switch IO if that is deemed necessary */
	private static void switchIO(String[] found_ports, CEController ce, PEController pe) throws IOException {
	     if (!ce.isInitializedCorrectly() && !pe.isInitializedCorrectly())
	     {
	         // Get SerialPort InputStream and OutputStream
	         InputStream ce_in = ce.getInputStream();
	         OutputStream ce_out = ce.getOutputStream();
	         // Get Firmware version retrieved from BLE:Bit during initialization stage
	         short ce_fw = ce.getFirmwareVersion();
	         // get Serial Port
	         BLESerialPort ce_port = ce.getSerialPort();
	         // Exchange Ports
	         String serial_ce = found_ports[0];
	         found_ports[0] = found_ports[1];
	         found_ports[1] = serial_ce;
	         // Switch Input/Output streams, Serial Ports and Firmware Version
	         ce.switchIO(pe.getInputStream(), pe.getOutputStream(), pe.getSerialPort(), pe.getFirmwareVersion());
	         pe.switchIO(ce_in, ce_out, ce_port, ce_fw);
	     }
	}
	
}
