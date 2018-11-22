package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_VOICE_CONTROL_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_VOICE_DATA_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_VOICE_SERVICE_UUID;

public class OmniVoice extends ATVVoice {
    private BluetoothGattCharacteristic mOmniControlChara;
    private BluetoothGattCharacteristic mOmniVoiceChara;
    @Override
    public void onGattServiceConnected(BluetoothGatt gatt){
        super.onGattServiceConnected(gatt);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            BluetoothGattService service = gatt.getService(UUID.fromString(OMNI_VOICE_SERVICE_UUID.value));
            if(service != null){
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for(BluetoothGattCharacteristic characteristic:characteristics){
                    String uuid = characteristic.getUuid().toString();
                    if(uuid.compareToIgnoreCase(OMNI_VOICE_CONTROL_UUID.value)==0){
                        mOmniControlChara = characteristic;
                    }
                    if(uuid.compareToIgnoreCase(OMNI_VOICE_DATA_UUID.value)==0){
                        mOmniVoiceChara = characteristic;
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
    public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
        super.onCharacteristicChanged(gatt,characteristic);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
    }
}
