package cybervelia.tools.cli;

import java.io.DataInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fazecast.jSerialComm.SerialPort;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEConnectionParameters;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.ce.callbacks.CEAdvertisementCallback;
import cybervelia.sdk.controller.ce.callbacks.CEConnectionCallback;
import cybervelia.sdk.controller.ce.callbacks.CENotificationEventCallback;
import cybervelia.sdk.controller.ce.callbacks.CEScanCallback;
import cybervelia.sdk.controller.pe.AdvertisementData;
import cybervelia.sdk.controller.pe.AuthorizedData;
import cybervelia.sdk.controller.pe.CustomAdvertisementData;
import cybervelia.sdk.controller.pe.ManufacturerData;
import cybervelia.sdk.controller.pe.NotificationValidation;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.PEConnectionParameters;
import cybervelia.sdk.controller.pe.PEController;
import cybervelia.sdk.controller.pe.ServiceData;
import cybervelia.sdk.controller.pe.callbacks.BondKeysCallback;
import cybervelia.sdk.controller.pe.callbacks.BondingCallback;
import cybervelia.sdk.controller.pe.callbacks.PEConnectionCallback;
import cybervelia.sdk.controller.pe.callbacks.PENotificationDataCallback;
import cybervelia.sdk.controller.pe.callbacks.PEWriteEventCallback;
import cybervelia.sdk.controller.pe.callbacks.PEReadCallback;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesCommon.AddressType;
import cybervelia.server.CryptoHelper;
import cybervelia.sdk.types.ConnectionTypesPE;


// BLE:Bit MiTM Framework
public class BBMFramework {
	protected static CEController ce = null;
	protected static PEController pe = null;
	private static String target_bdaddr = new String(); // Black Smart Lock - Public Addr
	protected static String remote_ce = "Central";
	protected static String remote_pe = "Peripheral";
	protected static ConnectionTypesCommon.AddressType type_of_target_address = ConnectionTypesCommon.AddressType.PUBLIC_ADDR;
	private static String sim_client = "ff:55:ee:fe:4a:af"; // our address for CE
	private static ArrayList<BLEService> new_services = new ArrayList<BLEService>();
	private static HashMap<BLECharacteristic, ByteBuffer> map = new HashMap<BLECharacteristic, ByteBuffer>();
	private static String device_name = null;
	private static short appearance_value = -1;
	private static boolean load_services_from_file = false;
	// Adv Services
	private static ArrayList<BLEService> adv_service_list_16_inc = new ArrayList<BLEService>();
	private static ArrayList<String> adv_service_list_str_16_inc = new ArrayList<String>();
	private static ArrayList<BLEService> adv_service_list_16_comp = new ArrayList<BLEService>();
	private static ArrayList<String> adv_service_list_str_16_comp = new ArrayList<String>();
	private static ArrayList<BLEService> adv_service_list_16_sol = new ArrayList<BLEService>();
	private static ArrayList<String> adv_service_list_str_16_sol = new ArrayList<String>();
	private static ArrayList<BLEService> adv_service_list_128_inc = new ArrayList<BLEService>();
	private static ArrayList<String> adv_service_list_str_128_inc = new ArrayList<String>();
	private static ArrayList<BLEService> adv_service_list_128_comp = new ArrayList<BLEService>();
	private static ArrayList<String> adv_service_list_str_128_comp = new ArrayList<String>();
	private static ArrayList<BLEService> adv_service_list_128_sol = new ArrayList<BLEService>();
	private static ArrayList<String> adv_service_list_str_128_sol = new ArrayList<String>();
	// Scan Services
	private static ArrayList<BLEService> scn_service_list_16_inc = new ArrayList<BLEService>();
	private static ArrayList<String> scn_service_list_str_16_inc = new ArrayList<String>();
	private static ArrayList<BLEService> scn_service_list_16_comp = new ArrayList<BLEService>();
	private static ArrayList<String> scn_service_list_str_16_comp = new ArrayList<String>();
	private static ArrayList<BLEService> scn_service_list_16_sol = new ArrayList<BLEService>();
	private static ArrayList<String> scn_service_list_str_16_sol = new ArrayList<String>();
	private static ArrayList<BLEService> scn_service_list_128_inc = new ArrayList<BLEService>();
	private static ArrayList<String> scn_service_list_str_128_inc = new ArrayList<String>();
	private static ArrayList<BLEService> scn_service_list_128_comp = new ArrayList<BLEService>();
	private static ArrayList<String> scn_service_list_str_128_comp = new ArrayList<String>();
	private static ArrayList<BLEService> scn_service_list_128_sol = new ArrayList<BLEService>();
	private static ArrayList<String> scn_service_list_str_128_sol = new ArrayList<String>();
	private static CEConnectionCallback connectionCECallback = null;
	private static AdvertisementData adv_data = null;
	private static AdvertisementData scn_data = null;
	private static boolean erase_pe_keys = false;
	protected static boolean parse_adv_data = false; 
	public static volatile boolean wait_for_scan_data = false;
	
	public static void setPEDeviceAddress(String addr) {
		sim_client = addr;
	}
	
	public static void setRemoteNameCE(String name) {
		remote_ce = name;
	}
	
	public static void setRemoteNamePE(String name) {
		remote_pe = name;
	}
	
	public static void setDeviceName(String name) {
		device_name = name;
	}
	
	public static void setAppearanceValue(short value) {
		appearance_value = value;
	}
	
	public static void loadServicesFromFile() {
		load_services_from_file = true;
	}
	
	public static void advertiseComplete16UUIDService(String uuid) {
		adv_service_list_str_16_comp.add(uuid);
	}
	
	public static void advertiseComplete128UUIDService(String uuid) {
		adv_service_list_str_128_comp.add(uuid);
	}
	
	public static void advertiseIncomplete16UUIDService(String uuid) {
		adv_service_list_str_16_inc.add(uuid);
	}
	
	public static void advertiseIncomplete128UUIDService(String uuid) {
		adv_service_list_str_128_inc.add(uuid);
	}
	
	public static void advertiseSolicited16UUIDService(String uuid) {
		adv_service_list_str_16_sol.add(uuid);
	}
	
