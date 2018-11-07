package com.omniremotes.remoteverify.fragment;

import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.omniremotes.remoteverify.R;
import com.omniremotes.remoteverify.adapter.ScanListAdapter;
import com.omniremotes.remoteverify.service.CoreServiceManager;
import com.omniremotes.remoteverify.service.ICoreServiceListener;

public class ScanListFragment extends Fragment {
    private static final String TAG="RemoteVerify-ScanListFragment";
    private static ScanListFragment mScanListFragment;
    private ScanListAdapter mAdapter;
    private OnScanListFragmentEvents mListener;
    private CoreServiceManager mServiceManager;
    public static ScanListFragment getInstance(){
        if(mScanListFragment == null){
            mScanListFragment = new ScanListFragment();
        }
        return mScanListFragment;
    }
    public interface OnScanListFragmentEvents{
        void onDeviceClicked(ScanResult result);
    }
    public ScanListFragment(){
    }

    public void registerOnScanListFragmentEvents(OnScanListFragmentEvents listener){
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        return inflater.inflate(R.layout.fragment_scan_list_layout,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"onViewCreated");
        ListView listView = view.findViewById(R.id.scan_list);
        if(mAdapter == null){
            mAdapter = new ScanListAdapter(getActivity());
        }
        mAdapter.registerOnDeviceClickedListener(new ScanListAdapter.OnDeviceClickedListener() {
            @Override
            public void onDeviceClicked(ScanResult scanResult) {
                if(mListener != null){
                    mListener.onDeviceClicked(scanResult);
                }
            }
        });
        listView.setAdapter(mAdapter);
        mServiceManager = CoreServiceManager.getInstance();
        mServiceManager.registerCoreServiceListener(new CoreServiceListener());
        if(mServiceManager.isServiceConnected()){
            mServiceManager.startScan(null,ScanSettings.SCAN_MODE_LOW_POWER);
        }
    }

    private class CoreServiceListener implements ICoreServiceListener{
        @Override
        public void onServiceDisconnected() {
            Log.d(TAG,"core service disconnected");
        }

        @Override
        public void onServiceConnected() {
            Log.d(TAG,"core service connected");
            if(mServiceManager != null){
                mServiceManager.startScan(null,ScanSettings.SCAN_MODE_LOW_POWER);
            }
        }

        @Override
        public void onScanResult(ScanResult result) {
            if(mAdapter != null){
                mAdapter.notifyDataSetChanged(result);
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        Log.d(TAG,"hidden changed:"+hidden);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG,"onAttach");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mServiceManager != null){
            mServiceManager.stopScan();
            mServiceManager.unRegisterCoreServiceListener();
        }
        if(mAdapter != null){
            mAdapter.clearDataSet();
        }
        Log.d(TAG,"onDestroy");
    }
}
