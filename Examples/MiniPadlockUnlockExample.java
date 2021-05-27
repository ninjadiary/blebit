import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fazecast.jSerialComm.SerialPort;
import com.sun.tools.javac.util.ByteBuffer;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEConnectionParameters;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.ce.callbacks.CEAdvertisementCallback;
import cybervelia.sdk.controller.ce.callbacks.CEBondCallback;
import cybervelia.sdk.controller.ce.callbacks.CENotificationEventCallback;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class MiniPadlockUnlockExample {
	
	static String target = "10:08:2c:3f:9d:7c";
	static byte[] spin = {48, 48, 49, 50, 51, 48}; // could by any static pin
	static CEController ce = null;
	static boolean debugging = false;
	
	public static void main(String... args) 
	{
		try 
		{
			// Handle events
			CEBLEDeviceCallbackHandler devCallback = getCallbackHandler();
			
			// Find BLE:Bit Central
			String[] fports = findPorts();
			if (fports.length < 1) {
				System.err.println("BLE:Bit Central not found");
				System.exit(1);
			}
			
			ce = new CEController(fports[0], devCallback);
			
			if (!ce.isInitializedCorrectly())
			{
				System.err.println("You have provided a BLE:Bit PE but this program needs a central controller. Provide central directly or remove the BLE:Bit Peripheral");
				System.exit(1);
			}
			
			// Setup device
			CEConnectionParameters con_params = new CEConnectionParameters();
			con_params.setMinConnectionIntervalMs(10);
			con_params.setMaxConnectionIntervalMs(15);
			ce.sendConnectionParameters(con_params);
			ce.sendBluetoothDeviceAddress("ff:55:ee:fe:4a:af", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			ce.configurePairing(ConnectionTypesCommon.PairingMethods.KEYBOARD, null);
			ce.finishSetup();
			
			System.out.println("Device started OK");
			System.out.println("Requesting for " + target );
			
			if (ce.connectNow(target, ConnectionTypesCommon.AddressType.PUBLIC_ADDR)){
				
				System.out.println("Connected to " + target);
				
				// Enumerate services
				List<BLEService> services = startDiscovery(ce);
				
				// Enable all supported notif-characteristics
				short notifications[] = new short[] {getCharacteristicCCCDHandle(services,"0000ffd6-0000-1000-8000-00805f9b34fb"), 
						getCharacteristicCCCDHandle(services,"0000ffd4-0000-1000-8000-00805f9b34fb"), 
						getCharacteristicCCCDHandle(services,"0000ffdf-0000-1000-8000-00805f9b34fb"), getCharacteristicCCCDHandle(services,"0000ffdd-0000-1000-8000-00805f9b34fb")};
				enableNotifications(notifications);
				
				// Prepare data
				ByteBuffer buffer = new ByteBuffer();
				buffer.appendBytes(new byte[] {(byte) 41, (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 1, (byte) 2, (byte) 40});
				
				// Print TX data
				System.out.print("Sending: ");
				for(int i = 0; i<6; ++i)
					System.out.print(String.format("%02x", buffer.elems[i]));
				System.out.println();
				
				/** Authenticate and Unlock the padlock **/
				
				// Authenticate (Padlock's Default Password: 123412)
				if (!ce.writeData(buffer.elems, 0, buffer.length, getCharacteristicValueHandle(services, "0000ffd9-0000-1000-8000-00805f9b34fb")))
					System.err.println("Write failed");
				
				// Open lock
				buffer.reset();
				buffer.appendBytes(new byte[]{(byte) -2, (byte) 79, (byte) 80, (byte) 69, (byte) 78, (byte) 0, (byte) 0, (byte) 0, (byte) -16, (byte) -3});
				
				if (!ce.writeData(buffer.elems, 0, buffer.length, getCharacteristicValueHandle(services, "0000ffd9-0000-1000-8000-00805f9b34fb")))
					System.err.println("Write failed");
				
				
			}
				
		}catch(IOException ex) {
			System.err.println("Exception in main catched: " + ex.getMessage());
		}
		
	}
	
	private static CEBLEDeviceCallbackHandler getCallbackHandler() {
		CEBLEDeviceCallbackHandler devCallback = new CEBLEDeviceCallbackHandler();
		
		// Install Advertisement Receiver Callback
		CEAdvertisementCallback adv_callback = new CEAdvertisementCallback()  {
			@Override
			public void advertisementPacket(byte[] address, byte address_type, byte scan_adv,
					byte advertisement_type, byte rssi, byte datalen, byte[] data) {
				
				System.out.println(ConnectionTypesCommon.addressToStringFormat(address) + ConnectionTypesCommon.getAddressTypeFromCode(address_type));
			}
		};
		devCallback.installAdvertisementCallback(adv_callback);
		
		
		devCallback.installNotificationEventCallback(new CENotificationEventCallback() {

			@Override
			public void notificationReceived(BLECharacteristic characteristic, byte[] data, int data_len) {
				System.out.print("Data From " + characteristic.getUUID().toString() + " ");
				for(int i = 0; i < data_len; ++i)
					System.out.print(String.format("%02x", data[i]));
				System.out.println();
			}
			
			@Override
			public void notificationReceivedRaw(short handle, byte[] data, int data_len) {
				System.out.print("Data From " + String.valueOf(handle) + " ");
				for(int i = 0; i < data_len; ++i)
					System.out.print(String.format("%02x", data[i]));
				System.out.println();
			}
			
		});
		
		
		
		// Setup callback handlers
		CEBondCallback cebondcallback = new CEBondCallback() {
		
				@Override
				public byte[] getPIN() {
					String upin = new String(spin);
					int ipin = Integer.parseInt(upin);
					ipin += 1;
					String fpin = String.format("%06d", ipin);
					for(int i=0; i<6; ++i)
						spin[i] = (byte) fpin.charAt(i);
					System.out.println("PIN Provided: " + fpin);
					return spin;
				}

				@Override
				public void authStatus(int status, int peers_fault) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void bondingSucceed(int precedure) {
					System.err.println("Bond Succeeded");
					
				}

				@Override
				public void bondingFailed(short error, int bond_error_src) {
					System.err.println("Bond Failed");
				}
				
				@Override
				public void peerBondDeleteError() {
					// TODO Auto-generated method stub
					
				}
				
			};
			
		devCallback.installBondCallback(cebondcallback);
		
		return devCallback;
	}
	
	private static void enableNotifications(short handles[]) {
		for(short handle: handles) {
			ce.enableNotifications(handle);
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
			System.out.println("BLE:Bit device at " + ports[i]);
		}
		
		return ports;
	}
		
		
		
		public static List<BLEService> startDiscovery(CEController ce)
		{
			if (ce.startDiscovery(true))
				System.out.println("Discovery Done");
			else
				System.err.println("Discovery Failure");
			
			BLECharacteristic foundchr = null;
			List<BLEService> services = ce.getDiscoveredServices();
			if(services != null)
			{
				for(BLEService srv : services)
				{
					System.out.print(srv.is_128bit_uuid() + " ("+srv.getServiceId()+") ");
					System.out.print(" Handle: "+srv.getHandle());
					System.out.println(" "+srv.getUUID());
					
					for(BLECharacteristic characteristic : srv.getAllCharacteristics())
					{
						if (characteristic.getValueHandle() == 14)
							foundchr = characteristic;
						
						System.out.println("\t" + characteristic.is_128bit_uuid() + " ("+ characteristic.getCharacteristicId() +") " + 
											characteristic.getUUID().toString() + 
											" (" + (characteristic.isReadEnabled() ? "R," : "") + (characteristic.isWriteCMDEnabled() ? "WCMD," : "") + (characteristic.isWriteEnabled() ? "W," : "") + (characteristic.isNotificationAuthorized() ? "N," : "") + (characteristic.isIndicationAuthorized() ? "I" : "") + ")"+
											(characteristic.isCCCDEnabled() ? " CCCD:" + String.valueOf(characteristic.getCCCDHandle()) : "") +
											" Handle: " + characteristic.getValueHandle() );
					}
				}
			}
			return services;
			
		}
		
		public static short getCharacteristicCCCDHandle(List<BLEService> services, String uuid) {
			short handle = 0;
			if (services == null)
				return -1;
			
			for(BLEService service : services) {
				for(BLECharacteristic ch : service.getAllCharacteristics())
				{
					if (ch.getUUID().toString().equals(uuid))
					{
						handle = ch.getCCCDHandle();
						break;
					}
				}
			}
			
			return handle;
		}
		
		public static short getCharacteristicValueHandle(List<BLEService> services, String uuid) {
			short handle = 0;
			if (services == null)
				return -1;
			
			for(BLEService service : services) {
				for(BLECharacteristic ch : service.getAllCharacteristics())
				{
					if (ch.getUUID().toString().equals(uuid))
					{
						handle = ch.getValueHandle();
						break;
					}
				}
			}
				
			return handle;
		}
		
		public static short getServiceHandle(List<BLEService> services, String uuid) {
			short handle = 0;
			if (services == null)
				return -1;
			
			for(BLEService service : services) {
				if (service.getUUID().toString().equals(uuid))
				{
					handle = service.getHandle();
					break;
				}
			}
				
			return handle;
		}
		
}
