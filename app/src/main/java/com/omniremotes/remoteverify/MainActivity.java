package com.omniremotes.remoteverify;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.omniremotes.remoteverify.fragment.ScanListFragment;
import com.omniremotes.remoteverify.fragment.TestCaseFragment;
import com.omniremotes.remoteverify.interfaces.IBluetoothEventListener;
import com.omniremotes.remoteverify.service.CoreService;
import com.omniremotes.remoteverify.service.ICoreService;
import com.omniremotes.remoteverify.service.IRemoteControl;
import com.omniremotes.remoteverify.service.RemoteControlService;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="RemoteVerify-MainActivity";
    private static final int REQUEST_BLUETOOTH_ENABLE = 0;
    private static final int REQUEST_NECESSARY_PERMISSIONS = 1;
    private TestCaseFragment mTestCaseFragment;
    private ScanListFragment mScanListFragment;
    public static String[] mBluetoothPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    };
    private ICoreService mCoreService;
    private IRemoteControl mRemoteControlService;
    private BluetoothDevice mDeviceUnderTest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        doBind(this);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null){
            Log.d(TAG,"This device does not support bluetooth");
            return;
        }
        if(checkPermissions()){
            requestEnableBluetooth();
        }else{
            requestNecessaryPermissions();
        }
    }

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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG,"onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
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

    public void doBind(Context context){
        bindCoreService(context);
        bindRemoteControlService(context);
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

    private boolean checkPermissions(){
        for(String permission:mBluetoothPermissions){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private void initUI(){
        initScanListFragment();
        initTestCaseFragment();
    }
    private void initTestCaseFragment(){
        mTestCaseFragment = new TestCaseFragment();
        mTestCaseFragment.registerOnTestCaseFragmentEventListener(new TestCaseFragment.OnTestCaseFragmentEventListener() {
            @Override
            public void onStartButtonClicked(String testCase,BluetoothDevice device,boolean running) {
                if(mCoreService != null){
                    if(testCase.equals(getResources().getString(R.string.pair_test))){
                        Log.d(TAG,"onDeviceClicked");
                        if(device == null){
                            Log.d(TAG,"device is null");
                        }
                        try{
                            mCoreService.startPair(device);
                        }catch (RemoteException e){
                            Log.d(TAG,""+e);
                        }
                    }else if(testCase.equals(getResources().getString(R.string.voice_test))){
                        try{
                            if(running){
                                mRemoteControlService.startVoice(device);
                            }else {
                                mRemoteControlService.stopVoice(device);
                            }
                        }catch (RemoteException e){
                            Log.d(TAG,""+e);
                        }
                    }
                }
            }
        });
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.test_case_fragment,mTestCaseFragment);
        fragmentTransaction.commit();
    }

    private void initScanListFragment(){

        mScanListFragment = new ScanListFragment();
        mScanListFragment.registerOnScanListFragmentEvents(new ScanListFragment.OnScanListFragmentEvents() {
            @Override
            public void onDeviceClicked(ScanResult result) {
                if(mTestCaseFragment != null){
                    mTestCaseFragment.notifyOnDeviceClicked(result);
                    mDeviceUnderTest = result.getDevice();
                }
            }

            @Override
            public void onPairedDeviceClicked(BluetoothDevice device) {
                if(mTestCaseFragment != null){
                    boolean connected = false;
                    CoreService coreService = CoreService.getCoreService();
                    if(coreService != null){
                        connected = coreService.isDeviceConnected(device);
                        if(connected){
                            Toast.makeText(getBaseContext(),"device connected",Toast.LENGTH_SHORT).show();
                            connectDevice(device);
                        }
                    }
                    mTestCaseFragment.notifyOnPairedDeviceClicked(device,connected);
                    mDeviceUnderTest = device;
                }
            }
        });
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,mScanListFragment,"ScanList");
        fragmentTransaction.commit();
    }

    private void disconnectDevice(BluetoothDevice device){
        if(mRemoteControlService == null){
            return;
        }
        try{
            mRemoteControlService.disconnect(device);
        }catch (RemoteException e){
            Log.d(TAG,""+e);
        }
    }

    private void connectDevice(BluetoothDevice device){
        if(mRemoteControlService == null){
            return;
        }
        try{
            mRemoteControlService.connect(device);
        }catch (RemoteException e){
            Log.d(TAG,""+e);
        }
    }

    public void requestEnableBluetooth(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(!adapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_BLUETOOTH_ENABLE);
            return;
        }
        initUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if(resultCode == RESULT_OK){
                initUI();
            }else{
                Toast.makeText(getBaseContext(),"Failed to open bluetooth",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public void requestNecessaryPermissions(){
        boolean granted = true;
        for(String permission:mBluetoothPermissions){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                granted = false;
            }
        }
        if(!granted){
            requestPermissions(mBluetoothPermissions,REQUEST_NECESSARY_PERMISSIONS);
            Log.d(TAG,"request permission");
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = true;
        if(requestCode == REQUEST_NECESSARY_PERMISSIONS){
            int count = permissions.length;
            for (int i = 0; i < count; i++){
                if(permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)){
                    if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        granted = false;
                    }
                }
                if(permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)){
                    if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                        granted = false;
                    }
                }
            }
            if(granted){
                requestEnableBluetooth();
            }else {
                finish();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.this,RemoteControlService.class));
        if(mRemoteControlService != null || mCoreService != null){
            unbindService(mConnection);
        }
    }

    private class BluetoothEventListener implements IBluetoothEventListener{
        @Override
        public void onStartParing(BluetoothDevice device) {
            Log.d(TAG,"onStartPairing");
            if(mTestCaseFragment != null){
                mTestCaseFragment.onStartPairing(device);
            }
            if(mScanListFragment != null){
                mScanListFragment.onStartPairing(device);
            }
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int preState, int state) {
            Log.d(TAG,"onConnectionStateChanged:preState,"+preState+
                    ",state:"+state);
            mTestCaseFragment.notifyConnectionStateChanged(device,preState,state);
            if(state == BluetoothProfile.STATE_CONNECTED && device.equals(mDeviceUnderTest)){
                connectDevice(device);
            }else if(state == BluetoothProfile.STATE_DISCONNECTED&&device.equals(mDeviceUnderTest)){
                disconnectDevice(device);
            }
        }

        @Override
        public void onBondStateChanged(BluetoothDevice device, int preState, int state) {
            mTestCaseFragment.notifyBondStateChanged(device,preState,state);
            mScanListFragment.notifyBondStateChanged(device,preState,state);
        }

        @Override
        public void onAclDisconnectRequest(BluetoothDevice device) {

        }

        @Override
        public void onAclDisconnected(BluetoothDevice device) {
            mTestCaseFragment.notifyAclDisconnected(device);
        }

        @Override
        public void onAclConnected(BluetoothDevice device) {
            mTestCaseFragment.notifyAclConnected(device);
        }

        @Override
        public void onScanResult(ScanResult result) {
            if(mScanListFragment != null){
                mScanListFragment.notifyOnScanResult(result);
            }
        }
    }
}
