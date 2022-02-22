package cybervelia.tools.bleharverster;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.types.ConnectionTypesCommon;
import cybervelia.sdk.types.ConnectionTypesCommon.AddressType;
import cybervelia.server.CryptoHelper;

class BLEDevice {
	private ArrayList<DeviceDatum> data;
	private AddressType address_type;
	private String address = "";
	private int datacounter = 0;
	private String adv_data = null;
	private String scan_data = null;
	private String devname = null;
	private JSONArray discovery_services = null;
	private boolean bond_happened = false;
	
	BLEDevice(AddressType address_type, String address){
		this.address = address;
		this.address_type = address_type;
		data = new ArrayList<DeviceDatum>();
	}
	
	public void setOnlyAdvData(String data) {
		this.adv_data = data;
	}
	
	public void setOnlyScanData(String data) {
		this.scan_data = data;
	}
	
	public boolean hasAdvData() {
		return this.adv_data == null ? false : true;
	}
	
	public void bond() {
		this.bond_happened = true;
	}
	
	public boolean hasScanData() {
		return this.scan_data == null ? false : true;
	}
	
	public void setDeviceName(String devname) {
		this.devname = devname;
	}
	
	public void setDiscoveryData(List<BLEService> services) throws JSONException {
		this.discovery_services = jsonSerializeDiscoveryData(services);
	}
	
	public void putData(String characteristic, byte[] value, int datalen, DeviceDatum.operation operation) {
		String strvalue = "";
		if (datalen > 0)
			strvalue = CryptoHelper.bytesToHex(value, datalen);
		DeviceDatum datum = new DeviceDatum(characteristic, strvalue, datacounter++, operation);
		data.add(datum);
	}
	
	public void setAdvertisementData(String adv_data, String scan_data) {
		this.adv_data = adv_data;
		this.scan_data = scan_data;
	}
	
	private JSONArray jsonSerializeData() throws JSONException {
		JSONArray json_data = new JSONArray();
		for(DeviceDatum datum : data) {
			json_data.put(datum.jsonSerialize());
		}
		return json_data;
	}
	
	private JSONArray jsonSerializeDiscoveryData(List<BLEService> services) throws JSONException {
		JSONArray json_services = new JSONArray();
		if (services == null) {
			System.err.println("Discovery services cannot be retrieved");
			return null;
		}
		for(BLEService service : services) {
			JSONObject json_service = new JSONObject();
			json_service.put("service", service.getUUID().toString());
			json_service.put("handle", String.valueOf(service.getHandle()));
			json_service.put("characteristics", service.serializeCharacteristics());
			json_services.put(json_service);
		}
		return json_services;
	}
	
	public JSONObject jsonSerialize() throws JSONException {
		JSONObject device = new JSONObject();
		if (discovery_services == null) return null;
		
		device.put("address", address);
		device.put("address_type", ConnectionTypesCommon.getStringFromAddressType(address_type));
		device.put("data", jsonSerializeData());
		if (hasAdvData())
			device.put("advpacket", adv_data);
		if (hasScanData())
			device.put("scanpacket", scan_data);
		if (devname != null)
			device.put("name", devname);
		device.put("discovery", discovery_services);
		device.put("bond", bond_happened);
		return device;
	}
}