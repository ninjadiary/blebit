package cybervelia.sdk.controller.ce.callbacks;

import cybervelia.sdk.types.ConnectionTypesCommon;

public interface CEConnectionCallback {
	void disconnected(int reason);
	void connected(ConnectionTypesCommon.AddressType address_type, String address);
}
