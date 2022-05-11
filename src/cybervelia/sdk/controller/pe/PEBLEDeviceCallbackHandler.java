package cybervelia.sdk.controller.pe;

import java.io.IOException;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.pe.callbacks.BondKeysCallback;
import cybervelia.sdk.controller.pe.callbacks.PEBondCallback;
import cybervelia.sdk.controller.pe.callbacks.PEConnectionCallback;
import cybervelia.sdk.controller.pe.callbacks.PENotificationDataCallback;
import cybervelia.sdk.controller.pe.callbacks.PEWriteEventCallback;
import cybervelia.sdk.controller.pe.callbacks.PEReadCallback;
import cybervelia.sdk.controller.pe.callbacks.UpdateValueCallback;
import cybervelia.sdk.controller.pe.handlers.PEAdvertisingHandler;
import cybervelia.sdk.controller.pe.handlers.PEBondKeysHandler;
import cybervelia.sdk.controller.pe.handlers.PEBondingHandler;
import cybervelia.sdk.controller.pe.handlers.PEConnectionHandler;
import cybervelia.sdk.controller.pe.handlers.PENotificationDataHandler;
import cybervelia.sdk.controller.pe.handlers.PEReadHandler;
import cybervelia.sdk.controller.pe.handlers.PEUpdateValueHandler;
import cybervelia.sdk.controller.pe.handlers.PEWriteEventHandler;
import cybervelia.server.CryptoHelper;

public class PEBLEDeviceCallbackHandler{

		private PEUpdateValueHandler update_value_handler;
		private PENotificationDataHandler notification_data_handler;
		private PEReadHandler read_handler;
		private PEAdvertisingHandler advertising_handler;
		private PEBondingHandler bonding_handler;
		private PEWriteEventHandler write_handler;
		private PEBondKeysHandler keys_handler;
		private PEConnectionHandler con_handler;
		private PEController controller = null;
		
		public PEBLEDeviceCallbackHandler()
		{
			this.update_value_handler = new PEUpdateValueHandler(this);
			this.notification_data_handler = new PENotificationDataHandler(this);
			this.read_handler = new PEReadHandler(this);
			this.advertising_handler = new PEAdvertisingHandler(this);
			this.bonding_handler = new PEBondingHandler(this);
			this.write_handler = new PEWriteEventHandler(this);
			this.keys_handler = new PEBondKeysHandler(this);
			this.con_handler = new PEConnectionHandler(this);
		}
		
		// called by user
		public void setController(PEController controller)
		{
			this.controller = controller;
		}
		
		// called by handler
		public boolean pushWrite(short handle, final byte []data, int data_len) {
			
			BLECharacteristic chr = controller.getCharacteristicByHandle(handle);
			if (chr != null)
			{
				chr.setValue(data, data_len);
				return true;
			}
			else
			{
				BLECharacteristic chr_cccd = controller.getCharacteristicByCCCDHandle(handle);
				if (chr_cccd == null)
				{
					System.err.println("WARNING: Device writes to non-existing characteristic");
					return false;
				}
				else
				{
					NotificationValidation validation = chr_cccd.notificationDataSentValidation(data, data_len);
					if (validation != NotificationValidation.NOT_VALID)
					{
						notification_data_handler.setNotificationEnabledWithHandle(chr_cccd.getCCCDHandle(), validation);
						return true;
					}
					else
						return false;
				}
			}
		}
		
		// called by handlers
		public BLECharacteristic getCharacteristicByCCCDHandle(short handle)
		{
			return controller.getCharacteristicByCCCDHandle(handle);
		}
		
		// called by handlers
		public BLECharacteristic getCharacteristicByHandle(short handle)
		{
			return controller.getCharacteristicByHandle(handle);
		}
		
		// called by user - blocks
		public boolean setNotification(short handle, final byte[]data, int data_len) throws IOException {
			byte characteristic_id;
			BLECharacteristic chr = controller.getCharacteristicByCCCDHandle(handle);
			if (chr == null) throw new IOException("Characteristic not found");
			if (data_len > chr.getMaxValueLength()) throw new IOException("Data cannot fit into specified buffer");
			characteristic_id = (byte) chr.getCharacteristicId();
			return notification_data_handler.setNotificationData(characteristic_id, data, data_len);
		}
		
		// called by handler
		public void pushNotification(int characteristic_id, final byte[] data, int data_len) {
			BLECharacteristic chr = controller.getCharacteristicById(String.valueOf(characteristic_id));
			if (chr != null)
			{
				pushWrite(chr.getValueHandle(), data, data_len);
			}
		}
		
