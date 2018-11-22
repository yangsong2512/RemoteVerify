package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class OmniBase {
    private static final String TAG="OmniBase";
    private BluetoothGatt mBluetoothGatt;
    public void onGattServiceConnected(BluetoothGatt gatt){
        mBluetoothGatt = gatt;
    }

    public void onGattServiceDisconnected(){
        mBluetoothGatt = null;
    }

    public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){

    }

    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

    }

    public void enableNotification(BluetoothGattCharacteristic characteristic){
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
        }
    }
}
