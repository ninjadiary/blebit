package cybervelia.sdk.controller.ce;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cybervelia.sdk.controller.BLECharacteristic;
import cybervelia.sdk.controller.BLEService;
import cybervelia.sdk.controller.ce.callbacks.CEAdvertisementCallback;
import cybervelia.sdk.controller.ce.callbacks.CEBondCallback;
import cybervelia.sdk.controller.ce.callbacks.CEBondKeysCallback;
import cybervelia.sdk.controller.ce.callbacks.CEConnectionCallback;
import cybervelia.sdk.controller.ce.callbacks.CEDiscoveryCallback;
import cybervelia.sdk.controller.ce.callbacks.CENotificationEventCallback;
import cybervelia.sdk.controller.ce.callbacks.CEReadCallback;
import cybervelia.sdk.controller.ce.callbacks.CEScanCallback;
import cybervelia.sdk.controller.ce.callbacks.CEWriteCallback;
import cybervelia.sdk.controller.ce.handlers.CEAdvertisementHandler;
import cybervelia.sdk.controller.ce.handlers.CEBondKeysHandler;
import cybervelia.sdk.controller.ce.handlers.CEBondingHandler;
import cybervelia.sdk.controller.ce.handlers.CEConnectionHandler;
import cybervelia.sdk.controller.ce.handlers.CEDiscoveryHandler;
import cybervelia.sdk.controller.ce.handlers.CENotificationEventHandler;
import cybervelia.sdk.controller.ce.handlers.CEReadRequestHandler;
import cybervelia.sdk.controller.ce.handlers.CEScanHandler;
import cybervelia.sdk.controller.ce.handlers.CEWriteRequestHandler;
import cybervelia.sdk.types.ConnectionTypesCommon;

public class CEBLEDeviceCallbackHandler {
	
	private CEController controller = null;
	
	private CEConnectionHandler con_handler;
	private CEBondingHandler bond_handler;
	private CEBondKeysHandler keys_handler; 
	private CEScanHandler scan_handler;
	private CEAdvertisementHandler adv_handler;
	private CEDiscoveryHandler discovery_handler;
	private CEReadRequestHandler read_handler;
	private CEWriteRequestHandler write_handler;
	private CENotificationEventHandler notification_handler;
	
	public CEBLEDeviceCallbackHandler()
	{
		con_handler = new CEConnectionHandler(this);
		bond_handler = new CEBondingHandler(this);
		keys_handler = new CEBondKeysHandler(this);
		scan_handler = new CEScanHandler(this);
		adv_handler = new CEAdvertisementHandler(this);
		discovery_handler = new CEDiscoveryHandler(this);
		read_handler = new CEReadRequestHandler(this, bond_handler);
		write_handler = new CEWriteRequestHandler(this, bond_handler);
		notification_handler = new CENotificationEventHandler(this);
	}
	
	// called by user
	public void setController(CEController controller)
	{
		this.controller = controller;
	}
	
	public boolean connect(byte []addr, ConnectionTypesCommon.AddressType type, boolean block) {
		return con_handler.connectRequest(addr, type, block);
	}
	
	// connected event
	public void deviceConnectedEvent(byte address[], byte addr_type, short peer_id) {
		con_handler.deviceConnectedEvent(address, addr_type, peer_id);
		scan_handler.clearScanInProgress();
		bond_handler.setPeerIdOnConnect(peer_id);
	}
	
	public void deviceDisconnectedEvent(int reason) 
	{
		write_handler.reset();
		read_handler.reset();
		bond_handler.reset();
		keys_handler.reset();
		discovery_handler.reset();
		con_handler.deviceDisconnectedEvent(reason);
	}
	
	// This is called from the CEDiscoveryHandler when discovery is finished successfully
	public void addDiscoveryServicesOnComplete(List<BLEService> services_list) {
		controller.addServicesOnDiscoveryComplete(services_list);
	}
	
	public BLECharacteristic getCharacteristicByHandle(short handle)
	{
		return controller.getCharacteristicByHandle(handle, con_handler.getClientAddress());
	}
	
	public BLECharacteristic getCharacteristicByCCCDHandle(short handle)
	{
		return controller.getCharacteristicByCCCDHandle(handle, con_handler.getClientAddress());
	}
	
	public int readLTKPeerKey(byte[] data) throws IOException {
		return this.keys_handler.readLTKPeerKey(data);
	}
	
	public int readLTKOwnKey(byte[] data) throws IOException
	{
		return this.keys_handler.readLTKOwnKey(data);
	}
	
	public boolean startScan() {
		return this.scan_handler.startScan();
	}
	
	public void stopScan() {
		this.scan_handler.stopScan();
	}
	
	public boolean isScanInProgress()
	{
		return this.scan_handler.isScanInProgress();
	}
	