		// called by user - blocks
		public boolean setUpdateValue(short handle, final byte[]data, int data_len) throws IOException {
			byte characteristic_id;
			BLECharacteristic chr = controller.getCharacteristicByHandle(handle);
			if (chr == null) throw new IOException("Characteristic not found");
			if (data_len > chr.getMaxValueLength()) throw new IOException("Data cannot fit into specified buffer");
			characteristic_id = (byte) chr.getCharacteristicId();
			return update_value_handler.setUpdateValueData(characteristic_id, data, data_len);
		}
		
		// called by handler
		public void pushUpdateValue(int characteristic_id, final byte[] data, int data_len) {
			BLECharacteristic chr = controller.getCharacteristicById(String.valueOf(characteristic_id));
			if (chr != null)
			{
				pushWrite(chr.getValueHandle(), data, data_len);
			}
		}
		
		// called by User - does not block
		public final int getCharacteristicValue(short handle, byte[] data) throws IOException {
			BLECharacteristic chr = controller.getCharacteristicByHandle(handle);
			if (chr == null) throw new IOException("Characteristic not found");
			return chr.getLocalValue(data);
		}
		
		public void terminate()
		{
			con_handler.terminate();
		}
		
		// called by my handlers
		public boolean isDisconnectTriggeredNC()
		{
			return con_handler.isDisconnectTriggeredNC();
		}
		public boolean isBonded()
		{
			return this.bonding_handler.isBonded();
		}
		
		public void disconnect()
		{
			this.con_handler.disconnect();
		}
		
		public boolean bondNow(boolean force_repairing)
		{
			return this.bonding_handler.bondNow(force_repairing);
		}
		
		public int getDisconnectionReason()
		{
			return this.con_handler.getDisconnectionReason();
		}
		
		public void startAdvertising() throws IOException 
		{
			advertising_handler.startAdvertising();
		}
		
		public int readLTKOwnKey(byte[] data) throws IOException {
			return keys_handler.readLTKOwnKey(data);
		}
		
		public int readLTKPeerKey(byte[] data) throws IOException {
			return keys_handler.readLTKPeerKey(data);
		}
		
		public void stopAdvertising()
		{
			advertising_handler.stopAdvertising();
		}
		
		public boolean isNotificationAllowed(short handle)
		{
			return this.notification_data_handler.isNotificationAllowed(handle);
		}
		
		public short getPeerId()
		{
			return this.bonding_handler.getPeerId();
		}
		
		public void deletePeerBond(short peer_id)
		{
			this.bonding_handler.deletePeerBond(peer_id);
		}
		
		/** User Callback Installs **/
		
		public void installValueUpdateCallback(UpdateValueCallback callback)
		{
			update_value_handler.setCallback(callback);
		}
		
		public void installNotificationDataCallback(PENotificationDataCallback callback)
		{
			notification_data_handler.setCallback(callback);
		}
		
		public void installReadCallback(PEReadCallback callback)
		{
			read_handler.setCallback(callback);
		}
		
		public void installBondCallback(PEBondCallback callback)
		{
			bonding_handler.setCallback(callback);
		}
		
		public void installWriteCallback(PEWriteEventCallback callback)
		{
			write_handler.setCallback(callback);
		}
		
		public void installBondKeysCallback(BondKeysCallback callback)
		{
			keys_handler.setCallback(callback);
		}
		
		public void installConnectionCallback(PEConnectionCallback callback)
		{
			con_handler.setCallback(callback);
		}
		
		// Getters for Event Handler
		
		protected PEUpdateValueHandler getUpdateValueHandler()
		{
			return update_value_handler;
		}
		
		protected PENotificationDataHandler getNotificationDataHandler()
		{
			return notification_data_handler;
		}
		
		protected PEReadHandler getReadHandler()
		{
			return read_handler;
		}
		
		protected PEAdvertisingHandler getAdvertisingHandler()
		{
			return advertising_handler;
		}
		
		protected PEBondingHandler getBondingHandler()
		{
			return bonding_handler;
		}
		
		protected PEWriteEventHandler getWriteEventHandler()
		{
			return write_handler;
		}
		
		protected PEBondKeysHandler getBondKeyHandler()
		{
			return keys_handler;
		}
		
		protected PEConnectionHandler getConnectionHandler()
		{
			return con_handler;
		}
		
		// triggered by handlers
		
		public void untriggerAll() {
			bonding_handler.reset();
			update_value_handler.reset();
			notification_data_handler.reset();
			read_handler.reset();
			advertising_handler.reset();
			keys_handler.reset();
		}
		
		public void setBondPeerId(short peer_id)
		{
			bonding_handler.setPeerId(peer_id);
		}
		
		public void clearBondedFlag()
		{
			bonding_handler.clearBondedFlag();
		}
		
		public boolean isDeviceConnected()
		{
			return con_handler.isDeviceConnected();
		}
		
}
