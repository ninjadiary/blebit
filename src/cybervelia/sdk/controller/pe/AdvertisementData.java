package cybervelia.sdk.controller.pe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.types.ConnectionTypesPE;

public class AdvertisementData {
	private enum NameType {
		NONE,
		COMPLETE,
		SHORT
	}
	
	private CustomAdvertisementData custom_adv_data;
	private ArrayList<ServiceData> service_data;
	private ArrayList<BLEService> uuid_complete;
	private ArrayList<BLEService> uuid_incomplete;
	private ArrayList<BLEService> uuid_solicitated;
	
	private boolean include_dev_address = false;
	private String device_name;
	private NameType name_type;
	private boolean include_appearance = false;
	private boolean include_tx_power = false;
	private byte tx_power_value = 0;
	private byte flags = 0;
	private int device_name_short_size = 0;
	private boolean include_flags = false;
	private ManufacturerData manufacturer_data = null;
	private short slave_min_con_int = 0;
	private short slave_max_con_int = 0;
	
	// Flags
	public static final byte FLAG_LE_LIMITED_DISCOVERABLE_MODE 	= 1;
	public static final byte FLAG_LE_GENERAL_DISCOVERABLE_MODE 	= 2;
	public static final byte FLAG_ER_BDR_NOT_SUPPORTED 			= 4;
	
	public AdvertisementData() {
		device_name = null;
		name_type = NameType.NONE;
		service_data = new ArrayList<ServiceData>();
		uuid_complete = new ArrayList<BLEService>();
		uuid_incomplete = new ArrayList<BLEService>();
		uuid_solicitated = new ArrayList<BLEService>();
		custom_adv_data = null;
	}
	
	public void includeDeviceAddress() {
		this.include_dev_address = true;
	}
	
	public void includeDeviceShortName(int short_size) throws IOException {
		if (short_size > 29) throw new IOException("Device name cannot be more than 29 bytes");
		name_type = NameType.SHORT;
		this.device_name_short_size = short_size;
	}
	
	public void includeDeviceName() throws IOException {
		name_type = NameType.COMPLETE;
	}
	
	protected void setDeviceName(String devname) throws IOException {
		if (devname.length() > 31) throw new IOException("Device Name Length too long");
		if (this.device_name == null) // if device_name set that means its a short device name, set by the user
			this.device_name = devname;
	}
	
	public void setCustomAdvertisingData(CustomAdvertisementData data) {
		this.custom_adv_data = data;
	}
	
	public int fillBufferFromCustomData(byte[] buffer) {
		if (custom_adv_data != null)
		{
			System.arraycopy(custom_adv_data.data, 0, buffer, 0, custom_adv_data.size);
			return custom_adv_data.size;
		}else return 0;
	}
	
	public void addServiceData(ServiceData data) throws IOException {
		if (data.data.length < data.size || data.size > 31 || data.uuid.length != 2) throw new IOException("Advertisement Service Data length error");
		if (this.service_data.size() >= 5) throw new IOException("Number of allowed service data counter exceeded");
		this.service_data.add(data);
	}
	
	public void setSlaveConnectionIntTU(short min, short max) throws IOException
	{
		// TU = 1.25ms
		if (min < 6 || min > 3200 || max < 6 || max > 3200 || max < min) throw new IOException("Invalid slave connection interval TU parameters");
		
		slave_min_con_int = min;
		slave_max_con_int = max;
	}
	
	public void includeAppearance() {
		this.include_appearance = true;
	}
	
	public void setTXPower(byte value) {
		tx_power_value = value;
		include_tx_power = true;
	}
	
	public void setFlags(int flags) {
		this.flags = (byte) flags;
		this.include_flags = true;
	}
	
	public void setManufacturerData(ManufacturerData md) throws IOException {
		if (md.data.length < md.size || md.size > 31) throw new IOException("Advertisement Manufacturer Data length error");
		this.manufacturer_data = md;
	}
	
	public void addServiceUUIDComplete(BLEService service) throws IOException {
		if (uuid_complete.size() >= 10) throw new IOException("Maximum Complete UUID reached");
		this.uuid_complete.add(service);
	}
	
	public void addServiceUUIDIncomplete(BLEService service) throws IOException {
		if (uuid_incomplete.size() >= 10) throw new IOException("Maximum Complete UUID reached");
		this.uuid_incomplete.add(service);
	}
	
