package com.omniremotes.remoteverify.fragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.Toast;

import com.omniremotes.remoteverify.R;
import com.omniremotes.remoteverify.adapter.DeviceListAdapter;
import com.omniremotes.remoteverify.adapter.ScanListAdapter;

public class ScanListFragment extends Fragment {
    private static final String TAG="RemoteVerify-ScanListFragment";
    private static ScanListFragment mScanListFragment;
    private ScanListAdapter mAdapter;
    private DeviceListAdapter mDeviceListAdapter;
    private OnScanListFragmentEvents mListener;
    public interface OnScanListFragmentEvents{
        void onDeviceClicked(ScanResult result);
        void onPairedDeviceClicked(BluetoothDevice device);
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

    private AdapterView.OnItemClickListener mOnPairedDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            BluetoothDevice device =(BluetoothDevice) mDeviceListAdapter.getItem(position);
            if(mListener != null){
                mListener.onPairedDeviceClicked(device);
            }
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG,"onViewCreated");
        ListView listView = view.findViewById(R.id.scan_list);
        listView.setOnItemClickListener(mOnItemClickListener);
        ListView deviceListView = view.findViewById(R.id.device_list);
        deviceListView.setOnItemClickListener(mOnPairedDeviceClickListener);
        if(mAdapter == null){
            mAdapter = new ScanListAdapter(getActivity());
        }
        if(mDeviceListAdapter == null){
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            mDeviceListAdapter = new DeviceListAdapter(getActivity(),adapter.getBondedDevices());
            deviceListView.setAdapter(mDeviceListAdapter);
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
