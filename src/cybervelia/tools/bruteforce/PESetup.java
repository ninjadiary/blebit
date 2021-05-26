package cybervelia.tools.bruteforce;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import com.fazecast.jSerialComm.SerialPort;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEConnectionParameters;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.pe.AdvertisementData;
import cybervelia.sdk.controller.pe.ManufacturerData;
import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.PEConnectionParameters;
import cybervelia.sdk.controller.pe.PEController;
import cybervelia.sdk.controller.pe.ServiceData;
import cybervelia.sdk.controller.pe.callbacks.BondingCallback;
import cybervelia.sdk.types.BLEAttributePermission;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesPE;

public class PESetup {
	static String custom_ftdi = "Silicon Labs CP210x USB to UART Bridge";
	static String nordic_ftdi = "ftdi_sio";
	static String prolific_ftdi = "pl2303";
	
	private static String startComm(String device)
	{
		SerialPort[] sp = SerialPort.getCommPorts();
		String com_port = null;
		for(SerialPort s : sp)
		{
			System.out.println(s.getDescriptivePortName());
			 if (s.getDescriptivePortName().indexOf(device) >= 0)
			 {
				 com_port = s.getSystemPortName();
			 }
		}
		
		System.out.println("PE Opening: " + com_port);
		
		if (com_port == null)
		{
			System.err.println("COM Port does not exist");
			System.exit(1);
		}
		
		return com_port;
	}
	
	public static void main(String[] args) 
	{	
		setupPINProtectedProfile(startComm(nordic_ftdi));
		
		/*
		String com_port_peripheral = startComm(nordic_ftdi);
		String com_port_central = startComm(prolific_ftdi);
		startCloningProfile(com_port_peripheral, com_port_central);
		*/
		
		//startPEFullProfile(startComm(nordic_ftdi));
	}
	
	
	
	
	
