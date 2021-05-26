package cybervelia.sdk.controller.ce.callbacks;

import cybervelia.sdk.controller.BLECharacteristic;

public interface CEReadCallback {
	public void readResponse(BLECharacteristic characteristic, final byte[] read_req_data, int data_len, boolean with_error);
	public void readResponseRaw(short handle, final byte[] read_req_data, int data_len, boolean with_error);
}
