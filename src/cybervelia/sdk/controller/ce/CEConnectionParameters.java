package cybervelia.sdk.controller.ce;

import java.io.IOException;

public class CEConnectionParameters {

	private int min_conn_interval_ms; 
	private int max_conn_interval_ms;
	private int slave_latency_evts;
	private int con_sup_latency_ms; 
	private int scan_interval;
	private int scan_window;
	private int scan_timeout;
	private int conn_req_interval;
	private int conn_req_window;
	private int conn_req_sup_timeout; 
	private boolean enforce_scan_requests;
	
	// Original Values
	private int o_min_conn_interval_ms;
	private int o_max_conn_interval_ms;
	private int o_con_sup_latency_ms;
	
	public CEConnectionParameters() {
		setMinConnectionIntervalMs(50);
		setMaxConnectionIntervalMs(120);
		setSlaveLatency(0);
		setConnectionSupervisionLatencyMs(4000);
		setScanInterval(500);
		setScanWindow(400);
		setScanTimeoutSeconds(0);
		setConnectionRequestInterval(500);
		setConnectionRequestWindow(400);
		setConnectionRequestSupervisionTimeoutMs(0xffff);
		enforceScanRequests(false);
	}
	
	// getters
	
	public int getMinConnectionIntervalMs() {
		return min_conn_interval_ms;
	}
	
	public int getMaxConnectionIntervalMs() {
		return max_conn_interval_ms;
	}
	
	public int getSlaveLatency() {
		return slave_latency_evts;
	}
	
	public int getConnectionSupervisionLatencyMs() {
		return con_sup_latency_ms;
	}
	
	public int getScanInterval() {
		return scan_interval;
	}
	
	public int getScanWindow() {
		return scan_window;
	}
	
	public int getScanTimeout() {
		return scan_timeout;
	}
	
	public int getConnectionRequestInterval() {
		return conn_req_interval;
	}
	
	public int getConnectionRequestWindow() {
		return conn_req_window;
	}
	
	public int getConnectionRequestSupervisionTimeout() {
		return conn_req_sup_timeout;
	}
	
	public boolean scanRequestsEnforced() {
		return enforce_scan_requests;
	}
	
	// setters
	
	public void setMinConnectionIntervalMs(int ms) {
		min_conn_interval_ms = ms;
		o_min_conn_interval_ms = ms;
		min_conn_interval_ms = (((min_conn_interval_ms) * 1000) / (1250));
	}
	
	public void setMaxConnectionIntervalMs(int ms) {
		o_max_conn_interval_ms = ms;
		max_conn_interval_ms = ms;
		max_conn_interval_ms = (((max_conn_interval_ms) * 1000) / (1250));
	}
	
	public void setSlaveLatency(int value) {
		slave_latency_evts = value;
	}
	
	public void setConnectionSupervisionLatencyMs(int ms) {
		o_con_sup_latency_ms = ms;
		con_sup_latency_ms = ms;
		con_sup_latency_ms = con_sup_latency_ms / 10;
	}
	
	public void setScanInterval(int ms) {
		scan_interval = ms;
		scan_interval = (((scan_interval) * 1000) / (625));
	}
	
	public void setScanWindow(int ms) {
		scan_window = ms;
		scan_window = (((scan_window) * 1000) / (625));
	}
	
	public void setScanTimeoutSeconds(int ms) {
		scan_timeout = ms;
	}
	
	public void setConnectionRequestInterval(int ms) {
		conn_req_interval = ms;
		conn_req_interval = (((conn_req_interval) * 1000) / (625));
	}
	
	public void setConnectionRequestWindow(int ms) {
		conn_req_window = ms;
		conn_req_window = (((conn_req_window) * 1000) / (625));
	}
	
	public void setConnectionRequestSupervisionTimeoutMs(int ms) {
		conn_req_sup_timeout = ms;
	}
	
	public void enforceScanRequests(boolean value) {
		enforce_scan_requests = value;
	}
	
	public void validate() throws IOException {
		if (min_conn_interval_ms < 0x0006 || (min_conn_interval_ms > 0x0C80 && min_conn_interval_ms != 0xffff))
			throw new IOException("Min.Con Interval Value out of boundary error");
			
			if (max_conn_interval_ms < 0x0006 || (max_conn_interval_ms > 0x0C80 && max_conn_interval_ms != 0xffff))
			throw new IOException("Max.Con Interval Value out of boundary error");
			
			if (con_sup_latency_ms < 0x000A || (con_sup_latency_ms > 0x0C80 && con_sup_latency_ms != 0xffff))
			throw new IOException("Con.sup Interval Value out of boundary error");
			
			if (slave_latency_evts > 0x03E8)
			throw new IOException("Slave.latency: Number of Events out of boundary error");
			
			if (scan_interval < scan_window) 
			throw new IOException("Scan.Interval have to be larger than Scan.Window");
			
			if (scan_timeout > 0xffff)
			throw new IOException("Scan.timeout Interval Value out of boundary error");
			
			if (scan_window < 4 || scan_window > 0x4000)
			throw new IOException("Scan.window Interval Value out of boundary error");
			
			if (scan_interval < 4 || scan_interval > 0x4000)
			throw new IOException("Scan.window Interval Value out of boundary error");
			
			if (conn_req_interval < conn_req_window) 
			throw new IOException("ConnReq.Interval have to be larger than ConnReq.Window");
			
			if (conn_req_sup_timeout == 0  || conn_req_sup_timeout > 0xffff)
			throw new IOException("Conn.timeout Interval Value out of boundary error");
			
			if (conn_req_window < 4 || conn_req_window > 0x4000)
			throw new IOException("Conn.window Interval Value out of boundary error");
			
			if (conn_req_interval < 4 || conn_req_interval > 0x4000)
			throw new IOException("Conn.window Interval Value out of boundary error");
			
			if ((o_con_sup_latency_ms) < ((1 + slave_latency_evts) * o_max_conn_interval_ms * 2))
			throw new IOException("CE - Connection Sup Value error ((con_sup_ms ["+String.valueOf(o_con_sup_latency_ms)+"] ) > ((1 + slave_latency_evts ["+String.valueOf(slave_latency_evts)+")] * max_conn_interval_ms ["+String.valueOf(o_max_conn_interval_ms)+"] * 2))");
	}
}
