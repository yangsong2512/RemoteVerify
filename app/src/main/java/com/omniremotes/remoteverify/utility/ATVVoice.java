package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_CHAR_CTL;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_CHAR_RX;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_CHAR_TX;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_SERVICE_UUID;

public class ATVVoice extends OmniBase {
    private BluetoothGattCharacteristic mATVTXChara;
    private BluetoothGattCharacteristic mATVRXChara;
    private BluetoothGattCharacteristic mATVCTLChara;
    @Override
    public void onGattServiceConnected(BluetoothGatt gatt){

        super.onGattServiceConnected(gatt);
    }
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            BluetoothGattService service = gatt.getService(UUID.fromString(ATVV_SERVICE_UUID.value));
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic:characteristics){
                String uuid = characteristic.getUuid().toString();
                if(uuid.compareToIgnoreCase(ATVV_CHAR_CTL.value)==0){
                    mATVCTLChara = characteristic;
                }
                if(uuid.compareToIgnoreCase(ATVV_CHAR_RX.value)==0){
                    mATVRXChara = characteristic;
                }
                if(uuid.compareToIgnoreCase(ATVV_CHAR_TX.value)==0){
                    mATVTXChara = characteristic;
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