	public static void advertiseSolicited128UUIDService(String uuid) {
		adv_service_list_str_128_sol.add(uuid);
	}
	
	public static void advertiseScanComplete16UUIDService(String uuid) {
		scn_service_list_str_16_comp.add(uuid);
	}
	
	public static void advertiseScanComplete128UUIDService(String uuid) {
		scn_service_list_str_128_comp.add(uuid);
	}
	
	public static void advertiseScanIncomplete16UUIDService(String uuid) {
		scn_service_list_str_16_inc.add(uuid);
	}
	
	public static void advertiseScanIncomplete128UUIDService(String uuid) {
		scn_service_list_str_128_inc.add(uuid);
	}
	
	public static void advertiseScanSolicited16UUIDService(String uuid) {
		scn_service_list_str_16_sol.add(uuid);
	}
	
	public static void advertiseScanSolicited128UUIDService(String uuid) {
		scn_service_list_str_128_sol.add(uuid);
	}
	
	public static void setAdvData(AdvertisementData data) {
		adv_data = data;
	}
	
	public static void setScnData(AdvertisementData data) {
		scn_data = data;
	}
	
	public static void setTargetAddress(String bdaddr) {
		target_bdaddr = bdaddr;
	}
	
	public static void autoParseAdvertisementData() {
		parse_adv_data = true;
	}
	
	public static void eraseKeys() {
		erase_pe_keys = true;
	}
	
	public static void setCEConnectionCallbackHandler(CEConnectionCallback handler) {
		connectionCECallback = handler;
	}
	
	public static void waitForScanData() {
		wait_for_scan_data = true;
	}
	
	public static String getDeviceName() {
		return device_name;
	}
	
	public static String getTargetAddress() {
		return target_bdaddr;
	}
	
	public static short getAppearanceValue() {
		return appearance_value;
	}
	
	private static String startComm(String device)
	{
		SerialPort[] sp = SerialPort.getCommPorts();
		String com_port = null;
		for(SerialPort s : sp)
		{
			 if (s.getSystemPortName().equals(device))
			 {
				 com_port = s.getSystemPortName();
			 }
		}
		
		if (com_port == null)
		{
			System.err.println("COM Port "+com_port+" does not exist");
			System.exit(1);
		}
		
		return com_port;
	}
	
