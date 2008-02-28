/* 
 * Copyright (C) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.wikinotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.Menu.Item;
import android.widget.TextView;

import java.util.regex.Pattern;

import com.google.android.wikinotes.db.WikiNote;

/**
 * The WikiNotes activity is the default handler for displaying individual
 * wiki notes. It takes the wiki note name to view from the intent data URL
 * and defaults to a page name defined in R.strings.start_page if no page name 
 * is given. The lifecycle events store the current content URL being viewed.
 * It uses Linkify to turn wiki words in the body of the wiki note into
 * clickable links which in turn call back into the WikiNotes activity
 * from the Android operating system.
 * 
 * If a URL is passed to the WikiNotes activity for a page that does not
 * yet exist, the WikiNoteEditor activity is automatically started to
 * place the user into edit mode for a new wiki note with the given
 * name.
 */
public class WikiNotes extends Activity {
    /**
     * The default page name to use if none is specified
     */
    public static final int DEFAULT_NOTE = R.string.start_page;
    
    /**
     * The view URL which is prepended to a matching wikiword in order to
     * fire the WikiNotes activity again through Linkify
     */
    
    private static final String KEY_URL = "wikiNotesURL";
    
    public static final int EDIT_ID = Menu.FIRST;
    public static final int HOME_ID = Menu.FIRST + 1;
    public static final int SEARCH_ID = Menu.FIRST + 2;
    public static final int LIST_ID = Menu.FIRST + 3;
    public static final int DELETE_ID = Menu.FIRST + 4;
    public static final int ACTIVITY_EDIT = 1;
    public static final int ACTIVITY_DELETE = 2;
    public static final int ACTIVITY_LIST = 3;
    public static final int ACTIVITY_SEARCH = 4;
    
    private TextView mNoteView;
    private Cursor mCursor;
    private Uri mURI;
    private String mNoteName;
    private static final Pattern WIKI_WORD_MATCHER;
    
    static {
        // Compile the regular expression pattern that will be used to
        // match WikiWords in the body of the note
        WIKI_WORD_MATCHER = Pattern.compile("\\b[A-Z]+[a-z0-9]+[A-Z][A-Za-z0-9]+\\b");
    }
    
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        
        mNoteView = (TextView) findViewById(R.id.noteview);
        
        // get the URL we are being asked to view
        Uri uri = getIntent().getData();
        
        if ((uri == null) && (icicle != null)) {
            // perhaps we have the URI in the icicle instead?
            uri = Uri.parse(icicle.getString(KEY_URL));
        }
        
        // do we have a correct URI including the note name?
        if ((uri == null) || (uri.getPathSegments().size() < 2)) {
            // if not, build one using the default StartPage name
            uri = Uri.withAppendedPath(WikiNote.Notes.CONTENT_URI,  
                    getResources().getString(DEFAULT_NOTE));
        }
        
        // can we find a matching note?
        Cursor cursor = managedQuery(uri, WikiNote.WIKI_NOTES_PROJECTION, null, null);
        
        boolean newNote = false;
        if ((cursor == null) || (cursor.count() == 0)) {
            // no matching wikinote, so create it
            uri = getContentResolver().insert(uri, null);
            if (uri == null) {
                Log.e("WikiNotes", "Failed to insert new wikinote into "
                        + getIntent().getData());
                finish();
                return;
            }
            // make sure that the new note was created successfully, and select it
            cursor = managedQuery(uri, WikiNote.WIKI_NOTES_PROJECTION, null, null);
            if ((cursor == null) || (cursor.count() == 0)) {
                Log.e("WikiNotes", "Failed to open new wikinote: "
                        + getIntent().getData());
                finish();
                return;
            }
            newNote = true;
        }
        
        mURI = uri;
        mCursor = cursor;
        cursor.first();
        
        // get the note name
        String noteName = cursor.getString(cursor.getColumnIndex(WikiNote.Notes.TITLE));
        mNoteName = noteName;
        
        // set the title to the name of the page
        setTitle(getResources().getString(R.string.wiki_title, noteName));
        
        // If a new note was created, jump straight into editing it
        if (newNote) {
            editNote();
        }
        
