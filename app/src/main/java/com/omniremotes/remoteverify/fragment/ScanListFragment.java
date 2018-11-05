package com.omniremotes.remoteverify.fragment;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.omniremotes.remoteverify.R;
import com.omniremotes.remoteverify.adapter.ScanListAdapter;
import com.omniremotes.remoteverify.service.CoreService;

public class ScanListFragment extends Fragment {
    private final String TAG="RemoteVerify-ScanListFragment";
    private ListView mScanListView;
    private boolean mDetached = false;
    private ScanListAdapter mAdapter;
    private OnScanListFragmentEvents mListener;
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
        mScanListView = view.findViewById(R.id.scan_list);
        if(mAdapter == null){
            mAdapter = new ScanListAdapter(getActivity());
            mAdapter.registerOnDeviceClickedListener(new ScanListAdapter.OnDeviceClickedListener() {
                @Override
                public void onDeviceClicked(ScanResult scanResult) {
                    if(mListener != null){
                        mListener.onDeviceClicked(scanResult);
                    }
                }
            });
        }
        mScanListView.setAdapter(mAdapter);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG,"onAttach");
        mDetached = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach");
        mDetached = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    public void notifyDataSetChanged(ScanResult result){
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged(result);
        }
    }

    public boolean isFragmentDetached() {
        return mDetached;
    }
}
