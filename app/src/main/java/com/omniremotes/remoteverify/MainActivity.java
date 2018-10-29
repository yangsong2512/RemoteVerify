package com.omniremotes.remoteverify;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ListView;
import android.widget.Toast;

import com.omniremotes.remoteverify.adapter.ScanListAdapter;
import com.omniremotes.remoteverify.service.CoreService;
import com.omniremotes.remoteverify.service.ICoreService;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG="RemoteVerify-MainActivity";
    private static final int REQUST_BLUETOOTH_ENABLE = 0;
    private static final int REQUEST_NECESSARY_PERMISSIONS = 1;
    private boolean mEnabled = false;
    private ICoreService mService;
    private Handler mUiHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver,filter);
        if(!checkBluetoothState()){
            Toast.makeText(this,"Device does not support bluetooth",Toast.LENGTH_LONG).show();
            finish();
        }else {
            doBind();
        }
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
                startActivityForResult(intent,REQUST_BLUETOOTH_ENABLE);
                mEnabled = false;
            }else {
                mEnabled = true;
            }
            return true;
        }
        return false;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action == null){
                return;
            }
            switch (action){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                    if(state == BluetoothAdapter.STATE_ON){
                        mEnabled = true;
                    }else if(state == BluetoothAdapter.STATE_OFF){
                        mEnabled = false;
                    }
                    try{
                        if(mService != null){
                            mService.notifyBluetoothStateChanged(mEnabled);
                        }
                    }catch (RemoteException e){
                        Log.d(TAG,""+e);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUST_BLUETOOTH_ENABLE){
            if(resultCode == RESULT_OK){
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if(adapter.isEnabled()){
                    mEnabled = true;
                }
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
                if(continueInitialize()){
                    Log.d(TAG,"initialize cannot continue");
                }
            }
        }
    }

    private boolean continueInitialize(){
        ListView scanListView = findViewById(R.id.scan_list);
        CoreService coreService = CoreService.getCoreService();
        if(coreService == null){
            return false;
        }
        ScanListAdapter adapter = coreService.getScanListAdapter();
        if(adapter == null){
            return false;
        }
        scanListView.setAdapter(adapter);
        return true;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG,"onServiceConnected");
            mService = ICoreService.Stub.asInterface(service);
            try{
                mService.notifyBluetoothStateChanged(mEnabled);
            }catch (RemoteException e){
                Log.d(TAG,""+e);
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(requestNecessaryPermissions()){
                        continueInitialize();
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
        if(mReceiver != null){
            Log.d(TAG,"unregisterReceiver");
            unregisterReceiver(mReceiver);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if(mService != null){
                try{
                    mService.stopScan();
                }catch (RemoteException e){
                    Log.d(TAG,""+e);
                }
            }
            if(mReceiver != null){
                unregisterReceiver(mReceiver);
                mReceiver = null;
            }
            if(mService != null){
                unbindService(mConnection);
                mService = null;
            }
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }
}