package com.omniremotes.remoteverify.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class VoiceService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private static class VoiceServiceBinder extends IVoiceService.Stub{
        VoiceService svc;
        VoiceServiceBinder(VoiceService service){
            svc = service;
        }
        @Override
        public void startVoice() {
            if(svc == null){
                return;
            }
            svc.startVoice();
        }

        @Override
        public void stopVoice() {
            if(svc == null){
                return;
            }
            svc.stopVoice();
        }
    }

    private void startVoice(){

    }

    private void stopVoice(){

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

}
