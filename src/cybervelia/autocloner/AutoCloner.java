package cybervelia.autocloner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;


import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fazecast.jSerialComm.SerialPort;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEConnectionParameters;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.ce.callbacks.CEAdvertisementCallback;
import cybervelia.sdk.controller.ce.callbacks.CEBondCallback;
import cybervelia.sdk.controller.ce.callbacks.CEConnectionCallback;
import cybervelia.sdk.controller.ce.callbacks.CENotificationEventCallback;
import cybervelia.sdk.controller.ce.callbacks.CEScanCallback;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesCommon.AddressType;
import cybervelia.server.CryptoHelper;

public class AutoCloner {
	static boolean debugging = false;
	static CEBLEDeviceCallbackHandler mycecallbackHandler = null;
	static CEController ce;
	static FileOutputStream fout;
	static HashMap<String, BLEDevice> devices;
	static String current_device_mac = "";
	static HashMap<String, AtomicInteger> failed_devices;
	static LinkedBlockingQueue<AdvertisementPacket> queue;
	static int failed_connection_attempts = 3;
	static volatile boolean shutdown = false;
	static volatile boolean sefetoshutdown = false;
	private static ArrayList<String> ignore_list;
	private static ArrayList<String> whitelist_names;
	private static int connection_min_interval_ms = 9;
	private static int connection_max_interval_ms = 15;
	private static int connection_supervision_timeout_ms = 2000;
	
	public static void main(String[] args) throws Exception {

		/* Initialization variables */
		devices = new HashMap<String, BLEDevice>();
		failed_devices = new HashMap<String, AtomicInteger>();
		queue = new LinkedBlockingQueue<AdvertisementPacket>();
		ignore_list = new ArrayList<String>();
		whitelist_names = new ArrayList<String>();
		boolean device_connected = false;
		
		getConfigurations();
		connectToDevice();
		installCabllbacks();
		
		/* Start scanning for devices */
		ce.startScan();
		
		attachShutdownProcedure();
		
		for(String name : whitelist_names)
			System.out.println("Whitelisted: " + name);

		for(String name : ignore_list)
			System.out.println("ignore: " + name);
		
		while(!shutdown) 
		{
			/* Wait for an advertisement packet */
			device_connected = false;
			AdvertisementPacket adpacket = queue.take();
			
			if(adpacket != null && !shutdown) 
			{
				String raddr = adpacket.getAddress();
				boolean is_adv = (adpacket.getAdvertisementData() == null ? false : true);
				
				String devname = adpacket.getDeviceName();
				
				/* check for whitelist-only filtering - case-insensitive */
				if (whitelist_names.size() > 0)
				{
					boolean found = false;
					for(String whitelist_dev : whitelist_names)
					{
						if (devname.toLowerCase().contains(whitelist_dev.toLowerCase()))
						{
							found = true;
							break;
						}
					}
					
					if (!found) 
						continue;
				}
				
				/* Decide if we need to examine this packet */
				if (!devices.containsKey(raddr) && (!failed_devices.containsKey(raddr) || (failed_devices.containsKey(raddr) && failed_devices.get(raddr).intValue() < failed_connection_attempts)))
				{
					// Connect to device, initiate discovery and read all characteristics and services
					device_connected = true;
					if (huntDevice(adpacket.getAddress(), adpacket.getAddressType()))
					{
						System.err.println(adpacket.getAddress() + ": " + devname);
						devices.get(raddr).setAdvertisementData(adpacket.getAdvertisementData(), adpacket.getScanData());
						if (devname.length() > 0)
							devices.get(raddr).setDeviceName(devname);
					}
					else
					{
						if (ce.isDeviceConnected())
							ce.disconnect(19);
						
						/* Record failed attempts */
						if (failed_devices.containsKey(raddr))
							failed_devices.get(raddr).incrementAndGet();
						else
							failed_devices.put(raddr, new AtomicInteger(1));
					}
				}
				else if (devices.containsKey(raddr) && is_adv && !devices.get(raddr).hasAdvData())
				{
					System.err.println(adpacket.getAddress() + ": ADV:" + devname);
					devices.get(raddr).setOnlyAdvData(adpacket.getAdvertisementData());
					if (devname.length() > 0)
						devices.get(raddr).setDeviceName(devname);
				}
				else if (devices.containsKey(raddr) && !is_adv && !devices.get(raddr).hasScanData())
				{
					System.err.println(adpacket.getAddress() + ": SCN:" + devname);
					devices.get(raddr).setOnlyScanData(adpacket.getScanData());
					if (devname.length() > 0)
						devices.get(raddr).setDeviceName(devname);
				}
			}
			
			if (device_connected && !shutdown)
			{
				try {Thread.sleep(5000);}catch(InterruptedException iex) {}
				
				if (ce.startScan())
					System.out.println("Scan started");
				else
					System.err.println("Scan failed to start");
			}
		}
		
		System.out.println("Flushing data to output file...");
		
		if (ce.isDeviceConnected())
			ce.disconnect(19);
		
		ce.terminate();
		
		/* Save data to file */
		fout = new FileOutputStream(getOutputFile());
		String data = flushDevicesToFile();
		fout.write(data.getBytes());
		fout.flush();
		fout.close();
		sefetoshutdown = true;
	}
	
