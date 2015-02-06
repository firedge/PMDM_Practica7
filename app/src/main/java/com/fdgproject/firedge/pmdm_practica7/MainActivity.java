package com.fdgproject.firedge.pmdm_practica7;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


public class MainActivity extends Activity {

    private ListView lv;
    private SeekBar sb;
    private TextView tv;
    private Button btPlay;
    private RelativeLayout menu;
    private Cursor cursor;
    private final String PROGRESO = "progreso";
    private final int CTEGRABAR = 1;
    private boolean reproducirTodo = false, pause = false;
    private int duracion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] projection = { MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM_ID };
        Cursor thumbnails = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        sb = (SeekBar)findViewById(R.id.seekBar);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(Servicio.encendido){
                    Intent intent = new Intent(MainActivity.this, Servicio.class);
                    intent.putExtra("pos", seekBar.getProgress());
                    intent.setAction(Servicio.POSITION);
                    startService(intent);
                }
            }
        });
        tv = (TextView)findViewById(R.id.tv_menu);
        btPlay = (Button)findViewById(R.id.bt_play);
        menu = (RelativeLayout)findViewById(R.id.rl_menu);
        lv = (ListView)findViewById(R.id.listView);
        lv.setAdapter(new ListViewAdapter(this, thumbnails));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                reproducirTodo = false;
                cursor = (Cursor)lv.getItemAtPosition(i);
                reproducir();
                menu.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Servicio.encendido = false;
        unregisterReceiver(receptor);
        stopService(new Intent(this, Servicio.class));
    }

    private void reproducir(){
        btPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
        int song_path_index = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        Uri uri = Uri.parse(cursor.getString(song_path_index));
        int song_duration_index = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        duracion = cursor.getInt(song_duration_index);
        sb.setProgress(0);
        sb.setMax(duracion);
        int song_title_index = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        tv.setText(cursor.getString(song_title_index));
        //DETENER POR SI HAY REPRODUCCION EN CURSO
        Intent intent = new Intent(MainActivity.this, Servicio.class);
        intent.setAction(Servicio.STOP);
        startService(intent);
        //CARGAR LA CANCION
        intent = new Intent(MainActivity.this, Servicio.class);
        intent.putExtra("song", uri.getPath());
        intent.setAction(Servicio.ADD);
        startService(intent);
        //REPRODUCIR LA CANCION
        intent = new Intent(MainActivity.this, Servicio.class);
        intent.setAction(Servicio.PLAY);
        startService(intent);
    }

    public void pause(View v){
        if(!pause){
            pause = true;
            btPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
        } else {
            pause = false;
            btPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_pause));
        }
        Intent intent = new Intent(this, Servicio.class);
        intent.setAction(Servicio.PAUSE);
        startService(intent);
    }

    public void next(View v){
        if(cursor.moveToNext())
            reproducir();
        else if(cursor.moveToFirst())
            reproducir();
    }

    public void previous(View v){
        if(cursor.moveToPrevious())
            reproducir();
        else if(cursor.moveToLast())
            reproducir();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reptodo) {
            reproducirTodo = true;
            menu.setVisibility(View.VISIBLE);
            cursor = (Cursor)lv.getItemAtPosition(0);
            reproducir();
            return true;
        } else if (id == R.id.action_grabar){
            Intent intent = new Intent(
                    MediaStore.Audio.Media.
                            RECORD_SOUND_ACTION);
            startActivityForResult(intent, CTEGRABAR);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int
            resultCode, Intent data) {
        if (resultCode == RESULT_OK &&
                requestCode == CTEGRABAR) {
            Uri uri = data.getData();
            Log.v("GRABAR", uri.getPath());
        }
    }

    //BroadcastReceiver
    private BroadcastReceiver receptor= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            int pos = bundle.getInt("actual");
            if(Servicio.encendido) {
                sb.setProgress(pos);
            }
            if(pos == duracion) {
                pause = true;
                btPlay.setBackground(getResources().getDrawable(android.R.drawable.ic_media_play));
                if (reproducirTodo) {
                    if (cursor.moveToNext())
                        reproducir();
                    else if (cursor.moveToFirst())
                        reproducir();
                }
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        Servicio.encendido = true;
        registerReceiver(receptor, new IntentFilter(PROGRESO));
    }
}
