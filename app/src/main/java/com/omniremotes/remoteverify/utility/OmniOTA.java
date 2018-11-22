package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_CMD_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_IMG_BLK_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_SERVICE_UUID;

public class OmniOTA extends OmniVoice{
    private BluetoothGattCharacteristic mOTAImageBlockChara;
    private BluetoothGattCharacteristic mOTACommandChara;
    @Override
    public void onGattServiceConnected(BluetoothGatt gatt){
        super.onGattServiceConnected(gatt);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            BluetoothGattService service = gatt.getService(UUID.fromString(OMNI_OTA_SERVICE_UUID.value));
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic:characteristics){
                String uuid = characteristic.getUuid().toString();
                if(uuid.compareToIgnoreCase(OMNI_OTA_IMG_BLK_UUID.value)==0){
                    mOTAImageBlockChara = characteristic;
                }else if(uuid.compareToIgnoreCase(OMNI_OTA_CMD_UUID.value)==0){
                    mOTACommandChara = characteristic;
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
