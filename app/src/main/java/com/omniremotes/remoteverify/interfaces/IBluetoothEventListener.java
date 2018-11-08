package com.omniremotes.remoteverify.interfaces;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;

public interface IBluetoothEventListener {
    void onBondStateChanged(BluetoothDevice device,int preState,int state);
    void onConnectionStateChanged(BluetoothDevice device,int preState,int state);
    void onAclConnected(BluetoothDevice device);
    void onAclDisconnected(BluetoothDevice device);
    void onAclDisconnectRequest(BluetoothDevice device);
    void onScanResult(ScanResult result);
    void onStartParing();
}
