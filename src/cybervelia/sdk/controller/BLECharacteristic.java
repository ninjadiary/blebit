package cybervelia.sdk.controller;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;
import org.json.JSONObject;

import cybervelia.sdk.controller.pe.NotificationValidation;
import cybervelia.sdk.types.BLEAttributePermission;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class BLECharacteristic {
	
	private static AtomicInteger next_characteristic_id = new AtomicInteger(1);
	private static String uuid_16bit = "0000DUMP-0000-1000-8000-00805f9b34fb";
	
	// characteristic property values
	/*
	protected static final byte PRP_READ 					 	= 0x01;
	protected static final byte PRP_WRITE 					 	= 0x02;
	protected static final byte PRP_WRITE_NO_RESP 		 	 	= 0x04;
	protected static final byte PRP_INDICATE 					= 0x08;
	protected static final byte PRP_NOTIFY 						= 0x16;
	*/
	
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
    
    private static final byte PROPERTY_READ 			= 1;
    private static final byte PROPERTY_WRITE_NO_RESP 	= 2;
    private static final byte PROPERTY_WRITE			= 4;
    private static final byte PROPERTY_NOTIFY			= 8;
    private static final byte PROPERTY_INDICATE			= 16;
    
    
	// Object's Methods and fields
	private String characteristic_id;
	private String service_id;
	private boolean is_128_uuid;
	private UUID uuid;
	private byte euuid[];
	private short value_handle;
	private short cccd_handle;
	private boolean is_value_variable = false;
	private int max_value_length = 31;
	private byte properties;
	private BLEAttributePermission read_permissions;
	private BLEAttributePermission write_permissions;
	private BLEAttributePermission read_cccd;
	private BLEAttributePermission write_cccd;
	private byte value[];
	private int current_value_length = 0;
	private boolean hook_on_read;
	private int initial_value_length;
	private boolean finalized = false;
	
	public BLECharacteristic(String str_uuid, String characteristic_id)  throws IllegalArgumentException, IOException
	{
		byte val[] = new byte[] {0};
		init(str_uuid, val, characteristic_id);
	}
	
	public BLECharacteristic(String str_uuid, byte value[]) throws IllegalArgumentException, IOException{
		init(str_uuid, value, String.valueOf(next_characteristic_id.getAndIncrement()));
	}
	
	private void init(String str_uuid, byte value[], String characteristic_id) throws IllegalArgumentException, IOException
	{
		str_uuid = str_uuid.toLowerCase();
		is_128_uuid = false;
		byte temp_uuid[];
		
		if (value.length > 31)
			throw new IOException("Characteristic Value Length should be up to 31 bytes");
		
		initial_value_length = value.length;
		current_value_length = initial_value_length;
		max_value_length = initial_value_length;
		this.value = new byte[initial_value_length];
		System.arraycopy(value, 0, this.value, 0, initial_value_length);
		this.uuid = UUID.fromString(str_uuid);
		this.characteristic_id = characteristic_id;
		
		for(int i = 0; i<str_uuid.length(); ++i)
			if ((i<4 || i>7) && (str_uuid.charAt(i) != BLECharacteristic.uuid_16bit.charAt(i)))
				{
					is_128_uuid = true;
					break;
				}
		
		if (is_128_uuid == false)
		{
			temp_uuid = BLECharacteristic.getBytesFromUUID(uuid);
			euuid = new byte[2];
			euuid[0] = temp_uuid[3];
			euuid[1] = temp_uuid[2];
			//System.out.println("CHR 16-bit UUID: " + str_uuid);
		}
		else
		{
			euuid = BLECharacteristic.getBytesFromUUID(uuid);
			//System.out.println("CHR 128-bit UUID: " + str_uuid);
		}
		this.cccd_handle = 0;
		is_value_variable = true;
		max_value_length = 31;
		service_id = null;
		read_permissions = BLEAttributePermission.OPEN;
		write_permissions = BLEAttributePermission.OPEN;
		read_cccd = BLEAttributePermission.OPEN;
		write_cccd = BLEAttributePermission.OPEN;
		hook_on_read = false;
	}
	
	public void setRawProperties(byte properties) throws IOException
	{
		if ((properties & 1) > 0) enableRead();
		if ((properties & 2) > 0) enableWriteCMD();
		if ((properties & 4) > 0) enableWrite();
		if ((properties & 8) > 0) enableNotification();
		if ((properties & 16) > 0) enableIndication();
		this.properties = properties;
		// properties & 32 -> Signed Write
		// properties & 64 -> Char. Broadcast Enabled
	}
	
	public BLEAttributePermission getValuePermissionRead() {
		return this.read_permissions;
	}
	
	public BLEAttributePermission getValuePermissionWrite() {
		return this.write_permissions;
	}
	
	public BLEAttributePermission getCCCDPermissionRead() {
		return this.read_cccd;
	}
	
	public BLEAttributePermission getCCCDPermissionWrite() {
		return this.write_cccd;
	}
	
	public byte getProperties() {
		return this.properties;
	}
	
	public boolean is_128bit_uuid() {
		return this.is_128_uuid;
	}
	
	public void setAttributePermissions(BLEAttributePermission read, BLEAttributePermission write) throws IOException
	{
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.read_permissions = read;
		this.write_permissions = write;
	}
	
	public void enableNotification() throws IOException
	{
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.read_cccd = BLEAttributePermission.OPEN;
		this.write_cccd = BLEAttributePermission.OPEN;
		this.properties = (byte)(this.properties | PROPERTY_NOTIFY);
		this.properties = (byte)(this.properties & (PROPERTY_NOTIFY | PROPERTY_READ | PROPERTY_WRITE | PROPERTY_WRITE_NO_RESP));
	}
	
	public boolean isNotificationAuthorized() {
		return ((this.properties & PROPERTY_NOTIFY) > 0);
	}
	
	public boolean isIndicationAuthorized() {
		return ((this.properties & PROPERTY_INDICATE) > 0);
	}
	
	public void enableIndication() throws IOException
	{
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.read_cccd = BLEAttributePermission.OPEN;
		this.write_cccd = BLEAttributePermission.OPEN;
		this.properties = (byte)(this.properties | PROPERTY_INDICATE);
		this.properties = (byte)(this.properties & (PROPERTY_INDICATE | PROPERTY_READ | PROPERTY_WRITE | PROPERTY_WRITE_NO_RESP));
	}
	
	public void disableNotification() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties & (PROPERTY_INDICATE | PROPERTY_READ | PROPERTY_WRITE | PROPERTY_WRITE_NO_RESP));
	}
	
	public void disableIndication() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties & (PROPERTY_NOTIFY | PROPERTY_READ | PROPERTY_WRITE | PROPERTY_WRITE_NO_RESP));
	}
	
	public void enableRead() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties | PROPERTY_READ);
	}
	
	public void disableRead() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties & (PROPERTY_NOTIFY | PROPERTY_INDICATE | PROPERTY_WRITE | PROPERTY_WRITE_NO_RESP));
	}
	
	public boolean isReadEnabled() {
		return ((this.properties & PROPERTY_READ) > 0) ? true : false;
	}
	
	public boolean isWriteEnabled() {
		return ((this.properties & PROPERTY_WRITE) > 0) ? true : false;
	}
	
	public void disableWrite() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties & (PROPERTY_NOTIFY | PROPERTY_INDICATE | PROPERTY_READ | PROPERTY_WRITE_NO_RESP));
	}
	
	public void enableWrite() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties | PROPERTY_WRITE);
	}
	
	public boolean isWriteCMDEnabled() {
		return ((this.properties & PROPERTY_WRITE_NO_RESP) > 0) ? true : false;
	}
	
	public void disableWriteCMD() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties & ( PROPERTY_NOTIFY | PROPERTY_INDICATE | PROPERTY_WRITE | PROPERTY_READ));
	}
	
	public void enableWriteCMD() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.properties = (byte)(this.properties | PROPERTY_WRITE_NO_RESP);
	}
	
	public void setValueLengthVariable(boolean is_variable_length) throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.is_value_variable = is_variable_length;
	}
	
	public void setMaxValueLength(int max) throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		if (max > 31) throw new IOException("Length Overload - 31 bytes");
		this.max_value_length = max;
	}
	
	public int getMaxValueLength() {
		return this.max_value_length;
	}
	
	public boolean isValueLengthVariable() {
		return this.is_value_variable;
	}
	
	public void setValueHandle(short handle) {
		this.value_handle = handle;
	}
	
	public short getValueHandle() {
		return this.value_handle;
	}
	
	public void setCCCDHandle(short handle) {
		this.cccd_handle = handle;
	}
	
	public short getCCCDHandle() {
		return this.cccd_handle;
	}
	
	public boolean setServiceId(String service_id) {
		if (this.service_id == null)
		{
			this.service_id = service_id;
			return true;
		}
		else
			return false;
	}
	
	public int getServiceId() throws IOException {
		if (service_id == null) throw new IOException("The Characteristic not attached to any service");
		return Integer.valueOf(this.service_id).intValue();
	}
	
	public int getCharacteristicId() {
		return Integer.parseInt(this.characteristic_id);
	}
	
	public int getInitialValueLength() {
		return this.initial_value_length;
	}
	
	public int getLocalValue(byte[] data) throws IOException {
		if (current_value_length > data.length) 
			throw new IOException("Current data (" + String.valueOf(current_value_length) + " bytes) length is bigger than buffer provided (" + String.valueOf(data.length) + " bytes)");
		
		synchronized(this)
		{
			System.arraycopy(this.value, 0, data, 0, current_value_length);
			return current_value_length;
		}
	}
	
	public void setValue(final byte[] value, int data_len) {
		// It is allowed to grow as much as to 31 bytes
		synchronized(this)
		{
			if(data_len > max_value_length)
				data_len = max_value_length;
			
			if (current_value_length < data_len)
				this.value = new byte[data_len];
			
			System.arraycopy(value, 0, this.value, 0, data_len);
			current_value_length = data_len;
		}
	}
	
	public void setInitialValue(final byte[] value, int data_len) throws IOException
	{
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		if(data_len > max_value_length) throw new IOException("Initial value length have to be larger than maximum value length");
		this.value = new byte[data_len];
		System.arraycopy(value, 0, this.value, 0, data_len);
		current_value_length = data_len;
		initial_value_length = data_len;
	}
	
	public void enableHookOnRead() throws IOException{
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.hook_on_read = true;
	}
    
	public void disableHookOnRead() throws IOException {
		if (finalized) throw new IOException("The characteristic is aleady sent to the device");
		this.hook_on_read = false;
	}
	
	public boolean isHookOnReadEnabled() {
		return this.hook_on_read;
	}
	
	public boolean isCCCDEnabled() {
		if ((this.properties & ( PROPERTY_NOTIFY | PROPERTY_INDICATE)) > 0)
			return true;
		else
			return false;
	}
	
	public NotificationValidation notificationDataSentValidation(final byte[] data, int data_len)
	{
		if (data_len == 2)
		{
			if (data[0] == 1 && data[1] == 0)
				return NotificationValidation.NOTIFICATION_ENABLED;
			else if (data[0] == 2 && data[1] == 0)
				return NotificationValidation.INDICATION_ENABLED;
			else if (data[0] == 0 && data[1] == 0)
				return NotificationValidation.NOTIF_INDIC_DISABLED;
			else
				return NotificationValidation.NOT_VALID;
		}else return NotificationValidation.NOT_VALID;
	}
	
	public UUID getUUID() {
		return this.uuid;
	}
	
	public byte[] getUUIDBytes() {
		return euuid;
	}
	
	public void finalize() {
		finalized = true;
	}
	
	public boolean isFinalized() {
		return finalized;
	}
	
	public JSONObject serialize() throws JSONException
	{
		JSONObject serialized_object = new JSONObject();
		
		// Serialise Characteristic to JSON
		serialized_object.put("uuid", getUUID().toString());
		serialized_object.put("can_read", isReadEnabled());
		serialized_object.put("can_write", isWriteEnabled());
		serialized_object.put("can_write_cmd", isWriteCMDEnabled());
		serialized_object.put("notifications", isNotificationAuthorized());
		serialized_object.put("indications", isIndicationAuthorized());
		serialized_object.put("value_variable", isValueLengthVariable());
		serialized_object.put("max_value_length", getMaxValueLength());
		serialized_object.put("value_handle", value_handle);
		serialized_object.put("cccd_handle", cccd_handle);
		serialized_object.put("service_id", service_id); // ine null kai den to vazei dioti akoma na tu vali service
		serialized_object.put("characteristic_id", characteristic_id);
		serialized_object.put("value", ConnectionTypesCommon.bytesToHex(this.value));
		int read_perm = 0;
		
		switch(getValuePermissionRead())
		{
			case NO_ACCESS:
				read_perm = 0;
				break;
			case OPEN:
				read_perm = 1;
				break;
			case ENCRYPTION:
				read_perm = 2;
				break;
			case ENCRYPTION_MITM:
				read_perm = 3;
				break;
			
		}
		serialized_object.put("value_perm_read", read_perm);
		int write_perm = 0;
		switch(getValuePermissionWrite())
		{
			case NO_ACCESS:
				write_perm = 0;
				break;
			case OPEN:
				write_perm = 1;
				break;
			case ENCRYPTION:
				write_perm = 2;
				break;
			case ENCRYPTION_MITM:
				write_perm = 3;
				break;
			
		}
		serialized_object.put("value_perm_write", write_perm);
		serialized_object.put("hook_on_read", isHookOnReadEnabled());
		
		return serialized_object;
	}
	
	public static BLECharacteristic deserialize(JSONObject object) throws JSONException, IOException
	{
		String uuid = object.getString("uuid");
		byte[] value = ConnectionTypesCommon.hexToBytes(object.getString("value"));
		BLECharacteristic chr = new BLECharacteristic(uuid, String.valueOf(object.getInt("characteristic_id")));
		chr.setInitialValue(value, value.length);
		chr.setMaxValueLength(object.getInt("max_value_length"));
		chr.setValueLengthVariable(object.getBoolean("value_variable"));
		if (object.getBoolean("can_read")) chr.enableRead();
		if (object.getBoolean("can_write")) chr.enableWrite();
		if (object.getBoolean("can_write_cmd")) chr.enableWriteCMD();
		if (object.getBoolean("notifications")) chr.enableNotification();
		else if (object.getBoolean("indications")) chr.enableIndication();
		if (object.getBoolean("hook_on_read")) chr.enableHookOnRead();
		int readp = object.getInt("value_perm_read");
		BLEAttributePermission read_permission;
		int writep = object.getInt("value_perm_write");
		BLEAttributePermission write_permission;
		chr.setValueHandle((short) object.getInt("value_handle"));
		chr.setCCCDHandle((short)object.getInt("cccd_handle"));
		chr.setServiceId(object.getString("service_id"));
		
		switch(readp)
		{
			case 0:
				read_permission = BLEAttributePermission.NO_ACCESS;
				break;
			case 1:
				read_permission = BLEAttributePermission.OPEN;
				break;
			case 2:
				read_permission = BLEAttributePermission.ENCRYPTION;
				break;
			case 3:
				read_permission = BLEAttributePermission.ENCRYPTION_MITM;
				break;
			default:
				read_permission = BLEAttributePermission.OPEN;
		}
		
		switch(writep)
		{
			case 0:
				write_permission = BLEAttributePermission.NO_ACCESS;
				break;
			case 1:
				write_permission = BLEAttributePermission.OPEN;
				break;
			case 2:
				write_permission = BLEAttributePermission.ENCRYPTION;
				break;
			case 3:
				write_permission = BLEAttributePermission.ENCRYPTION_MITM;
				break;
			default:
				write_permission = BLEAttributePermission.OPEN;
		}
		chr.setAttributePermissions(read_permission, write_permission);
		
		return chr;
	}
	
	public BLECharacteristic cloneCharacteristic() throws IOException
	{
		BLECharacteristic cloned = new BLECharacteristic(this.uuid.toString(), String.valueOf(characteristic_id));
		cloned.setInitialValue(this.value, this.value.length);
		if (isHookOnReadEnabled()) cloned.enableHookOnRead();
		cloned.setAttributePermissions(getValuePermissionRead(), getValuePermissionWrite());
		cloned.setRawProperties(getProperties());
		cloned.setMaxValueLength(getMaxValueLength());
		cloned.setValueLengthVariable(isValueLengthVariable());
		cloned.setServiceId(service_id);
		// avoid setting handles, as this is a clone
		//cloned.setCCCDHandle(cccd_handle);
		//cloned.setValueHandle(value_handle);
		return cloned;
	}
}
