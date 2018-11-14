package com.omniremotes.remoteverify.decoder;

public abstract class VoiceDecoder {
    public static final int ENCODE_TYPE_ADPCM_8K_16BIT = 1;
    public static final int ENCODE_TYPE_ADPCM_16K_16BIT = 2;
    public static final int ENCODE_TYPE_SBC_16K_16BIT = 3;
    public static final int ENCODE_TYPE_MSBC_16K_16BIT = 4;
    private int mVersion;
    private int mSampleRate;
    private int mBitDepth;
    private int mEncoder;
    private int mBytesPerFrame;
    private int mBytesPerPacket;
    private int mCodecSupported;
    private int version;

    public VoiceDecoder(short version,short codecSupported,short bytesPerFrame,short bytesPerChara){
        mVersion = version;
        mCodecSupported = codecSupported;
    }

    public void setEncoder(int encoder){
        mEncoder = encoder;
    }

    public void setBitDepth(int bitDepth){
        mBitDepth = bitDepth;
    }

    public void setSampleRate(int sampleRate){
        mSampleRate = sampleRate;
    }

    public void setBytesPerPacket(int bytesPerPacket){
        mBytesPerPacket = bytesPerPacket;
    }

    public void setBytesPerFrame(int bytesPerFrame){
        mBytesPerFrame = bytesPerFrame;
    }

    abstract void decode(byte[] bytes);
}
