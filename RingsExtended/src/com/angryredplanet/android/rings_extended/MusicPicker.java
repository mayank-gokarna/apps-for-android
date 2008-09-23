package com.angryredplanet.android.rings_extended;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaFile;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.text.Collator;
import java.util.Formatter;
import java.util.Locale;

public class MusicPicker extends ListActivity
        implements View.OnClickListener, MediaPlayer.OnCompletionListener {
    static final boolean DBG = false;
    static final String TAG = "MusicPicker";
    
    static final String[] CURSOR_COLS = new String[] {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.TITLE_KEY,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.DURATION
    };
    
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    private Uri mBaseUri;
    
    private Cursor mCursor;
    private String mSortOrder;

    private View mOkayButton;
    private View mCancelButton;
    
    private long mSelectedId = -1;
    private Uri mSelectedUri;
    
    private long mPlayingId = -1;
    
    /**
     * This is used for playing previews of the music files.
     */
    private MediaPlayer mMediaPlayer;
    
    class TrackListAdapter extends SimpleCursorAdapter
            implements FastScrollView.SectionIndexer {
        final ListView mListView;
        
        final int mIdIdx;
        final int mTitleIdx;
        final int mArtistIdx;
        final int mAlbumIdx;
        final int mDurationIdx;
        final int mAudioIdIdx;

        private final StringBuilder mBuilder = new StringBuilder();
        private final String mUnknownArtist;
        private final String mUnknownAlbum;

        private String [] mAlphabet;
        private AlphabetIndexer mIndexer;
        
        class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            RadioButton radio;
            ImageView play_indicator;
            CharArrayBuffer buffer1;
            char [] buffer2;
        }
        
        TrackListAdapter(Context context, ListView listView, int layout, Cursor cursor,
                String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            mListView = listView;
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            
            mIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            mTitleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            mArtistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            mAlbumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            mDurationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int audioIdIdx = cursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID);
            if (audioIdIdx < 0) {
                audioIdIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            }
            mAudioIdIdx = audioIdIdx;
            
            getAlphabet(context);
            mIndexer = new AlphabetIndexer(cursor, mTitleIdx, mAlphabet);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.duration = (TextView) v.findViewById(R.id.duration);
            vh.radio = (RadioButton) v.findViewById(R.id.radio);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.buffer1 = new CharArrayBuffer(100);
            vh.buffer2 = new char[200];
            v.setTag(vh);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder vh = (ViewHolder) view.getTag();
            
            cursor.copyStringToBuffer(mTitleIdx, vh.buffer1);
            vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);
            
            int secs = cursor.getInt(mDurationIdx) / 1000;
            if (secs == 0) {
                vh.duration.setText("");
            } else {
                vh.duration.setText(makeTimeString(context, secs));
            }
            
            final StringBuilder builder = mBuilder;
            builder.delete(0, builder.length());

            String name = cursor.getString(mAlbumIdx);
            if (name == null || name.equals(MediaFile.UNKNOWN_STRING)) {
                builder.append(mUnknownAlbum);
            } else {
                builder.append(name);
            }
            builder.append('\n');
            name = cursor.getString(mArtistIdx);
            if (name == null || name.equals(MediaFile.UNKNOWN_STRING)) {
                builder.append(mUnknownArtist);
            } else {
                builder.append(name);
            }
            int len = builder.length();
            if (vh.buffer2.length < len) {
                vh.buffer2 = new char[len];
            }
            builder.getChars(0, len, vh.buffer2, 0);
            vh.line2.setText(vh.buffer2, 0, len);

            final long id = cursor.getLong(mIdIdx);
            if (DBG) Log.v(TAG, "Binding id=" + id + " sel=" + mSelectedId
                    + " playing=" + mPlayingId + " cursor=" + cursor);
            vh.radio.setChecked(id == mSelectedId);
            ImageView iv = vh.play_indicator;
            if (id == mPlayingId) {
                iv.setImageResource(R.drawable.now_playing);
                iv.setVisibility(View.VISIBLE);
            } else {
                iv.setVisibility(View.GONE);
            }
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            if (DBG) Log.v(TAG, "Setting cursor to: " + cursor
                    + " from: " + MusicPicker.this.mCursor);
            MusicPicker.this.mCursor = cursor;
            updateIndexer(cursor);
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (DBG) Log.v(TAG, "Getting new cursor...");
            return getTrackCursor(constraint.toString());
        }
        
        private void getAlphabet(Context context) {
            String alphabetString = context.getResources().getString(R.string.alphabet);
            mAlphabet = new String[alphabetString.length()];
            for (int i = 0; i < mAlphabet.length; i++) {
                mAlphabet[i] = String.valueOf(alphabetString.charAt(i));
            }
        }
        
        private void updateIndexer(Cursor cursor) {
            if (mIndexer == null) {
                mIndexer = new AlphabetIndexer(cursor, mTitleIdx, mAlphabet);
            } else {
                mIndexer.setCursor(cursor);
            }
        }
        
        public int getPositionForSection(int section) {
            if (mIndexer == null) {
                Cursor cursor = MusicPicker.this.mCursor;
                if (cursor == null) {
                    // No cursor, the section doesn't exist so just return 0
                    return 0;
                }
                mIndexer = new AlphabetIndexer(cursor, mTitleIdx, mAlphabet);
            }

            return mIndexer.indexOf(section);
        }

        public int getSectionForPosition(int position) {
            return 0;
        }

        public Object[] getSections() {
            return mAlphabet;
        }
    }

    public static String makeTimeString(Context context, long secs) {
        String durationformat = context.getString(R.string.durationformat);
        
        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }
    
    public static Cursor doQuery(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
         } catch (UnsupportedOperationException ex) {
            return null;
        }
        
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle == null) {
            mSelectedUri = getIntent().getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        } else {
            mSelectedUri = (Uri)icicle.getParcelable(
                    RingtoneManager.EXTRA_RINGTONE_EXISTING_URI);
        }
        if (Intent.ACTION_GET_CONTENT.equals(getIntent().getAction())) {
            mBaseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            mBaseUri = getIntent().getData();
            if (mBaseUri == null) {
                Log.w("MusicPicker", "No data URI given to PICK action");
                finish();
                return;
            }
        }
        init();
    }

    @Override public void onRestart() {
        super.onRestart();
        if (mCursor != null) {
            mCursor.requery();
        }
    }
    
    @Override public void onPause() {
        super.onPause();
        stopMediaPlayer();
    }
    
    @Override public void onStop() {
        super.onStop();
        if (mCursor != null) {
            mCursor.deactivate();
        }
    }
    
    @Override public void onDestroy() {
        super.onDestroy();
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public void init() {

        setContentView(R.layout.music_picker);

        mCursor = getTrackCursor(null);
        if (DBG) Log.v(TAG, "Initial cursor: " + mCursor);
        if (null == mCursor || 0 == mCursor.getCount()) {
            return;
        }

        final ListView listView = getListView();

        listView.setItemsCanFocus(false);
        
        TrackListAdapter adapter = new TrackListAdapter(this, listView,
                R.layout.track_list_item, mCursor, new String[] {},
                new int[] {});

        setListAdapter(adapter);
        
        listView.setTextFilterEnabled(true);
        
        mOkayButton = findViewById(R.id.okayButton);
        mOkayButton.setOnClickListener(this);
        mCancelButton = findViewById(R.id.cancelButton);
        mCancelButton.setOnClickListener(this);
        
        if (mSelectedUri != null) {
            Uri.Builder builder = mSelectedUri.buildUpon();
            String path = mSelectedUri.getEncodedPath();
            int idx = path.lastIndexOf('/');
            if (idx >= 0) {
                path = path.substring(0, idx);
            }
            builder.encodedPath(path);
            Uri baseSelectedUri = builder.build();
            if (DBG) Log.v(TAG, "Selected Uri: " + mSelectedUri);
            if (DBG) Log.v(TAG, "Selected base Uri: " + baseSelectedUri);
            if (DBG) Log.v(TAG, "Base Uri: " + mBaseUri);
            if (baseSelectedUri.equals(mBaseUri)) {
                mSelectedId = ContentUris.parseId(mSelectedUri);
            }
        }
    }

    private Cursor getTrackCursor(String filterstring) {
        Cursor ret = null;
        mSortOrder = MediaStore.Audio.Media.TITLE_KEY;
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Media.TITLE + " != ''");
        
        // Add in the filtering constraints
        String [] keywords = null;
        if (filterstring != null) {
            String [] searchWords = filterstring.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(Collator.PRIMARY);
            for (int i = 0; i < searchWords.length; i++) {
                keywords[i] = '%' + MediaStore.Audio.keyFor(searchWords[i]) + '%';
            }
            for (int i = 0; i < searchWords.length; i++) {
                where.append(" AND ");
                where.append(MediaStore.Audio.Media.ARTIST_KEY + "||");
                where.append(MediaStore.Audio.Media.ALBUM_KEY + "||");
                where.append(MediaStore.Audio.Media.TITLE_KEY + " LIKE ?");
            }
        }
        
        where.append(" AND " + MediaStore.Audio.Media.IS_MUSIC + "=1");
        ret = doQuery(this, mBaseUri,
            CURSOR_COLS, where.toString() , keywords, mSortOrder);
        return ret;
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        mCursor.moveToPosition(position);
        if (DBG) Log.v(TAG, "Click on " + position + " (id=" + id
                + ", cursid="
                + mCursor.getLong(mCursor.getColumnIndex(MediaStore.Audio.Media._ID))
                + ") in cursor " + mCursor
                + " adapter=" + l.getAdapter());
        setSelected(mCursor);
    }
    
    private void setSelected(Cursor c) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        long newId = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Audio.Media._ID));
        mSelectedUri = ContentUris.withAppendedId(uri, newId);
        
        mSelectedId = newId;
        if (newId != mPlayingId || mMediaPlayer == null) {
            stopMediaPlayer();
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(this, mSelectedUri);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
                mPlayingId = newId;
                getListView().invalidateViews();
            } catch (IOException e) {
                Log.w("MusicPicker", "Unable to play track", e);
            }
        } else if (mMediaPlayer != null) {
            stopMediaPlayer();
            getListView().invalidateViews();
        }
    }
    
    public void onCompletion(MediaPlayer mp) {
        if (mMediaPlayer == mp) {
            mp.stop();
            mp.release();
            mMediaPlayer = null;
            mPlayingId = -1;
            getListView().invalidateViews();
        }
    }
    
    private void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mPlayingId = -1;
        }
    }
    
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okayButton:
                if (mSelectedId >= 0) {
                    setResult(RESULT_OK, new Intent().setData(mSelectedUri));
                    finish();
                }
                break;

            case R.id.cancelButton:
                finish();
                break;
        }
    }
}
