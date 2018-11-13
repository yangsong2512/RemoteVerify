package com.omniremotes.remoteverify.decoder;

import android.speech.tts.Voice;

import com.omniremotes.remoteverify.service.VoiceInfo;

import javax.net.ssl.SNIHostName;

public class ADPCMDecoder {
    private VoiceInfo mVoiceInfo;
    private int expSeqNum = 0;
    private short mDroppedPackets = 0;
    private boolean mVoiceStartFlag = false;
    private byte[] mRawData;
    public void setVoiceStartFlag(boolean start){
        mVoiceStartFlag = start;
        if(!start){
            mDroppedPackets = 0;
        }
    }
    public ADPCMDecoder(VoiceInfo voiceInfo){
        mVoiceInfo = voiceInfo;
        mRawData = new byte[voiceInfo.bytesPerFrame];
    }
    /*Step size look up table*/
    private static short[] steptab = {
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
    private static  byte[] indexTable = {
            -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8
    };

    public void append(byte[] bytes){
        int seqNum = bytes[1]&0xff+(bytes[0]&0xff<<8);
        if(seqNum != expSeqNum && seqNum != 0){
            int dropped = seqNum - expSeqNum;
            mDroppedPackets += dropped;
        }
        expSeqNum  = seqNum+1;
    }

    public short[] decode(byte[] rawData){

        short[] pcmData = new short[rawData.length*2];
        short preSample = (short)(rawData[4]&0xff+(rawData[3]&0xff<<8));
        short index =(short)(rawData[5]&0xff);
        int n = 6;
        boolean odd = true;
        byte code = 0;
        int diff = 0;
        int sampx = 0;
        int len = 0;
        while(n < rawData.length){
            diff = 0;
            if(odd) code =(byte)((rawData[n]&0xff)>>4);
            else code =(byte) (rawData[n]&0x0f);
            if ((code & 4)!=0) diff = diff + steptab[index];
            if ((code & 2)!=0) diff = diff + (steptab[index] >> 1);
            if ((code & 1)!=0) diff = diff + (steptab[index] >> 2);
            diff = diff + (steptab[index] >> 3);
            if ((code & 8)!=0) sampx = preSample - diff;
            else sampx = preSample + diff;
            // check sampx
            if(diff<=32767)
            {
                if (sampx < -32767) sampx = -32767;
                if (sampx > 32767) sampx = 32767;
            }
            pcmData[len] =(short) sampx;
            preSample = pcmData[len];
            len++;
            // adjust index
            index =(short) (index + indexTable[code]);
            if (index < 0) index = 0;
            if (index >88) index = 88;

            odd = (!odd);
            if (odd)  n++;
            if (len > mVoiceInfo.bytesPerFrame/2)
                return null;
            n++;
        }
        return pcmData;
    }

    public int getDroppedPacketsNum(){
        return mDroppedPackets;
    }
}
