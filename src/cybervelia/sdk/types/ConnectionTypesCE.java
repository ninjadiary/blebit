package cybervelia.sdk.types;

public class ConnectionTypesCE {
	
	// Errors
	public static final byte ERR_SUCCESS             = 0x00;
	public static final byte STP_ERROR               = 0x2f;
	
	public static final byte EVT_EMPTY_DEVICE        = 0x00;
	
	// Unassociated Events
	
	// Sent by User
	public static final byte STP_CONN_PARAMS         = 0x01;
	public static final byte STP_ERASE_BONDS         = 0x02;
	public static final byte STP_DISABLE_REPAIRING   = 0x03;
	public static final byte STP_PAIRING_INFO        = 0x04;
	public static final byte STP_SET_ADDRESS         = 0x05;
	public static final byte STP_FINISH              = 0x06;
	public static final byte EVT_CONNECT_REQ         = 0x07;
	public static final byte EVT_STARTSTOP_SCAN      = 0x08;
	public static final byte EVT_OPENCLOSE_LED       = 0x09;
	public static final byte EVT_CONNECT_CANCEL      = 0x0a;
	public static final byte EVT_BOND_ON_CONNECT     = 0x0b;
	
	// Sent by Device
	public static final byte EVT_ADV_INFO            = 0x21;
	public static final byte EVT_BTN_PRESS           = 0x22;
	public static final byte EVT_CONN_TIMEOUT        = 0x23;
	public static final byte EVT_SCAN_TIMEOUT        = 0x24;
	// 0x28 reserved by associated-sent-by-device
	// 0x29 reserved by associated-sent-by-device
	
	// Associated Events
	
	// Sent by User
	public static final byte EVT_START_DISCOVERY	= 0x40;
	public static final byte EVT_WRITE_REQ			= 0x41;
	public static final byte EVT_READ_REQ			= 0x42;
	public static final byte EVT_PIN_REPLY			= 0x43;
	public static final byte EVT_DISCONNECT_NOW     = 0x44;
	public static final byte EVT_BOND_NOW		    = 0x45;
	public static final byte EVT_WRITECMD_REQ		= 0x46;
	
	// Sent by Device
	public static final byte EVT_DELETE_PEER_BOND	 = 0x28;
	public static final byte EVT_BONDING_STATUS		 = 0x29;
	public static final byte EVT_LTK_UPDATE          = 0x30;
	public static final byte EVT_CONNECTED           = 0x31;
	public static final byte EVT_DISCONNECTED        = 0x32;
	public static final byte EVT_DISCOVERY_DONE		 = 0x33;
	public static final byte EVT_DISCOVERY_ITEM		 = 0x34;
	public static final byte EVT_WRITE_RESP			 = 0x35;
	public static final byte EVT_READ_RESP			 = 0x36;
	public static final byte EVT_NOTIFICATION		 = 0x37;
	public static final byte EVT_PIN_REQUEST		 = 0x38;
	public static final byte EVT_PIN_STATUS			 = 0x39;
	
	public static final byte EVT_RESET 				 = (byte) 0xfe;
}
