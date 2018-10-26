package com.omniremotes.remoteverify.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.omniremotes.remoteverify.adapter.DeviceListAdapter;
import com.omniremotes.remoteverify.adapter.ScanListAdapter;

import java.util.Arrays;
import java.util.Set;

public class CoreService extends Service {
    // Used to load the 'native-lib' library on application startup.
    private String TAG = "RemoteVerify-CoreService";
    private BluetoothAdapter mBluetoothAdapter;
    private CoreServiceBinder mBinder;
    private boolean mEnabled = false;
    private DeviceListAdapter mDeviceListAdapter;
    private ScanListAdapter mScanListAdapter;
    private boolean mScanning = false;
    private DeviceScanCallback mScanCallback;
    static {
        System.loadLibrary("native-lib");
    }

    private static CoreService sCoreService;
    public static synchronized CoreService getCoreService(){
        if(sCoreService != null){
            return sCoreService;
        }
        return null;
    }
    public CoreService() {

    }

    private static class CoreServiceBinder extends ICoreService.Stub{
        CoreService svc;
        public CoreServiceBinder(CoreService service){
            svc = service;
        }
        @Override
        public void initHid(){

        }
        @Override
        public void notifyBluetoothStateChanged(boolean enabled) {
            if (svc == null) {
                return;
            }
            svc.notifyBluetoothStateChanged(enabled);
        }

        public boolean startScan(){
            if(svc== null){
                return false;
            }
            return svc.startScan();
        }

        public boolean stopScan(){
            if(svc == null){
                return false;
            }
            return svc.stopScan();
        }
    }


    public void notifyBluetoothStateChanged(boolean enabled){
        mEnabled = enabled;
        startScan();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(TAG,"onBind");
        return mBinder;
    }

    private static synchronized void setCoreService(CoreService instance){
        sCoreService = instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        mBinder = new CoreServiceBinder(this);
        setCoreService(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            Log.d(TAG,"This device does not support bluetooth");
            return;
        }
        if(mDeviceListAdapter == null) {
            mDeviceListAdapter = new DeviceListAdapter(getBaseContext());
        }
        if(mScanListAdapter == null){
            mScanListAdapter = new ScanListAdapter(getBaseContext());
        }
        startScan();
    }

    private class DeviceScanCallback extends ScanCallback{
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG,"on new scan result");
        }
    }

    public boolean startScan(){
        if(!mEnabled||mScanning){
            Log.d(TAG,"bluetooth is not enabled or is scanning");
            return false;
        }
        Log.d(TAG,"startScan");
        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if(bluetoothLeScanner == null){
            Log.d(TAG,"bluetoothLeScanner is null");
            return false;
        }

        ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
        ScanFilter filter = filterBuilder.build();
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        ScanSettings settings = settingsBuilder.build();
        mScanCallback = new DeviceScanCallback();
        bluetoothLeScanner.startScan(Arrays.asList(filter),settings,mScanCallback);
        mScanning = true;
        Log.d(TAG,"ble is scanning");
        return true;
    }

    public boolean stopScan(){
        if(!mScanning || mBluetoothAdapter == null){
            return false;
        }
        BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
        mScanning = false;
        return true;
    }

    public ScanListAdapter getScanListAdapter() {
        return mScanListAdapter;
    }

    public DeviceListAdapter getDeviceListAdapter(){
        return mDeviceListAdapter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sCoreService = null;
        stopScan();
    }

    public native String stringFromJNI();
    public native void initHidNative();
}
