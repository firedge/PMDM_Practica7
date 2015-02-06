package com.fdgproject.firedge.pmdm_practica7;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class Servicio extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener{

    private MediaPlayer mp;
    private EnviarProgreso progress;
    public final static String PLAY = "play", STOP = "stop", ADD = "add", PAUSE = "pause", POSITION="position";
    private String song = null;
    private final String PROGRESO = "progreso";
    private boolean reproducir;
    private enum States{
        idle,
        initialized,
        preparing,
        prepared,
        started,
        paused,
        completed,
        stopped,
        end,
        error
    };
    private States state;

    public Servicio() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mp = new MediaPlayer();
        mp.setOnPreparedListener(this);
        mp.setOnCompletionListener(this);
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            //Que hacer?
        }
        state = States.idle;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(PLAY)) {
            play();
        } else if (action.equals(STOP)) {
            stop();
        } else if (action.equals(PAUSE)){
            pause();
        } else if(action.equals(ADD)){
            String cancion = intent.getStringExtra("song");
            //Uri urisong = Uri.parse(intent.getExtras().getString("uri"));
            if(cancion!=null) {
                add(cancion);
            }
        } else if(action.equals(POSITION)){
            int pos = intent.getIntExtra("pos", 0);
            mp.seekTo(pos);
            //mp.start();
            //progress.cancel(true);
            //progress = new EnviarProgreso();
            //progress.execute();
            Log.v("POS", pos+"");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void play(){
        if(song != null){
            if(state.equals(States.error)){
                Log.v("State", "ERROR");
                state = States.idle;
            }
            if(state.equals(States.idle)){
                Log.v("State", "IDLE");
                reproducir = true;
                try {
                    //mp.setDataSource(this, urisong);
                    mp.setDataSource(song);
                    state = States.initialized;
                } catch (IOException e) {
                    state = States.error;
                }
            }
            if(state.equals(States.initialized) || state.equals(States.stopped)){
                Log.v("State", "INITIALIZED STOPPED");
                reproducir = true;
                mp.prepareAsync();
                state = States.preparing;
            } else if(state.equals(States.preparing)){
                Log.v("State", "PREPARING");
                reproducir = true;
            } else if(state.equals(States.prepared) || state.equals(States.paused) || state.equals(States.completed)){
                Log.v("State", "PREPARED PAUSED COMPLETED");
                mp.start();
                state = States.started;
            } else if(state.equals(States.started)){
                Log.v("State", "STARTED");
            }
        }
    }

    private void pause(){
        if(state.equals(States.started)){
            mp.pause();
            state = States.paused;
        } else if(state.equals(States.paused)){
            mp.start();
            state = States.started;
            progress.cancel(true);
            progress = new EnviarProgreso();
            progress.execute();
        } else if(state.equals(States.completed)) {
            mp.seekTo(0);
            mp.start();
            state = States.started;
            progress.cancel(true);
            progress = new EnviarProgreso();
            progress.execute();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        state = States.prepared;
        if(reproducir) {
            if(progress!=null){
                progress.cancel(true);
            }
            mp.start();
            Log.v("onPrepared", "START");
            state = States.started;
            progress = new EnviarProgreso();
            progress.execute();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        state = States.completed;
    }

    @Override
    public void onAudioFocusChange(int i) {
        switch (i) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mp.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mp.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private void stop(){
        if(state.equals(States.prepared) || state.equals(States.started) || state.equals(States.paused) ||
                state.equals(States.completed)){
            mp.stop();
            state = States.stopped;
            mp.reset();
            state = States.idle;
        }
        reproducir = false;
    }

    private void add(String cancion){
        this.song = cancion;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mp.reset();
        mp.release();
        mp = null;
    }

    public static boolean encendido = true;
    private class EnviarProgreso extends AsyncTask<Void, Integer, Integer>{

        private void progreso(int i){
            Intent intent = new Intent(PROGRESO);
            intent.putExtra("actual", i);
            sendBroadcast(intent);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Log.v("ASYNCTASK", "inicio");
            while(mp.getDuration() > mp.getCurrentPosition()){
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(state.equals(States.started) && !isCancelled()) {
                    Log.v("POSITION", mp.getCurrentPosition()+"");
                    progreso(mp.getCurrentPosition());
                } else {
                    Log.v("CURRENT", "entra en el else: "+state);
                    if(state.equals(States.completed))
                        return mp.getDuration();
                    return mp.getCurrentPosition();
                }
            }
            return mp.getDuration();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            Log.v("FINAL", integer+"");
            progreso(integer);
        }
    }
}
