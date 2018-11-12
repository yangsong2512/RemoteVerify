package com.omniremotes.remoteverify.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.omniremotes.remoteverify.interfaces.IBluetoothEventListener;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class CoreService extends Service {
    // Used to load the 'native-lib' library on application startup.
    private String TAG = "RemoteVerify-CoreService";
    private BluetoothAdapter mBluetoothAdapter;
    private CoreServiceBinder mBinder;
    private boolean mEnabled = false;
    private boolean mScanning = false;
    private DeviceScanCallback mScanCallback;
    private BluetoothEventReceiver mReceiver;
    private IBluetoothEventListener mListener;
    private String mPairingAddress;
    private MyHandler mHandler = new MyHandler();
    private static final int MESSAGE_START_PAIR = 0;
    private BluetoothProfile mInputDeviceProxy;
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
                    int preState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE,BluetoothAdapter.ERROR);
                    if(state == BluetoothAdapter.STATE_ON){
                        mEnabled = true;
                        startScan(null,ScanSettings.SCAN_MODE_LOW_POWER);
                    }
                }
                break;
                case BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED:
                {

                }
                break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                {
                    int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,BluetoothDevice.ERROR);
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.ERROR);
                    Log.d(TAG,"bond state changed:preState"+preState+",state:"+state);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(state == BluetoothDevice.BOND_BONDED){
                        try{
                            if(mInputDeviceProxy != null){
                                Method method =  mInputDeviceProxy.getClass().getMethod("connect",
                                        BluetoothDevice.class);
                                method.invoke(mInputDeviceProxy,device);
                            }
                        }catch (Exception e){
                            Log.d(TAG,""+e);
                        }
                        mPairingAddress = null;
                    }else if(preState == BluetoothDevice.BOND_BONDED && state == BluetoothDevice.BOND_NONE){
                        String address = device.getAddress();
                        if(mPairingAddress != null && mPairingAddress.equals(address)){
                            startPair(mPairingAddress);
                        }
                    }
                    if(mListener != null){
                        mListener.onBondStateChanged(device,preState,state);
                    }
                }
                break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                {
                    Log.d(TAG,"ACL connected");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(mListener != null){
                        mListener.onAclConnected(device);
                    }
                }
                break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                {
                    Log.d(TAG,"ACL disconnected");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(mListener != null){
                        mListener.onAclDisconnected(device);
                    }
                }
                break;
                case "android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED":
                {
                    int preState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE,BluetoothDevice.ERROR);
                    int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,BluetoothDevice.ERROR);
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if(mListener != null){
                        mListener.onConnectionStateChanged(device,preState,state);
                    }
                    Log.d(TAG,"preState:"+preState+",state:"+state);
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

        public boolean startScan(String address,int scanMode){
            if(svc== null){
                return false;
            }
            return svc.startScan(null,ScanSettings.SCAN_MODE_LOW_POWER);
        }

        public synchronized void startPair(String address){
            if(svc == null){
                return;
            }
            svc.startPair(address);
        }

        public boolean stopScan(){
            if(svc == null){
                return false;
            }
            return svc.stopScan();
        }
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

    private BluetoothProfile.ServiceListener mServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if(profile == 4){
                mInputDeviceProxy = proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if(profile == 4){
                mInputDeviceProxy = null;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            Log.d(TAG,"Device dose not support bluetooth");
            return;
        }
        mBluetoothAdapter.getProfileProxy(getBaseContext(),mServiceListener,4);
        mEnabled = mBluetoothAdapter.isEnabled();
        mBinder = new CoreServiceBinder(this);
        setCoreService(this);
        mReceiver = new BluetoothEventReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        registerReceiver(mReceiver,filter);
        if(mEnabled){
            startScan(null,ScanSettings.SCAN_MODE_LOW_POWER);
        }
    }

    private class DeviceScanCallback extends ScanCallback{
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if(mPairingAddress != null){
                BluetoothDevice device = result.getDevice();
                String address = device.getAddress();
                if(address.equals(mPairingAddress)&&mScanning){
                    stopScan();
                    mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_START_PAIR,device));
                }
            }
            if(mListener != null){
                mListener.onScanResult(result);
            }
        }
    }

    private class MyHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_START_PAIR:
                {
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    if(device != null){
                        Log.d(TAG,"start to create bond");
                        device.createBond();
                    }
                }
                break;
            }
            super.handleMessage(msg);
        }
    }

    public boolean startScan(String address,int scanMode){
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
        if(address!=null){
            filterBuilder.setDeviceAddress(address);
        }
        ScanFilter filter = filterBuilder.build();
        ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
        settingsBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES);
        settingsBuilder.setScanMode(scanMode);
        ScanSettings settings = settingsBuilder.build();
        mScanCallback = new DeviceScanCallback();
        bluetoothLeScanner.startScan(Arrays.asList(filter),settings,mScanCallback);
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

    private boolean startPairProcedure(String address){
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device:devices){
            String bondedAddress = device.getAddress();
            if(address.equals(bondedAddress)){
                try{
                    Method method =  device.getClass().getMethod("removeBond",
                            (Class[]) null);
                    method.invoke(device,(Object[]) null);
                }catch (Exception e) {
                    Log.d(TAG, "" + e);
                }
                return false;
            }
        }
        return true;
    }

    public void startPair(String address){
        if(mBluetoothAdapter == null){
            return;
        }
        mPairingAddress = address;
        stopScan();
        Log.d(TAG,"onStartPair");
        if(startPairProcedure(address)){
            SystemClock.sleep(500);
            startScan(address,ScanSettings.SCAN_MODE_LOW_LATENCY);
        }
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

    public boolean isEnabled(){
        if(mBluetoothAdapter == null){
            return false;
        }
        return mBluetoothAdapter.isEnabled();
    }

    public void registerOnBluetoothEventListener(IBluetoothEventListener listener){
        mListener = listener;
    }

    public boolean isDeviceConnected(BluetoothDevice device){
        if(mInputDeviceProxy != null){
            List<BluetoothDevice> connectedDevice = mInputDeviceProxy.getConnectedDevices();
            for(BluetoothDevice tmp:connectedDevice){
                if(tmp.equals(device)){
                    return true;
                }
            }
        }
        return false;
    }

    public void unRegisterOnBluetoothEventListener(){
        mListener = null;
    }

    public native String stringFromJNI();
    public native void initHidNative();
}
