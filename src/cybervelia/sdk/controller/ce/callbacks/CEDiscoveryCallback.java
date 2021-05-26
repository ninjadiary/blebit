package cybervelia.sdk.controller.ce.callbacks;

import java.util.ArrayList;
import java.util.List;

import cybervelia.sdk.controller.BLEService;

public interface CEDiscoveryCallback {
	public void discoveryDone(List<BLEService> services, boolean finished_with_error);
}