	protected static void startMitm(String pe_port, String ce_port)
	{
		if (target_bdaddr.length() == 0) {
			System.err.println("Target address is not set");
			return;
		}
		
		String com_port_peripheral = startComm(pe_port);
		String com_port_central = startComm(ce_port);
		try {
			PEController.reset(com_port_peripheral);
			
			System.out.println("Starting Setup of Central...");
			// Start Central 
			Object wait_for_packet = new Object();
			CEBLEDeviceCallbackHandler ce_callback_handler = new CEBLEDeviceCallbackHandler();
			AdvertisementHandler adv_handler = new AdvertisementHandler(ce, target_bdaddr, wait_for_packet, wait_for_scan_data);
			ce_callback_handler.installAdvertisementCallback(adv_handler);
			if (connectionCECallback == null)
			{
				connectionCECallback = new CEConnectionCallback() {
					
					@Override
					public void disconnected(int reason) {
						System.out.println("[CE] Target Device disconnected!");
					}

					@Override
					public void connected(AddressType address_type, String address) {
						System.out.println("[CE] Target Device connected : " + address);
					}
					
				};
			}
			ce_callback_handler.installConnectionCallback(connectionCECallback);
			
			ce = new CEController(com_port_central, ce_callback_handler);
			CEConnectionParameters con_params = new CEConnectionParameters();
			con_params.enforceScanRequests(wait_for_scan_data);
			
			ce.sendConnectionParameters(con_params);
			ce.sendBluetoothDeviceAddress(sim_client, ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			ce.configurePairing(ConnectionTypesCommon.PairingMethods.NO_IO, null);
			ce.eraseBonds();
			ce.finishSetup();
			System.out.println("Setup of Central Finished, start scanning");
			ce.startScan();
			
			System.out.println("Waiting for target to appear...");
			try {synchronized(wait_for_packet) {wait_for_packet.wait();}}catch(InterruptedException iex) {}
			ce.stopScan();
			System.out.println("Start cloning...");
			startCloning(ce);
			
			
			// Start Peripheral
			PEBLEDeviceCallbackHandler mycallbackHandler = new PEBLEDeviceCallbackHandler();
			MyReadCallback read_callback = new MyReadCallback(ce);
			MyNotificationDataCallback notification_data_callback = new MyNotificationDataCallback(ce);
			MyPEWriteEventCallback write_callback = new MyPEWriteEventCallback(ce);
			mycallbackHandler.installWriteCallback(write_callback);
			mycallbackHandler.installReadCallback(read_callback);
			mycallbackHandler.installNotificationDataCallback(notification_data_callback);
			mycallbackHandler.installConnectionCallback(new PEConnectionCallback() {
				
				@Override
				public void disconnected(int reason) {
					System.out.println("[PE] Target Device disconnected!");
				}

				@Override
				public void connected(AddressType address_type, String address) {
					System.out.println("[PE] Target Device connected : " + address);
				}
			});
			
			pe = new PEController(com_port_peripheral, mycallbackHandler);
			
			MyCENotificationEventCallback notif_event_callback = new MyCENotificationEventCallback(pe);
			ce_callback_handler.installNotificationEventCallback(notif_event_callback);
			
			adv_data = adv_handler.getAdvertisementData();
			scn_data = adv_handler.getScanData();
			setupPeripheral(pe, adv_data, scn_data);
			
		}catch(IOException ex)
		{
			if (pe != null) pe.terminate();
			if (ce != null) ce.terminate();
			System.err.println(ex.getMessage());
		}
	}
	
	private static void startCloning(CEController ce) throws IOException
	{	
		boolean serv_restored = false;
		List<BLEService> services = null;
		
		System.out.println("Preparing for connection");
		if (ce.connectNow(target_bdaddr, type_of_target_address))
			System.out.println("Connected!");
		else
		{
			System.out.println("Connection error");
			System.exit(1);
		}
		
		// Start Discovery or load from file		
		// Load from cache file
		if (load_services_from_file)
		{
			try {
				services = loadServicesFromFile("SCE-"+ce.getClientAddress());
				serv_restored = true;
				System.out.println("Services loaded from file...");
			}catch(Exception ex) {
				/* Ignore */
			}
		}
		
		// Discover Services Online
		if (serv_restored == false)
		{
			// Start Discovery
			if (!ce.startDiscovery(true))
				System.err.println("Discovery Fail to start");
			
			// Check for discovery error
			if (!ce.getDiscoverySuccess())
			{
				System.err.println("Discovery has failed! Exiting...");
				System.exit(1);
			}
			
			// get services
			services = ce.getDiscoveredServices();		// here the service_id is set to the characteristics.
			displayServices(services);
		}
		
		List<BLEService> cloned_services = cloneServices(services);
		
		// Display Services
		System.out.println("Remote (CE) Services");
		displayServices(services);
		
		// Load services to static mapper
		Mapper.setRemoteServices(services); // for CE communication (keep the handles that have been setup by the remote device)
		Mapper.setLocalServices(cloned_services); // for peripheral (will setup new handles)
		
		new_services.addAll(cloned_services);
		
		// Reading Initial Characteristics Value
		System.out.println("Reading Initial Characteristics Value...");
		if (serv_restored == false)
		{
			byte[] temp_buffer = new byte[31];
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService service = iterator.next();
				ArrayList<BLECharacteristic> char_list = service.getAllCharacteristics();
				for(BLECharacteristic ch : char_list)
				{
					if (ch.isReadEnabled())
					{
						ch.enableHookOnRead(); // enabled because we wish to have them
						int data_read = 0;
						ch.setValueLengthVariable(true);
						if ((data_read = ce.readData(temp_buffer, 0, 31, Mapper.mapLocalRemoteCharacteristicValueHandle(ch.getUUID().toString()))) == 0)
						{
							// only warning as some devices does return 0 length in data
							System.err.println("WARNING: Characteristic "+ ch.getUUID().toString() +" returned 0 data length");
							data_read = 31; // in case something goes wrong, then set a valid non-zero data-length (as device will fail with zero data length)
							ch.setValueLengthVariable(false);
						}
						ch.setInitialValue(temp_buffer, data_read);
					}
				}
			}
		}
		
		// Remove services that are unwanted by our PE chipset
		for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
		{
			BLEService service = iterator.next();
			// 0x1800 is defined by SIG standard and has to be set on each device. The chipset will handle this for us
			if (service.getUUID().toString().equals("00001800-0000-1000-8000-00805f9b34fb") || 
				service.getUUID().toString().equals("00001801-0000-1000-8000-00805f9b34fb") || 
				service.getUUID().toString().equals("0000180a-0000-1000-8000-00805f9b34fb") ||
				service.getUUID().toString().equals("00001802-0000-1000-8000-00805f9b34fb"))
			{
				System.out.println("Removing Service " + service.getUUID().toString() + " with handle " + service.getHandle());
				// Extract & Remove Device Name Characteristic
				BLECharacteristic device_name_char =  service.getCharacteristicByUUID("00002a00-0000-1000-8000-00805f9b34fb");
				if (device_name_char != null)
				{
					System.out.println("Removing Characteristic " + device_name_char.getUUID().toString() + " with handle" + device_name_char.getValueHandle() + " CCCD" + (device_name_char.getCCCDHandle() != 0 ? device_name_char.getCCCDHandle() : 0));
					byte[] dev_n = new byte[31];
					int r = device_name_char.getLocalValue(dev_n);
					if (r > 0)
					{
						if (device_name != null && device_name.length() == 0)
							device_name = new String(dev_n, 0, r);
					}
				}
				
				// Extract & Remove Appearance Characteristic
				BLECharacteristic device_appearance_char =  service.getCharacteristicByUUID("00002a01-0000-1000-8000-00805f9b34fb");
				if (device_appearance_char != null)
				{
					System.out.println("Removing Characteristic " + device_appearance_char.getUUID().toString() + " with handle" + device_appearance_char.getValueHandle() + " CCCD" + (device_appearance_char.getCCCDHandle() != 0 ? device_appearance_char.getCCCDHandle() : 0));
					byte[] aprnce_v = new byte[31];
					int r = device_appearance_char.getLocalValue(aprnce_v);
					if (r > 0 && r <= 2)
					{
						appearance_value = (short) (aprnce_v[0] & (aprnce_v[1] << 8));
					}
				}
				
				// do not propagate this service to our simulated PE
				iterator.remove();
				continue;
			}
		}
		
		System.out.println("Local (PE) Services");
		displayServices(new_services);
		
		// Add user-specified services to adv
		for(String to_be_advertised : adv_service_list_str_16_comp)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					adv_service_list_16_comp.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : adv_service_list_str_128_comp)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					adv_service_list_128_comp.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : adv_service_list_str_16_inc)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					adv_service_list_16_inc.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : adv_service_list_str_128_inc)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					adv_service_list_128_inc.add(iservice);
					break;
				}
			}
		}
		
		for(String to_be_advertised : adv_service_list_str_16_sol)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					adv_service_list_16_sol.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : adv_service_list_str_128_sol)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					adv_service_list_128_sol.add(iservice);
					break;
				}
			}
		}
		
		// Add user-specified services to scan packet
		for(String to_be_advertised : scn_service_list_str_16_comp)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					scn_service_list_16_comp.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : scn_service_list_str_128_comp)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					scn_service_list_128_comp.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : scn_service_list_str_16_inc)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					scn_service_list_16_inc.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : scn_service_list_str_128_inc)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					scn_service_list_128_inc.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : scn_service_list_str_16_sol)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					scn_service_list_16_sol.add(iservice);
					break;
				}
			}
		}
		for(String to_be_advertised : scn_service_list_str_128_sol)
		{
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				BLEService iservice = iterator.next();
				if(iservice.getUUID().toString().equals(to_be_advertised.toLowerCase()))
				{
					scn_service_list_128_sol.add(iservice);
					break;
				}
			}
		}
		
		// Cache Services
		if (!serv_restored && load_services_from_file)
		{
			try {
				saveServicesToFile("SCE-"+ce.getClientAddress(), services);
				System.out.println("Services saved to file...");
			}catch(Exception ex) {System.err.println("Unable to save services to file... : " + ex.getMessage());}
		}
	}
	
	private static void setupPeripheral(PEController pe, AdvertisementData adv_data, AdvertisementData scan_data) throws IOException
	{	
		System.out.println("Setting up peripheral...");
		
		ConnectionTypesCommon.BITAddressType pe_addr;
		
		if (type_of_target_address == ConnectionTypesCommon.AddressType.PUBLIC_ADDR)
			pe_addr = ConnectionTypesCommon.BITAddressType.PUBLIC;
		else
			pe_addr = ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE;
		
		// Configuring SimPE
		PEConnectionParameters con_params = new PEConnectionParameters();
		con_params.setMinConnectionIntervalMS(50);
		con_params.setMaxConnectionIntervalMS(60);
		con_params.setConnectionSupervisionTimeoutMS(3000);
		
		// Alter discovery mode as it may limit the attack time frame
		if (adv_data != null) {
			if ((adv_data.getFlags() & AdvertisementData.FLAG_LE_LIMITED_DISCOVERABLE_MODE) > 0)
			{
				/** Overwrite to General Purpose mode **/
				adv_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE + AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
				
				/** In case we want to have limited discovery mode instead **/
				/*
				con_params.setAdvertisementTimeoutTU(280); // 180 ms = MAX
				pe.sendAdvIntervalTU(70);
				con_params.setConnectionType(ConnectionTypesPE.BLE_GAP_ADV_TYPE_ADV_DIRECT_IND);
				// Set Direct Address mode when in Limited Discovery - required by protocol and chipset vendor..
				if ((adv_data.getFlags() & AdvertisementData.FLAG_LE_LIMITED_DISCOVERABLE_MODE) > 0)
					pe.setDirectAddress(target_bdaddr, pe_addr);
				*/
			}
		}
		
		if (scan_data != null) {
			if ((scan_data.getFlags() & AdvertisementData.FLAG_LE_LIMITED_DISCOVERABLE_MODE) > 0)
			{
				/** Overwrite to General Purpose mode **/
				scan_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE + AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
			}
		}
		
		pe.sendBluetoothDeviceAddress(target_bdaddr, pe_addr);
		pe.sendConnectionParameters(con_params);
		pe.configurePairing(ConnectionTypesCommon.PairingMethods.NO_IO, null);
		
		pe.disableAdvertisingChannels(ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39);
		
		BLEService service = null;
		Iterator<BLEService> iterator = new_services.iterator();
		
		while(iterator.hasNext())
		{
			service = iterator.next();
			
			if (pe.sendBLEService(service) == false)
				System.err.println("Setting Service faild: " + service.getUUID().toString());
		}
		
		// Retrieved manually by the services
		
		if (device_name != null) pe.sendDeviceName(device_name); // make sure is sent before sending adv data
		if (appearance_value >= 0) pe.setAppearanceValue(appearance_value);
		
		// Setup advertising packet
		try {
			
			if (adv_data != null)
			{
				
				for(BLEService adv_service : adv_service_list_16_comp)
					adv_data.addServiceUUIDComplete(adv_service);
				for(BLEService adv_service : adv_service_list_128_comp)
					adv_data.addServiceUUIDComplete(adv_service);
				for(BLEService adv_service : adv_service_list_16_inc)
					adv_data.addServiceUUIDIncomplete(adv_service);
				for(BLEService adv_service : adv_service_list_128_inc)
					adv_data.addServiceUUIDIncomplete(adv_service);
				for(BLEService adv_service : adv_service_list_16_sol)
					adv_data.addServiceUUIDSolicitated(adv_service);
				for(BLEService adv_service : adv_service_list_128_sol)
					adv_data.addServiceUUIDSolicitated(adv_service);
			}
			
			if (scan_data != null)
			{
				for(BLEService sservice : scn_service_list_16_comp)
					scan_data.addServiceUUIDComplete(sservice);
				for(BLEService sservice : scn_service_list_128_comp)
					scan_data.addServiceUUIDComplete(sservice);
				for(BLEService sservice : scn_service_list_16_inc)
					scan_data.addServiceUUIDIncomplete(sservice);
				for(BLEService sservice : scn_service_list_128_inc)
					scan_data.addServiceUUIDIncomplete(sservice);
				for(BLEService sservice : scn_service_list_16_sol)
					scan_data.addServiceUUIDSolicitated(sservice);
				for(BLEService sservice : scn_service_list_128_sol)
					scan_data.addServiceUUIDSolicitated(sservice);
			}
			
			if (adv_data != null)
				Mitm.targetAdvertisementData(adv_data);
			if (scan_data != null)
				Mitm.targetScanData(scn_data);
			
			if (adv_data != null)
				pe.sendAdvertisementData(adv_data);
			if (scan_data != null)
				pe.sendScanData(scan_data);
			
		}catch(Exception ex)
		{
			System.err.println("Cannot set advertisement/scan data: " + ex.getMessage()); // does not matter, continue...
			System.exit(1);
		}
		
		if (erase_pe_keys)
			pe.eraseBonds();
		
		pe.finishSetup();
		
		// Display Services
		System.out.println("Local Services");
		displayServices(new_services);
		
		System.out.println("Peripheral Setup Finished - Advertisement Started");
	}
	
	public static void saveServicesToFile(String address, List<BLEService> services) throws Exception
	{
		FileOutputStream fout = new FileOutputStream(new File(address.replace(':', '_') + "_device.json"));
		BLEService service = null;
		Iterator<BLEService> iterator = services.iterator();
		
		JSONObject device = new JSONObject();
		JSONObject services_json = new JSONObject();
		
		while(iterator.hasNext())
		{
			service = iterator.next();
			JSONArray chars = service.serializeCharacteristics();
			JSONObject chars_service = new JSONObject();
			chars_service.put("characteristics", chars);
			chars_service.put("service_id", String.valueOf(service.getServiceId()));
			chars_service.put("service_handle", String.valueOf(service.getHandle()));
			services_json.put(service.getUUID().toString(), chars_service);
		}
		device.put("services", services_json);
		fout.write(device.toString().getBytes());
	}
	
	public static List<BLEService> loadServicesFromFile(String address) throws Exception
	{
		List<BLEService> services = new ArrayList<BLEService>();
		File file = new File(address.replace(':', '_') + "_device.json");
		DataInputStream fin = new DataInputStream(new FileInputStream(file));
		byte[] fileData = new byte[(int)file.length()];
		fin.readFully(fileData);
		JSONObject device = new JSONObject(new String(fileData));
		JSONObject services_json = device.getJSONObject("services");
		Iterator<String> uuids = services_json.keys();
		while(uuids.hasNext())
		{
			String uuid = uuids.next();
			JSONObject chars_service = services_json.getJSONObject(uuid);
			String service_id = chars_service.getString("service_id");
			short service_handle = (short) chars_service.getInt("service_handle");
			JSONArray characteristics_json = chars_service.getJSONArray("characteristics");
			BLEService service = new BLEService(uuid, service_id);
			service.setHandle(service_handle);
			service.loadCharacteristicsFromJSON(characteristics_json);
			services.add(service);
		}
		return services;
	}
	
	public static void displayServices(List<BLEService> services)
	{
		if(services != null)
		{
			for(BLEService srv : services)
			{
				System.out.print(" (ID:"+srv.getServiceId()+")");
				System.out.print(" "+srv.getUUID());
				System.out.println(" H: "+srv.getHandle());
				
				for(BLECharacteristic characteristic : srv.getAllCharacteristics())
				{
					System.out.println("\t" + " (ID:"+ characteristic.getCharacteristicId() +") " + 
										characteristic.getUUID().toString() + 
										" (" + (characteristic.isReadEnabled() ? "R," : "") + (characteristic.isWriteCMDEnabled() ? "WCMD," : "") + (characteristic.isWriteEnabled() ? "W," : "") + (characteristic.isNotificationAuthorized() ? "N," : "") + (characteristic.isIndicationAuthorized() ? "I" : "") + ")"+
										(characteristic.isCCCDEnabled() ? " CCCD:" + String.valueOf(characteristic.getCCCDHandle()) : "") +
										" H: " + characteristic.getValueHandle() );
				}
			}
		}
	}
	
	// clone is needed as different handles will be assigned to each characteristic and service (different handles by real PE and emulated PE)
	private static List<BLEService> cloneServices(List<BLEService> services) throws IOException
	{
		List<BLEService> cloned_services = new ArrayList<BLEService>();
		for(BLEService service : services)
			cloned_services.add(service.cloneService());
		return cloned_services;
	}
	
	public static String printableAdvData(AdvertisementData data) {
		StringBuilder strbld = new StringBuilder();
		if (data.isLEOnlySupported())
			strbld.append("+ LE Only" + System.lineSeparator());
		if (data.isInGeneralDiscoverableMode())
			strbld.append("+ LE General Discovery" + System.lineSeparator());
		else if (data.isInLimitedDiscoverableMode())
			strbld.append("+ LE Limited Discovery" + System.lineSeparator());
		
		if (data.isDeviceNameIncluded())
			strbld.append("Device Name: " + BBMFramework.getDeviceName() + System.lineSeparator());
		if (data.getSlaveConnectionIntMin() > 0)
			strbld.append("Slave Connection Interval Min " + data.getSlaveConnectionIntMin() + System.lineSeparator() + "Slave Connection Interval Max " + data.getSlaveConnectionIntMax() + System.lineSeparator());
		
		if (data.getServiceUUIDComplete().size() > 0)
		{
			strbld.append("Service UUID - Complete List" + System.lineSeparator());
			ArrayList<BLEService> list = data.getServiceUUIDComplete();
			for(BLEService service : list)
				strbld.append(service.getUUID().toString() + System.lineSeparator());
		}
		if (data.getServiceUUIDIncomplete().size() > 0)
		{
			strbld.append("Service UUID - Incomplete List" + System.lineSeparator());
			ArrayList<BLEService> list = data.getServiceUUIDIncomplete();
			for(BLEService service : list)
				strbld.append(service.getUUID().toString() + System.lineSeparator());
		}
		if (data.getServiceUUIDSolicitated().size() > 0)
		{
			strbld.append("Service UUID - Solicitation List" + System.lineSeparator());
			ArrayList<BLEService> list = data.getServiceUUIDSolicitated();
			for(BLEService service : list)
				strbld.append(service.getUUID().toString() + System.lineSeparator());
		}
		if (data.isTxPowerIncluded())
			strbld.append("TxPower: " + data.getTxPower()  + System.lineSeparator());
		
		if (data.isAddressIncluded())
			strbld.append("BDADDR: " + BBMFramework.getTargetAddress()  + System.lineSeparator());
		
		if (data.getManfucaturerData() != null)
		{
			ManufacturerData mdata = data.getManfucaturerData();
			byte[] mauf_data = mdata.getData();
			short company_identifier = mdata.getIdentifier();
			strbld.append("CompanyIdentifier: " + String.format("0x%04x", company_identifier) + " data: " + CryptoHelper.bytesToHex(mauf_data) + System.lineSeparator());
		}
		
		if (data.isAppearanceIncluded())
			strbld.append("Appear as: " + ConnectionTypesCommon.getAppearanceDescription(BBMFramework.getAppearanceValue()) + System.lineSeparator());
		
		if (data.getServiceData().size() > 0)
		{
			for(ServiceData sdata : data.getServiceData())
			{
				byte[] uuid = sdata.getUUID();
				strbld.append("Service Data: " + String.format("0x%02x%02x",uuid[1], uuid[0]) + " - " + CryptoHelper.bytesToHex(sdata.getServiceData()) + System.lineSeparator());
			}
			
		}
		
		return strbld.toString();
	}
}
















