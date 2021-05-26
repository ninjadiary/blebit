package cybervelia.autocloner;

import org.json.JSONException;
import org.json.JSONObject;

import cybervelia.autocloner.DeviceDatum.operation;

class DeviceDatum {
	String characteristic;
	String hexdata;
	int counter;
	enum operation {
		READ,
		NOTIFINDI
	}
	operation op;
	DeviceDatum(String characteristic, String hexdata, int counter, DeviceDatum.operation operation){
		this.characteristic = characteristic;
		this.hexdata = hexdata;
		this.counter = counter;
		this.op = operation;
	}
	
	public JSONObject jsonSerialize() throws JSONException {
		JSONObject datum = new JSONObject();
		if (op == operation.READ)
			datum.put("Operation", "READ");
		else if (op == operation.NOTIFINDI)
			datum.put("Operation", "NOTIFINDI");
		datum.put("hexdata", hexdata);
		datum.put("counter", counter);
		datum.put("characteristic", characteristic);
		return datum;
	}
}
