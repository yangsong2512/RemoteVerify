package com.omniremotes.remoteverify.adapter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import java.util.Set;

public class LocalBluetoothAdapter {
    private static LocalBluetoothAdapter mLocalAdapter;
    private static final int REQUEST_BLUETOOTH_ENABLE = 0;
    public static LocalBluetoothAdapter getInstance(){
        if(mLocalAdapter == null){
            mLocalAdapter = new LocalBluetoothAdapter();
        }
        return mLocalAdapter;
    }

    private LocalBluetoothAdapter(){
    }

    public boolean deviceHasBluetoothFeature(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null;
    }

    public boolean isEnabled(){
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter.isEnabled();
    }

    public Set<BluetoothDevice> getBondedDevices(){
        if(!isEnabled()){
            return null;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter.getBondedDevices();
    }
}
