package cybervelia.sdk.controller.pe.handlers;

import java.io.DataInputStream;
import java.io.IOException;

import cybervelia.sdk.controller.pe.PEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.pe.callbacks.PEBondCallback;

public class PEBondingHandler {
	private volatile boolean bond_triggered = false;
	private boolean bonded = false;
	private Object block_until_bond = new Object();
	private boolean bonding_in_progress = false;
	private boolean bonding_with_error = false;
	private boolean force_repairing = false;
	private short peer_id = -1;
	private short peer_id_delete_bond = -1;
	private PEBLEDeviceCallbackHandler parent_handler;
	private PEBondCallback user_callback = null;
	private volatile boolean delete_peer_bond_trigger = false;
	private final byte[] EMPTY_PIN = new byte[] {0,0,0,0,0,0};
	
	
	
	public PEBondingHandler(PEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(PEBondCallback callback)
	{
		this.user_callback = callback;
	}
	
	
	
	public void setPeerId(short peer_id)
	{
		this.peer_id = peer_id;
	}
	
	public void setBondedFlag()
	{
		this.bonded = true;
	}
	
	public void clearBondedFlag()
	{
		this.bonded = false;
	}
	
	public void deletePeerBond(short peer_id)
	{
		this.peer_id_delete_bond = peer_id;
		delete_peer_bond_trigger = true;
	}
	
	public boolean isDeletePeerBondTriggered() {
		if (delete_peer_bond_trigger)
		{
			delete_peer_bond_trigger = false;
			return true;
		}
		else return false;
	}
	
	public short getDeleteBondPeerId()
	{
		return peer_id_delete_bond;
	}
	
	public short getPeerId()
	{
		return peer_id;
	}
	
	public void deletePeerBondRiseError()
	{
		if (user_callback != null)
			user_callback.deletePeerBondRiseError();
	}
	
	public void deletePeerBondSuccess()
	{
		if(user_callback != null)
			user_callback.deletePeerBondSuccessful();
	}
	
	
	public boolean bondNow(boolean force_repairing) {
		synchronized(block_until_bond)
		{
			if(bonded == false)
			{
				if (!bonding_in_progress)
				{
					this.force_repairing = force_repairing;
					bond_triggered = true;
				}
			}
			else 
				return false;
		}
		
		// Block
		try {
			synchronized(block_until_bond) 
			{
				block_until_bond.wait();
			}
		}catch(InterruptedException e) {}
		
		return !bonding_with_error;
	}
	
	// Bond Failed
	public void bondingFailed(short error, int bond_error_src)
	{
		bonding_with_error = true;
		bonding_in_progress = false;
		
		synchronized(block_until_bond) {block_until_bond.notify();}
		
		if (user_callback != null)
			user_callback.bondUnsuccessful(error, bond_error_src); // 0 = Local Failure, 1 = Remote Failure
	}
	
	// Bond Succeed
	public void bondingSucceed(int procedure)
	{
		bonded = true;
		
		bonding_with_error = false;
		bonding_in_progress = false;
		
		synchronized(block_until_bond) {block_until_bond.notify();}
		
		if(user_callback != null)
			user_callback.bondSuccessful(procedure);
	}
	
	// Force re-Pairing on Bond
	public boolean getForceRepairing()
	{
		return force_repairing;
	}
	
	// bond triggered
	public boolean isBondNowTriggered() {
		synchronized(block_until_bond)
		{
			if(bond_triggered)
			{
				bonding_in_progress = true;
				bond_triggered = false;
				return true;
			}
			else
				return false;
		}
	}
	
	// bonded
	public boolean isBonded() {
		return bonded;
	}
	
	public final byte[] replyWithPasskey() throws IOException {
		if (user_callback != null)
		{
			return user_callback.getPIN();
		}
		else
		{
			return EMPTY_PIN;
		}
	}
	
	// Possibly called because of Missing Keys - Repairing
	public void setRepairingHappened() {
		if (user_callback != null)
			user_callback.authStatus(0xbb00, 0);
	}
	
	public void authStatus(int status, int reason)
	{
		if (user_callback != null)
			user_callback.authStatus(status, reason);
	}
	
	public void reset()
	{
		peer_id = -1;
		delete_peer_bond_trigger = false;
		bond_triggered = false;
		this.bonding_in_progress = false;
		this.bonding_with_error = false;
		this.bonded = false;
		synchronized(block_until_bond) {block_until_bond.notify();}
	}
}
