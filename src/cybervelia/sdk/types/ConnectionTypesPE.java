package cybervelia.sdk.types;

public class ConnectionTypesPE {
	// Request Events from the device
	public static final byte EVT_EMPTY_DEVICE			   = 0x00;
	public static final byte EVT_CLIENT_CONNECTED          = 0x31;
	public static final byte EVT_CLIENT_DISCONNECTED       = 0x32;
	public static final byte EVT_WRITE                     = 0x33;
	public static final byte EVT_READ                      = 0x34;
	public static final byte EVT_READ_AUTH_REQ             = 0x35;
	public static final byte EVT_PIN_STATUS                = 0x36;
	public static final byte EVT_PIN_REQUEST               = 0x37;
	public static final byte EVT_NOTIFICATION_ENABLED      = 0x38;
	public static final byte EVT_NOTIFICATION_DISABLED     = 0x39;
	public static final byte EVT_BONDING_STATUS			   = 0x40;
	public static final byte EVT_INDICATION_ENABLED        = 0x3a;
	public static final byte EVT_INDICATION_DISABLED       = 0x3b;
	public static final byte EVT_SEC_PARAMS_UPDATED        = 0x3d;
	public static final byte EVT_START_ADV                 = 0x3e;
	public static final byte EVT_STOP_ADV                  = 0x3f;
	public static final byte EVT_ENC_LTK_UPDATE            = 0x41;
	public static final byte EVT_REPAIRING		           = 0x42;
	public static final byte EVT_ERROR                     = 0x4f;
	
	// Response Events from user
	public static final byte EVT_EMPTY_USER                = 0x50;
	public static final byte EVT_DELETE_PEER_BOND          = 0x51;
	//
	public static final byte EVT_NOTIFICATION_SEND         = 0x53;
	public static final byte EVT_READ_AUTHORIZE            = 0x54;
	public static final byte EVT_READ_AUTHORIZE_W_DATA     = 0x55;
	public static final byte EVT_VALUE_UPDATE              = 0x56;
	public static final byte EVT_DISCONNECT                = 0x57;
	public static final byte EVT_BOND                      = 0x58;
	public static final byte EVT_PIN_REPLY                 = 0x59;

	// setup
	public static final byte STP_CON_PARAMS 				= 0x01;
	public static final byte STP_CON_PAIRING				= 0x02;
	public static final byte STP_BOND_ON_CONNECT 			= 0x03;
	public static final byte STP_FINISH_SETUP				= 0x04;
	public static final byte STP_NEW_SERVICE_128   			= 0x05; 
	public static final byte STP_NEW_CHARACTERISTIC_128 	= 0x06; 
	public static final byte STP_NEW_SERVICE_16     		= 0x07; 
	public static final byte STP_NEW_CHARACTERISTIC_16 		= 0x08;
	public static final byte STP_ADV_DATA                   = 0x09;
	public static final byte STP_ADV_INTERVAL		  		= 0x0a;
	public static final byte STP_DEV_NAME					= 0x0b;
	public static final byte STP_DEV_APPEARANCE				= 0x0c;
	public static final byte STP_SET_ADDRESS				= 0x0d;
	public static final byte STP_ERASE_BONDS				= 0x0e;
	public static final byte STP_DISABLE					= 0x0f;
	public static final byte STP_CHANNELS_DISABLE			= 0x11;
	
	public static final byte STP_NEW_HANDLE_RETURN			= 0x20;
	
	// advertising
	public static final byte ADV_UUID_COMPLETE				= 0x01;
	public static final byte ADV_UUID_INCOMPLETE        	= 0x02;
	public static final byte ADV_UUID_SOLICITATED			= 0x03;
	public static final byte ADV_MANUFACTURER_DATA			= 0x04;
	public static final byte ADV_FLAGS						= 0x05;
	public static final byte ADV_SH_NAME_LEN				= 0x06;
	public static final byte ADV_SLAVE_CON_INT_ADV			= 0x07;
	public static final byte ADV_SLAVE_CON_INT_SCN			= 0x08;
	
	public static final byte ADV_INCLUDE_DEV_ADDRESS		= 0x09;
	public static final byte ADV_TX_POWER					= 0x0a;
	public static final byte ADV_SERVICE_DATA				= 0x0b;
	public static final byte ADV_DEVICE_NAME_TYPE			= 0x0c;
	public static final byte ADV_APPEARANCE					= 0x0d;
	public static final byte ADV_CUSTOM_DATA				= 0x0e;

			// scan data
	public static final byte SCN_UUID_COMPLETE				= 0x21;
	public static final byte SCN_UUID_INCOMPLETE         	= 0x22;
	public static final byte SCN_UUID_SOLICITATED			= 0x23;
	public static final byte SCN_MANUFACTURER_DATA			= 0x24;
	public static final byte SCN_FLAGS						= 0x25;
	public static final byte SCN_SH_NAME_LEN				= 0x26;
		    
	
	public static final byte SCN_INCLUDE_DEV_ADDRESS		= 0x29;
	public static final byte SCN_TX_POWER					= 0x2a;
	public static final byte SCN_SERVICE_DATA				= 0x2b;
	public static final byte SCN_DEVICE_NAME_TYPE			= 0x2c;
	public static final byte SCN_APPEARANCE					= 0x2d;
	public static final byte SCN_CUSTOM_DATA				= 0x2e;
	
	// errors
	public static final byte ERR_SUCCESS = 0x00;
	public static final byte STP_ERROR  					= 0x2f;
	
	public static final byte EVT_RESET 				 = (byte) 0xfe;
	
	// Device Info
	public static final byte STP_DEVICE_INFO 				= 0x12;
	
	// Dev info subfunctions
	public static final byte DEV_INFO_SUB_SW_INFO			= 0x01;
	public static final byte DEV_INFO_SUB_FW_INFO			= 0x02;
	public static final byte DEV_INFO_SUB_HW_INFO			= 0x03;
	public static final byte DEV_INFO_SUB_MODEL_INFO		= 0x04;
	public static final byte DEV_INFO_SUB_MANUF_NAME_INFO	= 0x05;
	
	// Advertising Channel Type
	public static final byte ADV_CH_37 						= 0x01;
	public static final byte ADV_CH_38 						= 0x02;
	public static final byte ADV_CH_39 						= 0x04;
	
	// Connection Types
	public static final byte BLE_GAP_ADV_TYPE_ADV_UNDIRECTED 	= 0x00;   /**< Connectable undirected. */
	public static final byte BLE_GAP_ADV_TYPE_ADV_DIRECT_IND   	= 0x01;   /**< Connectable directed. */
	public static final byte BLE_GAP_ADV_TYPE_ADV_SCAN_IND     	= 0x02;   /**< Scannable undirected. */
	public static final byte BLE_GAP_ADV_TYPE_ADV_NONCONN_IND 	= 0x03;   /**< Non connectable undirected. */
	
	

    
    public enum UUIDType {
    	COMPLETE_UUID,
    	INCOMPLETE_UUID,
    	SOLICITATED_UUID
    };
    
 
}
