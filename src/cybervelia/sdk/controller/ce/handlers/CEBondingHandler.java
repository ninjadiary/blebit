package cybervelia.sdk.controller.ce.handlers;

import java.io.DataInputStream;
import java.io.IOException;

import cybervelia.sdk.controller.ce.CEBLEDeviceCallbackHandler;
import cybervelia.sdk.controller.ce.callbacks.CEBondCallback;

public class CEBondingHandler {
	private CEBLEDeviceCallbackHandler parent_handler;
	private CEBondCallback user_callback = null;
	
	private volatile boolean bond_now_triggered = false;
	private volatile boolean delete_peer_bond_trigger = false;
	// bonded
	private boolean bonded = false;
	public Object block_until_bond = new Object();
	private boolean bonding_in_progress = false;
	private boolean bonding_finished_with_success = false;
	private boolean force_repairing = false;
	private short peer_id = -1;
	private short peer_id_delete_bond = -1;
	private final byte[] EMPTY_PIN = new byte[] {0,0,0,0,0,0};
	
	
	public CEBondingHandler(CEBLEDeviceCallbackHandler parent_handler)
	{
		this.parent_handler = parent_handler;
	}
	
	public void setCallback(CEBondCallback callback)
	{
		this.user_callback = callback;
	}
	
	public final byte[] replyWithPasskey() throws IOException {
		if(user_callback != null)
		{
			return user_callback.getPIN();
		}
		else
		{
			return EMPTY_PIN;
		}
	}
	
	public void setForceRepairing()
	{
		this.force_repairing = true;
	}
	
	public void clearForceRepairing()
	{
		this.force_repairing = false;
	}
	
	public void triggerBondNowCN()
	{
		this.bond_now_triggered = true;
	}
	
	public boolean deletePeerBond(short peer_id)
	{
		if (parent_handler.isScanInProgress()) return true;
		
		this.peer_id_delete_bond = peer_id;
		delete_peer_bond_trigger = true;
		
		return false;
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
	
	public void setPeerIdOnConnect(short peer_id)
	{
		this.peer_id = peer_id;
	}
	
	public void deletePeerBondRiseError()
	{
		user_callback.peerBondDeleteError();
	}
	
	// PIN
	public void pinStatus(int status, int peers_fault) // peer fault can be -1 if bonding failed
	{
		if(user_callback != null)
			user_callback.authStatus(status, peers_fault);
	}
	
	// Bond Now
	public boolean bondNow(boolean force_repairing)
	{
		synchronized(block_until_bond)
		{
			if(bonded == false)
			{
				if (!bonding_in_progress)
				{
					this.force_repairing = force_repairing;
					bond_now_triggered = true;
				}
			}
			else 
				return true;
		}
		
		// Block
		try {
			synchronized(block_until_bond) 
			{
				block_until_bond.wait();
			}
		}catch(InterruptedException e) {}
		
		return bonding_finished_with_success;
	}
	
	// bond triggered
	public boolean isBondNowTriggered() {
		synchronized(block_until_bond)
		{
			if(bond_now_triggered)
			{
				bonding_in_progress = true;
				bond_now_triggered = false;
				return true;
			}
			else
				return false;
		}
	}
	
	// Force re-Pairing on Bond
	public boolean getForceRepairing()
	{
		return force_repairing;
	}
	
	// Bond Failed
	public void bondingFailed(short error, int bond_error_src)
	{
		System.out.println("Bond Failed!");
		
		bonding_finished_with_success = false;
		bonding_in_progress = false;
		
		synchronized(block_until_bond) {block_until_bond.notify();}
	}
	
	// Bond Succeed
	public void bondingSucceed(int procedure)
	{
		bonded = true;
		
		System.out.println("Bond Succeed!");
		
		bonding_finished_with_success = true;
		bonding_in_progress = false;
		
		synchronized(block_until_bond) {block_until_bond.notify();}
		
		if (user_callback != null)
			user_callback.bondingSucceed(procedure);
	}
	
	public boolean isBonded()
	{
		return this.bonded;
	}
	
	public void reset()
	{
		delete_peer_bond_trigger = false;
		this.bonded = false;
		this.bonding_in_progress = false;
		this.bonding_finished_with_success = false;
		synchronized(block_until_bond) {block_until_bond.notify();}
	}
}
