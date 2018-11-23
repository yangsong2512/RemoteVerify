package com.omniremotes.remoteverify.utility;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.media.AudioFormat;
import android.util.Log;

import com.omniremotes.remoteverify.decoder.ADPCMDecoder;
import com.omniremotes.remoteverify.decoder.AudioTrackPlayer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_CHAR_CTL;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_CHAR_RX;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_CHAR_TX;
import static com.omniremotes.remoteverify.utility.RemoteUuid.ATVV_SERVICE_UUID;
import static com.omniremotes.remoteverify.utility.RemoteUuid.CLIENT_CHARACTERISTIC_CONFIG;

public class ATVVoice extends OmniBase implements ADPCMDecoder.OnPcmDataReadyListener{
    private static final String TAG="RemoteVerify-ATVVoice";
    private BluetoothGattCharacteristic mATVTXChara;
    private BluetoothGattCharacteristic mATVRXChara;
    private BluetoothGattCharacteristic mATVCTLChara;
    private boolean mATVVoiceSupported = false;
    private boolean mNotificationEnabled = false;
    private boolean mCapabilityResponse = false;
    private static final byte AUDIO_STOP = 0x00;
    private static final byte AUDIO_START = 0x04;
    private static final byte SEARCH_START = 0x08;
    private static final byte AUDIO_SYNC = 0x0A;
    private static final byte GET_CAPS_RESP = 0x0B;
    private static final byte MIC_OPEN_ERROR = 0x0C;
    public short mVersion = 0x0;
    public short mCodecSupported = 0x01;
    public short mBytesPerFrame = 134;
    public short mBytesPerChara = 20;
    public static final short ADPCM_8K_16BIT = 0x0001;
    ATVVoice(Context context){
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
        String uuid = characteristic.getUuid().toString();
        if(uuid.compareToIgnoreCase(ATVV_CHAR_TX.value)==0){
            Log.d(TAG,"tx write");
            byte[] bytes = characteristic.getValue();
            if(bytes[0] == 0xA){
                Log.d(TAG,"get capability");
                openMic();
            }
        }
    }

