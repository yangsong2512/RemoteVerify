package com.omniremotes.remoteverify.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.nfc.Tag;
import android.os.IBinder;
import android.speech.tts.Voice;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class VoiceService extends Service {
    private VoiceServiceBinder mBinder;
    private static final String TAG="RemoteVerify-VoiceService";
    private static VoiceService sVoiceService;
    private static final String ATVV_SERVICE_UUID="AB5E0001-5A21-4F05-BC7D-AF01F617B664";
    private static final String ATVV_CHAR_TX="AB5E0002-5A21-4F05-BC7D-AF01F617B664";
    private static final String ATVV_CHAR_RX="AB5E0003-5A21-4F05-BC7D-AF01F617B664";
    private static final String ATVV_CHAR_CTL="AB5E0004-5A21-4F05-BC7D-AF01F617B664";
    private static final String CLIENT_CHARACTERISTIC_CONFIG="00002902-0000-1000-8000-00805F9B34FB";
    private BluetoothGattCharacteristic mTXChara;
    private BluetoothGattCharacteristic mRXChara;
    private BluetoothGattCharacteristic mCTLChara;
    private BluetoothGatt mBluetoothGatt;
    private class ATVInfo{
        short version;
        short codecSupported;
        short bytesPerFrame;
        short bytesPerChara;
        ATVInfo(short version,short codecSupported,short bytesPerFrame,short bytesPerChara){
            this.version = version;
            this.codecSupported = codecSupported;
            this.bytesPerChara = bytesPerChara;
            this.bytesPerFrame = bytesPerFrame;
        }
    }

    private ATVInfo mATVInfo;
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
        public void startVoice() {
            if(svc == null){
                return;
            }
            svc.startVoice();
        }

        @Override
        public void stopVoice() {
            if(svc == null){
                return;
            }
            svc.stopVoice();
        }

        @Override
        public void connect(String address){
            if(svc == null){
                return;
            }
            svc.connect(address);
        }
    }

    private void connect(String address){

    }

    private void startVoice(){

    }

    private void stopVoice(){

    }

    public void onConnectionStateChanged(BluetoothDevice device,int preState,int state){

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

    private void ATVVOpenMic(){
        byte[] bytes  = new byte[]{0x0c,0x00,0x01,0x00,0x00};
        if(mTXChara != null && mBluetoothGatt != null){
            mTXChara.setValue(bytes);
            mBluetoothGatt.writeCharacteristic(mTXChara);
        }
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
            Log.d(TAG,"onCharacteristicRead");
            if(characteristic.equals(mCTLChara)){
                Log.d(TAG,"on remote response");
            }
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
                byte[] bytes = characteristic.getValue();
                if(bytes[0] == 0x0B){
                    short version =(byte) (bytes[2]&0xff);
                    version |=(byte) ((bytes[1]&0xff) << 8);
                    short codecSupported = (byte)(bytes[4]&0xff);
                    codecSupported |= (byte)(bytes[3]&0xff<<8);
                    short bytesPerFrame =(short) (bytes[6]&0xff);
                    bytesPerFrame |= (byte)(bytes[5]&0xff<<8);
                    short bytesPerChara =(byte) (bytes[8]&0xff);
                    bytesPerChara |= (byte)(bytes[7]&0xff<<8);
                    mATVInfo = new ATVInfo(version,codecSupported,bytesPerFrame,bytesPerChara);
                    Log.d(TAG,"version:"+version+",codec:"+codecSupported+",bytesPerFrame:"+bytesPerFrame+",bytesPerChara:"+bytesPerChara);
                }
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
                //ATVVOpenMic();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG,"onStartCommand");
        BluetoothDevice device = intent.getParcelableExtra("Device");
        if(device != null){
            mBluetoothGatt = device.connectGatt(getBaseContext(),false,
                    mBluetoothGattCallback,BluetoothDevice.TRANSPORT_LE);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothGatt.close();
        Log.d(TAG,"onDestroy");
    }
}
