package com.omniremotes.remoteverify.adapter;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.omniremotes.remoteverify.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class ScanListAdapter extends BaseAdapter {
    private List<ScanResult> mScanList;
    private OnDeviceClickedListener mListener;
    private Context mContext;
    interface OnDeviceClickedListener{
        void onDeviceClicked(ScanResult scanResult);
    }

    private class ViewHolder {
        TextView rssiView;
        TextView infoView;
        TextView statusView;
    }

    public void registerOnDeviceClickedListener(OnDeviceClickedListener listener) {
        this.mListener = listener;
    }

    public ScanListAdapter(Context context){
        mContext = context;
        mScanList  = new ArrayList<>();
    }
    @Override
    public Object getItem(int position) {
        return mScanList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getCount() {
        return mScanList.size();
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(mContext,"Device Clicked",Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if(convertView == null){
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.scan_list_item,parent,false);
            viewHolder.rssiView = convertView.findViewById(R.id.device_rssi);
            viewHolder.infoView = convertView.findViewById(R.id.device_info);
            viewHolder.statusView = convertView.findViewById(R.id.device_status);
            convertView.setOnClickListener(mOnClickListener);
            convertView.setTag(viewHolder);
        }else {
            viewHolder =(ViewHolder) convertView.getTag();
        }
        ScanResult scanResult = mScanList.get(position);
        viewHolder.rssiView.setText(""+scanResult.getRssi());
        BluetoothDevice device = scanResult.getDevice();
        if(device == null){
            return null;
        }
        String name = device.getName();
        viewHolder.infoView.setText(name==null?"":name);
        String address = device.getAddress();
        viewHolder.infoView.append(address==null?"":address);
        viewHolder.statusView.setText("A");
        return convertView;
    }

    public synchronized void notifyDataSetChanged(ScanResult result){
        int index = 0;
        int max =  0;
        ScanResult dupResult = null;
        for(ScanResult tmp:mScanList){
            String address = tmp.getDevice().getAddress();
            int curIndex = mScanList.indexOf(tmp);
            String inAddress = result.getDevice().getAddress();
            int inRssi = result.getRssi();
            int rssi = tmp.getRssi();
            if(address.equals(inAddress)){
                if(inRssi == rssi){
                    return;
                }else {
                    dupResult = tmp;
                }
            }
            if(inRssi>rssi && max != 1){
                max = 1;
                index = curIndex;
            }else if(max == 0){
                ++index;
            }
        }
        mScanList.add(index,result);
        if(dupResult!=null){
            mScanList.remove(dupResult);
        }
        notifyDataSetChanged();
    }
}