    @Override
    public void onGattServiceDisconnected(){
        super.onGattServiceDisconnected();
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        if(status == BluetoothGatt.GATT_SUCCESS){
            BluetoothGattService service = gatt.getService(UUID.fromString(ATVV_SERVICE_UUID.value));
            if(service != null) {
                Log.d(TAG, "find ATV voice service");
                mATVVoiceSupported = true;
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    String uuid = characteristic.getUuid().toString();
                    if (uuid.compareToIgnoreCase(ATVV_CHAR_CTL.value) == 0) {
                        Log.d(TAG,"atv ctl characteristic");
                        mATVCTLChara = characteristic;
                    }
                    if (uuid.compareToIgnoreCase(ATVV_CHAR_RX.value) == 0) {
                        Log.d(TAG,"atv rx characteristic");
                        mATVRXChara = characteristic;
                    }
                    if (uuid.compareToIgnoreCase(ATVV_CHAR_TX.value) == 0) {
                        Log.d(TAG,"atv tx characteristic");
                        mATVTXChara = characteristic;
                    }
                }
            }
        }
        super.onServicesDiscovered(gatt,status);
    }
    private void processCapabilityResponse(byte[] bytes){
        mVersion =(short) (bytes[2]&0xff);
        mVersion |=(short) ((bytes[1]&0xff) << 8);
        mCodecSupported = (short) (bytes[4]&0xff);
        mCodecSupported |= (short) (bytes[3]&0xff<<8);
        mBytesPerFrame =(short) (bytes[6]&0xff);
        mBytesPerFrame |= (short) (bytes[5]&0xff<<8);
        mBytesPerChara =(short) (bytes[8]&0xff);
        mBytesPerChara |= (short) (bytes[7]&0xff<<8);
        Log.d(TAG,"version:"+mVersion+",codec:"+mCodecSupported+",bytesPerFrame:"+mBytesPerFrame+
                ",bytesPerChara:"+mBytesPerChara);
        //openMic();
    }


    void onAudioStart(){
        try{
            mFileOutputStream = mContext.openFileOutput("test.pcm",MODE_PRIVATE);
            mAudioPlayer = new AudioTrackPlayer(8000,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);
        }catch (FileNotFoundException e){
            Log.d(TAG,""+e);
        }
        mAdpcmDecoder = new ADPCMDecoder(mVersion,mCodecSupported,mBytesPerFrame,mBytesPerChara);
        mAdpcmDecoder.registerOnPcmDataReadyListener(this);
        mAdpcmDecoder.onVoiceStart();

    }

    void onAudioStop(){
        if(mAudioPlayer != null){
            mAudioPlayer.onAudioStop();
        }
        mAdpcmDecoder.onVoiceStop();
        if(mFileOutputStream != null){
            try{
                mFileOutputStream.flush();
                mFileOutputStream.close();
                mFileOutputStream = null;
            }catch (IOException e){
                Log.d(TAG,""+e);
            }
        }
    }

    private void processControlResponse(BluetoothGattCharacteristic characteristic){
        byte[] bytes = characteristic.getValue();
        switch (bytes[0]){
            case AUDIO_STOP:
                onAudioStop();
                break;
            case AUDIO_START:
                onAudioStart();
                break;
            case SEARCH_START:
                break;
            case AUDIO_SYNC:
                mAdpcmDecoder.onVoiceSync();
                break;
            case GET_CAPS_RESP:
                processCapabilityResponse(bytes);
                break;
            case MIC_OPEN_ERROR:
                break;
        }
    }
    public void processAudioData(BluetoothGattCharacteristic characteristic){
        if(characteristic == null){
            return;
        }
        byte[] bytes = characteristic.getValue();
        if(mAdpcmDecoder == null){
            Log.d(TAG,"decoder is not ready");
            return;
        }
        mAdpcmDecoder.append(bytes);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
        super.onCharacteristicChanged(gatt,characteristic);
        if(mATVVoiceSupported){
            if(characteristic.equals(mATVCTLChara)){
                processControlResponse(characteristic);
            }else if(characteristic.equals(mATVRXChara)){
                processAudioData(characteristic);
            }
        }
        super.onCharacteristicChanged(gatt,characteristic);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt,status,newState);
    }

    /**
     * Step-1: enable notification
     * Step-2: send open mic command
     */
    @Override
    public void openMic(){
        if(mATVVoiceSupported){
            Log.d(TAG,"try to open mic");
            if(!mNotificationEnabled){
                Log.d(TAG,"notification not enabled");
                enableNotification(mATVCTLChara);
                return;
            }
            byte[] bytes  = new byte[]{0x0c,0x00,0x01};
            if(mATVTXChara != null && mBluetoothGatt != null){
                Log.d(TAG,"send command to open mic");
                mATVTXChara.setValue(bytes);
                mBluetoothGatt.writeCharacteristic(mATVTXChara);
                return;
            }
        }
        super.openMic();
    }
    @Override
    public void closeMic(){
        byte[] bytes = new byte[]{0x0D,0x00,0x00};
        if(mATVTXChara != null && mBluetoothGatt != null){
            mATVTXChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mATVTXChara);
            return;
        }
        super.closeMic();
    }

    private void getCapabilities (){
        if(mATVTXChara != null && mBluetoothGatt!=null){
            mATVTXChara.setValue(new byte[]{0x0A,0x00,0x00,0x00,0x00});
            mBluetoothGatt.writeCharacteristic(mATVTXChara);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        String uuid = descriptor.getUuid().toString();
        if(mATVVoiceSupported){
            if(uuid.compareToIgnoreCase(CLIENT_CHARACTERISTIC_CONFIG.value)==0){
                Log.d(TAG,"onDescriptorWrite:"+uuid);
                if(descriptor.getCharacteristic().equals(mATVCTLChara)){
                    Log.d(TAG,"ATV control characteristic descriptor set");
                    enableNotification(mATVRXChara);
                }else if(descriptor.getCharacteristic().equals(mATVRXChara)){
                    Log.d(TAG,"notification enabled");
                    mNotificationEnabled = true;
                    if(!mCapabilityResponse){
                        getCapabilities();
                    }else {
                        openMic();
                    }
                }
            }
        }
        super.onDescriptorWrite(gatt,descriptor,status);
    }

    @Override
    public void onPcmDataReady(byte[] pcm) {
        if(mAudioPlayer != null){
            mAudioPlayer.onDataReady(pcm);
        }
        if(mFileOutputStream != null){
            try{
                mFileOutputStream.write(pcm);
            }catch (IOException e){
                Log.d(TAG,""+e);
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}
