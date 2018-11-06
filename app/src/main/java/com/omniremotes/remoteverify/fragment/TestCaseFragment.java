package com.omniremotes.remoteverify.fragment;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.SpannableString;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.omniremotes.remoteverify.R;

import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;

public class TestCaseFragment extends Fragment {
    private static final String TAG="RemoteVerify-TestCaseFragment";
    static private TestCaseFragment mTestCaseFragment;
    public static TestCaseFragment getInstance(){
        if(mTestCaseFragment == null){
            mTestCaseFragment = new TestCaseFragment();
        }
        return mTestCaseFragment;
    }

    private String makeString(String string){
        if(string == null){
            string = "NULL";
        }
        SpannableString spannableString = new SpannableString(string);
        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView textView = view.findViewById(R.id.device_detail);
        Bundle bundle = getArguments();
        if(bundle == null){
            return;
        }
        ScanResult scanResult = bundle.getParcelable("ScanResult");
        if(scanResult == null){
            return;
        }
        BluetoothDevice device = scanResult.getDevice();
        if(device == null){
            return;
        }
        String name = makeString(device.getName());
        name=name!=null?name:"NULL";
        textView.setText(name);
        textView.append("\nAddress:"+device.getAddress());
        textView.append("\nRSSI:"+scanResult.getRssi());
        ScanRecord scanRecord = scanResult.getScanRecord();
        StringBuilder builder = new StringBuilder();
        byte[] raw = scanRecord.getBytes();
        for(byte data:raw){
            builder.append(String.format("%02x",data&0xff)+" ");
        }
        textView.append("\nAdvData:"+builder.toString());
        scanRecord.getServiceUuids();
        scanRecord.getManufacturerSpecificData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        return inflater.inflate(R.layout.fragment_test_case_layout,container,false);
    }
}
