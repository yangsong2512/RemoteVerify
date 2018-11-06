package com.omniremotes.remoteverify.service;

import android.bluetooth.le.ScanResult;

public interface ICoreServiceListener {
    void onServiceConnected();
    void onServiceDisconnected();
    void onScanResult(ScanResult result);
}