	public void addServiceUUIDSolicitated(BLEService service) throws IOException {
		if (uuid_solicitated.size() >= 10) throw new IOException("Maximum Complete UUID reached");
		this.uuid_solicitated.add(service);
	}
	
	public short getSlaveConnectionIntMin() {
		return slave_min_con_int;
	}
	
	public short getSlaveConnectionIntMax() {
		return slave_max_con_int;
	}
	
	public byte getFlags() {
		return flags;
	}
	
	public ArrayList<BLEService> getServiceUUIDComplete(){
		return this.uuid_complete;
	}
	
	public ArrayList<BLEService> getServiceUUIDIncomplete(){
		return this.uuid_incomplete;
	}
	
	public ArrayList<BLEService> getServiceUUIDSolicitated(){
		return this.uuid_solicitated;
	}
	
	public boolean isInLimitedDiscoverableMode() {
		return (((int)flags & AdvertisementData.FLAG_LE_LIMITED_DISCOVERABLE_MODE) > 0);
	}
	
	public boolean isInGeneralDiscoverableMode() {
		return (((int)flags & AdvertisementData.FLAG_LE_GENERAL_DISCOVERABLE_MODE) > 0);
	}
	
	public boolean isLEOnlySupported() {
		return ((((int)flags & AdvertisementData.FLAG_ER_BDR_NOT_SUPPORTED) & 0xff) > 0);
	}
	
	public boolean isDeviceNameIncluded()
	{
		return !(name_type == NameType.NONE);
	}
	
	public boolean isTxPowerIncluded() {
		return this.include_tx_power;
	}
	
	public int getTxPower() {
		return ((int)tx_power_value) & 0xff;
	}
	
	public boolean isAddressIncluded() {
		return include_dev_address;
	}
	
	public boolean isAppearanceIncluded() {
		return include_appearance;
	}
	
	public ManufacturerData getManfucaturerData() {
		return manufacturer_data;
	}
	
	public ArrayList<ServiceData> getServiceData(){
		return service_data;
	}
	
