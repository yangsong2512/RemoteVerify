package com.omniremotes.remoteverify.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends BaseAdapter {
    private List<BluetoothDevice> mDeviceList;
    private Context mContext;
    public DeviceListAdapter(Context context){
        mDeviceList = new ArrayList<>();
        mContext = context;
    }
    @Override
    public Object getItem(int position) {
        if(mDeviceList.size()>0){
            return mDeviceList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
