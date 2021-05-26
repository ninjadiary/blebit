package cybervelia.sdk.controller.ce.callbacks;


public interface CEBondCallback {
	public byte[] getPIN();
	/*
		byte pin[] = new byte[6];
		byte input = 0;
		System.out.print("User called to enter PIN Key:");
		DataInputStream keyboard = new DataInputStream(System.in);
		for(int i = 0; i<6; ++i)
		{
			do
			{
				input = keyboard.readByte();
			}
			while(input < 48 || input > 57);
			pin[i] = input;
		}
		System.out.println();
		
		return pin;
	 * */
	
	public void authStatus(int status, int peers_fault);
	public void bondingSucceed(int precedure);
	public void bondingFailed(short error, int bond_error_src);
	public void peerBondDeleteError();
}