// the handle that has the remote device (real device) will be different than handles set by our peripheral
// The mapper is used to map the UUID with the handle of the remote (connected real PE) or local (emulated PE) device 
class Mapper
{
	private static List<BLEService> remote_services; // CE
	private static List<BLEService> local_services;  // PE
	public static void setRemoteServices(List<BLEService> rs)
	{
		Mapper.remote_services = rs;
	}
	public static void setLocalServices(List<BLEService> ls)
	{
		Mapper.local_services = ls;
	}
	
	// REMOTE
	
	public static short mapLocalRemoteCharacteristicValueHandle(String uuid)
	{
		return mapRemoteCharacteristic(uuid, true);
	}
	
	public static short mapLocalRemoteCharacteristicCCCDHandle(String uuid)
	{
		return mapRemoteCharacteristic(uuid, false);
	}
	
	// retrieve remote handle based on UUID
	private static short mapRemoteCharacteristic(String local_uuid, boolean is_value_handle)
	{
		short handle = -1;
		for(BLEService service : remote_services)
		{
			ArrayList<BLECharacteristic> characteristics = service.getAllCharacteristics();
			for(BLECharacteristic characteristic : characteristics)
			{
				if (characteristic.getUUID().toString().equals(local_uuid))
				{
					if (is_value_handle)
						handle = characteristic.getValueHandle();
					else
						handle = characteristic.getCCCDHandle();
				}
			}
		}
		return handle;
	}
	
