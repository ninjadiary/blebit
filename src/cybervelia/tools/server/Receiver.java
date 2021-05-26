package cybervelia.tools.server;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import org.json.JSONObject;

import cybervelia.sdk.controller.ce.CEController;
import cybervelia.sdk.controller.pe.PEController;
import cybervelia.server.CryptoHelper;

public class Receiver implements Runnable
{
	private Socket client_socket;
	private CEController ce;
	private PEController pe;
	private Transmitter transmitter;
	
	public Receiver(Socket client_socket, Transmitter transmitter) {
		this.client_socket = client_socket;
		this.transmitter = transmitter;
	}
	
	@Override
	public void run() {
		try {
			
			MitmActive.dinRx = new DataInputStream(client_socket.getInputStream());
			
			int rin = MitmActive.dinRx.readInt();
			System.out.println("Received RX config len: " + rin);
			JSONObject settings = new JSONObject(new String(MitmActive.dinRx.readNBytes(rin)));
			
			/* Configure PE and CE Device */
			MitmActive.setTargetAddress(settings.getString("target"));
			MitmActive.setPEDeviceAddress(settings.getString("bdaddr"));
			
			if (settings.getBoolean("loadServicesFromFile"))
				MitmActive.loadServicesFromFile();
			
			if (settings.getBoolean("autoParseAdvertisementData"))
				MitmActive.autoParseAdvertisementData();
			
			if (settings.getBoolean("waitForScanData"))
				MitmActive.waitForScanData();
			
			MitmActive.sendStatus("Setting up central");
			
			// It will block until the target is found.
			Thread mtimt = new Thread(new Runnable() {
				@Override
				public void run() {
					if (!MitmActive.startMitm())
						MitmActive.sendStatus("Proxy failed to start");
				}
			});
			mtimt.start();
			
			System.out.println("Settings applied OK");
			
			// Handle BLE Connections
			while(true) {
				rin = MitmActive.dinRx.readInt();
				JSONObject cmd_json = new JSONObject(new String(MitmActive.dinRx.readNBytes(rin)));
				mtimt.join(); // in case discovery is not over, but a message has arrived. When mitm isn't finished, pe object is not set, so wait for PEController object
				if (MitmActive.isShutdownInProgress())
				{
					transmitter.shutdown();
					return;
				}
				ce = MitmActive.getCEController();
				pe = MitmActive.getPEController();
				if (ce == null || pe == null) 
					throw new IOException("ce or pe null. This should not happen");
				String command = cmd_json.getString("cmd");
				System.out.println("Received CMD: " + command + " " + cmd_json.toString());
				if (command.equals("inject"))
				{
					byte[] data = CryptoHelper.hexStringToByteArray(cmd_json.getString("data"));
					short handle = (short) cmd_json.getInt("handle");
					String cmd_type = cmd_json.getString("type");
					
					System.out.println("RCV: " + cmd_json.toString());
					
					if (cmd_type.equals("READ"))
					{
						byte[] read_data = new byte[31];
						if (ce.readData(read_data, 0, read_data.length, handle) > 0)
							System.out.println("Inject-READ OK: " + cmd_json.getString("data"));
						else
							System.err.println("Inject-READ Failed: " + cmd_json.toString());
					}
					else if (cmd_type.equals("WRITE"))
					{
						System.out.println("CEObject:" + ce);
						if (ce.writeData(data, 0, data.length, handle))
							System.out.println("Inject-WRITE OK: " + cmd_json.getString("data"));
						else
							System.err.println("Inject-WRITE Failed: " + cmd_json.toString());
					}
					else if (cmd_type.equals("WRITECMD"))
					{
						System.out.println("T-WRITECMD");
						ce.writeDataCMD(data, 0, data.length, handle);
						System.out.println("WRITECMD Send (unverified): " + cmd_json.getString("data"));
					}
					else if (cmd_type.equals("NOTIF"))
					{
						if (pe.sendNotification(handle, data, data.length))
							System.out.println("Inject-NOTIF OK: " + cmd_json.getString("data"));
						else
							System.err.println("Inject-NOTIF Failed: " + cmd_json.toString());
					}
					else
						System.err.println("Malformed Inject Packet: " + cmd_json.toString());
				}
			}
			
		}catch(Exception ex) {
			System.err.println("Handler of RX Channel: " + ex.getMessage());
			transmitter.shutdown();
			return;
		}
	}
	
}