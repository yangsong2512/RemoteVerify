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
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.Toast;

import com.omniremotes.remoteverify.fragment.ScanListFragment;
import com.omniremotes.remoteverify.fragment.TestCaseFragment;
import com.omniremotes.remoteverify.interfaces.IBluetoothEventListener;
import com.omniremotes.remoteverify.service.CoreService;
import com.omniremotes.remoteverify.service.ICoreService;

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
    private ICoreService mService;
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

    public void doBind(Context context){
        Intent intent = new Intent();
        intent.setAction("com.omniremotes.remoteverify.service.CoreService");
        intent.setComponent(new ComponentName("com.omniremotes.remoteverify","com.omniremotes.remoteverify.service.CoreService"));
        if(context.bindService(intent,mConnection,BIND_AUTO_CREATE)){
            Log.e(TAG,"bind service success");
        }else{
            Log.e(TAG,"bind service failed");
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG,"onServiceConnected");
            mService = ICoreService.Stub.asInterface(service);
            if(mService == null){
                Log.d(TAG,"service is null");
            }
            CoreService coreService = CoreService.getCoreService();
            if(coreService == null){
                return;
            }
            coreService.registerOnBluetoothEventListener(new BluetoothEventListener());
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
            public void onStartButtonClicked(String testCase,String address) {
                if(mService != null){
                    if(testCase.equals("Pairing Test")){
                        try{
                            mService.startPair(address);
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

        mScanListFragment = ScanListFragment.getInstance();
        mScanListFragment.registerOnScanListFragmentEvents(new ScanListFragment.OnScanListFragmentEvents() {
            @Override
            public void onDeviceClicked(ScanResult result) {
                if(mTestCaseFragment != null){
                    mTestCaseFragment.notifyOnDeviceClicked(result);
                }
            }
        });
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,mScanListFragment,"ScanList");
        fragmentTransaction.commit();
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            boolean visible = ScanListFragment.getInstance().isVisible();
            if(!visible){
                initUI();
                return false;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mService!=null){
            unbindService(mConnection);
        }
    }

    private class BluetoothEventListener implements IBluetoothEventListener{
        @Override
        public void onStartParing() {

        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int preState, int state) {
            Log.d(TAG,"onConnectionStateChanged:preState,"+preState+",state:"+state);
            mTestCaseFragment.notifyConnectionStateChanged(device,preState,state);
        }

        @Override
        public void onBondStateChanged(BluetoothDevice device, int preState, int state) {
            mTestCaseFragment.notifyBondStateChanged(device,preState,state);
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
