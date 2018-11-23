package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import java.util.List;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.CLIENT_CHARACTERISTIC_CONFIG;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_VOICE_CONTROL_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_VOICE_DATA_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_VOICE_SERVICE_UUID;

public class OmniVoice extends ATVVoice{
    private static final String TAG="RemoteVerify-OmniVoice";
    private BluetoothGattCharacteristic mOmniControlChara;
    private BluetoothGattCharacteristic mOmniVoiceChara;
    private boolean mOmniVoiceServiceSupported = false;
    private boolean mNotificationEnabled = false;

    OmniVoice(Context context){
        super(context);
    }
    @Override
    public void onGattServiceConnected(BluetoothGatt gatt){
        super.onGattServiceConnected(gatt);
        Log.d(TAG,"onGattServiceConnected");
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        super.onCharacteristicWrite(gatt,characteristic,status);
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            BluetoothGattService service = gatt.getService(UUID.fromString(OMNI_VOICE_SERVICE_UUID.value));
            if(service != null){
                Log.d(TAG,"find Omni voice service");
                mOmniVoiceServiceSupported = true;
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
        if(mOmniVoiceServiceSupported){
            if(characteristic.equals(mOmniVoiceChara)){
                processAudioData(characteristic);
                return;
            }
        }

        super.onCharacteristicChanged(gatt,characteristic);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
    }

    @Override
    public void openMic(){
        if(mOmniVoiceServiceSupported){
            Log.d(TAG,"try to open mic");
            if(!mNotificationEnabled){
                enableNotification(mOmniVoiceChara);
                return;
            }
            byte[] bytes = new byte[]{(byte)0xA2,0x01};
            if(mOmniControlChara != null && mBluetoothGatt != null){
                Log.d(TAG,"open omni mic");

                mOmniControlChara.setValue(bytes);
                mBluetoothGatt.writeCharacteristic(mOmniControlChara);
                mVersion = 0x0;
                mCodecSupported = 0x02;
                mBytesPerFrame = 134;
                mBytesPerChara = 20;
                onAudioStart();
                return;
            }
        }

        super.openMic();
    }

    @Override
    public void closeMic(){
        byte[] bytes = new byte[]{(byte)0xA2,0x00};
        if(mOmniControlChara != null && mBluetoothGatt != null){
            mOmniControlChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mOmniControlChara);
            onAudioStop();
            return;
        }
        super.closeMic();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        if(mOmniVoiceServiceSupported && status == BluetoothGatt.GATT_SUCCESS ){
            String uuid = descriptor.getUuid().toString();
            Log.d(TAG,"onDescriptorWrite:"+uuid);
            if(uuid.compareToIgnoreCase(CLIENT_CHARACTERISTIC_CONFIG.value)==0){
                if(descriptor.getCharacteristic().equals(mOmniVoiceChara)){
                    mNotificationEnabled = true;
                    openMic();
                }
            }
        }
        super.onDescriptorWrite(gatt,descriptor,status);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
