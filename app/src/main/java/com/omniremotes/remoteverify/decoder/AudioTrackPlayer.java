package com.omniremotes.remoteverify.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioTrackPlayer {
    private static final String TAG="RemoteVerify-"+AudioTrackPlayer.class.getSimpleName();
    private AudioTrack mAudioTrack;
    private Thread mThread;
    private boolean mQuit = false;
    private final Object mLock = new Object();
    private static final int MAX_DATA_IN_QUEUE = 6;
    private ByteArrayOutputStream mByteStream;
    private List<byte[]> mDataList;
    private int mMinBufferSize;
    public AudioTrackPlayer(int sampleRate,int channelConfig,int audioFormat){
        Log.d(TAG,"AudioTrackPlayer:sampleRate"+sampleRate);
        mByteStream = new ByteArrayOutputStream();
        mDataList = new ArrayList<>();
        mMinBufferSize = AudioTrack.getMinBufferSize(sampleRate,channelConfig,audioFormat);
        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                mMinBufferSize,
                AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        mThread = new Thread(new AudioTrackPlayThread());
        mThread.start();
    }

    public void onAudioStop(){
        Log.d(TAG,"onAudioStop");
        mQuit = true;
        synchronized (mLock){
            mLock.notifyAll();
        }
        if(mAudioTrack != null){
            mAudioTrack.stop();
            mAudioTrack.release();
        }
        if(mThread != null){
            mThread.interrupt();
            mThread = null;
        }
    }

    private class AudioTrackPlayThread implements Runnable{
        @Override
        public void run() {
            while (!mQuit){
                synchronized (mLock){
                    try{
                        mLock.wait();
                        if(mDataList.size()>0){
                            byte[] pcm = mDataList.remove(mDataList.size()-1);
                            mAudioTrack.write(pcm,0,pcm.length);
                        }
                    }catch (InterruptedException e){
                        Log.d(TAG,""+e);
                    }
                }
            }
        }
    }

    public synchronized void onDataReady(byte[] data){
        if(mByteStream.size()<mMinBufferSize){
            try{
                mByteStream.write(data);
                if(mByteStream.size()>=mMinBufferSize){
                    if(mDataList.size()==MAX_DATA_IN_QUEUE){
                        mDataList.remove(mDataList.size()-1);
                    }
                    byte[] trackData = mByteStream.toByteArray();
                    mDataList.add(0,trackData);
                    mByteStream.reset();
                    synchronized (mLock){
                        mLock.notifyAll();
                    }
                }
            }catch (IOException e){
                Log.d(TAG,""+e);
            }
        }
    }
}
