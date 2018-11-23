package com.omniremotes.remoteverify.interfaces;

public interface IRemoteServiceListener {
    void onGattServiceConnected();
    void onGattServiceDisconnected();
    void onGattServiceDiscovered();
}
