package com.omniremotes.remoteverify.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.omniremotes.remoteverify.interfaces.IRemoteServiceListener;
import com.omniremotes.remoteverify.utility.OmniOTA;

public class RemoteControlService extends Service {
    private static final String TAG="RemoteVerify-RemoteControlService";
    private static RemoteControlService sRemoteControlService;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mCurrentDevice;
    private RemoteControlServiceBinder mBinder;
    private OmniOTA mOmniBase;
    private IRemoteServiceListener mListener;
    public static synchronized RemoteControlService getInstance(){
        if(sRemoteControlService != null){
            return sRemoteControlService;
        }
        return null;
    }
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
        if(mBinder == null){
            mBinder = new RemoteControlServiceBinder(this);
        }
        mOmniBase = new OmniOTA(getBaseContext());
        setRemoteControlService(this);
    }

    private void setRemoteControlService(RemoteControlService svc){
        sRemoteControlService = svc;
    }


    private static class RemoteControlServiceBinder extends IRemoteControl.Stub{
        RemoteControlService svc;
        RemoteControlServiceBinder(RemoteControlService service){
            svc = service;
        }
        @Override
        public void startVoice(BluetoothDevice device) {
            if(svc == null){
                return;
            }
            svc.startVoice(device);
        }

        @Override
        public void stopVoice(BluetoothDevice device) {
            if(svc == null){
                return;
            }
            svc.stopVoice(device);
        }

        @Override
        public void connect(BluetoothDevice device){
            if(svc == null){
                return;
            }
            svc.connect(device);
        }
    }

    private void connect(BluetoothDevice device){
        if(mCurrentDevice != null && mCurrentDevice.equals(device)){
            Log.d(TAG,"device already under test");
            return;
        }
        if(mCurrentDevice != null && (!mCurrentDevice.equals(device))){
            Log.d(TAG,"close gatt");
            mBluetoothGatt.close();
        }
        mCurrentDevice = device;
        mBluetoothGatt = device.connectGatt(getBaseContext(),false,mBluetoothGattCallback,BluetoothDevice.TRANSPORT_LE);
    }

    private void startVoice(BluetoothDevice device){
        if(mBluetoothGatt == null && (!device.equals(mCurrentDevice))){
            connect(device);
        }else{
            if(mOmniBase != null){
                mOmniBase.openMic();
            }
        }
    }

    private void stopVoice(BluetoothDevice device) {
        if (mBluetoothGatt == null && (!device.equals(mCurrentDevice))) {
            return;
        }
        if (mOmniBase != null) {
            mOmniBase.closeMic();
        }
    }

    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED){
                mBluetoothGatt = gatt;
                if(gatt.discoverServices()){
                    Log.d(TAG,"start to discover service");
                }
                mOmniBase.onGattServiceConnected(gatt);
            }
            if(newState == BluetoothGatt.STATE_DISCONNECTED){
                gatt.close();
                mOmniBase.onGattServiceDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                if(mOmniBase == null){
                    return;
                }
                mOmniBase.onServicesDiscovered(gatt,status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(mOmniBase == null){
                return;
            }
            mOmniBase.onCharacteristicWrite(gatt,characteristic,status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if(mOmniBase == null){
                return;
            }
            mOmniBase.onCharacteristicChanged(gatt,characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d(TAG,"onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG,"onDescriptorWrite");
            super.onDescriptorWrite(gatt, descriptor, status);
            if(status == BluetoothGatt.GATT_FAILURE){
                return;
            }
            if(mOmniBase != null){
                mOmniBase.onDescriptorWrite(gatt,descriptor,status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.d(TAG,"onDescriptorRead");
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand:"+startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mOmniBase != null){
            mOmniBase.onDestroy();
        }
        Log.d(TAG,"onDestroy");
    }
}
