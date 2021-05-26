package cybervelia.tools.server;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class Transmitter implements Runnable
{
	private Socket client_socket;
	private LinkedBlockingQueue<String> queue;
	private volatile boolean shutdown = false;
	
	public Transmitter(Socket client_socket, LinkedBlockingQueue<String> queue) {
		this.client_socket = client_socket;
		this.queue = queue;
	}
	
	public void shutdown() {
			queue.add(new String()); // unblock/weak thread
			shutdown = true;
	}
	
	@Override
	public void run() {
		try {
			
			MitmActive.doutTx = new DataOutputStream(client_socket.getOutputStream());
			
			while(true) {
				if (shutdown) return;
				String cmd = queue.take();
				if (cmd.length() > 0)
				{
					System.out.println("TX CMD: " + cmd);
					MitmActive.doutTx.writeInt(cmd.length());
					MitmActive.doutTx.write(cmd.toString().getBytes());
					MitmActive.doutTx.flush();
				}
			}
		}catch(Exception ex) {
			System.err.println("Handler of TX Channel: " + ex.getMessage());
			return;
		}
	}
		
	
}