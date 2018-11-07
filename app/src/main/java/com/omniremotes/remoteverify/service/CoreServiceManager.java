package com.omniremotes.remoteverify.service;

import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import static android.content.Context.BIND_AUTO_CREATE;

public class CoreServiceManager {
    private static final String TAG="RemoteVerify-CoreServiceManager";
    private ICoreService mService;
    private static CoreServiceManager mServiceManager;
    private boolean mServiceConnected = false;
    private ICoreServiceListener mListener;
    public static CoreServiceManager getInstance(){
        if(mServiceManager == null){
            mServiceManager = new CoreServiceManager();
        }
        return mServiceManager;
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

    public void doUnbind(Context context){
        context.unbindService(mConnection);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.e(TAG,"onServiceConnected");
            mService = ICoreService.Stub.asInterface(service);
            if(mService == null){
                Log.d(TAG,"service is null");
            }

            mServiceConnected = true;
            if(mListener != null){
                mListener.onServiceConnected();
            }
            CoreService coreService = CoreService.getCoreService();
            if(coreService == null){
                return;
            }
            coreService.registerOnCoreServiceEventsListener(new CoreService.OnCoreServiceEvents() {
                @Override
                public void onScanResults(ScanResult scanResult) {
                    if(mListener != null){
                        mListener.onScanResult(scanResult);
                    }
                }
            });
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG,"onServiceDisconnected");
            mServiceConnected = false;
            mService = null;
            if(mListener != null){
                mListener.onServiceDisconnected();
            }
        }
    };

    public void  registerCoreServiceListener(ICoreServiceListener listener){
        mListener = listener;
    }

    public void unRegisterCoreServiceListener(){
        mListener = null;
    }

    public boolean isServiceConnected() {
        return mServiceConnected;
    }

    public boolean stopScan(){
        if(mService == null){
            return false;
        }
        try{
            return mService.stopScan();
        }catch (RemoteException e){
            Log.d(TAG,""+e);
        }
        return false;
    }

    public boolean startScan(){
        if(mService == null){
            return false;
        }
        try{
            return mService.startScan();
        }catch (RemoteException e){
            Log.d(TAG,""+e);
        }
        return false;
    }
}