	private static void setupPINProtectedProfile(String com_port_peripheral)
	{
		try {
			PEBLEDeviceCallbackHandler mycallbackHandler = new PEBLEDeviceCallbackHandler();
			BondingCallback bcallback = new BondingCallback() {

				@Override
				public void authStatus(int status, int reason) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public byte[] getPIN() {
					System.err.println("PE-Provide 6-digit PIN Number:");
					byte[] pin = new byte[6];
					Scanner scn = new Scanner(System.in);
					String num = scn.nextLine();
					for(int i = 0; i<6; ++i)
						pin[i] = (byte) num.charAt(i);
					return pin;
				}
				
				@Override
				public void bondSuccess(int procedure) {
					System.err.println("Bond Success");
				}

				@Override
				public void bondFailure(short error, int bond_error_src) {
					System.err.println("Bond Failed");
				}

				@Override
				public void deletePeerBondRiseError() {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void deletePeerBondSuccess() {
					// TODO Auto-generated method stub
					
				}
				
			};
			
			mycallbackHandler.installBondingCallback(bcallback);
			
			PEController pe = new PEController(com_port_peripheral, mycallbackHandler);
			PEConnectionParameters con_params = new PEConnectionParameters();
			con_params.setMinConnectionIntervalMS(50);
			con_params.setMaxConnectionIntervalMS(60);
			pe.sendConnectionParameters(con_params);
			
			pe.sendDeviceName("HeartRateService");
			pe.setAppearanceValue((short)833);
			pe.configurePairing(ConnectionTypesCommon.PairingMethods.DISPLAY, "001234");
			
			
			pe.sendBluetoothDeviceAddress("ea:bb:cc:11:33:12", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			
			pe.eraseBonds();
			pe.deviceDisableRepairingAfterBonding();
			
			pe.disableAdvertisingChannels(ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39);
			
			
			
			// Add BLE Services
			BLEService heart_rate_service = new BLEService(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB").toString());
			
			// Create Advertisement Data
			AdvertisementData adv_data = new AdvertisementData();
			adv_data.includeDeviceShortName(3);
			adv_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE | AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
			adv_data.addServiceUUIDComplete(heart_rate_service);
			
			// Add BLE Characteristic Heart Beat Measurement
			byte[] value = new byte[10];
			for(int i = 0; i<10; ++i) value[i] = 0;
			String uuid_char = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB").toString();
			BLECharacteristic hr_measurement = new BLECharacteristic(uuid_char, value);
			hr_measurement.enableRead();
			//hr_measurement.enableWriteCMD();
			hr_measurement.enableWrite();
			hr_measurement.setMaxValueLength(31);
			hr_measurement.setValueLengthVariable(true);
			hr_measurement.enableNotification();
			hr_measurement.setAttributePermissions(BLEAttributePermission.ENCRYPTION_MITM, BLEAttributePermission.ENCRYPTION_MITM);
			heart_rate_service.addCharacteristic(hr_measurement);
			
			pe.sendBLEService(heart_rate_service);
			
			pe.sendAdvertisementData(adv_data);
			
			pe.finishSetup();
			
			System.out.println("CCCD: " + hr_measurement.getCCCDHandle());
			System.out.println("Val.H: " + hr_measurement.getValueHandle());
			
			boolean bb = false;
			while(true)
			{
				/*
				bb = false;
				while(pe.isDeviceConnected())
				{
					if (bb == false)
					{
						pe.bondNow(true);
						bb = true;
					}
					
				}*/
			}
			
			
			
			
		}catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private static void startCloningProfile(String com_port_peripheral, String com_port_central)
	{
		try {
			System.out.println("Start Cloning Profile...");
			// Start Central
			CEBLEDeviceCallbackHandler ce_callback_handler = new CEBLEDeviceCallbackHandler();
			CEController ce = new CEController(com_port_central, ce_callback_handler);
			ce.sendConnectionParameters(new CEConnectionParameters());
			ce.sendBluetoothDeviceAddress("ff:55:ee:fe:4a:af", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			ce.configurePairing(ConnectionTypesCommon.PairingMethods.NO_IO, null);
			ce.eraseBonds();
			ce.finishSetup();
			System.out.println("Preparing for connection");
			if (ce.connectNow("e9:8D:D4:52:A0:59", ConnectionTypesCommon.AddressType.RANDOM_STATIC_ADDR))
				System.out.println("Connect error");
			else
				System.out.println("Connected!");
			if (!ce.startDiscovery(true))
				System.out.println("Discovery OK");
			else
				System.err.println("Discovery Fail to start");
			
			List<BLEService> services = ce.getDiscoveredServices();
			ArrayList<BLEService> new_services = new ArrayList<BLEService>();
			new_services.addAll(services);
			
			HashMap<BLECharacteristic, ByteBuffer> map = new HashMap<BLECharacteristic, ByteBuffer>();
			byte[] temp_buffer = new byte[31];
			for(Iterator<BLEService> iterator = new_services.iterator(); iterator.hasNext();)
			{
				
				BLEService service = iterator.next();
				
				ArrayList<BLECharacteristic> char_list = service.getAllCharacteristics();
				
				for(BLECharacteristic ch : char_list)
				{
					ByteBuffer buff = ByteBuffer.allocate(31);
					
					int init_val_len = ce.readData(temp_buffer, 0, 31, ch.getValueHandle());
					buff.put(temp_buffer, 0, init_val_len);
					
					map.put(ch, buff);
				}
			}
			
			ce.disconnect(19);
			
			// Start Peripheral
			PEBLEDeviceCallbackHandler mycallbackHandler = new PEBLEDeviceCallbackHandler();
			PEController pe = new PEController(com_port_peripheral, mycallbackHandler);
			pe.sendConnectionParameters(new PEConnectionParameters());
			pe.sendDeviceName("HeartRateService");
			pe.setAppearanceValue((short)833);
			pe.configurePairing(ConnectionTypesCommon.PairingMethods.NO_IO, null);
			pe.sendBluetoothDeviceAddress("ea:bb:cc:11:33:12", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			pe.disableAdvertisingChannels(ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39);
			
			BLEService service = null;
			Iterator<BLEService> iterator = new_services.iterator();
			
			while(iterator.hasNext())
			{
				System.out.println("Before iterator.next");
				service = iterator.next();
				
				
				ArrayList<BLECharacteristic> char_list = service.getAllCharacteristics();
				
				if (char_list.size() > 0)
				{
					for(BLECharacteristic ch : char_list)
					{
						System.out.println("Serving Characteristic " + ch.getUUID().toString());
						ByteBuffer buffer = map.get(ch);
						ch.setMaxValueLength(buffer.position()); // need to happen before setting a value
						System.out.println("Characteristic Max Value: " + ch.getMaxValueLength());
						ch.setInitialValue(buffer.array(), buffer.position());
						ch.setValueLengthVariable(false);
					}
				}
				
				
				if (pe.sendBLEService(service) == false)
					System.err.println("Service cannot be added " + String.format("%04x", service.getHandle()));
				
				
			}
			
			System.out.println("Setting Advertisement Data");
			AdvertisementData adv_data = new AdvertisementData();
			adv_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE | AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
			adv_data.includeDeviceShortName(2);
			adv_data.includeAppearance();
			
			System.out.println("Setting Last Setup Details");
			pe.sendAdvertisementData(adv_data);
			pe.eraseBonds();
			pe.finishSetup();
			System.out.println("Peripheral Setup Finished");
			
			System.out.println("Ready for reset - PRESS ENTER");
			Scanner scanner = new Scanner(System.in);
			scanner.nextLine();
			
			pe.terminate();
			ce.terminate();
			
		}catch(IOException ex)
		{
			System.err.println(ex.getMessage());
		}
	}
	
	
	private static void startPESimpleProfile(String com_port)
	{
		try {
			PEBLEDeviceCallbackHandler mycallbackHandler = new PEBLEDeviceCallbackHandler();
			
			PEController pe = new PEController(com_port, mycallbackHandler);
			
			pe.sendConnectionParameters(new PEConnectionParameters());
			
			pe.sendDeviceName("HeartRateService");
			pe.setAppearanceValue((short)833);
			pe.configurePairing(ConnectionTypesCommon.PairingMethods.NO_IO, null);
			//pe.sendAdvIntervalTU(300);
			
			pe.sendBluetoothDeviceAddress("ea:bb:cc:11:33:12", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			
			
			
			pe.disableAdvertisingChannels(ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39);
			
			
			
			// Add BLE Services
			BLEService heart_rate_service = new BLEService(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB").toString());
			
			// Create Advertisement Data
			AdvertisementData adv_data = new AdvertisementData();
			adv_data.includeDeviceShortName(3);
			adv_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE | AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
			adv_data.addServiceUUIDComplete(heart_rate_service);
			
			// Add BLE Characteristic Heart Beat Measurement
			byte[] value = new byte[10];
			for(int i = 0; i<10; ++i) value[i] = 0;
			String uuid_char = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB").toString();
			BLECharacteristic hr_measurement = new BLECharacteristic(uuid_char, value);
			hr_measurement.enableRead();
			//hr_measurement.enableWriteCMD();
			hr_measurement.enableWrite();
			hr_measurement.setMaxValueLength(31);
			hr_measurement.setValueLengthVariable(true);
			hr_measurement.enableNotification();
			hr_measurement.setAttributePermissions(BLEAttributePermission.OPEN, BLEAttributePermission.OPEN);
			heart_rate_service.addCharacteristic(hr_measurement);
			
			pe.sendBLEService(heart_rate_service);
			
			pe.sendAdvertisementData(adv_data);
			pe.eraseBonds();
			pe.finishSetup();
			
			System.out.println("CCCD: " + hr_measurement.getCCCDHandle());
			System.out.println("Val.H: " + hr_measurement.getValueHandle());
			
			System.out.println("Ready for reset - PRESS ENTER");
			Scanner scanner = new Scanner(System.in);
			scanner.nextLine();
			
			pe.terminate();
			
			
		}catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}	
	
		
		
		
		
		
	private static void startPEFullProfile(String com_port)
	{
		try {
			PEBLEDeviceCallbackHandler mycallbackHandler = new PEBLEDeviceCallbackHandler();
			
			PEController pe = new PEController(com_port, mycallbackHandler);
			
			pe.sendConnectionParameters(new PEConnectionParameters());
			
			pe.sendDeviceName("HeartRateService");
			pe.setAppearanceValue((short)833);
			pe.configurePairing(ConnectionTypesCommon.PairingMethods.NO_IO, null);
			//pe.sendAdvIntervalTU(300);
			
			
			
			pe.sendBluetoothDeviceAddress("ea:bb:cc:11:33:11", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			
			
			
			pe.disableAdvertisingChannels(ConnectionTypesPE.ADV_CH_38 | ConnectionTypesPE.ADV_CH_39);
			
			
			
			// Add BLE Services
			BLEService heart_rate_service = new BLEService(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB").toString());
			BLEService custom_service = new BLEService(UUID.randomUUID().toString());
			BLEService custom_service2 = new BLEService(UUID.randomUUID().toString());
			BLEService custom_service3 = new BLEService(UUID.randomUUID().toString());
			BLEService custom_service4 = new BLEService(UUID.randomUUID().toString());
			BLEService custom_service5 = new BLEService(UUID.randomUUID().toString());
			
			System.out.println(custom_service.getUUID().toString());
			System.out.println(custom_service2.getUUID().toString());
			System.out.println(custom_service3.getUUID().toString());
			System.out.println(custom_service4.getUUID().toString());
			System.out.println(custom_service5.getUUID().toString());
			
			// Create Advertisement Data
			AdvertisementData adv_data = new AdvertisementData();
			adv_data.includeDeviceShortName(3);
			adv_data.setFlags(AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE | AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED);
			adv_data.addServiceUUIDComplete(heart_rate_service);
			
			// Add BLE Characteristic Heart Beat Measurement
			byte[] value = new byte[10];
			for(int i = 0; i<10; ++i) value[i] = 0;
			String uuid_char = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB").toString();
			BLECharacteristic hr_measurement = new BLECharacteristic(uuid_char, value);
			hr_measurement.enableRead();
			hr_measurement.setMaxValueLength(31);
			hr_measurement.setValueLengthVariable(true);
			hr_measurement.enableNotification();
			hr_measurement.setAttributePermissions(BLEAttributePermission.ENCRYPTION, BLEAttributePermission.OPEN);
			heart_rate_service.addCharacteristic(hr_measurement);
			System.out.println(hr_measurement.getUUID().toString());
			
			// Add Custom Characteristic
			byte[] myval = new byte[] {1};
			BLECharacteristic custom_chr = new BLECharacteristic(UUID.randomUUID().toString(), myval);
			custom_chr.enableNotification();
			custom_chr.enableRead();
			custom_chr.enableWrite();
			custom_chr.setMaxValueLength(10);
			custom_service2.addCharacteristic(custom_chr);
			System.out.println(custom_chr.getUUID().toString());
			
			custom_chr = new BLECharacteristic(UUID.randomUUID().toString(), myval);
			custom_chr.enableNotification();
			custom_chr.enableRead();
			custom_chr.enableWrite();
			custom_chr.setMaxValueLength(10);
			custom_service2.addCharacteristic(custom_chr);
			System.out.println(custom_chr.getUUID().toString());
			
			custom_chr = new BLECharacteristic(UUID.randomUUID().toString(), myval);
			custom_chr.enableNotification();
			custom_chr.enableRead();
			custom_chr.enableWrite();
			custom_chr.setMaxValueLength(10);
			custom_service2.addCharacteristic(custom_chr);
			System.out.println(custom_chr.getUUID().toString());
			
			custom_chr = new BLECharacteristic(UUID.randomUUID().toString(), myval);
			custom_chr.enableNotification();
			custom_chr.enableRead();
			custom_chr.enableWrite();
			custom_chr.setMaxValueLength(10);
			custom_service2.addCharacteristic(custom_chr);
			System.out.println(custom_chr.getUUID().toString());
			
			custom_chr = new BLECharacteristic(UUID.fromString("00002231-0000-1000-8000-00805F9B34FB").toString(), myval);
			custom_chr.enableNotification();
			custom_chr.enableRead();
			custom_chr.enableWrite();
			custom_chr.setMaxValueLength(10);
			custom_service2.addCharacteristic(custom_chr);
			System.out.println(custom_chr.getUUID().toString());
			
			pe.sendBLEService(heart_rate_service);
			pe.sendBLEService(custom_service);
			pe.sendBLEService(custom_service2);
			pe.sendBLEService(custom_service3);
			pe.sendBLEService(custom_service4);
			pe.sendBLEService(custom_service5);
			
			pe.sendAdvertisementData(adv_data);
			pe.bondConnect(true);
			pe.eraseBonds();
			pe.finishSetup();
			
			
			/*
			while(true)
			{
				
				if(pe.isDeviceConnected())
				{
					System.out.println("Device Connected - Attempt to bond");
					if (pe.bondNow(true))
						System.err.println("Bond Failed");
					else
						System.out.println("Bond Succeeded");
					
					System.out.println("Device Connected & Bonded OK - wait");
					while(pe.isDeviceConnected())
					{
						try{Thread.sleep(1000);}catch(InterruptedException ex) {}
					}
					System.out.println("Device Disconnected - out of lookp");
				}
			}
			*/
			
			/*
			System.out.println("HRM Char Handle " + String.format("%d", hr_measurement.getValueHandle()) + " with CCCD Handle" + String.format("%d", hr_measurement.getCCCDHandle()));
			System.out.println("Custom Char Handle " + String.format("%d", custom_chr.getValueHandle()) + " with CCCD Handle" + String.format("%d", custom_chr.getCCCDHandle()));
			
			byte []rr_data = new byte[] {01, 0x30, 0x00};
			while(true)
			{
				if (pe.isDeviceConnected() && pe.sendNotification(hr_measurement.getValueHandle(), rr_data, 3))
				{
					rr_data[1] += 1;
				}
				try {Thread.sleep(1000);}catch(InterruptedException e) {}
			}
			*/
			
			/*
			try {Thread.sleep(10000);}catch(InterruptedException e) {}
			
			byte []data = new byte[4];
			int i = 0;
			int l = 0;
			while(true)
			{
				data[0] = (byte)i++;
				while(pe.updateValue(chr.getValueHandle(), data, data.length) == true) {try {Thread.sleep(1000);}catch(InterruptedException e) {} System.out.print(".");};
				l = pe.getCharacteristicValue(chr.getValueHandle(), data);
				for(int j = 0; j<l; ++j)
					System.out.print(String.format("%02x ", data[j]));
				System.out.println();
				//try {Thread.sleep(1000);}catch(InterruptedException e) {}
			}
			*/
			
		}catch(IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