	private static void getConfigurations() {
		try {
			File config_file = new File("config.properties");
			if (config_file.exists())
			{
				Configurations configs = new Configurations();
				Configuration config = configs.properties(config_file);
				
				String[] namesarr = config.getStringArray("whitelist.names");
				for(String nm : namesarr)
					whitelist_names.add(nm);
				
				
				failed_connection_attempts = config.getInt("connection.attempts", 3);
				connection_min_interval_ms = config.getInt("connection.interval.min", connection_min_interval_ms);
				connection_max_interval_ms = config.getInt("connection.interval.max", connection_max_interval_ms);
				connection_supervision_timeout_ms = config.getInt("connection.supervision", connection_supervision_timeout_ms);
				
			}
		}catch(ConfigurationException ex) {
			System.err.println("Configuration error: " + ex.getMessage());
		}
	}
	
	private static void attachShutdownProcedure() {
	    Runtime.getRuntime().addShutdownHook(new Thread() {
	        @Override
	        public void run() {
	        	queue.add(new AdvertisementPacket());
	        	if (ce.isDeviceConnected()) 
	        		ce.disconnect(19);
	        	System.out.println("Shutting down..");
	        	shutdown = true;
	        	while(!sefetoshutdown);
	        }
	    });
	}
	
	public static File getOutputFile() throws IOException {
		Date datetime = new Date();
		String filename = datetime.toString().replace(' ', '-').replace('+', '-').replace(':', '-');
		System.out.println("Output File: " + filename);
		File file = new File(filename);
		if (!file.createNewFile())
		{
			System.err.println("Cannot create output file");
			System.exit(1);
		}
		return file;
	}
	
	public static String flushDevicesToFile() throws JSONException {
		JSONArray devices_array = new JSONArray();
		for(BLEDevice device : devices.values())
		{
			JSONObject device_json = device.jsonSerialize();
			if (device_json != null)
				devices_array.put(device_json);
		}
		return devices_array.toString();
	}
	
	/* Hunt Device
	 * Description: Connects to the peer device specified by the parameters.
	 * Parameters: 
	 * 		address: address of the device we wish to connect
	 * 		addr_type: type of the address of peer device
	 * Return: successful collection of information. Anything that fails should be treated as a failure  
	 * */
	private static boolean huntDevice(String address, AddressType addr_type) {
		System.out.println("Initiating connection to " + address);
		
		try {
			if (!ce.connectNow(address, addr_type))
				return false;
			
			BLEDevice newDevice = new BLEDevice(addr_type, address);
			devices.put(address, newDevice);
			
			if (!ce.startDiscovery(true)) {
				System.err.println("Discovery failed for " + address);
				ce.disconnect(19);
				return false;
			}
			
			while (ce.isDiscoveryInProgress());
			
			List<BLEService> services = ce.getDiscoveredServices();
			
			newDevice.setDiscoveryData(services);
			
			/* Reading Initial Characteristics Value */
			System.out.println("Reading Characteristics Value ...");
			byte[] temp_buffer = new byte[31];
			for(Iterator<BLEService> iterator = services.iterator(); iterator.hasNext();)
			{
				BLEService service = iterator.next();
				ArrayList<BLECharacteristic> char_list = service.getAllCharacteristics();
				for(BLECharacteristic ch : char_list)
				{
					if (ch.isReadEnabled())
					{
						int data_len = 0;
						data_len = ce.readData(temp_buffer, 0, 31, ch.getValueHandle());
						if (ch.isNotificationAuthorized())
							ce.enableNotifications(ch.getCCCDHandle());
						
						newDevice.putData(ch.getUUID().toString(), temp_buffer, data_len, DeviceDatum.operation.READ);
					}
				}
			}
			
			
			try {Thread.sleep(1000);}catch(InterruptedException iex) {}
			
			System.err.println("End of hunting - Attempt to disconnect");
			ce.disconnect(19);
			
			return true;
		}catch(Exception ex) {
			System.err.println("Exception in hunting function: " + ex.getMessage());
			StringWriter swriter = new StringWriter();
			PrintWriter writer = new PrintWriter(swriter);
			ex.printStackTrace(writer);
			System.out.println(swriter.toString());
			
			return false;
		}
	}
	
