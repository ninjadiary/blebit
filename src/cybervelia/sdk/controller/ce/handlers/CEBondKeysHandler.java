package cybervelia.sdk.controller.ce.handlers;

import java.io.IOException;

import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEBondKeysCallback;

public class CEBondKeysHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEBondKeysCallback user_callback = null;
	//UpdateKey
	private byte []own_key = new byte[31];
	private byte []peer_key = new byte[31];
	private int ltkown_len = 0;
	private int ltkpeer_len = 0;
	
	public CEBondKeysHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	} 
	
	public void setCallback(CEBondKeysCallback callback)
	{
		this.user_callback = callback;
	}
	
	
	
	// LTK Update 
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
	
	public void setLTKKey(final byte[] own_key, int ltkown_len, final byte[] peer_key, int ltkpeer_len)
	{
		if (user_callback != null)
			user_callback.setLTKKeys(own_key, ltkown_len, peer_key, ltkpeer_len);
		
		System.out.print("LTK OWN: ");
		for(int i = 0; i<ltkown_len; ++i)
			System.out.print(String.format("%02x", own_key[i]));
		System.out.println();
		
		System.out.print("LTK PEER: ");
		for(int i = 0; i<ltkpeer_len; ++i)
			System.out.print(String.format("%02x", peer_key[i]));
		System.out.println();
		
		System.arraycopy(peer_key, 0, this.own_key, 0, ltkown_len);
		System.arraycopy(own_key, 0, this.own_key, 0, ltkpeer_len);
		
		this.ltkpeer_len = ltkpeer_len;
		this.ltkown_len = ltkown_len;
	}
	
	public void reset()
	{
		this.ltkown_len = 0;
		this.ltkpeer_len = 0;
	}
}
