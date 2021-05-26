package cybervelia.sdk.types;

import java.io.IOException;

public class ConnectionTypesCommon {
	protected static String uuid_16bit = "0000DUMP-0000-1000-8000-00805f9b34fb";
	
	public static final short BLE_APPEARANCE_UNKNOWN = 0;
	public static final short BLE_APPEARANCE_GENERIC_PHONE = 64;
	public static final short BLE_APPEARANCE_GENERIC_COMPUTER = 128;
	public static final short BLE_APPEARANCE_GENERIC_WATCH = 192;
	public static final short BLE_APPEARANCE_WATCH_SPORTS_WATCH = 193;
	public static final short BLE_APPEARANCE_GENERIC_CLOCK = 256;
	public static final short BLE_APPEARANCE_GENERIC_DISPLAY = 320;
	public static final short BLE_APPEARANCE_GENERIC_REMOTE_CONTROL = 384;
	public static final short BLE_APPEARANCE_GENERIC_EYE_GLASSES = 448;
	public static final short BLE_APPEARANCE_GENERIC_TAG = 512;
	public static final short BLE_APPEARANCE_GENERIC_KEYRING = 576;
	public static final short BLE_APPEARANCE_GENERIC_MEDIA_PLAYER = 640;
	public static final short BLE_APPEARANCE_GENERIC_BARCODE_SCANNER = 704;
	public static final short BLE_APPEARANCE_GENERIC_THERMOMETER = 768;
	public static final short BLE_APPEARANCE_THERMOMETER_EAR = 769;
	public static final short BLE_APPEARANCE_GENERIC_HEART_RATE_SENSOR = 832;
	public static final short BLE_APPEARANCE_HEART_RATE_SENSOR_HEART_RATE_BELT = 833;
	public static final short BLE_APPEARANCE_GENERIC_BLOOD_PRESSURE = 896;
	public static final short BLE_APPEARANCE_BLOOD_PRESSURE_ARM = 897;
	public static final short BLE_APPEARANCE_BLOOD_PRESSURE_WRIST = 898;
	public static final short BLE_APPEARANCE_GENERIC_HID = 960;
	public static final short BLE_APPEARANCE_HID_KEYBOARD = 961;
	public static final short BLE_APPEARANCE_HID_MOUSE = 962;
	public static final short BLE_APPEARANCE_HID_JOYSTICK = 963;
	public static final short BLE_APPEARANCE_HID_GAMEPAD = 964;
	public static final short BLE_APPEARANCE_HID_DIGITIZERSUBTYPE = 965;
	public static final short BLE_APPEARANCE_HID_CARD_READER = 966;
	public static final short BLE_APPEARANCE_HID_DIGITAL_PEN = 967;
	public static final short BLE_APPEARANCE_HID_BARCODE = 968;
	public static final short BLE_APPEARANCE_GENERIC_GLUCOSE_METER = 1024;
	public static final short BLE_APPEARANCE_GENERIC_RUNNING_WALKING_SENSOR = 1088;
	public static final short BLE_APPEARANCE_RUNNING_WALKING_SENSOR_IN_SHOE = 1089;
	public static final short BLE_APPEARANCE_RUNNING_WALKING_SENSOR_ON_SHOE = 1090;
	public static final short BLE_APPEARANCE_RUNNING_WALKING_SENSOR_ON_HIP = 1091;
	public static final short BLE_APPEARANCE_GENERIC_CYCLING = 1152;
	public static final short BLE_APPEARANCE_CYCLING_CYCLING_COMPUTER = 1153;
	public static final short BLE_APPEARANCE_CYCLING_SPEED_SENSOR = 1154;
	public static final short BLE_APPEARANCE_CYCLING_CADENCE_SENSOR = 1155;
	public static final short BLE_APPEARANCE_CYCLING_POWER_SENSOR = 1156;
	public static final short BLE_APPEARANCE_CYCLING_SPEED_CADENCE_SENSOR = 1157;
	private static final int SDK_SOFTWARE_VERSION = 17;
	
	public enum PairingMethods {
		NO_IO,
		KEYBOARD,
		DISPLAY,
		KEY_DISP
	};
	
	public enum BITAddressType {
		PUBLIC,
		STATIC_PRIVATE
	}
	
	// address type
	public enum AddressType{
	    PUBLIC_ADDR                   ,
	    RANDOM_STATIC_ADDR           ,
	    RANDOM_RESOLVABLE_ADDR       ,
	    RANDOM_NON_RESOLVABLE_ADDR   
	}
	
	// Pairing Method Codes
	public static final byte PAIRING_METHOD_NO_IO 			= 0x01;
	public static final byte PAIRING_METHOD_KEYBOARD		= 0x02;
	public static final byte PAIRING_METHOD_DISPLAY 		= 0x03;
	public static final byte PAIRING_METHOD_KEY_DISP 		= 0x04;
	
