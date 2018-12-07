package com.omniremotes.remoteverify;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.omniremotes.remoteverify.adapter.DeviceListAdapter;
import com.omniremotes.remoteverify.adapter.LocalBluetoothAdapter;
import com.omniremotes.remoteverify.adapter.ScanListAdapter;
import com.omniremotes.remoteverify.interfaces.IBluetoothEventListener;
import com.omniremotes.remoteverify.service.CoreService;
import com.omniremotes.remoteverify.service.ICoreService;
import com.omniremotes.remoteverify.service.IRemoteControl;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

public class VerifyActivity extends AppCompatActivity {
    private static final String TAG="RemoteVerify-VerifyActivity";
    private DeviceListAdapter mDeviceListAdapter;
    private ScanListAdapter mScanListAdapter;
    private LocalBluetoothAdapter mAdapter;
    private ICoreService mCoreService;
    private IRemoteControl mRemoteControlService;
    private static final byte REQUEST_CODE_BLUETOOTH_PERMISSION = 0;
    private static final byte REQUEST_CODE_BLUETOOTH_ENABLE = 1;
    public static String[] mBluetoothPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        setupUI();
        mAdapter = LocalBluetoothAdapter.getInstance();
        if(!mAdapter.deviceHasBluetoothFeature()){
            finish();
        }else{
            doBind();
            checkBluetoothPermissions();
        }
    }

    public void setupUI(){
        ListView pairedDevice = findViewById(R.id.device_paired);
        mDeviceListAdapter = new DeviceListAdapter(this);
        pairedDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
        pairedDevice.setAdapter(mDeviceListAdapter);

        mScanListAdapter = new ScanListAdapter(this);
        ListView availableDevice = findViewById(R.id.device_available);
        availableDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
        availableDevice.setAdapter(mScanListAdapter);
    }

    public void checkBluetoothPermissions(){
        List<String> permissions = new ArrayList<>();
        for (String permission:mBluetoothPermissions){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                permissions.add(permission);
            }
        }
        if(permissions.size()==0){
            requestEnableBluetooth();
            return;
        }
        int size = permissions.size();
        String[] requestPermissions = permissions.toArray(new String[size]);
        if(requestPermissions == null){
            return;
        }
        requestPermissions(requestPermissions,REQUEST_CODE_BLUETOOTH_PERMISSION);
    }

    public void doBind(){
        bindCoreService(this);
        bindRemoteControlService(this);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG,"onServiceConnected");
            String className = componentName.getClassName();
            if(className.equals("com.omniremotes.remoteverify.service.CoreService")){
                mCoreService = ICoreService.Stub.asInterface(service);
                if(mCoreService == null){
                    Log.d(TAG,"service is null");
                }
                CoreService coreService = CoreService.getCoreService();
                if(coreService == null){
                    return;
                }
                coreService.registerOnBluetoothEventListener(new BluetoothEventListener());
            }else if(className.equals("com.omniremotes.remoteverify.service.RemoteControlService")){
                Log.d(TAG,"onVoiceService connected");
                mRemoteControlService = IRemoteControl.Stub.asInterface(service);
                if(mRemoteControlService == null){
                    Log.d(TAG,"voice service is null");
                }
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG,"onServiceDisconnected");
        }
    };



    private void bindRemoteControlService(Context context){
        Intent intent = new Intent();
        intent.setAction("com.omniremotes.remoteverify.service.RemoteControlService");
        intent.setComponent(new ComponentName("com.omniremotes.remoteverify","com.omniremotes.remoteverify.service.RemoteControlService"));
        if(context.bindService(intent,mConnection,BIND_AUTO_CREATE)){
            Log.d(TAG,"bind voice service success");
        }else{
            Log.d(TAG,"failed to bind voice service");
        }
    }

    private void bindCoreService(Context context){
        Intent intent = new Intent();
        intent.setAction("com.omniremotes.remoteverify.service.CoreService");
        intent.setComponent(new ComponentName("com.omniremotes.remoteverify","com.omniremotes.remoteverify.service.CoreService"));
        if(context.bindService(intent,mConnection,BIND_AUTO_CREATE)){
            Log.e(TAG,"bind service success");
        }else{
            Log.e(TAG,"bind service failed");
        }
    }

    private void requestEnableBluetooth(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(!adapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_CODE_BLUETOOTH_ENABLE);
            return;
        }
        mDeviceListAdapter.addDevices(mAdapter.getBondedDevices());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_BLUETOOTH_ENABLE){
            if(resultCode == RESULT_OK){
                mDeviceListAdapter.addDevices(mAdapter.getBondedDevices());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_BLUETOOTH_PERMISSION){
            for(int i = 0;i < permissions.length;i++){
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"request:"+permissions[i],Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            requestEnableBluetooth();
        }
    }

    private class BluetoothEventListener implements IBluetoothEventListener{
        @Override
        public void onScanResult(ScanResult result) {
            mScanListAdapter.notifyDataChanged(result);
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int preState, int state) {

        }

        @Override
        public void onAclConnected(BluetoothDevice device) {

        }

        @Override
        public void onAclDisconnected(BluetoothDevice device) {

        }

        @Override
        public void onAclDisconnectRequest(BluetoothDevice device) {

        }

        @Override
        public void onBondStateChanged(BluetoothDevice device, int preState, int state) {

        }

        @Override
        public void onStartParing(BluetoothDevice device) {

        }
    }

    public void onPairButtonClicked(View view){

    }

    public void onOtaButtonClicked(View view){

    }

    public void onVoiceButtonClicked(View view){

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCoreService != null){
            try{
                mCoreService.stopScan();
            }catch (RemoteException e){
                Log.d(TAG,""+e);
            }
        }
        unbindService(mConnection);
    }
}
