package com.omniremotes.remoteverify;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
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
import com.omniremotes.remoteverify.service.CoreService;
import com.omniremotes.remoteverify.service.ICoreService;
public class MainActivity extends AppCompatActivity implements CoreService.OnCoreServiceEvents {
    private static final String TAG="RemoteVerify-MainActivity";
    private static final int REQUEST_BLUETOOTH_ENABLE = 0;
    private static final int REQUEST_NECESSARY_PERMISSIONS = 1;
    private ICoreService mService;
    private ScanListFragment mScanListFragment;
    private TestCaseFragment mTestCaseFragment;
    private CoreService mCoreService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFragment();
        doBind();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG,"onServiceConnected");
            mService = ICoreService.Stub.asInterface(service);
            mCoreService = CoreService.getCoreService();
            if(mCoreService == null){
                Log.d(TAG,"CoreService is null");
                return;
            }
            mCoreService.registerOnCoreServiceEventsListener(MainActivity.this);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    continueInitProcess();
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG,"onServiceDisconnected");
            mService = null;
        }
    };

    public void requestEnableBluetooth(){
        if(mCoreService.getBluetoothState() == CoreService.BLUETOOTH_NOT_ENABLED){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_BLUETOOTH_ENABLE);
            return;
        }
        if(mCoreService.isEnabled()){
            try{
                mService.startScan();
            }catch (RemoteException e){
                Log.d(TAG,""+e);
            }
        }
    }

    private void continueInitProcess(){
        Log.d(TAG,"continueInitProcess");
        CoreService coreService = CoreService.getCoreService();
        if(coreService == null){
            return;
        }
        if(coreService.getBluetoothState()==CoreService.BLUETOOTH_FEATURE_NOT_SUPPORTED){
            finish();
            return;
        }
        if(coreService.getBluetoothState()==CoreService.BLUETOOTH_PERMISSION_NOT_GRANTED){
            requestNecessaryPermissions();
            return;
        }
        requestEnableBluetooth();
    }

    private void initializeFragment(){
        mScanListFragment = new ScanListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,mScanListFragment);
        fragmentTransaction.commit();
    }

    private void doBind(){
        Intent intent = new Intent();
        intent.setAction("com.omniremotes.remoteverify.service.CoreService");
        intent.setComponent(new ComponentName("com.omniremotes.remoteverify","com.omniremotes.remoteverify.service.CoreService"));
        if(bindService(intent,mConnection,BIND_AUTO_CREATE)){
            Log.e(TAG,"bind service success");
        }else{
            Log.e(TAG,"bind service failed");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if(resultCode == RESULT_OK){
                Log.d(TAG,"Start turning on bluetooth");
                if(mCoreService.isEnabled()){
                    try{
                        mService.startScan();
                    }catch (RemoteException e){
                        Log.d(TAG,""+e);
                    }
                }
            }else{
                Toast.makeText(getBaseContext(),"Failed to open bluetooth",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public boolean requestNecessaryPermissions(){
        boolean granted = true;
        for(String permission:CoreService.mBluetoothPermissions){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                granted = false;
            }
        }
        if(!granted){
            requestPermissions(CoreService.mBluetoothPermissions,REQUEST_NECESSARY_PERMISSIONS);
            finish();
            return false;
        }
        requestEnableBluetooth();
        return true;
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
            }
        }
    }

    /*
    private void switch2TestCaseFragment(ScanResult scanResult){
        Toast.makeText(getBaseContext(),"switch to new fragment",Toast.LENGTH_LONG).show();
        mTestCaseFragment = new TestCaseFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,mTestCaseFragment);
        fragmentTransaction.commit();
    }
    */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
        if(mConnection != null){
            Log.d(TAG,"unbindService");
            if(mService != null){
                try{
                    mService.stopScan();
                }catch (RemoteException e){
                    Log.d(TAG,""+e);
                }
            }
            unbindService(mConnection);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if(mScanListFragment.isFragmentDetached()){
                Log.d(TAG,"mScanListFragment is detached");
                //initializeFragment();
            }else{
                Log.d(TAG,"mScanListFragment is not detached");
                if(mService != null){
                    try{
                        mService.stopScan();
                    }catch (RemoteException e){
                        Log.d(TAG,""+e);
                    }
                }
                if(mService != null){
                    unbindService(mConnection);
                    mService = null;
                }
                finish();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onScanResults(ScanResult scanResult) {
        if(mScanListFragment != null){
            mScanListFragment.notifyDataSetChanged(scanResult);
        }
    }

    @Override
    public void onBluetoothStateChanged(int state, int preState) {
        if(state == BluetoothAdapter.STATE_ON){
            if(mService == null){
                return;
            }
            try{
                mService.startScan();
            }catch (RemoteException e){
                Log.d(TAG,""+e);
            }
        }
    }
}