	// Disable
	public static final byte DISABLE_ALLOW_REPAIRING 		= 0x02;
	
    public static final byte CPUBLIC_ADDR                   = 0x00;
    public static final byte CRANDOM_STATIC_ADDR           = 0x01;
    public static final byte CRANDOM_RESOLVABLE_ADDR       = 0x02;
    public static final byte CRANDOM_NON_RESOLVABLE_ADDR   = 0x03;
	
    public static String getStringFromAddressType(AddressType type) {
    	String addr_type;
    	switch(type) {
    		case PUBLIC_ADDR:
    			addr_type = "Public";
    			break;
    		case RANDOM_STATIC_ADDR:
    			addr_type = "Random Static";
    			break;
    		case RANDOM_RESOLVABLE_ADDR:
    			addr_type = "Random Resolvable";
    			break;
    		case RANDOM_NON_RESOLVABLE_ADDR:
    			addr_type = "Random Non-Resolvable";
    			break;
    		default:
    			addr_type = "Unknown Type";
    			break;
    	}
    	return addr_type;
    }
    
    public static String addressToStringFormat(byte []addr) {
    	String str_addr = new String();
    	for(int i=0; i<6; ++i) str_addr += String.format("%02x", addr[i]) + (i<5 ? ":":"");
    	return str_addr;
    }
    
	   public static byte getCodeFromAddressType(AddressType type) {
	    	switch(type) {
	    		case PUBLIC_ADDR:
	    			return CPUBLIC_ADDR;
	    			
	    		case RANDOM_STATIC_ADDR:
	    			return CRANDOM_STATIC_ADDR;
	    			
	    		case RANDOM_RESOLVABLE_ADDR:
	    			return CRANDOM_RESOLVABLE_ADDR;
	    			
	    		case RANDOM_NON_RESOLVABLE_ADDR:
	    			return CRANDOM_NON_RESOLVABLE_ADDR;
	    			
	    		default:
	    			return CPUBLIC_ADDR;
	    	}
	    }
	    
	    public static AddressType getAddressTypeFromCode(byte code) {
	    	switch(code) {
	    		case  CPUBLIC_ADDR:
	    			return AddressType.PUBLIC_ADDR;
	    		
	    		case CRANDOM_STATIC_ADDR:
	    			return AddressType.RANDOM_STATIC_ADDR;
	    		
	    		case CRANDOM_RESOLVABLE_ADDR:
	    			return AddressType.RANDOM_RESOLVABLE_ADDR;
	    			
	    		case CRANDOM_NON_RESOLVABLE_ADDR:
	    			return AddressType.RANDOM_NON_RESOLVABLE_ADDR;
	    			
	    		default:
	    			return AddressType.PUBLIC_ADDR;
	    	}
	    }
	    
