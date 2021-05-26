package cybervelia.sdk.controller.ce.callbacks;

public interface CEBondKeysCallback {
	public void setLTKKeys(final byte[] own_key, int ownkey_size, final byte[] peer_key, int peerkey_size);
}
