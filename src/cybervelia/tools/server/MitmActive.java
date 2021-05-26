package cybervelia.tools.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import com.fazecast.jSerialComm.SerialPort;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLESerialPort;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.pe.AdvertisementData;
import cybervelia.sdk.controller.pe.ManufacturerData;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.PEController;
import cybervelia.sdk.controller.pe.ServiceData;
import cybervelia.sdk.types.ConnectionTypesCE;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.server.CryptoHelper;
import cybervelia.tools.cli.Mitm;

public class MitmActive extends RBBMFramework {

	static String nokelock_bdaddr = "34:03:de:2b:0a:74";
	static String anboud_bdaddr = "34:15:13:d1:5e:b1";
	static String mi1s = "c8:0f:10:8c:d8:8c";
	static DataInputStream dinRx = null;
	static DataOutputStream doutTx = null;
	static LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>();
	static ServerSocket serversocket;
	static Socket client_socket;
	private static Transmitter transmitter;
	private static boolean shutdown = false;
	private static boolean debugging = false;
	
	public static void main(String...args)
	{
		// Print Available Communication channels
		SerialPort[] sp = SerialPort.getCommPorts();
		for(SerialPort s : sp)
			System.out.println(s.getSystemPortName() + " - " + s.getDescriptivePortName());
		System.out.println(" --------------- ");
		
		// Start TCP Server
		startServer(9090);
		
		// Find and print available serial ports
		String[] fports = findPorts();
		if (fports.length != 2)
		{
			System.err.println("Devices Found: " + fports.length);
			System.exit(1);
		}
		
		try {
			
			while(true) {
				
				// Initialise CE & PC
				try {
					mypecallbackHandler = new PEBLEDeviceCallbackHandler();
					pe = new PEController(fports[1], mypecallbackHandler);
					ce_callback_handler = new CEBLEDeviceCallbackHandler();
					ce = new CEController(fports[0], ce_callback_handler);
					switchIO(fports, ce, pe);
				}catch(IOException ioex) {
					System.err.println("Failed to initialize pe and ce: " + ioex.getMessage());
					System.exit(1);
				}
				
				System.out.println("SDK Version: " + ConnectionTypesCommon.getSDKVersion());
				System.out.println("CE FW Version: " + ce.getFirmwareVersion());
				System.out.println("PE FW Version: " + pe.getFirmwareVersion());
				
				System.out.println("Waiting for a client...");
				try {
					client_socket = serversocket.accept();
					System.out.println("Client Connected: " + client_socket.getInetAddress().getHostAddress());
				}catch(IOException ioex) {
					System.err.println("Can't accept socket: " + ioex.getMessage());
				}
				
				shutdown = false;
				
				transmitter = new Transmitter(client_socket, queue);
				
				Thread rx = new Thread(new Receiver(client_socket, transmitter));
				Thread tx = new Thread(transmitter);
				
				// Clear Queue for any leftover messages
				queue.clear();
				
				// Stat rx and tx
				rx.start();
				tx.start();
				
				rx.join();
				tx.join();
				
				System.out.println("Client connection closed");
				
				shutdown = true;
				synchronized(wait_for_packet) {wait_for_packet.notify();}
				
				clearCachedServices(getTargetAddress());
				
				if (ce != null) ce.terminate();
				if (pe != null) pe.terminate();
				
				System.out.println("all terminated");
				
				ce = null;
				pe = null;
				dinRx = null;
				doutTx = null;
				
				// Wait a second for hard-reset to take effect
				try {Thread.sleep(2000);}catch(InterruptedException iex) {}
				
				
			}
			
		}catch(InterruptedException iex) {
			System.err.println("Main Loop Interrupted Exception: " + iex.getMessage());
		}
		catch(Exception iex) {
			System.err.println("Main Loop Exception: " + iex.getMessage());
		}
		
	}
	
	/* Identify the correct device (ce or pe ) and switch IO if necessary */
	private static void switchIO(String[] fports, CEController ce, PEController pe) throws IOException {
		if (!ce.isInitializedCorrectly() && !pe.isInitializedCorrectly())
		{
			System.out.println(" ------------------ SWITCH IO ----------------- ");
			InputStream ce_in = ce.getInputStream();
			OutputStream ce_out = ce.getOutputStream();
			short ce_fw = ce.getFirmwareVersion();
			BLESerialPort ce_port = ce.getSerialPort();
			String serial_ce = fports[0];
			fports[0] = fports[1];
			fports[1] = serial_ce;
			ce.switchIO(pe.getInputStream(), pe.getOutputStream(), pe.getSerialPort(), pe.getFirmwareVersion());
			pe.switchIO(ce_in, ce_out, ce_port, ce_fw);
			
		}
		else if ((ce.isInitializedCorrectly() ^ pe.isInitializedCorrectly()))
		{
			System.err.println("Device Type Mismatch");
			System.exit(1);
		}
	}
	
	/* Identify BLE:Bit devices */
	private static String[] findPorts() {
		ArrayList<String> portsFound = new ArrayList<String>();
		
		SerialPort[] sp = SerialPort.getCommPorts();
		for(SerialPort s : sp)
		{
			if (!debugging && (s.getDescriptivePortName().toLowerCase().contains("cp210x") && System.getProperty("os.name").startsWith("Windows")))
				portsFound.add(s.getSystemPortName());
			else if (!debugging && (s.getDescriptivePortName().toLowerCase().contains("cp210x") && System.getProperty("os.name").startsWith("Linux")))
				portsFound.add(s.getSystemPortName());
			else if (debugging  && System.getProperty("os.name").startsWith("Windows") && (s.getDescriptivePortName().contains("Prolific") || s.getDescriptivePortName().contains("USB Serial Port")))
				portsFound.add(s.getSystemPortName());
			else if (debugging  && System.getProperty("os.name").startsWith("Linux") && (s.getDescriptivePortName().contains("pl2303") || s.getDescriptivePortName().contains("ftdi_sio")))
				portsFound.add(s.getSystemPortName());
		}
		
		String[] ports = new String[portsFound.size()];
		for(int i = 0; i<portsFound.size(); ++i)
		{
			ports[i] = portsFound.get(i);
			System.out.println("ADDED: " + ports[i]);
		}
		
		return ports;
	}
	