        // Set the menu shortcut keys to be default keys for the activity as well
        setDefaultKeyMode(SHORTCUT_DEFAULT_KEYS);
    }

    @Override
    protected void onFreeze(Bundle outState) {
        super.onFreeze(outState);
        // Put the URL currently being viewed into the icicle
        outState.putString(KEY_URL, mURI.toString());
    }


    @Override
    protected void onResume() {
        super.onResume();
        Cursor c = mCursor;
        c.requery();
        c.first();
        showWikiNote(c.getString(c.getColumnIndex(WikiNote.Notes.BODY)));
    }
    
    /**
     * Show the wiki note in the text edit view with both the default
     * Linkify options and the regular expression for WikiWords matched
     * and turned into live links.
     * @param body The plain text to linkify and put into the edit view.
     */
    private void showWikiNote(CharSequence body) {
        TextView noteView = mNoteView;
        noteView.setText(body);
        
        // Add default links first - phone numbers, URLs, etc.
        Linkify.addLinks(noteView, Linkify.ALL);
        
        // Now add in the custom linkify match for WikiWords
        Linkify.addLinks(noteView, WIKI_WORD_MATCHER, WikiNote.Notes.CONTENT_URI.toString() + "/");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, EDIT_ID, R.string.menu_edit).setShortcut('3', 'e');
        menu.add(0, HOME_ID, R.string.menu_home).setShortcut('#', 'h');
        menu.add(0, SEARCH_ID, R.string.menu_search).setShortcut('*', 's');
        menu.add(0, LIST_ID, R.string.menu_list);
        menu.add(0, DELETE_ID, R.string.menu_delete);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, Item item) {
        switch (item.getId()) {
        case EDIT_ID:
            editNote();
            return true;
        case HOME_ID:
            goHome();
            return true;
        case DELETE_ID:
            deleteNote();
            return true;
        case SEARCH_ID:
            searchNotes();
            return true;
        case LIST_ID:
            listNotes();
            return true;
        default:
            return false;
        }
    }

    /**
     * If a list of notes is requested, fire the WikiNotes list Content URI and
     * let the WikiNotesList activity handle it.
     */
    private void listNotes() {
        Intent i = new Intent(Intent.DEFAULT_ACTION, WikiNote.Notes.CONTENT_URI);
        startActivity(i);
    }
    
    /**
     * If requested, go back to the start page wikinote by requesting an intent
     * with the default start page URI
     */
    private void goHome() {
        Uri startPageURL = Uri.withAppendedPath(WikiNote.Notes.CONTENT_URI, 
                getResources().getString(DEFAULT_NOTE));
        Intent startPage = new Intent(Intent.DEFAULT_ACTION, startPageURL);
        startActivity(startPage);
    }
    
    /**
     * Create an intent to start the WikiNoteEditor using the current
     * title and body information (if any).
     */
    private void editNote() {
    	// This intent could use the android.intent.action.EDIT for a wiki note
    	// to invoke, but instead I wanted to demonstrate the mechanism for invoking
    	// an intent on a known class within the same application directly. Note
    	// also that the title and body of the note to edit are passed in using
    	// the extras bundle.
        Intent i = new Intent(this, WikiNoteEditor.class);
        i.putExtra(WikiNote.Notes.TITLE, mNoteName);
        Cursor c = mCursor;
        i.putExtra(WikiNote.Notes.BODY, c.getString(c.getColumnIndex(WikiNote.Notes.BODY)));
        startSubActivity(i, ACTIVITY_EDIT);        
    }
    
    /**
     * If requested, delete the current note. The user is prompted to confirm
     * this operation with a dialog, and if they choose to go ahead with the
     * deletion, the current activity is finish()ed once the data has been
     * removed, so that android naturally backtracks to the previous activity.
     */
    private void deleteNote() {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getString(R.string.delete_title))
                .setMessage(R.string.delete_message)
                .setPositiveButton(R.string.yes_button, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int arg1) {
                        Cursor c = mCursor;
                        c.deleteRow();
                        setResult(RESULT_OK);
                        finish();
                    }
                })
                .setNegativeButton(R.string.no_button, null)
                .show();
    }
    
    /**
     * Fire a targeted intent request to the WikiSearch class to show a search
     * screen.
     */
    private void searchNotes() {
        Intent i = new Intent(this, WikiSearch.class);
        startActivity(i);
    }
    
    /**
     * If the note was edited and not canceled, commit the update to 
     * the database and then refresh the current view of the linkified note.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, String data, Bundle extras) {
        super.onActivityResult(requestCode, resultCode, data, extras);
        if ((requestCode == ACTIVITY_EDIT) && (resultCode == RESULT_OK)) {
            // edit was confirmed - store the update
            Cursor c = mCursor;
            c.requery();
            c.first();
            c.updateString(c.getColumnIndex(WikiNote.Notes.BODY), data);
            c.updateLong(c.getColumnIndex(WikiNote.Notes.MODIFIED_DATE), 
                    System.currentTimeMillis());
            managedCommitUpdates(c);
            showWikiNote(c.getString(c.getColumnIndex(WikiNote.Notes.BODY)));
        }
    }

}