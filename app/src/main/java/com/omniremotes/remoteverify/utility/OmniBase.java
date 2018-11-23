package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;
import android.util.Log;

import com.omniremotes.remoteverify.decoder.ADPCMDecoder;
import com.omniremotes.remoteverify.decoder.AudioTrackPlayer;

import java.io.FileOutputStream;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.CLIENT_CHARACTERISTIC_CONFIG;

public class OmniBase {
    private static final String TAG="RemoteVerify-OmniBase";
    public BluetoothGatt mBluetoothGatt;
    public FileOutputStream mFileOutputStream;
    public AudioTrackPlayer mAudioPlayer;
    public ADPCMDecoder mAdpcmDecoder;
    public Context mContext;
    OmniBase(Context context){
        mContext = context;
    }
    public void onGattServiceConnected(BluetoothGatt gatt){
        mBluetoothGatt = gatt;
        Log.d(TAG,"onGattServiceConnected");
    }

    public void onGattServiceDisconnected(){
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){

    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

    }

    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){

    }

    void enableNotification(BluetoothGattCharacteristic characteristic){
        if(characteristic == null){
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic,true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG.value));
        if(descriptor == null){
            Log.d(TAG,"no ccc descriptor");
            return;
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
    }

    public void openMic(){
        Log.d(TAG,"Error to handling open mic");
    }

    public void closeMic(){
        Log.d(TAG,"Error to handling close mic");
    }

    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
    }

    public void onDestroy(){
        if(mBluetoothGatt != null){
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        if(mContext != null){
            mContext = null;
        }
    }
}
