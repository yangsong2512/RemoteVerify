package com.omniremotes.remoteverify.decoder;

public class VoiceInfo {
    public short version;
    public short codecSupported;
    public short bytesPerFrame;
    public short bytesPerChara;
    public VoiceInfo(short version,short codecSupported,short bytesPerFrame,short bytesPerChara){
        this.version = version;
        this.codecSupported = codecSupported;
        this.bytesPerChara = bytesPerChara;
        this.bytesPerFrame = bytesPerFrame;
    }
}