	public void advertisementPacket(byte []address, byte address_type, byte scan_adv, byte advertisement_type, byte rssi, byte datalen, byte []data)
	{
		this.adv_handler.advertisementPacket(address, address_type, scan_adv, advertisement_type, rssi, datalen, data);
	}
	
	public boolean startDiscovery(boolean block) {
		return this.discovery_handler.startDiscovery(block);
	}
	
	public boolean isDiscoveryInProgress() {
		return this.discovery_handler.isDiscoveryInProgress();
	}
	
	public boolean getDiscoverySuccess() {
		return this.discovery_handler.getDiscoverySuccess();
	}
	
	public boolean bondNow(boolean force_repairing)
	{
		return this.bond_handler.bondNow(force_repairing);
	}
	
	public boolean deletePeerBond(short peer_id)
	{
		return this.bond_handler.deletePeerBond(peer_id);
	}
	
	public int readData(byte[] data, int offset, int len, short handle, int tm_ms) throws IOException
	{
		return this.read_handler.readData(data, offset, len, handle, tm_ms);
	}
	
	public void terminate()
	{
		con_handler.terminate();
	}
	
	/** ------------- Functions called by handlers ------------------ **/
	public void bondingFailed(short error, int bond_error_src)
	{
		write_handler.bondFailed();
		read_handler.onBondFailed();
		bond_handler.bondingFailed(error, bond_error_src);
	}
	
	/** ------------- Functions called by handlers ------------------ **/
	public void bondingSucceed(int procedure)
	{
		write_handler.bondSucceed();
		read_handler.onBondSucceed();
		bond_handler.bondingSucceed(procedure);
	}
	
	/** ------------- Functions called by handlers ------------------ **/
	public boolean isBonded()
	{
		return bond_handler.isBonded();
	}
	
	public void notificationReceived(final byte []data, int data_len, short handle)
	{
		this.notification_handler.notificationReceived(data, data_len, handle);
	}
	
	public boolean writeData(byte[] data, int offset, int len, short handle, int tm_ms)
	{
		return write_handler.writeData(data, offset, len, handle, tm_ms);
	}
	
	public void writeDataCMD(byte[] data, int offset, int len, short handle)
	{
		write_handler.writeDataCMD(data, offset, len, handle);
	}
	
	// General Helper Functions for user
	public boolean isDeviceConnected() {
		return con_handler.isDeviceConnected();
	}
	
	public String getClientAddress() {
		return con_handler.getClientAddress();
	}
	
	public ConnectionTypesCommon.AddressType getClientAddressType(){
		return con_handler.getClientAddressType();
	}
	
	public void disconnect(int reason) {
		con_handler.disconnect(reason);
	}
	
	public boolean isConnectRequestInProgress()
	{
		return con_handler.isConnectRequestInProgress();
	}
	
	public boolean cancelConnectRequest() {
		return con_handler.cancelConnectRequest();
	}
	
	public int getDisconnectionReason() {
		return con_handler.getDisconnectionReason();
	}
	
	// Installers
	
	public void installConnectionCallback(CEConnectionCallback callback) {
		con_handler.setCallback(callback);
	}
	
	public void installBondCallback(CEBondCallback callback) {
		bond_handler.setCallback(callback);
	}
	
	public void installBondKeysCallback(CEBondKeysCallback callback)
	{
		keys_handler.setCallback(callback);
	}
	
	public void installScanCallback(CEScanCallback callback)
	{
		scan_handler.setCallback(callback);
	}
	
	public void installAdvertisementCallback(CEAdvertisementCallback callback)
	{
		adv_handler.setCallback(callback);
	}
	
	public void installDiscoveryCallback(CEDiscoveryCallback callback)
	{
		discovery_handler.setCallback(callback);
	}
	
	public void installReadCallback(CEReadCallback callback)
	{
		read_handler.setCallback(callback);
	}
	
	public void installWriteCallback(CEWriteCallback callback)
	{
		write_handler.setCallback(callback);
	}
	
	public void installNotificationEventCallback(CENotificationEventCallback callback)
	{
		notification_handler.setCallback(callback);
	}
	
	// Handlers Getters
	
	public CEConnectionHandler getConnectionHandler() {
		return con_handler;
	}
	
	public CEBondingHandler getBondHandler() {
		return bond_handler;
	}
	
	public CEBondKeysHandler getBondKeysHandler()
	{
		return keys_handler;
	}
	
	public CEScanHandler getScanHandler()
	{
		return scan_handler;
	}
	
	public CEAdvertisementHandler getAdvertisementHandler()
	{
		return adv_handler;
	}
	
	public CEDiscoveryHandler getDiscoveryHandler()
	{
		return discovery_handler;
	}
	
	public CEReadRequestHandler getReadRequestHandler()
	{
		return read_handler;
	}
	
	public CEWriteRequestHandler getWriteRequestHandler()
	{
		return write_handler;
	}
	
	public CENotificationEventHandler getNotificationHandler()
	{
		return notification_handler;
	}


}
