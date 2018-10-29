// ICoreService.aidl
package com.omniremotes.remoteverify.service;
import android.bluetooth.BluetoothDevice;
// Declare any non-default types here with import statements

interface ICoreService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    //void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
    //        double aDouble, String aString);
    void initHid();
    void notifyBluetoothStateChanged(boolean enabled);
    boolean startScan();
    boolean stopScan();
}