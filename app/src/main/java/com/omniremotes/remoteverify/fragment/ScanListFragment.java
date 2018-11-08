package com.omniremotes.remoteverify.fragment;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.omniremotes.remoteverify.R;
import com.omniremotes.remoteverify.adapter.ScanListAdapter;

public class ScanListFragment extends Fragment {
    private static final String TAG="RemoteVerify-ScanListFragment";
    private static ScanListFragment mScanListFragment;
    private ScanListAdapter mAdapter;
    private OnScanListFragmentEvents mListener;
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

    private AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            ScanResult scanResult = mAdapter.getScanList().get(position);
            if(mListener!=null){
                mListener.onDeviceClicked(scanResult);
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"onViewCreated");
        ListView listView = view.findViewById(R.id.scan_list);
        listView.setOnItemClickListener(mOnItemClickListener);
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
    }

    public void notifyOnScanResult(ScanResult result){
        if(mAdapter != null){
            mAdapter.notifyDataSetChanged(result);
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
        if(mAdapter != null){
            mAdapter.clearDataSet();
        }
        Log.d(TAG,"onDestroy");
    }
}
