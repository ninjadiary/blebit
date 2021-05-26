package cybervelia.sdk.controller.pe;

import java.io.IOException;

import cybervelia.sdk.types.ConnectionTypesPE;

public class PEConnectionParameters {
	private int first_conn_params_update_delay;
	private int next_conn_params_update_delay;
	private int max_conn_params_update_count;
	private int min_conn_interval_ms;
	private int max_conn_interval_ms;
	private int slave_latency_evts;
	private int con_sup_latency_ms;
	private int advertisement_timeout;
	private byte connection_type;
	
	public PEConnectionParameters()
	{
		first_conn_params_update_delay = 5000;
		next_conn_params_update_delay = 30000;
		max_conn_params_update_count = 3;
		min_conn_interval_ms = 50;
		max_conn_interval_ms = 60;
		slave_latency_evts = 0;
		con_sup_latency_ms = 4000;
		advertisement_timeout = 0; // advertisement timeout in 0.625 ms units, 0 = no timeout
		connection_type = ConnectionTypesPE.BLE_GAP_ADV_TYPE_ADV_UNDIRECTED;
	}
	
	// Getters
	
	public int getFirstConnectionParametersUpdateDelay()
	{
		return this.first_conn_params_update_delay;
	}
	
	public int getNextConnectionParametersUpdateDelay()
	{
		return this.next_conn_params_update_delay;
	}
	
	public int getMaxConnectionParametersUpdateCounter()
	{
		return this.max_conn_params_update_count;
	}
	
	public int getMinConnectionIntervalMS()
	{
		return this.min_conn_interval_ms;
	}
	
	public int getMaxConnectionIntervalMS()
	{
		return this.max_conn_interval_ms;
	}
	
	public int getSlaveLatencyMS()
	{
		return this.slave_latency_evts;
	}
	
	public int getConnectionSupervisionTimeoutMS()
	{
		return this.con_sup_latency_ms;
	}
	
	public int getAdvertisementTimeout()
	{
		return this.advertisement_timeout;
	}
	
	public byte getConnectionType()
	{
		return this.connection_type;
	}
	
	// Setters
	
	public void setFirstConnectionParametersUpdateDelay(int value)
	{
		this.first_conn_params_update_delay = value;
	}
	
	public void setNextConnectionParametersUpdateDelay(int value)
	{
		this.next_conn_params_update_delay = value;
	}
	
	public void setMaxConnectionParametersUpdateCounter(int value)
	{
		this.max_conn_params_update_count = value;
	}
	
	public void setMinConnectionIntervalMS(int value) throws IOException
	{
		if (value < 0x0006 || value > 0x0C80)
			throw new IOException("Interval Value out of boundary error");
		this.min_conn_interval_ms = value;
	}
	
	public void setMaxConnectionIntervalMS(int value) throws IOException
	{
		if (value < 0x0006 || value > 0x0C80)
			throw new IOException("Interval Value out of boundary error");
		this.max_conn_interval_ms = value;
	}
	
	public void setSlaveLatencyMS(int value) throws IOException
	{
		if (value > 0x01F3)
			throw new IOException("Number of Events out of boundary error");
		this.slave_latency_evts = value;
	}
	
	public void setConnectionSupervisionTimeoutMS(int value) throws IOException
	{
		if (value < 0x000A || value > 0x0C80)
			throw new IOException("Interval Value out of boundary error");
		this.con_sup_latency_ms = value;
	}
	
	public void setAdvertisementTimeoutTU(int value) throws IOException
	{
		// advertisement timeout in 0.625 ms units
		if (value > 0x4000 || (value < 0x0020 && value != 0))
			throw new IOException("Timeout Interval out of accepted range");
		this.advertisement_timeout = value;
	}
	
	public void setConnectionType(byte value) throws IOException
	{
		// 
		if (value > 3)
			throw new IOException("Unknown Connection Type");
		this.connection_type = value;
	}
	
	public boolean validate()
	{
		return  (con_sup_latency_ms) > ((1 + slave_latency_evts) * max_conn_interval_ms * 2);
	}
}
