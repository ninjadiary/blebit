package cybervelia.sdk.controller.pe.callbacks;

import java.io.DataInputStream;

public interface PEBondCallback {

	
/* AuthStatus Codes: */
/* #define BLE_GAP_SEC_STATUS_SUCCESS                0x00 */ /**< Procedure completed with success. */
/* #define BLE_GAP_SEC_STATUS_TIMEOUT                0x01 */ /**< Procedure timed out. */
/* #define BLE_GAP_SEC_STATUS_PDU_INVALID            0x02 */ /**< Invalid PDU received. */
/* #define BLE_GAP_SEC_STATUS_RFU_RANGE1_BEGIN       0x03 */ /**< Reserved for Future Use range #1 begin. */
/* #define BLE_GAP_SEC_STATUS_RFU_RANGE1_END         0x80 */ /**< Reserved for Future Use range #1 end. */
/* #define BLE_GAP_SEC_STATUS_PASSKEY_ENTRY_FAILED   0x81 */ /**< Passkey entry failed (user cancelled or other). */
/* #define BLE_GAP_SEC_STATUS_OOB_NOT_AVAILABLE      0x82 */ /**< Out of Band Key not available. */
/* #define BLE_GAP_SEC_STATUS_AUTH_REQ               0x83 */ /**< Authentication requirements not met. */
/* #define BLE_GAP_SEC_STATUS_CONFIRM_VALUE          0x84 */ /**< Confirm value failed. */
/* #define BLE_GAP_SEC_STATUS_PAIRING_NOT_SUPP       0x85 */ /**< Pairing not supported.  */
/* #define BLE_GAP_SEC_STATUS_ENC_KEY_SIZE           0x86 */ /**< Encryption key size. */
/* #define BLE_GAP_SEC_STATUS_SMP_CMD_UNSUPPORTED    0x87 */ /**< Unsupported SMP command. */
/* #define BLE_GAP_SEC_STATUS_UNSPECIFIED            0x88 */ /**< Unspecified reason. */
/* #define BLE_GAP_SEC_STATUS_REPEATED_ATTEMPTS      0x89 */ /**< Too little time elapsed since last attempt. */
/* #define BLE_GAP_SEC_STATUS_INVALID_PARAMS         0x8A */ /**< Invalid parameters. */
/* #define BLE_GAP_SEC_STATUS_DHKEY_FAILURE          0x8B */ /**< DHKey check failure. */
/* #define BLE_GAP_SEC_STATUS_NUM_COMP_FAILURE       0x8C */ /**< Numeric Comparison failure. */
/* #define BLE_GAP_SEC_STATUS_BR_EDR_IN_PROG         0x8D */ /**< BR/EDR pairing in progress. */
/* #define BLE_GAP_SEC_STATUS_X_TRANS_KEY_DISALLOWED 0x8E */ /**< BR/EDR Link Key cannot be used for LE keys. */
/* #define BLE_GAP_SEC_STATUS_RFU_RANGE2_BEGIN       0x8F */ /**< Reserved for Future Use range #2 begin. */
/* #define BLE_GAP_SEC_STATUS_RFU_RANGE2_END         0xFF */ /**< Reserved for Future Use range #2 end. */

	public void authStatus(int status, int reason);
	public byte[] getPIN();
	/*
		byte pin[] = new byte[6];
		byte input = 0;
		System.out.print("User called to enter PIN key:");
		DataInputStream keyboard = new DataInputStream(System.in);
		for(int i=0; i<6; ++i)
		{
			do 
			{
				input = keyboard.readByte();
			}
			while(input < 48 || input > 57);
			pin[i] = input;
		}
		System.out.println();
		
	 * */
	
	public void bondSuccessful(int procedure);
	public void bondUnsuccessful(short error, int bond_error_src);
	public void deletePeerBondRiseError();
	public void deletePeerBondSuccessful();
}
