package com.omniremotes.remoteverify.decoder;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ADPCMDecoder {
    private static final String TAG="RemoteVerify-ADPCMDecoder";
    private int mExpSeqNum = 0;
    private int mDroppedFrames = 0;
    private boolean mNewFrameStartFlag = false;
    private ByteArrayOutputStream mOutputStream ;
    private short mVersion;
    private short mCodecSupported;
    private short mBytesPerFrame;
    private short mBytesPerPacket;
    private OnPcmDataReadyListener mListener;
    private static final int APP_WAVE_HDR_SIZE=44;
    int DBG=1;

    public interface OnPcmDataReadyListener{
        void onPcmDataReady(byte[] data);
    }

    public ADPCMDecoder(short version, short codecSupported, short bytesPerFrame, short bytesPerChara){
        mVersion = version;
        mCodecSupported = codecSupported;
        mBytesPerFrame = bytesPerFrame;
        mBytesPerPacket = bytesPerChara;
    }

    public void registerOnPcmDataReadyListener(OnPcmDataReadyListener listener){
        mListener = listener;
    }
    /*Step size look up table*/
    private static int[] steptab = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
        19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
        130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
        876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
        2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };
    /*Index table*/
    private static int[] indexTable = {
            -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8
    };

    private static byte[] mWavHdr = new byte[]{
            'R','I','F','F',
            '\0', '\0', '\0', '\0',
            'W', 'A', 'V', 'E',
            'f', 'm', 't', ' ',
            0x10, 0x00, 0x00, 0x00,
            0x01, 0x00,
            '\0', '\0',
            '\0', '\0','\0', '\0',
            '\0', '\0',
            '\0', '\0',
            'd', 'a', 't', 'a',
            '\0', '\0', '\0', '\0'
    };


    public void onVoiceStart(){
        Log.d(TAG,"version:"+mVersion+",codecSupported:"+mCodecSupported+",bytesPerFrame:"+mBytesPerFrame
                +",bytesPerPacket:"+mBytesPerPacket);
        mNewFrameStartFlag = true;
        mOutputStream = new ByteArrayOutputStream();
        mOutputStream.reset();
    }
    public void onVoiceStop(){
        Log.d(TAG,"onVoiceStop:"+mDroppedFrames);
        if(mOutputStream != null){
            try{
                mOutputStream.close();
            }catch (IOException e){
                Log.d(TAG,""+e);
            }
        }
    }
    public void onVoiceSync(){
        mOutputStream.reset();
        mNewFrameStartFlag = true;
    }

    public synchronized void append(byte[] bytes){
        if(mNewFrameStartFlag){
            mNewFrameStartFlag = false;
            mOutputStream.reset();
            int seqNum = bytes[1]&0xff+((bytes[0]&0xff)<<8);
            if(seqNum != mExpSeqNum && seqNum != 0){
                int dropped = seqNum - mExpSeqNum;
                mDroppedFrames += dropped;
            }
            mExpSeqNum  = seqNum+1;
        }
        try{
            mOutputStream.write(bytes);
        }catch (IOException e){
            Log.d(TAG,""+e);
        }
        if(bytes.length<=14) {
            mNewFrameStartFlag = true;
            byte[] frame = mOutputStream.toByteArray();
            if(frame.length == mBytesPerFrame){
                decode(frame);
            }
        }
    }

    private static void fillWavHdr(int nbChannels,int sampleRate,int bitsPerSample,int dataSize){
        int byteRate = 0;
        int blockAlign = 0;
        if(bitsPerSample == 8){
            byteRate = nbChannels*sampleRate;
            blockAlign=nbChannels;
        }else if(bitsPerSample == 16){
            byteRate = nbChannels*2*sampleRate;
            blockAlign = nbChannels*2;
        }
        int chunkSize = dataSize+36;
        mWavHdr[4] = (byte) chunkSize;
        mWavHdr[5] = (byte)(chunkSize >> 8);
        mWavHdr[6] = (byte)(chunkSize >> 16);
        mWavHdr[7] = (byte)(chunkSize >> 24);

        mWavHdr[22] = (byte)nbChannels;
        mWavHdr[23] = 0; /* nb_channels is coded on 1 byte only */

        mWavHdr[24] = (byte)sampleRate;
        mWavHdr[25] = (byte)(sampleRate >> 8);
        mWavHdr[26] = (byte)(sampleRate >> 16);
        mWavHdr[27] = (byte)(sampleRate >> 24);

        mWavHdr[28] = (byte)byteRate;
        mWavHdr[29] = (byte)(byteRate >> 8);
        mWavHdr[30] = (byte)(byteRate >> 16);
        mWavHdr[31] = (byte)(byteRate >> 24);

        mWavHdr[32] = (byte)blockAlign;
        mWavHdr[33] = (byte)(blockAlign >> 8);

        mWavHdr[34] = (byte)bitsPerSample;
        mWavHdr[35] = (byte)(bitsPerSample >> 8);

        mWavHdr[40] = (byte)dataSize;
        mWavHdr[41] = (byte)(dataSize >> 8);
        mWavHdr[42] = (byte)(dataSize >> 16);
        mWavHdr[43] = (byte)(dataSize >> 24);
    }

    private synchronized void decode(byte[] rawData) {
        int n = 6;
        int code = 0;
        int diff = 0;
        int sampx = 0;
        int len = 0;
        int index = (rawData[5] & 0xff);
        boolean odd = true;
        byte[] pcmData = new byte[(rawData.length-6)*4];
        //int preSample = (rawData[4] & 0xff + ((rawData[3] & 0xff) << 8));
        int predictedSample = 0;
        while (n < rawData.length) {
            diff = 0;
            if (odd) code = ((rawData[n] & 0xff) >> 4);
            else code = ((rawData[n]) & 0x0f);

            if ((code & 0x04) != 0) diff = diff + steptab[index];
            if ((code & 0x02) != 0) diff = diff + (steptab[index] >> 1);
            if ((code & 0x01) != 0) diff = diff + (steptab[index] >> 2);
            diff = diff + (steptab[index] >> 3);
            if ((code & 0x08) != 0) diff = - diff;
            predictedSample += diff;
            if(predictedSample > 32767){
                predictedSample = 32767;
            }else if(predictedSample < -32768){
                predictedSample = -32768;
            }
            pcmData[len++] = (byte)((predictedSample&0xff));
            pcmData[len++] = (byte)((predictedSample&0xff00)>>8);
            // adjust index
            index += indexTable[code];
            if (index < 0) index = 0;
            if (index > 88) index = 88;
            odd = (!odd);
            if (odd) n++;
            if (len > (mBytesPerFrame-6)*4){
                return;
            }
        }
        if (mListener != null) {
            mListener.onPcmDataReady(pcmData);
        }
    }
}
