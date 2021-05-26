package cybervelia.sdk.controller.pe.callbacks;

import cybervelia.sdk.types.ConnectionTypesCommon;

public interface PEConnectionCallback {
	void disconnected(int reason);
	void connected(ConnectionTypesCommon.AddressType address_type, String address);
}