	public static boolean isShutdownInProgress() {
		return shutdown;
	}
	
	static void startServer(int port)
	{
		try {
			System.out.println("Serving TX&RX Channel on " + port);
			serversocket = new ServerSocket(port);
			serversocket.setReuseAddress(true);
			
		}catch(IOException ex) {
			System.err.println("Unable to start RX Channel" + ex.getMessage());
			System.exit(1);
		}
	}
	
	/** Mobile Device Communication **/
	
	public static int writeCapture(BLECharacteristic characteristic, byte[] data, int data_size, boolean is_cmd, short handle) {
		try {
			JSONObject write = new JSONObject();
			write.put("cmd", "data");
			write.put("type", "write");
			write.put("data", CryptoHelper.bytesToHex(data, data_size));
			if (is_cmd)
				write.put("metadata", "write-cmd");
			write.put("uuid", characteristic.getUUID().toString());
			write.put("handle", handle);
			queue.add(write.toString());
		}catch(JSONException jex) {
			System.err.println(jex.getMessage());
		}
		/*System.out.print((is_cmd ? "CMD-WRITE " : "WRITE ") + BBMFramework.remote_ce+" >> "+BBMFramework.remote_pe+"\t" + characteristic.getUUID().toString() + ":\t");
		for(int i = 0; i<data_size; ++i)
			System.out.print(String.format("%02X ", data[i]));
		System.out.println();
		*/
		return data_size;
	}
	
	public static int readCapture(BLECharacteristic characteristic, byte[] data, int data_len, short handle) {
		try {
			JSONObject write = new JSONObject();
			write.put("cmd", "data");
			write.put("type", "read");
			write.put("data", CryptoHelper.bytesToHex(data, data_len));
			write.put("uuid", characteristic.getUUID().toString());
			write.put("handle", handle);
			queue.add(write.toString());
		}catch(JSONException jex) {
			System.err.println(jex.getMessage());
		}
		/*
		System.out.print("READ "+remote_ce+" << "+remote_pe+"\t"+ characteristic.getUUID().toString() +":\t");
		for(int i=0; i<data_len; ++i) System.out.print(String.format("%02x ", data[i]));
		System.out.println();
		*/
		return data_len;
	}
	
	public static int notificationCapture(BLECharacteristic characteristic, byte[] data, int data_len, short handle) {
		try {
			JSONObject write = new JSONObject();
			write.put("cmd", "data");
			write.put("type", "notif");
			write.put("data", CryptoHelper.bytesToHex(data, data_len));
			write.put("uuid", characteristic.getUUID().toString());
			write.put("handle", handle);
			queue.add(write.toString());
		}catch(JSONException jex) {
			System.err.println(jex.getMessage());
		}
		/*
		System.out.print("N/I "+BBMFramework.remote_pe+" >> "+BBMFramework.remote_ce+"\t" + characteristic.getUUID().toString()+":\t");
		for(int i=0; i<data_len; ++i) System.out.print(String.format("%02x ", data[i]));
		System.out.println();
		*/
		return data_len;
	}
	
	public static void advertisement(String raddr, String raddr_type, byte scan_adv, byte advertisement_type, byte rssi, byte[] data, int datalen)
	{
		try {
			JSONObject write = new JSONObject();
			write.put("cmd", "data");
			write.put("type", "adv");
			write.put("data", CryptoHelper.bytesToHex(data, datalen));
			JSONObject advdata = new JSONObject();
			advdata.put("addr",raddr);
			advdata.put("type",raddr_type);
			advdata.put("sa",String.valueOf(scan_adv));
			advdata.put("at",String.valueOf(advertisement_type));
			advdata.put("rssi",String.valueOf(rssi));
			write.put("metadata", advdata.toString());
			queue.add(write.toString());
		}catch(JSONException jex) {
			System.err.println("advertisement() func: " + jex.getMessage());
		}
	}
	
	public static void targetAdvertisementData(AdvertisementData data) {
		System.out.println("Device Features:");
		System.out.print(printableAdvData(data));
	}
	
	public static void targetScanData(AdvertisementData data) {
		System.out.print(printableAdvData(data));
	}
	
	public static void CEDisconnected() {
		System.out.println("[CE] Target Device disconnected!");
		sendStatus("[CE] Target Device disconnected!");
	}
	
	public static void CEConnected(String address) {
		System.out.println("[CE] Target Device connected : " + address);
		sendStatus("[CE] Target Device connected : " + address);
	}
	
	public static void PEDisconnected() {
		System.out.println("[PE] Target Device disconnected!");
		sendStatus("[PE] Target Device disconnected!");
	}
	
	public static void PEConnected(String address) {
		System.out.println();
		sendStatus("[PE] Target Device connected : " + address);
	}
	
	public static void sendStatus(String status) {
		try {
			JSONObject jobject = new JSONObject();
			jobject.put("cmd", "status");
			jobject.put("data", status);
			queue.add(jobject.toString());
		}catch(JSONException jex) {
			// ignore
		}
	}
}







