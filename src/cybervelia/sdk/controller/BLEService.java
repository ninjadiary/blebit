package cybervelia.sdk.controller;

import java.awt.List;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BLEService {
	
	private static String uuid_16bit = "0000DUMP-0000-1000-8000-00805f9b34fb";
	
    private static byte[] getBytesFromUUID(UUID uuid) {
        ByteBuffer btbuff = ByteBuffer.wrap(new byte[16]);
        btbuff.putLong(uuid.getMostSignificantBits());
        btbuff.putLong(uuid.getLeastSignificantBits());   
        return btbuff.array();
    }
    
    private static UUID getUUIDFromBytes(byte[] bytes) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        Long gh = byteBuffer.getLong();
        Long gl = byteBuffer.getLong();
        return new UUID(gh, gl);
    }
	
	// Object's Methods and fields
	private String service_id;
	private boolean is_128_uuid;
	private UUID uuid;
	private byte euuid[];
	private short handle;
	private HashMap<String, BLECharacteristic> characteristics;
	private boolean finalized = false; // service and characteristics already sent
	
	public BLEService(String str_uuid)
	{
		init(str_uuid, "0");
	}
	
	public BLEService(String str_uuid, String service_id) throws IllegalArgumentException {
		init(str_uuid, service_id);
	}
	
	private void init(String str_uuid, String service_id)
	{
		str_uuid = str_uuid.toLowerCase();
		characteristics = new HashMap<String, BLECharacteristic>(); // pair<CharID, BLECharacteristic>
		is_128_uuid = false;
		byte temp_uuid[];
		this.uuid = UUID.fromString(str_uuid);
		
		for(int i = 0; i<str_uuid.length(); ++i)
			if ((i<4 || i>7) && (str_uuid.charAt(i) != BLEService.uuid_16bit.charAt(i)))
			{
					is_128_uuid = true;
					break;
			}
		
		this.service_id = service_id;
		
		if (is_128_uuid == false)
		{
			temp_uuid = BLEService.getBytesFromUUID(uuid);
			euuid = new byte[2];
			euuid[0] = temp_uuid[3];
			euuid[1] = temp_uuid[2];
			//System.out.println("SRV 16-bit UUID: " + str_uuid);
		}
		else
		{
			euuid = BLEService.getBytesFromUUID(uuid);
			/*byte temp = euuid[2]; 
			euuid[2] = euuid[3];
			euuid[3] = temp;*/
			//System.out.println("SRV 128-bit UUID: " + str_uuid);
		}
	}
	
	public final byte[] getUUIDLow() {
		if (euuid.length == 2)
			return euuid;
		else
			return new byte[] {euuid[2], euuid[3]};
	}
	
	public BLECharacteristic getCharacteristicById(String characteristic_id) {
		return characteristics.get(characteristic_id);
	}
	
	public BLECharacteristic getCharacteristicByCCCDHandle(short handle) {
		BLECharacteristic characteristic_found = null;
		for(HashMap.Entry<String, BLECharacteristic> entry : characteristics.entrySet())
		{
			if (entry.getValue().getCCCDHandle() == handle)
			{
				characteristic_found = entry.getValue();
				break;
			}
		}
		return characteristic_found;
	}
	
	public BLECharacteristic getCharacteristicByHandle(short handle) {
		BLECharacteristic characteristic_found = null;
		for(HashMap.Entry<String, BLECharacteristic> entry : characteristics.entrySet())
		{
			if (entry.getValue().getValueHandle() == handle)
			{
				characteristic_found = entry.getValue();
				break;
			}
		}
		return characteristic_found;
	}
	
	public BLECharacteristic getCharacteristicByUUID(String UUID) {
		UUID = UUID.toLowerCase();
		BLECharacteristic characteristic_found = null;
		for(HashMap.Entry<String, BLECharacteristic> entry : characteristics.entrySet())
		{
			if (entry.getValue().getUUID().toString().equals(UUID))
			{
				characteristic_found = entry.getValue();
				break;
			}
		}
		return characteristic_found;
	}
	
	public void addCharacteristic(BLECharacteristic chr) throws IOException {
		if (finalized) throw new IOException("Service and characteristics have been already sent to the device");
		characteristics.put(String.valueOf(chr.getCharacteristicId()), chr);
	}
	
	public boolean hasCharacteristic(String characteristic_id) {
		return characteristics.containsKey(characteristic_id);
	}
	
	public final byte[] getUUIDBytes() {
		return euuid;
	}
	
	public boolean is_128bit_uuid() {
		return this.is_128_uuid;
	}
	
	public byte getServiceId() {
		return Integer.valueOf(this.service_id).byteValue();
	}
	
	public void setServiceId(String service_id)
	{
		this.service_id = service_id;
	}
	
	public void setHandle(short handle) {
		this.handle = handle;
	}
	
	public short getHandle() {
		return this.handle;
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	public String getUUIDString() {
		return getUUID().toString();
	}
	
	public ArrayList<BLECharacteristic> getAllCharacteristics(){
		ArrayList<BLECharacteristic> chrs = new ArrayList<BLECharacteristic>();
		if (characteristics.size() > 0)
		{
			for(HashMap.Entry<String, BLECharacteristic> chr_id_characteristic_pair : characteristics.entrySet())
				chrs.add(chr_id_characteristic_pair.getValue());
		}
		return chrs;
	}
	
	public Collection<BLECharacteristic> getCharacteristicsAndFinalize() {
		finalized = true;
		for(BLECharacteristic characteristic : characteristics.values())
		{
			characteristic.setServiceId(service_id);
		}
		return characteristics.values();
	}
	
	public boolean isFinalized() {
		return finalized;
	}
	
	public JSONArray serializeCharacteristics() throws JSONException
	{
		JSONArray chars = new JSONArray();
		for(BLECharacteristic characteristic : characteristics.values())
		{
			JSONObject characteristic_json = characteristic.serialize();
			chars.put(characteristic_json);
		}
		return chars;
	}
	
	public void loadCharacteristicsFromJSON(JSONArray characteristics_json) throws JSONException, IOException
	{
		for(int i = 0; i < characteristics_json.length(); ++i)
		{
			JSONObject object = characteristics_json.getJSONObject(i);
			BLECharacteristic characteristic = BLECharacteristic.deserialize(object);
			addCharacteristic(characteristic);
		}
	}
	
	public BLEService cloneService() throws IOException
	{
		BLEService service = new BLEService(this.uuid.toString());
		service.setHandle(handle);
		service.setServiceId(service_id);
		if (characteristics.size() > 0)
		{
			for(HashMap.Entry<String, BLECharacteristic> chr_id_characteristic_pair : characteristics.entrySet())
				service.addCharacteristic(chr_id_characteristic_pair.getValue().cloneCharacteristic());
		}
		return service;
	}
}
