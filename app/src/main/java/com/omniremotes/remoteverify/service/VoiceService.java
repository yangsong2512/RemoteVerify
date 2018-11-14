package com.omniremotes.remoteverify.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.omniremotes.remoteverify.decoder.ADPCMDecoder;
import com.omniremotes.remoteverify.decoder.VoiceInfo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class VoiceService extends Service implements ADPCMDecoder.OnPcmDataReadyListener {
    private VoiceServiceBinder mBinder;
    private static final String TAG="RemoteVerify-VoiceService";
    private static VoiceService sVoiceService;
    private static final String ATVV_SERVICE_UUID="AB5E0001-5A21-4F05-BC7D-AF01F617B664";
    private static final String ATVV_CHAR_TX="AB5E0002-5A21-4F05-BC7D-AF01F617B664";
    private static final String ATVV_CHAR_RX="AB5E0003-5A21-4F05-BC7D-AF01F617B664";
    private static final String ATVV_CHAR_CTL="AB5E0004-5A21-4F05-BC7D-AF01F617B664";
    private static final String CLIENT_CHARACTERISTIC_CONFIG="00002902-0000-1000-8000-00805F9B34FB";
    private static final byte AUDIO_STOP = 0x00;
    private static final byte AUDIO_START = 0x04;
    private static final byte SEARCH_START = 0x08;
    private static final byte AUDIO_SYNC = 0x0A;
    private static final byte GET_CAPS_RESP = 0x0B;
    private static final byte MIC_OPEN_ERROR = 0x0C;
    private static final short ADPCM_8K_16BIT = 0x0001;
    private static final short ADPCM_8K_16K_16BIT = 0x0003;
    private static final short ADPCM_OPUS_8K_16BIT = 0x0005;
    private static final short ADPCM_OPUS_8K_16K_16BIT = 0x0007;
    private static boolean DBG = false;
    private BluetoothGattCharacteristic mTXChara;
    private BluetoothGattCharacteristic mRXChara;
    private BluetoothGattCharacteristic mCTLChara;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mCurrentDevice;
    private ADPCMDecoder mAdpcmDecoder;
    private FileOutputStream mFileOutputStream;
    public static synchronized VoiceService getInstance(){
        if(sVoiceService != null){
            return sVoiceService;
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
            mBinder = new VoiceServiceBinder(this);
        }

        setVoiceService(this);
    }

    private void setVoiceService(VoiceService svc){
        sVoiceService = svc;
    }


    private static class VoiceServiceBinder extends IVoiceService.Stub{
        VoiceService svc;
        VoiceServiceBinder(VoiceService service){
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
            ATVVOpenMic();
        }
    }

    private void stopVoice(BluetoothDevice device){
        if(mBluetoothGatt == null && (!device.equals(mCurrentDevice))){
            return;
        }
        ATVVCloseMic();
    }

    private void enableNotification(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic){
        gatt.setCharacteristicNotification(characteristic,true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        if(descriptor == null){
            Log.d(TAG,"no ccc descriptor");
            return;
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        gatt.writeDescriptor(descriptor);

    }

    private void searchATVVService(BluetoothGatt gatt){
        BluetoothGattService atvvService = gatt.getService(UUID.fromString(ATVV_SERVICE_UUID));
        if(atvvService == null){
            return;
        }
        List<BluetoothGattCharacteristic> characteristics = atvvService.getCharacteristics();
        for(BluetoothGattCharacteristic characteristic:characteristics){
            String charaUuid = characteristic.getUuid().toString().toUpperCase();
            switch (charaUuid){
                case ATVV_CHAR_CTL:
                    mCTLChara = characteristic;
                    break;
                case ATVV_CHAR_RX:
                    mRXChara = characteristic;
                    break;
                case ATVV_CHAR_TX:
                    mTXChara = characteristic;
                    break;
            }
        }
        enableNotification(gatt,mCTLChara);
    }

    private void ATVVGetCapabilities (){
        if(mTXChara != null && mBluetoothGatt!=null){
            mTXChara.setValue(new byte[]{0x0A,0x00,0x00,0x00,0x00});
            mBluetoothGatt.writeCharacteristic(mTXChara);
        }
    }

    private void ATVVCloseMic(){
        byte[] bytes = new byte[]{0x0D,0x00,0x00};
        if(mTXChara != null && mBluetoothGatt != null){
            mTXChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mTXChara);
        }
    }

    private void ATVVOpenMic(){
        byte[] bytes  = new byte[]{0x0c,0x00,0x01};
        if(mTXChara != null && mBluetoothGatt != null){
            mTXChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mTXChara);
        }
    }

    private void processCAPResp(byte[] bytes){
        short version =(short) (bytes[2]&0xff);
        version |=(short) ((bytes[1]&0xff) << 8);
        short codecSupported = (short) (bytes[4]&0xff);
        codecSupported |= (short) (bytes[3]&0xff<<8);
        short bytesPerFrame =(short) (bytes[6]&0xff);
        bytesPerFrame |= (short) (bytes[5]&0xff<<8);
        short bytesPerChara =(short) (bytes[8]&0xff);
        bytesPerChara |= (short) (bytes[7]&0xff<<8);
        if(codecSupported == ADPCM_8K_16BIT ){
            mAdpcmDecoder = new ADPCMDecoder(version,codecSupported,bytesPerFrame,bytesPerChara);
            mAdpcmDecoder.registerOnPcmDataReadyListener(this);
        }
        Log.d(TAG,"version:"+version+",codec:"+codecSupported+",bytesPerFrame:"+bytesPerFrame+",bytesPerChara:"+bytesPerChara);
    }

    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            Log.d(TAG,"stop voice");
            stopVoice(mCurrentDevice);
        }
    };

    private void processCTLResponse(){
        byte[] bytes = mCTLChara.getValue();
        switch (bytes[0]){
            case AUDIO_STOP:
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
                break;
            case AUDIO_START:
                try{
                    mFileOutputStream = getBaseContext().openFileOutput("test.pcm",MODE_PRIVATE);
                    Timer timer = new Timer();
                    timer.schedule(mTimerTask,10*1000);
                }catch (FileNotFoundException e){
                    Log.d(TAG,""+e);
                }
                mAdpcmDecoder.onVoiceStart();
                break;
            case SEARCH_START:
                Log.d(TAG,"receive search start");
                break;
            case AUDIO_SYNC:
                mAdpcmDecoder.onVoiceSync();
                break;
            case GET_CAPS_RESP:
                processCAPResp(bytes);
                break;
            case MIC_OPEN_ERROR:
                Log.d(TAG,"receive mic open error");
                break;
        }
    }

    private void processAudioData(){
        if(mRXChara == null){
            return;
        }
        byte[] bytes = mRXChara.getValue();
        if(mAdpcmDecoder == null){
            Log.d(TAG,"decoder is not ready");
            return;
        }
        mAdpcmDecoder.append(bytes);
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
            }
            if(newState == BluetoothGatt.STATE_DISCONNECTED){
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if(status == BluetoothGatt.GATT_SUCCESS){
                searchATVVService(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(characteristic.equals(mTXChara)){
                Log.d(TAG,"onTxCharaWrite");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if(characteristic.equals(mCTLChara)){
                processCTLResponse();
            }else if(characteristic.equals(mRXChara)){
                processAudioData();
            }
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
            if(descriptor.getCharacteristic().equals(mCTLChara)){
                Log.d(TAG,"ctr characteristic descriptor configured");
                enableNotification(gatt,mRXChara);
            }else if(descriptor.getCharacteristic().equals(mRXChara)){
                Log.d(TAG,"rc characteristic descriptor configured");
                ATVVGetCapabilities();
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

    public static void dump(byte[] bytes){
        if(!DBG){
            return;
        }
        StringBuilder builder = new StringBuilder();
        for(byte var:bytes){
            builder.append(String.format("%02x ",var));
        }
        Log.d(TAG,""+builder.toString());
    }

    @Override
    public void onPcmDataReady(short[] pcm) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(pcm.length*2);
        for(short data:pcm){
            byteBuffer.putShort(data);
        }
        dump(byteBuffer.array());
        if(mFileOutputStream != null){
            try{
                mFileOutputStream.write(byteBuffer.array());
            }catch (IOException e){
                Log.d(TAG,""+e);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand:"+startId);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mBluetoothGatt != null){
            mBluetoothGatt.close();
        }
        Log.d(TAG,"onDestroy");
    }
}