	public static String retrieveRemoteCharacteristicUUID(short handle) {
		for(BLEService service : remote_services)
		{
			ArrayList<BLECharacteristic> characteristics = service.getAllCharacteristics();
			for(BLECharacteristic characteristic : characteristics)
			{
				if (characteristic.getValueHandle() == handle || characteristic.getCCCDHandle() == handle)
					return characteristic.getUUID().toString();
			}
		}
		return null;
	}
	
	// LOCAL
	
	public static short mapRemoteLocalCharacteristicValueHandle(String uuid)
	{
		return mapLocalCharacteristic(uuid, true);
	}
	
	public static short mapRemoteLocalCharacteristicCCCDHandle(String uuid)
	{
		return mapLocalCharacteristic(uuid, false);
	}
	
	private static short mapLocalCharacteristic(String remote_uuid, boolean is_value_handle)
	{
		/** retrieve local handle based on UUID **/
		
		short handle = -1;
		for(BLEService service : local_services)
		{
			ArrayList<BLECharacteristic> characteristics = service.getAllCharacteristics();
			for(BLECharacteristic characteristic : characteristics)
			{
				if (characteristic.getUUID().toString().equals(remote_uuid))
				{
					if (is_value_handle)
						handle = characteristic.getValueHandle();
					else
						handle = characteristic.getCCCDHandle();
				}
			}
		}
		return handle;
	}
	
