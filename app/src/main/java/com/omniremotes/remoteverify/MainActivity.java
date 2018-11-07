package com.omniremotes.remoteverify;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.omniremotes.remoteverify.service.CoreServiceManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="RemoteVerify-MainActivity";
    private static final int REQUEST_BLUETOOTH_ENABLE = 0;
    private static final int REQUEST_NECESSARY_PERMISSIONS = 1;
    public static String[] mBluetoothPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CoreServiceManager serviceManager = CoreServiceManager.getInstance();
        serviceManager.doBind(this);
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

    private boolean checkPermissions(){
        for(String permission:mBluetoothPermissions){
            if(checkSelfPermission(permission)!=PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    private void initUI(){

        ScanListFragment scanListFragment = ScanListFragment.getInstance();
        scanListFragment.registerOnScanListFragmentEvents(new ScanListFragment.OnScanListFragmentEvents() {
            @Override
            public void onDeviceClicked(ScanResult result) {
                switch2TestCaseFragment(result);
            }
        });
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_main,scanListFragment,"ScanList");
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

    private void switch2TestCaseFragment(ScanResult scanResult){
        TestCaseFragment testCaseFragment = TestCaseFragment.getInstance();
        Bundle bundle = new Bundle();
        bundle.putParcelable("ScanResult",scanResult);
        testCaseFragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.replace(R.id.fragment_main,testCaseFragment);
        fragmentTransaction.commit();
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
        CoreServiceManager serviceManager = CoreServiceManager.getInstance();
        serviceManager.doUnbind(this);
    }
}
