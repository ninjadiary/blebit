package cybervelia.sdk.controller.pe.callbacks;

public interface BondKeysCallback {
	void setLTKKeys(final byte[] own_key, int ownkey_size, final byte[] peer_key, int peerkey_size);
}
