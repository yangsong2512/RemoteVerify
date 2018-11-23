package com.omniremotes.remoteverify.fragment;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
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

import static android.os.Looper.getMainLooper;

public class TestCaseFragment extends Fragment {
    private static final String TAG="RemoteVerify-TestCaseFragment";
    private TestCaseAdapter mAdapter;
    private TextView mDeviceDetailView;
    private TextView mDeviceStatusView;
    private String mDeviceAddress;
    private BluetoothDevice mCurrentDevice;
    private OnTestCaseFragmentEventListener mListener;
    private Handler mHandler;
    public interface OnTestCaseFragmentEventListener{
        void onStartButtonClicked(String testCase,BluetoothDevice device,boolean running);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        return inflater.inflate(R.layout.fragment_test_case_layout,container,false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAdapter = new TestCaseAdapter(getContext(),this,mDeviceAddress);
        mDeviceDetailView = view.findViewById(R.id.device_detail);
        mDeviceStatusView = view.findViewById(R.id.device_status);
        ListView listView = view.findViewById(R.id.device_cases);
        listView.setAdapter(mAdapter);
        mHandler = new Handler(getMainLooper());
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

    public void notifyOnDeviceClicked(final ScanResult scanResult){
        if(mHandler == null){
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                BluetoothDevice device = scanResult.getDevice();
                Log.d(TAG,"device clicked");
                if(device == null){
                    Log.d(TAG,"device is null");
                    return;
                }
                mCurrentDevice = device;
                mDeviceAddress = device.getAddress();
                SpannableString name = makeString("Name     ",device.getName());
                SpannableString address = makeString("\nAddress ",mDeviceAddress);
                mDeviceDetailView.setText(name);
                mDeviceDetailView.append(address);
                mDeviceStatusView.setText(getResources().getText(R.string.device_not_paired));
                ScanRecord scanRecord = scanResult.getScanRecord();
                if(scanRecord!=null){
                    SpannableString advertise = makeString("\nBroadcast  ",convertByteToString(scanRecord.getBytes()));
                    mDeviceDetailView.append(advertise);
                }
                mAdapter.clearDataSet();
                mAdapter.parserTestCase();
            }
        });

    }

    public void  registerOnTestCaseFragmentEventListener(OnTestCaseFragmentEventListener listener){
        mListener = listener;
    }

    public void onStartButtonClicked(String testCase,boolean running){
        if(mListener != null){
            if(mCurrentDevice == null){
                Log.d(TAG,"current device is null");
            }
            mListener.onStartButtonClicked(testCase,mCurrentDevice,running);
        }
    }

    public void notifyBondStateChanged(final BluetoothDevice device,final int preState,final int state){
        if(mHandler == null){
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String address = device.getAddress();
                if(address.equals(mDeviceAddress)){
                    if(state == BluetoothDevice.BOND_BONDING){
                        mDeviceStatusView.setText(getResources().getText(R.string.device_pairing));
                    }else if(state == BluetoothDevice.BOND_BONDED){
                        mDeviceStatusView.setText(getResources().getText(R.string.device_paired));
                        mAdapter.notifyPairSuccess();
                    }else if(state == BluetoothDevice.BOND_NONE){
                        mDeviceStatusView.setText(getResources().getText(R.string.device_not_paired));
                    }
                }
            }
        });

    }

    public void notifyConnectionStateChanged(final BluetoothDevice device,final int preState,final int state){
        if(mHandler == null){
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String address = device.getAddress();
                Log.d(TAG,"inAddr:"+address);
                Log.d(TAG,"oriAddr:"+mDeviceAddress);
                if(mDeviceAddress == null){
                    return;
                }
                if(address.equals(mDeviceAddress)){
                    if(state == BluetoothProfile.STATE_CONNECTING){
                        mDeviceStatusView.setText(getResources().getText(R.string.device_connecting));
                    }else if(state == BluetoothProfile.STATE_CONNECTED){
                        mDeviceStatusView.setText(getResources().getText(R.string.device_connected));
                        mAdapter.notifyDeviceConnected(address);
                    }else if(state == BluetoothProfile.STATE_DISCONNECTED){
                        mDeviceStatusView.setText(getResources().getText(R.string.device_disconnected));
                    }
                }
            }
        });
    }

    public void notifyAclDisconnected(final BluetoothDevice device){
        if(mHandler == null){
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String address = device.getAddress();
                if(address.equals(mDeviceAddress)){
                    mDeviceStatusView.setText(getResources().getText(R.string.acl_disconnected));
                }
            }
        });

    }

    public void notifyAclConnected(final BluetoothDevice device){
        if(mHandler == null){
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String address = device.getAddress();
                if(address.equals(mDeviceAddress)){
                    mDeviceStatusView.setText(getResources().getText(R.string.acl_connected));
                }
            }
        });

    }

    public void notifyOnPairedDeviceClicked(final BluetoothDevice device,final boolean connected){
        if(device == null || mHandler == null){
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mDeviceStatusView.setText(connected?"Device connected":"Device disconnected");
                mCurrentDevice = device;
                mDeviceAddress = device.getAddress();
                SpannableString name = makeString("Name     ",device.getName());
                SpannableString address = makeString("\nAddress ",mDeviceAddress);
                mDeviceDetailView.setText(name);
                mDeviceDetailView.append(address);
                mAdapter.clearDataSet();
                mAdapter.parserTestCase();
            }
        });

    }

    public void onStartPairing(BluetoothDevice device){
    }
}
