package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.renderscript.ScriptGroup;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.omniremotes.remoteverify.utility.RemoteUuid.CLIENT_CHARACTERISTIC_CONFIG;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_CMD_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_IMG_BLK_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.OMNI_OTA_SERVICE_UUID;

public class OmniOTA extends OmniVoice{
    private static final String TAG="RemoteVerify-OmniOTA";
    private BluetoothGattCharacteristic mOTAImageBlockChara;
    private BluetoothGattCharacteristic mOTACommandChara;
    private static final int STB_TO_RCU = 0;
    private static final int RCU_TO_STB = 1;
    private static final int CHARA_OTA_CMD = 0;
    private static final int CHARA_OTA_IMG = 1;
    private int mSoftWareVersion;
    private int mSoftWareId;
    private int mRequestBlockNum;
    private int mTotalBlockNum;
    private InputStream mInputStream;
    @Override
    public void onGattServiceConnected(BluetoothGatt gatt){
        super.onGattServiceConnected(gatt);
        Log.d(TAG,"onGattServiceConnected");
    }

    public OmniOTA(Context context){
        super(context);
    }

    @Override
    public void cleanup(){
        mOTACommandChara = null;
        mOTAImageBlockChara = null;
        super.cleanup();
    }

    public void startOta(BluetoothDevice device){
        if(mOTACommandChara == null){
            return;
        }
        enableNotification(mOTACommandChara);
    }

    public void stopOta(BluetoothDevice device){
    }

    private void processCommandResponse(BluetoothGattCharacteristic characteristic){
        byte[] bytes = characteristic.getValue();
        if(bytes[0]==(byte)0xA0&&bytes[1]==0x01){
            //rcu return remote info
            mSoftWareVersion |= bytes[2]&0xff;
            mSoftWareVersion |= (bytes[3]&0xff)<<8;
            mSoftWareId |= bytes[4]&0xff;
            mSoftWareId |= (bytes[5]&0xff)<<8;
            startImageTransfer();
        }else if(bytes[0]==(byte)0xA0&&bytes[1]==0x03&&bytes[2]==0x10){
            //rcu request get image info
            returnImageInfo();
        }
    }

    private void startTransfer(){
        if(mInputStream == null){
            try{
                mInputStream = mContext.getAssets().open("OTT-TL018-20181029-108-OTA.bin",Context.MODE_PRIVATE);
            }catch (IOException e){
                Log.d(TAG,""+e);
            }
        }else {
        }
    }

    private void processImageBlockResponse(BluetoothGattCharacteristic characteristic){
        byte[] bytes = characteristic.getValue();
        if(bytes.length == 8){
            if(bytes[0]==(byte)0xA0&&bytes[6]==1){
                mRequestBlockNum |= bytes[1]&0xff;
                mRequestBlockNum |= (bytes[2]&0xff) << 8;
                mRequestBlockNum |= (bytes[3]&0xff) << 16;
                startTransfer();
            }
        }
    }

    private void returnImageInfo(){
        if(mBluetoothGatt == null || mOTACommandChara == null){
            return;
        }
        try{
            byte[] bytes = new byte[18];
            bytes[0] = (byte)0xA0;
            bytes[1] = 0x03;
            InputStream  inputStream = mContext.getAssets().open("OTT-TL018-20181029-108-OTA.bin",Context.MODE_PRIVATE);
            if(inputStream.read(bytes,2,16) == 16){
                mOTACommandChara.setValue(bytes);
                mBluetoothGatt.writeCharacteristic(mOTACommandChara);
            }
            inputStream.close();
        }catch (IOException e){
            Log.d(TAG,""+e);
        }
    }

    private void startImageTransfer(){
        byte[] bytes = new byte[]{(byte) 0xA0, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        if(mBluetoothGatt != null && mOTACommandChara != null){
            mOTACommandChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mOTACommandChara);
        }
    }

    private void getRemoteInfo(){
        byte[] bytes = new byte[]{(byte) 0xA0, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        if(mBluetoothGatt != null && mOTACommandChara != null){
            mOTACommandChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mOTACommandChara);
        }
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
            processCommandResponse(characteristic);
            return;
        }else if(characteristic.equals(mOTAImageBlockChara)){
            processImageBlockResponse(characteristic);
            return;
        }
        super.onCharacteristicChanged(gatt,characteristic);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        String uuid = descriptor.getUuid().toString();
        if(uuid.compareToIgnoreCase(CLIENT_CHARACTERISTIC_CONFIG.value)==0){
            if(descriptor.getCharacteristic().equals(mOTACommandChara)){
                enableNotification(mOTAImageBlockChara);
                return;
            }else if(descriptor.getCharacteristic().equals(mOTAImageBlockChara)){
                getRemoteInfo();
                return;
            }
        }
        super.onDescriptorWrite(gatt,descriptor,status);
    }

    @Override
    public void closeMic(){
        super.closeMic();
    }
    @Override
    public void openMic(){
        super.openMic();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
