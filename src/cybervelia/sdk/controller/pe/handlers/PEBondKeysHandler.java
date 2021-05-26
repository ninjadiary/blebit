package cybervelia.sdk.controller.pe.handlers;

import java.io.IOException;

import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.BondKeysCallback;

public class PEBondKeysHandler {
	
	private byte []own_key = new byte[31];
	private byte []peer_key = new byte[31];
	private int ltkown_len = 0;
	private int ltkpeer_len = 0;
	private PEBLEDeviceCallbackHandler parent_handler;
	private BondKeysCallback user_callback;
	
	public PEBondKeysHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(BondKeysCallback callback)
	{
		this.user_callback = callback;
	}
	
	
	// LTK 
	public int readLTKOwnKey(byte[] data) throws IOException {
		if (!parent_handler.isDeviceConnected()) return 0;
		if (!parent_handler.isBonded()) return 0;
		
		if (data.length < ltkown_len)
			throw new IOException("LTK Own key length overflow");
		
		System.arraycopy(own_key, 0, data, 0, ltkown_len);
		
		return ltkown_len;
	}
	
	public int readLTKPeerKey(byte[] data) throws IOException {
		if (!parent_handler.isDeviceConnected()) return 0;
		if (!parent_handler.isBonded()) return 0;
		
		if (data.length < ltkpeer_len)
			throw new IOException("LTK Peer key length overflow");
		
		System.arraycopy(peer_key, 0, data, 0, ltkpeer_len);
		
		return ltkpeer_len;
	}
	
	public void setLTKKey(byte[] own_key, int ownkey_size, byte[] peer_key, int peerkey_size) {
		if (user_callback != null)
			user_callback.setLTKKeys(own_key, ownkey_size, peer_key, peerkey_size);
		
		System.out.print("OWN LTK: ");
		for(int i = 0; i<ownkey_size; ++i)
			System.out.print(String.format("%02X ", own_key[i]));
		System.out.println();
		System.out.print("PEER LTK: ");
		for(int i = 0; i<peerkey_size; ++i)
			System.out.print(String.format("%02X ", peer_key[i]));
		System.out.println();
		
		System.arraycopy(peer_key, 0, this.own_key, 0, ownkey_size);
		System.arraycopy(own_key, 0, this.own_key, 0, peerkey_size);
		
		this.ltkpeer_len = peerkey_size;
		this.ltkown_len = ownkey_size;
	}
	
	
	public void reset()
	{
		ltkown_len = 0;
		ltkpeer_len = 0;
	}
}
