package cybervelia.sdk.controller.pe.handlers;

import java.io.IOException;

import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.UpdateValueCallback;

public class PEUpdateValueHandler {
	private boolean is_update_value_data_triggered = false;
	private byte[] update_value_data = new byte[31];
	private int update_value_data_size = 0;
	private byte update_value_characteristic_id = 0;
	private Object lock_until_update_value_received = new Object();
	private volatile boolean update_value_error_generated = false;
	private PEBLEDeviceCallbackHandler parent_handler;
	private UpdateValueCallback user_callback = null;
	
	public PEUpdateValueHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(UpdateValueCallback callback)
	{
		this.user_callback = callback;
	}
	
	// Update Value
	public boolean isUpdateValueTriggered() {
		if (is_update_value_data_triggered)
		{
			is_update_value_data_triggered = false;
			return true;
		}
		else return false;
	}
	
	public final byte[] getUpdateValueData() {
		return update_value_data;
	}
	
	public int getUpdateValueDataSize() {
		return update_value_data_size;
	}
	
	public void setUpdateValueDataSent(boolean update_value_error_generated) {
		this.update_value_error_generated = update_value_error_generated;
		parent_handler.pushUpdateValue(update_value_characteristic_id, update_value_data, update_value_data_size);
		synchronized(lock_until_update_value_received) {lock_until_update_value_received.notify();}
	}
	
	public boolean setUpdateValueData(byte characteristic_id, final byte[] data, int size) throws IOException {
		if (parent_handler.isDisconnectTriggeredNC()) return false;
		if(!parent_handler.isDeviceConnected()) return false;
		if (size > data.length) throw new IOException("Given size is smaller than size of buffer");
		System.arraycopy(data, 0, update_value_data, 0, size);
		update_value_data_size = size;
		update_value_characteristic_id = characteristic_id;
		is_update_value_data_triggered = true;
		try {synchronized(lock_until_update_value_received) {lock_until_update_value_received.wait();}}catch(InterruptedException e) {}
		return !this.update_value_error_generated;
	}
	
	public byte getUpdateValueCharacteristicId() {
		return update_value_characteristic_id;
	}
	
	public void reset()
	{
		update_value_error_generated = true;
		is_update_value_data_triggered = false;
		synchronized(lock_until_update_value_received) {lock_until_update_value_received.notify();}
	}
}
