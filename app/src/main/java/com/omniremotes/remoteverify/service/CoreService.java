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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private BluetoothEventReceiver mReceiver;
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

    private class BluetoothEventReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null){
                return;
            }
            switch (action){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                    if(state == BluetoothAdapter.STATE_ON){
                        mEnabled = true;
                        startScan();
                    }
                }
                break;
            }
        }
    }

    private static class CoreServiceBinder extends ICoreService.Stub{
        CoreService svc;
        CoreServiceBinder(CoreService service){
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
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            Log.d(TAG,"Device dose not support bluetooth");
            return;
        }
        mEnabled = mBluetoothAdapter.isEnabled();
        mBinder = new CoreServiceBinder(this);
        setCoreService(this);
        mReceiver = new BluetoothEventReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver,filter);
        if(mDeviceListAdapter == null) {
            mDeviceListAdapter = new DeviceListAdapter(getBaseContext());
        }
        if(mScanListAdapter == null){
            mScanListAdapter = new ScanListAdapter(getBaseContext());
        }
        if(mEnabled){
            startScan();
        }
    }

    private class DeviceScanCallback extends ScanCallback{
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(mScanListAdapter != null){
                mScanListAdapter.notifyDataSetChanged(result);
            }
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
        settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        ScanSettings settings = settingsBuilder.build();
        mScanCallback = new DeviceScanCallback();
        bluetoothLeScanner.startScan(null,settings,mScanCallback);
        mScanning = true;
        Log.d(TAG,"ble is scanning");
        return true;
    }

    public boolean stopScan(){
        Log.d(TAG,"stop scan");
        if(!mScanning || mBluetoothAdapter == null){
            Log.e(TAG,"scan already stopped");
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
        Log.d(TAG,"onServiceDestroy");
        sCoreService = null;
        stopScan();
        if(mReceiver != null){
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    public native String stringFromJNI();
    public native void initHidNative();
}
