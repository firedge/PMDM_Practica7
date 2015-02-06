package com.fdgproject.firedge.pmdm_practica7;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class ListViewAdapter extends CursorAdapter {

    public ListViewAdapter(Context context, Cursor data) {
        super(context, data, true);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        LayoutInflater i = LayoutInflater.from(viewGroup.getContext());
        View v = i.inflate(R.layout.row_list, viewGroup, false);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = null;

        if (view != null) {
            holder = new ViewHolder();
            holder.tv_song = (TextView) view.findViewById(R.id.tv_song);
            holder.tv_artist = (TextView) view.findViewById(R.id.tv_artist);
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        int song_name_index = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        String songName = cursor.getString(song_name_index);
        holder.tv_song.setText(songName);
        int artist_name_index = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        String artistName = cursor.getString(artist_name_index);
        holder.tv_artist.setText(artistName);
    }

    static class ViewHolder {
        TextView tv_song;
        TextView tv_artist;
    }

}