	public static String retrieveLocalCharacteristicUUID(short handle) {
		for(BLEService service : local_services)
		{
			ArrayList<BLECharacteristic> characteristics = service.getAllCharacteristics();
			for(BLECharacteristic characteristic : characteristics)
			{
				if (characteristic.getValueHandle() == handle || characteristic.getCCCDHandle() == handle)
					return characteristic.getUUID().toString();
			}
		}
		return null;
	}
}



class MyCENotificationEventCallback implements CENotificationEventCallback {
	private PEController pe;
	MyCENotificationEventCallback(PEController pe)
	{
		this.pe = pe;
	}
	
	@Override
	public void notificationReceived(BLECharacteristic characteristic, byte[] data, int data_len) {
		try {
			
			int send_data_len = Mitm.notificationCapture(characteristic, data, data_len);
			
			if (!pe.sendNotification(Mapper.mapRemoteLocalCharacteristicCCCDHandle(characteristic.getUUID().toString()), data, send_data_len))
				System.err.println("Failed to send notification");
			
		}catch(IOException ex) {
			System.err.println(ex.getMessage());
		}
	}
	
	@Override
	public void notificationReceivedRaw(short remoteHandle, byte[] data, int data_len) {
		// We cannot find the corelated characteristic in simPE if we don't get the UUID. If raw notif received it means we don't have the characteristic.
		System.err.println("raw notif received: Cannot correlate handle with unknown characteristic");
	}
}


class MyReadCallback implements PEReadCallback {
	private CEController ce;
	private byte[] buffer;
	MyReadCallback(CEController ce)
	{
		this.ce = ce;
		buffer = new byte[31];
	}
	
	@Override
	public boolean authorizeRead(BLECharacteristic characteristic, byte[] data, int data_len,
			AuthorizedData authorized_data) {
			try {
				int read = ce.readData(buffer, 0, 31, Mapper.mapLocalRemoteCharacteristicValueHandle(characteristic.getUUID().toString()));
				if(read > 0)
				{
					// only warning as some devices does return 0 length in data
					//System.err.println("WARNING: Reading 0 data from CE");
				}
				
				read = Mitm.readCapture(characteristic, buffer, read);
				authorized_data.setAuthorizedData(buffer, read);
				
				return true;
			
			}catch(IOException ex)
			{
				System.err.println(ex.getMessage());
				return false;
			}
	}
	
	@Override
	public void readEvent(BLECharacteristic characteristic, byte[] data, int data_len) {
		// Never Used as all requests are set to be authorised
	}
	
}

class MyNotificationDataCallback implements PENotificationDataCallback {
	private CEController ce;
	MyNotificationDataCallback(CEController ce)
	{
		this.ce = ce;
	}
	
	@Override
	public void notification_event(BLECharacteristic char_used, NotificationValidation validation) {
			if (validation == NotificationValidation.NOTIFICATION_ENABLED)
				ce.enableNotifications(Mapper.mapLocalRemoteCharacteristicCCCDHandle(char_used.getUUID().toString()));
			else if (validation == NotificationValidation.INDICATION_ENABLED)
				ce.enableIndications(Mapper.mapLocalRemoteCharacteristicCCCDHandle(char_used.getUUID().toString()));
			else if (validation == NotificationValidation.NOTIF_INDIC_DISABLED)
				ce.disableIndications(Mapper.mapLocalRemoteCharacteristicCCCDHandle(char_used.getUUID().toString())); // Disabling notification or indications is the same value 0x0000
	}
	
};

class MyPEWriteEventCallback implements PEWriteEventCallback {
	private CEController ce;
	
	MyPEWriteEventCallback(CEController ce)
	{
		this.ce = ce;
	}
	