	/* Install central callback handlers
	 * Description: Handle the various events that BLE:Bit device sends back to SDK via the callback functions.
	 * 				Such events are connection-state-related events, encryption-related events, advertisement-based events etc. 
	 * */
	private static void installCabllbacks() {
		
		mycecallbackHandler.installConnectionCallback(new CEConnectionCallback() {

			@Override
			public void disconnected(int reason) {
				System.out.println("Disconnected event received");
			}
			
			@Override
			public void connected(AddressType address_type, String address) {
				System.out.println("Connected: " + address);
				current_device_mac = address;
			}
			
		});
		
		
		mycecallbackHandler.installNotificationEventCallback(new CENotificationEventCallback() {

			@Override
			public void notificationReceived(BLECharacteristic characteristic, byte[] data, int data_len) {
				devices.get(current_device_mac).putData(characteristic.getUUID().toString(), data, data_len, DeviceDatum.operation.NOTIFINDI);
			}
			
			@Override
			public void notificationReceivedRaw(short handle, byte[] data, int data_len) {
				// do nothing
			}
			
		});
		
		mycecallbackHandler.installScanCallback(new CEScanCallback() {

			@Override
			public void scanStopped() {
				System.out.println("Scan Stopped");
			}
			
		});
		
		mycecallbackHandler.installAdvertisementCallback(new CEAdvertisementCallback() {

			@Override
			public void advertisementPacket(byte[] address, byte address_type, byte scan_adv, byte advertisement_type, byte rssi, byte datalen, byte[] data) 
			{
				String raddr = ConnectionTypesCommon.addressToStringFormat(address).toLowerCase();
				
				if (ignore_list.contains(raddr))
					return;
				
				boolean is_adv = (scan_adv == 1 ? false : true);
				
				System.out.println("Received " + raddr);
				
				AdvertisementPacket packet = new AdvertisementPacket(raddr, ConnectionTypesCommon.getAddressTypeFromCode(address_type));
				
				if (is_adv)
					packet.setAdvertisementData(data, datalen);
				else
					packet.setScanData(data, datalen);
				queue.add(packet);
				
			}
			
		});
		
		mycecallbackHandler.installBondCallback(new CEBondCallback() {

			@Override
			public byte[] getPIN() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void authStatus(int status, int peers_fault) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void bondingSucceed(int precedure) {
				devices.get(current_device_mac).bond();
			}
			
			@Override
			public void bondingFailed(short error, int bond_error_src) {
				devices.get(current_device_mac).bond();
			}

			@Override
			public void peerBondDeleteError() {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}
	
	/* Function: findDevicePort
	 * Description: Iterate all description names of ports and find the matching device name
	 * Parameters: 
	 * 		match_device_name: BLE:Bit match-name - the argument changes regarding the OS running the application 
	 * Return: The port matched to the device name  
	 * */
	private static String findDevicePort(String match_device_name)
	{
		SerialPort[] sp = SerialPort.getCommPorts();
		String com_port = null;
		for(SerialPort s : sp)
		{
			System.out.println(s.getDescriptivePortName());
			 if (s.getDescriptivePortName().indexOf(match_device_name) >= 0)
			 {
				 com_port = s.getSystemPortName();
			 }
		}
		
		if (com_port == null)
		{
			System.err.println("COM Port does not exist");
			System.exit(1);
		}
		
		return com_port;
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
	
	public static void connectToDevice() throws IOException {
		// Print Available Communication channels
		SerialPort[] sp = SerialPort.getCommPorts();
		for(SerialPort s : sp)
			System.out.println(s.getSystemPortName() + " - " + s.getDescriptivePortName());
		System.out.println(" --------------- ");
		
		// Find and print available serial ports
		String[] fports = findPorts();
		
		System.err.println("Devices Found: " + fports.length);
		
		if(fports.length == 0)
		{
			System.err.println("No BLE:Bit Devices found");
			System.exit(1);
		}
		
		// Initialise CE
		try {
			mycecallbackHandler = new CEBLEDeviceCallbackHandler();
			ce = new CEController(fports[0], mycecallbackHandler);
			
			if (!ce.isInitializedCorrectly())
			{
				System.err.println("Not a BLEBit CE");
				System.exit(1);
			}
			
		}catch(IOException ioex) {
			System.err.println("Failed to initialize ce: " + ioex.getMessage());
			System.exit(1);
		}
		
		CEConnectionParameters con_params = new CEConnectionParameters();
		con_params.enforceScanRequests(true);
		con_params.setScanTimeoutSeconds(0);
		con_params.setConnectionRequestSupervisionTimeoutMs(connection_supervision_timeout_ms);
		con_params.setMinConnectionIntervalMs(connection_min_interval_ms);
		con_params.setMaxConnectionIntervalMs(connection_max_interval_ms);
		
		ce.sendConnectionParameters(con_params);
		ce.sendBluetoothDeviceAddress("ff:55:ee:fe:4a:af", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
		ce.configurePairing(ConnectionTypesCommon.PairingMethods.KEYBOARD, null);
		ce.eraseBonds();
		ce.finishSetup();
		
		System.out.println("SDK Version: " + ConnectionTypesCommon.getSDKVersion());
		System.out.println("CE FW Version: " + ce.getFirmwareVersion());
		
	}

}





