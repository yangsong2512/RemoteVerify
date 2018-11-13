// IVoiceService.aidl
package com.omniremotes.remoteverify.service;
import android.bluetooth.BluetoothDevice;

// Declare any non-default types here with import statements

interface IVoiceService {
    void startVoice(in BluetoothDevice device);
    void stopVoice(in BluetoothDevice device);
    void connect(in BluetoothDevice device);
}
