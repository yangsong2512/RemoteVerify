package com.omniremotes.remoteverify.interfaces;

import android.bluetooth.le.ScanResult;

public interface ICoreServiceListener {
    void onServiceConnected();
    void onServiceDisconnected();
}