	protected void sendSlaveConInt(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (slave_min_con_int > 0 && slave_max_con_int > 0)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_SLAVE_CON_INT_ADV);
			else
				out.write(ConnectionTypesPE.ADV_SLAVE_CON_INT_SCN);
			sendShort(out, slave_min_con_int);
			sendShort(out, slave_max_con_int);
			verifySuccess(in);
		}
	}
	
	protected void sendDeviceAddressInclusion(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (include_dev_address)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_INCLUDE_DEV_ADDRESS);
			else
				out.write(ConnectionTypesPE.SCN_INCLUDE_DEV_ADDRESS);
		}
	}
	
	protected void sendDeviceName(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (device_name != null && name_type != NameType.NONE)
		{	
			// Include Device Name (send type)
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_DEVICE_NAME_TYPE);
			else
				out.write(ConnectionTypesPE.SCN_DEVICE_NAME_TYPE);
			
			if (name_type == NameType.COMPLETE)
				out.write(2);
			else
				out.write(1);
			
			verifySuccess(in);
			
			// Send device name length in case of short name type
			if (name_type == NameType.SHORT)
			{
				if (device_name.length() < this.device_name_short_size) throw new IOException("Short device name length larger than complete device names length");
				
				out.write(ConnectionTypesPE.STP_ADV_DATA);
				if (are_advertising_data)
					out.write(ConnectionTypesPE.ADV_SH_NAME_LEN);
				else
					out.write(ConnectionTypesPE.SCN_SH_NAME_LEN);
				out.write((byte) this.device_name_short_size);
				verifySuccess(in);
			}
		}
	}
	
	protected void sendCustomData(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (custom_adv_data != null)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_CUSTOM_DATA);
			else
				out.write(ConnectionTypesPE.SCN_CUSTOM_DATA);
			
			out.write((byte)custom_adv_data.size);
			out.write(custom_adv_data.data, 0, custom_adv_data.size);
			verifySuccess(in);
		}
	}
	
	protected void sendServiceData(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (service_data.size() > 0)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_SERVICE_DATA);
			else
				out.write(ConnectionTypesPE.SCN_SERVICE_DATA);
			
			out.write((byte)service_data.size());
			for(ServiceData sdata : service_data) {
				out.write(sdata.uuid, 0, 2);
				out.write((byte)sdata.size);
				out.write(sdata.data, 0, sdata.size);
			}
			verifySuccess(in);
		}
	}
	
	protected void sendAppearanceInclusion(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (include_appearance)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_APPEARANCE);
			else
				out.write(ConnectionTypesPE.SCN_APPEARANCE);
			verifySuccess(in);
		}
	}
	
	protected void sendTXPower(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (include_tx_power)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_TX_POWER);
			else
				out.write(ConnectionTypesPE.SCN_TX_POWER);
			out.write(tx_power_value);
			verifySuccess(in);
		}
	}
	
	protected void sendFlags(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (include_flags)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_FLAGS);
			else
				out.write(ConnectionTypesPE.SCN_FLAGS);
			
			out.write(this.flags);
			verifySuccess(in);
		}
	}
	
	protected void sendManufacturerData(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (manufacturer_data != null)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_MANUFACTURER_DATA);
			else
				out.write(ConnectionTypesPE.SCN_MANUFACTURER_DATA);
			sendShort(out, manufacturer_data.size);
			sendShort(out, manufacturer_data.company_identifier);
			out.write(manufacturer_data.data, 0, manufacturer_data.size);
			verifySuccess(in);
		}
	}
	
	protected void sendServiceUUIDComplete(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (uuid_complete.size() > 0)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_UUID_COMPLETE);
			else
				out.write(ConnectionTypesPE.SCN_UUID_COMPLETE);
			out.write((byte)uuid_complete.size());
			for(BLEService service : uuid_complete)
				out.write(service.getServiceId());
			verifySuccess(in);
		}
	}
	
	protected void sendServiceUUIDIncomplete(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (uuid_incomplete.size() > 0)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_UUID_INCOMPLETE);
			else
				out.write(ConnectionTypesPE.SCN_UUID_INCOMPLETE);
			out.write((byte)uuid_incomplete.size());
			for(BLEService service : uuid_incomplete)
				out.write(service.getServiceId());
			verifySuccess(in);
		}
	}
	
	protected void sendServiceUUIDSolicitated(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		if (uuid_solicitated.size() > 0)
		{
			out.write(ConnectionTypesPE.STP_ADV_DATA);
			if (are_advertising_data)
				out.write(ConnectionTypesPE.ADV_UUID_SOLICITATED);
			else
				out.write(ConnectionTypesPE.SCN_UUID_SOLICITATED);
			out.write((byte)uuid_solicitated.size());
			for(BLEService service : uuid_solicitated)
				out.write(service.getServiceId());
			verifySuccess(in);
		}
	}
	
	protected void sendAll(InputStream in, OutputStream out, boolean are_advertising_data) throws IOException {
		sendDeviceAddressInclusion(in,out,are_advertising_data);
		sendDeviceName(in,out,are_advertising_data);
		sendCustomData(in,out,are_advertising_data);
		sendServiceData(in,out,are_advertising_data);
		sendAppearanceInclusion(in,out,are_advertising_data);
		sendTXPower(in,out,are_advertising_data);
		sendFlags(in,out,are_advertising_data);
		sendManufacturerData(in,out,are_advertising_data);
		sendServiceUUIDComplete(in,out,are_advertising_data);
		sendServiceUUIDIncomplete(in,out,are_advertising_data);
		sendServiceUUIDSolicitated(in,out,are_advertising_data);
		sendSlaveConInt(in,out,are_advertising_data);
	}
	
	private void verifySuccess(InputStream in) throws IOException {
		StringBuilder error_str = new StringBuilder();
		boolean generate_error = false;
		byte err_code;
		byte err_message_cnt = 0;
		
		err_code = (byte) in.read();
		if (err_code != ConnectionTypesPE.STP_ERROR)
			throw new IOException("Supposed to receive an error message");
		
		err_message_cnt = (byte) in.read();
		while(err_message_cnt > 0)
		{
			err_code = (byte) in.read();
			if (err_code != ConnectionTypesPE.ERR_SUCCESS)
			{
				generate_error = true;
				error_str.append("Error received ");
				error_str.append(String.valueOf(err_code));
				error_str.append("\n");
			}
			--err_message_cnt;
		}
		if (generate_error)
			throw new IOException(error_str.toString());
	}
	
	private void sendShort(OutputStream out, int value) throws IOException {
		out.write((byte) (value & 0xff));
		out.write((byte) ((value & 0xff00) >> 8));
	}
}

