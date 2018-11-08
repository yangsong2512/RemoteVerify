package com.omniremotes.remoteverify.fragment;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.omniremotes.remoteverify.R;
import com.omniremotes.remoteverify.adapter.TestCaseAdapter;

public class TestCaseFragment extends Fragment {
    private static final String TAG="RemoteVerify-TestCaseFragment";
    private TestCaseAdapter mAdapter;
    private TextView mDeviceDetailView;
    private String mDeviceAddress;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        return inflater.inflate(R.layout.fragment_test_case_layout,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDeviceDetailView = view.findViewById(R.id.device_detail);
        ListView listView = view.findViewById(R.id.device_cases);
        listView.setAdapter(mAdapter);
    }

    private SpannableString makeString(String key,String value){
        if(value == null){
            value = "NULL";
        }
        SpannableString spannableString = new SpannableString(key+":"+value);
        RelativeSizeSpan keySize = new RelativeSizeSpan(1.2f);
        spannableString.setSpan(keySize,0,key.length(),Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    private String convertByteToString(byte[] bytes){
        StringBuilder builder = new StringBuilder();
        for(byte data:bytes){
            builder.append(String.format("%02x",data&0xff)+" ");
        }
        return builder.toString();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG,"onAttach");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG,"onDetach");

    }

    public void notifyOnDeviceClicked(ScanResult scanResult){
        BluetoothDevice device = scanResult.getDevice();
        if(device == null){
            return;
        }
        mDeviceAddress = device.getAddress();
        SpannableString name = makeString("Name     ",device.getName());
        SpannableString address = makeString("\nAddress ",mDeviceAddress);
        mDeviceDetailView.setText(name);
        mDeviceDetailView.append(address);
        ScanRecord scanRecord = scanResult.getScanRecord();
        if(scanRecord!=null){
            SpannableString advertise = makeString("\nBroadcast  ",convertByteToString(scanRecord.getBytes()));
            mDeviceDetailView.append(advertise);
        }
        mAdapter.clearDataSet();
        mAdapter.parserTestCase();
    }

}
