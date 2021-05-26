package cybervelia.sdk.controller.pe.callbacks;

import cybervelia.sdk.controller.BLECharacteristic;

public interface PEWriteEventCallback {
	void writeEvent(BLECharacteristic characteristic, final byte[] data, int data_size, boolean is_cmd, short handle);
}
