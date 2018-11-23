package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_CMD_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_IMG_BLK_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_SERVICE_UUID;

public class OmniOTA extends OmniVoice{
    private static final String TAG="RemoteVerify-OmniOTA";
    private BluetoothGattCharacteristic mOTAImageBlockChara;
    private BluetoothGattCharacteristic mOTACommandChara;
    @Override
    public void onGattServiceConnected(BluetoothGatt gatt){
        super.onGattServiceConnected(gatt);
        Log.d(TAG,"onGattServiceConnected");

    }

    public OmniOTA(Context context){
        super(context);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            BluetoothGattService service = gatt.getService(UUID.fromString(OMNI_OTA_SERVICE_UUID.value));
            if(service != null) {
                Log.d(TAG, "find Omni OTA service");
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    String uuid = characteristic.getUuid().toString();
                    if (uuid.compareToIgnoreCase(OMNI_OTA_IMG_BLK_UUID.value) == 0) {
                        mOTAImageBlockChara = characteristic;
                    } else if (uuid.compareToIgnoreCase(OMNI_OTA_CMD_UUID.value) == 0) {
                        mOTACommandChara = characteristic;
                    }
                }
            }
        }
        super.onServicesDiscovered(gatt,status);
    }

    @Override
    public void onGattServiceDisconnected(){
        super.onGattServiceDisconnected();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        super.onCharacteristicWrite(gatt,characteristic,status);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
        if(characteristic.equals(mOTACommandChara)){
            return;
        }else if(characteristic.equals(mOTAImageBlockChara)){
            return;
        }
        super.onCharacteristicChanged(gatt,characteristic);
    }
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
    }

    @Override
    public void openMic(){
        super.openMic();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt,descriptor,status);
    }

    @Override
    public void closeMic(){
        super.closeMic();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
