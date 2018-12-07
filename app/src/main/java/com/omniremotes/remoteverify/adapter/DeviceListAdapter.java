package com.omniremotes.remoteverify.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.omniremotes.remoteverify.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceListAdapter extends BaseAdapter {
    private List<BluetoothDevice> mDeviceList;
    private Context mContext;
    public DeviceListAdapter(Context context){
        mDeviceList = new ArrayList<>();
        mContext = context;
    }
    public DeviceListAdapter(Context context, Set<BluetoothDevice> devices){
        mDeviceList = new ArrayList<>();
        mContext = context;
        mDeviceList.addAll(devices);
    }
    private class ViewHolder {
        TextView deviceInfo;
        TextView deviceRSSI;
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
        ViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.scan_list_item,parent,false);
            viewHolder.deviceInfo = convertView.findViewById(R.id.device_info);
            viewHolder.deviceRSSI = convertView.findViewById(R.id.device_rssi);
            viewHolder.deviceRSSI.setBackgroundResource(R.drawable.ic_bt_misc_hid);
            convertView.setTag(viewHolder);
            convertView.setId(position);
        }
        viewHolder = (ViewHolder)convertView.getTag();
        BluetoothDevice device = mDeviceList.get(position);
        String name = device.getName();
        name=name==null?"NULL":name;
        viewHolder.deviceInfo.setText(name);
        String address =device.getAddress();
        viewHolder.deviceInfo.append("\n"+address);
        return convertView;
    }

    public void removeDevice(BluetoothDevice device){
        for(BluetoothDevice dev:mDeviceList){
            if(dev.equals(device)){
                mDeviceList.remove(dev);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void addDevices(Set<BluetoothDevice> devices){
        mDeviceList.addAll(devices);
    }

    public void addDevice(BluetoothDevice device){
        mDeviceList.add(device);
        notifyDataSetChanged();
    }

}
