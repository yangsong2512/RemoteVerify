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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.ListView;
import android.widget.Toast;

import com.omniremotes.remoteverify.adapter.ScanListAdapter;
import com.omniremotes.remoteverify.fragment.ScanListFragment;
import com.omniremotes.remoteverify.fragment.TestCaseFragment;
import com.omniremotes.remoteverify.service.CoreService;
import com.omniremotes.remoteverify.service.ICoreService;
public class MainActivity extends AppCompatActivity {
    private static final String TAG="RemoteVerify-MainActivity";
    private static final int REQUEST_BLUETOOTH_ENABLE = 0;
    private static final int REQUEST_NECESSARY_PERMISSIONS = 1;
    private ICoreService mService;
    private ScanListFragment mScanListFragment;
    private TestCaseFragment mTestCaseFragment;
    private boolean mServiceConnected = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFragment();
        doBind();
    }

    private void initializeFragment(){
        mScanListFragment = new ScanListFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,mScanListFragment);
        mScanListFragment.registerOnDeviceClickedListener(new ScanListAdapter.OnDeviceClickedListener() {
            @Override
            public void onDeviceClicked(ScanResult scanResult) {
                switch2TestCaseFragment(scanResult);
            }
        });
        if(mServiceConnected){
            mScanListFragment.onCoreServiceConnected();
        }
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

    private boolean checkBluetoothState(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(adapter == null){
            Toast.makeText(this,"This device does not support bluetooth function"
            ,Toast.LENGTH_LONG).show();
        }else{
            if(!adapter.isEnabled()){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent,REQUEST_BLUETOOTH_ENABLE);
            }
            return true;
        }
        return false;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if(resultCode == RESULT_OK){
                Log.d(TAG,"Start turning on bluetooth");
            }else{
                Toast.makeText(getBaseContext(),"Failed to open bluetooth",Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    public boolean requestNecessaryPermissions(){
        boolean granted = true;
        String permissions[] = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};
        for(String permission:permissions){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                granted = false;
            }
        }
        if(!granted){
            requestPermissions(permissions,REQUEST_NECESSARY_PERMISSIONS);
            return false;
        }
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
            if(!granted){
                mScanListFragment.onCoreServiceConnected();
            }
        }
    }

    private void switch2TestCaseFragment(ScanResult scanResult){
        Toast.makeText(getBaseContext(),"switch to new fragment",Toast.LENGTH_LONG).show();
        if(mTestCaseFragment == null){
            mTestCaseFragment = new TestCaseFragment();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,mTestCaseFragment);
        fragmentTransaction.commit();
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG,"onServiceConnected");
            mService = ICoreService.Stub.asInterface(service);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(requestNecessaryPermissions()){
                        if(!checkBluetoothState()){
                            finish();
                        }
                        mScanListFragment.onCoreServiceConnected();
                    }
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG,"onServiceDisconnected");
            mService = null;
        }
    };

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
                initializeFragment();
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
}