	    public static String bytesToHex(byte[] value) {
	        StringBuffer hexString = new StringBuffer();
	        for (int i = 0; i < value.length; i++) {
	            String hex = Integer.toHexString(0xff & value[i]);
	            if (hex.length() == 1) hexString.append('0');
	            hexString.append(hex);
	        }
	        return hexString.toString();
	    }
	    
	    
	    public static byte[] hexToBytes(String s) {
	        int len = s.length();
	        byte[] data = new byte[len / 2];
	        for (int i = 0; i < len; i += 2) {
	            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                    + Character.digit(s.charAt(i+1), 16));
	        }
	        return data;
	    }
	    
	    public static String buildUUIDfromBytes(byte []data, boolean is128bit)
	    {
	    	String uuid;
	    	if (!is128bit)
	    	{
	    		uuid = uuid_16bit.replaceFirst("DU", String.format("%02x", data[0]));
	    		uuid = uuid.replaceFirst("MP", String.format("%02x", data[1]));
	    	}
	    	else
	    	{
	    		uuid = new String();
	    		for(int i = 0; i<16; ++i)
	    		{
	    			uuid = uuid + String.format("%02x", data[i]);
	    			if (i == 3 || i == 5 || i == 7 || i == 9) uuid += "-";
	    		}
	    	}
	    	return uuid;
	    }
	    
	    public static String getAppearanceDescription(short value) {
	    	switch(value) {
	    		case BLE_APPEARANCE_UNKNOWN:
	    			return "Unknown";
	    		case BLE_APPEARANCE_GENERIC_PHONE:
	    			return "Generic Phone";
	    		case BLE_APPEARANCE_GENERIC_COMPUTER:
	    			return "Generic Computer";
	    		case BLE_APPEARANCE_GENERIC_WATCH:
	    			return "Generic Watch";
	    		case BLE_APPEARANCE_WATCH_SPORTS_WATCH:
	    			return "Sports Watch";
	    		case BLE_APPEARANCE_GENERIC_CLOCK:
	    			return "Generic Clock";
	    		case BLE_APPEARANCE_GENERIC_DISPLAY:
	    			return "Generic Display";
	    		case BLE_APPEARANCE_GENERIC_REMOTE_CONTROL:
	    			return "Generic Remote Control";	
	    		case BLE_APPEARANCE_GENERIC_EYE_GLASSES:
	    			return "Generic Eye Glasses";	
	    		case BLE_APPEARANCE_GENERIC_TAG:
	    			return "Generic Tag";	
	    		case BLE_APPEARANCE_GENERIC_KEYRING:
	    			return "Generic Keyring";	
	    		case BLE_APPEARANCE_GENERIC_MEDIA_PLAYER:
	    			return "Generic Media Player";	
	    		case BLE_APPEARANCE_GENERIC_BARCODE_SCANNER:
	    			return "Generic Barcode Scanner";	
	    		case BLE_APPEARANCE_GENERIC_THERMOMETER:
	    			return "Generic Thermometer";	
	    		case BLE_APPEARANCE_THERMOMETER_EAR:
	    			return "Thermometer Ear";	
	    		case BLE_APPEARANCE_GENERIC_HEART_RATE_SENSOR:
	    			return "Generic Heart Rate Sensor";	
	    		case BLE_APPEARANCE_HEART_RATE_SENSOR_HEART_RATE_BELT:
	    			return "Heart Rate Sensor - Belt";	
	    		case BLE_APPEARANCE_GENERIC_BLOOD_PRESSURE:
	    			return "Generic Blood Perssure";	
	    		case BLE_APPEARANCE_BLOOD_PRESSURE_ARM:
	    			return "Blood Pressure - ARM";	
	    		case BLE_APPEARANCE_BLOOD_PRESSURE_WRIST:
	    			return "Blood Presure - Wrist";	
	    		case BLE_APPEARANCE_GENERIC_HID:
	    			return "Generic HID";	
	    		case BLE_APPEARANCE_HID_KEYBOARD:
	    			return "HID - Keyboard";	
	    		case BLE_APPEARANCE_HID_MOUSE:
	    			return "HID - Mouse";	
	    		case BLE_APPEARANCE_HID_JOYSTICK:
	    			return "HID - Joystick";	
	    		case BLE_APPEARANCE_HID_GAMEPAD:
	    			return "HID _ Gamepad";	
	    		case BLE_APPEARANCE_HID_DIGITIZERSUBTYPE:
	    			return "HID - Digitizer";	
	    		case BLE_APPEARANCE_HID_CARD_READER:
	    			return "HID - Card Reader";	
	    		case BLE_APPEARANCE_HID_DIGITAL_PEN:
	    			return "HID - Digital Pen";	
	    		case BLE_APPEARANCE_HID_BARCODE:
	    			return "HID - Barcode";	
	    		case BLE_APPEARANCE_GENERIC_GLUCOSE_METER:
	    			return "Generic Glucose Meter";	
	    		case BLE_APPEARANCE_GENERIC_RUNNING_WALKING_SENSOR:
	    			return "Generic Running Sensor";	
	    		case BLE_APPEARANCE_RUNNING_WALKING_SENSOR_IN_SHOE:
	    			return "Generic Running Sensor - In Shoe";	
	    		case BLE_APPEARANCE_RUNNING_WALKING_SENSOR_ON_SHOE:
	    			return "Generic Running Sensor - On Shoe";	
	    		case BLE_APPEARANCE_RUNNING_WALKING_SENSOR_ON_HIP:
	    			return "Generic Running Sensor - HIP";	
	    		case BLE_APPEARANCE_GENERIC_CYCLING:
	    			return "Generic Cycling";	
	    		case BLE_APPEARANCE_CYCLING_CYCLING_COMPUTER:
	    			return "Cycling Computer";	
	    		case BLE_APPEARANCE_CYCLING_SPEED_SENSOR:
	    			return "Cycling Speed Sensor";	
	    		case BLE_APPEARANCE_CYCLING_CADENCE_SENSOR:
	    			return "Cycling Cadence Sensor";	
	    		case BLE_APPEARANCE_CYCLING_POWER_SENSOR:
	    			return "Cycling Power Sensor";	
	    		case BLE_APPEARANCE_CYCLING_SPEED_CADENCE_SENSOR:
	    			return "Cycling Speed Cedence Sensor";	
	    		default:
	    			return "Unrecognized";
	    	}
	    }
	    
	    public static int getSDKVersion() {
	    	return SDK_SOFTWARE_VERSION;
	    }
	    
	    public static void validateVersion(short fw_version) throws IOException {
	    	if (fw_version < 15)
	    		throw new IOException("Incompatible Firmware Version with SDK");
	    }
}
