package cybervelia.sdk.controller.ce.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEDiscoveryCallback;

public class CEDiscoveryHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEDiscoveryCallback user_callback = null;
	private volatile boolean device_discovery_triggered = false;
	protected List<BLEService> services_list;
	protected boolean discovery_finished_with_success = false;
	protected boolean is_discovery_in_progress = false;
	protected boolean is_discovery_issued = false;
	private Object block_until_disc_done = new Object();
	
	public CEDiscoveryHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
		this.services_list = Collections.synchronizedList(new ArrayList<BLEService>());
	}
	
	public void setCallback(CEDiscoveryCallback callback)
	{
		this.user_callback = callback;
	}
	
	// Discovery
	public boolean startDiscovery(boolean block) {
		
		if (!parent_handler.isDeviceConnected()) return false;
		if (is_discovery_in_progress) return false;
		
		device_discovery_triggered = true;
		
		discovery_finished_with_success = false;
		is_discovery_in_progress = true;
		is_discovery_issued = true;
		
		if (block)
		{
			// Block
			try {
				synchronized(block_until_disc_done) 
				{
					block_until_disc_done.wait();
				}
			}catch(InterruptedException e) {}
			return discovery_finished_with_success;
		}
		else
			return true; // return success
	}
	
	public boolean isDiscoveryTriggered() {
		if(device_discovery_triggered)
		{
			device_discovery_triggered = false;
			return true;
		}
		else
			return false;
	}
	
	// checked
	public void discoveryDone(boolean discovery_success) {
		
		discovery_finished_with_success = discovery_success;
		is_discovery_in_progress = false;
		synchronized(block_until_disc_done) {block_until_disc_done.notify();}
		if (discovery_success == true)
			parent_handler.addDiscoveryServicesOnComplete(services_list);
		else
			parent_handler.addDiscoveryServicesOnComplete(Collections.synchronizedList(new ArrayList<BLEService>()));
		
		if(user_callback != null)
			user_callback.discoveryDone(services_list, discovery_success);
	}
	
	// Discovery Add Services/ Characteristics/ Descriptors -- called by Event Handler
	public void addService(BLEService service)
	{
		services_list.add(service);
	}
	
	public void addCharacteristic(BLECharacteristic chr, String service_id) // -- called by Event Handler
	{
		try {
			Iterator<BLEService> iter = services_list.iterator();
			while(iter.hasNext())
			{
				BLEService isrv = iter.next();
				if (isrv.getServiceId() == Long.valueOf(service_id))
				{
					isrv.addCharacteristic(chr);
					chr.setServiceId(service_id);
					break;
				}
			}
		}catch(IOException e) {
			System.err.println("Error Adding Characteristic: " + e.getMessage()); // Ignore exception Silently
		}
	}
	
	public void addDescriptor(short handle, String characteristic_id, String descriptor_id, short uuid) // -- called by Event Handler
	{
		// put the descriptor to characteristic depending on what type the descriptor is - ignore unknown descriptors
		
		if (uuid != 0x2902) return; // save only CCCDescriptors
		
		Iterator<BLEService> iter = services_list.iterator();
		while(iter.hasNext())
		{
			BLEService isrv = iter.next();
			BLECharacteristic chr = isrv.getCharacteristicById(characteristic_id);
			if (chr != null && chr.getCharacteristicId() == Integer.valueOf(characteristic_id))
			{
				chr.setCCCDHandle(handle);
				break;
			}
		}
	}
	
	public boolean isDiscoveryInProgress() {
		return is_discovery_in_progress;
	}
	
	public boolean getDiscoverySuccess() {
		return discovery_finished_with_success;
		//on error inform callback
	}
	
	public void reset()
	{
		is_discovery_in_progress = false;
		is_discovery_issued = false;
		discovery_finished_with_success = false;
		synchronized(block_until_disc_done) {block_until_disc_done.notify();}
		this.services_list.clear();
	}
}