	@Override
	public void writeEvent(BLECharacteristic characteristic, byte[] data, int data_size, boolean is_cmd, short handle) {
		try {
			short mapped_handle = 0;
			if (characteristic.getValueHandle() == handle)
				mapped_handle = Mapper.mapLocalRemoteCharacteristicValueHandle(characteristic.getUUID().toString());
			else
				mapped_handle = Mapper.mapLocalRemoteCharacteristicCCCDHandle(characteristic.getUUID().toString());
			
			data_size = Mitm.writeCapture(characteristic, data, data_size, is_cmd, mapped_handle);
			
			if (!is_cmd)
			{
				ce.writeData(data, 0, data_size, mapped_handle);
			}
			else
			{
				ce.writeDataCMD(data, 0, data_size, mapped_handle);
			}
			
		}catch(IOException ex) {
			System.err.println(ex.getMessage());
		}
	}
	
};

class AdvertisementHandler implements CEAdvertisementCallback {
	private CEController ce = null;
	private String mac_addr;
	private Object evt;
	private AdvertisementData adv_data = null;
	private AdvertisementData scn_data = null;
	private boolean scan_data_received = false;
	private boolean adv_data_received = false;
	private boolean wait_for_scan_data = false;
	
	AdvertisementHandler(CEController ce, String mac_addr, Object event, boolean wait_for_scan_data)
	{
		this.ce = ce;
		this.mac_addr = mac_addr;
		this.evt = event;
		this.wait_for_scan_data = wait_for_scan_data;
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
	
	private AdvertisementData parseData(byte[] ad, int ad_len, boolean is_adv_packet) {
		byte[] uuid_temp = new byte[2];
		byte[] uuid_temp16 = new byte[16];
		byte[] temp_buffer = new byte[31];
		AdvertisementData data = new AdvertisementData();
	    int position = 0;
	    
	    /*
	    while (position < ad_len) 
	    {
			int len = ((int) ad[position]) & 0xff;
			int type = ((int) ad[position + 1]) & 0xff;
		    
		    System.out.print("RAW ADV: ");
		    System.out.println("P:" + position + " B:" + len);
		    for(int i=position; i<position+len+1; ++i)
		    {
		    	
		    	System.out.print(String.format("%02x", ad[i]));
		    }
		    System.out.println();
		    position = position + len + 1;
	    }
	    position = 0;
	    */
	    
	    while (position < ad_len) 
	    {
	      int len = ((int) ad[position]) & 0xff;
	      int type = ((int) ad[position + 1]) & 0xff;
	      
	      //System.err.println("Checking Type... " + String.format("0x%02x", type) + " len: " + len);
	      // Set TX Power
	      if (type == 0x0a && len == 2)
	      {
	    	  //System.err.println("Setting TX Power...");
	    	  byte tx_power = ad[position + 2];
	    	  data.setTXPower(tx_power);
	      }
	      
	      // Set Flags
	      if (type == 0x01 && len == 2)
	      {
	    	  byte flags = ad[position + 2];
	    	  //System.err.println("Setting Flags..." + String.format("0x%02x", flags));
	    	  data.setFlags(flags);
	      }
	      
	      // Set Appearance
	      if (type == 0x19 && len == 3)
	      {
	    	  //System.err.println("Setting Appearance...");
	    	  short appearance = (byte) (ad[position + 2] + (ad[position + 3] << 8));
	    	  BBMFramework.setAppearanceValue(appearance);
	      }
	      
	      // Set Incomplete 16-bit Service UUIDs
	      if (type == 0x02 && (len-1) % 2 == 0 && (len-1) >= 2 && position + len < ad_len)
	      {
	    	  //System.err.println("Setting Incomplete 16bit Service UUIDs...");
	    	  for(int pos = position+2; pos < position+len; pos+=2) {
	    		  uuid_temp[1] = ad[pos];
	    		  uuid_temp[0] = ad[pos+1];
	    		  
	    		  if (is_adv_packet)
	    			  BBMFramework.advertiseIncomplete16UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp, false));
	    		  else
	    			  BBMFramework.advertiseScanIncomplete16UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp, false));
	    	  }
	      }
	      
	      // Set Complete 16-bit Service UUIDs
	      if (type == 0x03 && (len-1) % 2 == 0 && (len-1) >= 2 && position + len < ad_len)
	      {
	    	  //System.err.println("Setting Complete 16bit Service UUIDs...");
	    	  for(int pos = position+2; pos < position+len; pos+=2) {
	    		  uuid_temp[1] = ad[pos];
	    		  uuid_temp[0] = ad[pos+1];
	    		  if (is_adv_packet)
	    			  BBMFramework.advertiseComplete16UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp, false));
	    		  else
	    			  BBMFramework.advertiseScanComplete16UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp, false));
	    	  }
	      }
	      
	      // Set Solicitation 16-bit Service UUIDs
	      if (type == 0x14 && (len-1) % 2 == 0 && (len-1) >= 2 && position + len < ad_len)
	      {
	    	  //System.err.println("Setting Solicitation 16bit Service UUIDs...");
	    	  for(int pos = position+2; pos < position+len; pos+=2) {
	    		  uuid_temp[1] = ad[pos];
	    		  uuid_temp[0] = ad[pos+1];
	    		  if (is_adv_packet)
	    			  BBMFramework.advertiseSolicited16UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp, false));
	    		  else
	    			  BBMFramework.advertiseScanSolicited16UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp, false));
	    	  }
	      }
	      
	      // Set Complete 128-bit Service UUIDs
	      if (type == 0x07 && (len-1) % 16 == 0 && (len-1) >= 16)
	      {
	    	  //System.err.println("Setting Complete 128bit UUID...");
	    	  for(int pos = position+2; pos < position+len; pos+=16) {
	    		  // Copy one UUID
	    		  for(int p = pos, j=15; p<pos+16; ++p,--j) 
    				  uuid_temp16[j] = ad[p];
    			  
    			  if (is_adv_packet)
    				  BBMFramework.advertiseComplete128UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp16, true));
    			  else
    				  BBMFramework.advertiseScanComplete128UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp16, true));
	    	  }
	      }
	      
	      // Set Incomplete 128-bit Service UUIDs
	      if (type == 0x06 && (len-1) % 16 == 0 && (len-1) >= 16)
	      {
	    	  //System.err.println("Setting Incomplete 128bit...");
	    	  for(int pos = position+2; pos < position+len; pos+=16) {
	    		  // Copy one UUID
    			  for(int p = pos, j=15; p<pos+16; ++p,--j) 
    				  uuid_temp16[j] = ad[p];
    			  
    			  if (is_adv_packet)
    				  BBMFramework.advertiseIncomplete128UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp16, true));
    			  else
    				  BBMFramework.advertiseScanIncomplete128UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp16, true));
	    	  }
	      }
	      
	      // Set Solicited 128-bit Service UUIDs
	      if (type == 0x15 && (len-1) % 16 == 0 && (len-1) >= 16)
	      {
	    	  //System.err.println("Setting 128bit SOlicitation...");
	    	  for(int pos = position+2; pos < position+len; pos+=16) {
	    		  // Copy one UUID
	    		  for(int p = pos, j=15; p<pos+16; ++p,--j) 
    				  uuid_temp16[j] = ad[p];
    			  
    			  if (is_adv_packet)
    				  BBMFramework.advertiseSolicited128UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp16, true));
    			  else
    				  BBMFramework.advertiseScanSolicited128UUIDService(ConnectionTypesCommon.buildUUIDfromBytes(uuid_temp16, true));
	    	  }
	      }
	      
	      // Set Device Name
	      if (type == 0x09 || type == 0x08)
	      {
	    	  //System.err.println("Setting Devname...");
	    	  String devname = new String(ad,position+2,position+len-1);
	    	  //System.out.println("DEVNAME: " + devname + " " + devname.length());
	    	  BBMFramework.setDeviceName(devname);
	    	  if (type == 0x09)
	    		  try{ data.includeDeviceName(); } catch(Exception ex) {/* ignore */}
	    	  else
	    		  try{ data.includeDeviceShortName(devname.length()); } catch(Exception ex) {/* ignore */}
	      }
	      
	      // Set Device Address
	      if (type == 0x17 || type == 0x18)
	      {
	    	  //System.err.println("Setting DevAddress...");
	    	  data.includeDeviceAddress();
	      }
	      
	      // Set Manufacturer Data
	      if (type == 0xff)
	      {
	    	  //System.err.println("Setting ManufacturerData...");
	    	  if (position + 4 < position + len)
	    	  {
	    		  short manufacturer = (short) (((int)ad[position + 2] & 0xff) + ((((int)ad[position + 3]) & 0xff) << 8));
		    	  for(int pos = position + 4,j=0; pos < position+len; ++pos, ++j) 
		    		  temp_buffer[j] = ad[pos];
		    	  try { 
		    		  ManufacturerData mdata = new ManufacturerData(temp_buffer, len-3, manufacturer);
		    		  data.setManufacturerData(mdata);
		    	  }catch(Exception ex) {/*ignore*/}
	    	  }
	      }
	      
	      // Set Service Data
	      if (type == 0x16 && len >= 4)
	      {
    		  uuid_temp[0] = ad[position + 2];
    		  uuid_temp[1] = ad[position + 3];
    		  
    		  //System.err.println("Setting ServiceData...");
    		  for(int pos = position + 4,j=0; pos < position + len && j >= 0; ++pos, ++j)
    			  temp_buffer[j] = ad[pos];
    		  try {
    			  ServiceData sdata = new ServiceData(uuid_temp, temp_buffer, len - 3);
    			  data.addServiceData(sdata);
    		  }catch(IOException ioex) {}
	      }
	      
	      // Set Slave Connection Interval
	      if (type == 0x12 && len == 5)
	      {
	    	  //System.err.println("Setting Slave Connection Interval...");
	    	  short min = (short)(((((int)ad[position + 3]) & 0xff) << 8) + (((int)ad[position + 2]) & 0xff));
	    	  short max = (short)(((((int)ad[position + 5]) & 0xff) << 8) + (((int)ad[position + 4]) & 0xff));
	    	  try {
	    		  data.setSlaveConnectionIntTU(min, max);
	    	  }catch(IOException ex) {System.err.println("Failed to parse Slave Connection Interval Range");}
	      }
	      
	      position += len + 1;
	    }
	    
		return data;
	}
	

	
	@Override
	public void advertisementPacket(byte[] address, byte address_type, byte scan_adv,
			byte advertisement_type, byte rssi, byte datalen, byte[] data) 
	{
		String raddr = ConnectionTypesCommon.addressToStringFormat(address).toLowerCase();
		String raddr_type = "";
		boolean is_adv = (scan_adv == 1 ? false : true);
		
		ConnectionTypesCommon.AddressType addr_type = ConnectionTypesCommon.getAddressTypeFromCode(address_type);
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
		}
		System.out.println("Found " + raddr + " Type: " + raddr_type);
		if (raddr.equals(mac_addr) && evt != null)
		{
			// Parse Packet Data
			AdvertisementData temp_data = null;
			BBMFramework.type_of_target_address = addr_type;
			if (!BBMFramework.parse_adv_data)
			{
				Mitm.advertisement(raddr, raddr_type, scan_adv, advertisement_type, rssi, data, datalen);
			}
			else
			{
				if (isValid(data, datalen))
				{
					temp_data = parseData(data, datalen, is_adv);
				} else {
					System.err.println("Malformed Advertisement Data Received - Do not parse data");
					Mitm.advertisement(raddr, raddr_type, scan_adv, advertisement_type, rssi, data, datalen);
				}
			}
			
			if (is_adv)
			{
				adv_data = temp_data;
				adv_data_received = true;
				//System.out.println("--- ADVERTISEMENT DATA ---");
			}
			else
			{
				scn_data = temp_data;
				scan_data_received = true;
				//System.out.println("--- SCAN DATA ---");
			}
			
			// Do not send adv data until scan data have been received
			if ((this.wait_for_scan_data == false) || (BBMFramework.wait_for_scan_data == true && adv_data_received == true && scan_data_received == true))
			{
				synchronized(evt) {evt.notify();}
				evt = null;
			}
			
		}
	}
	
	public AdvertisementData getAdvertisementData() {
		return adv_data;
	}
	
	public AdvertisementData getScanData() {
		return scn_data;
	}
};



