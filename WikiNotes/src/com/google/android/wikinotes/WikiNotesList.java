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

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.google.android.wikinotes.db.WikiNote;

/**
 * Activity to list wikinotes. By default, the notes are listed in the
 * order of most recently modified to least recently modified. This
 * activity can handle requests to either show all notes, or the results
 * of a title or body search performed by the content provider.
 */
public class WikiNotesList extends ListActivity {

    /**
     * A key to store/retrieve the search criteria in a bundle
     */
    public static final String SEARCH_CRITERIA_KEY = "SearchCriteria";
    /**
     * The projection to use (columns to retrieve) for a query of wikinotes
     */
    public static final String[] PROJECTION = 
            { WikiNote.Notes._ID, WikiNote.Notes.TITLE, WikiNote.Notes.MODIFIED_DATE };
    
    private String mSearchCriteria;
    private Cursor mCursor;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        // Do we have a search URI passed in through the intent?
        Uri uri = getIntent().getData();
        
        if (uri == null) {
            // try and find the search criteria some other way
            // do we have a search criteria in either the icicle or the extras bundle?
            String searchCriteria = icicle == null ? null : icicle.getString(SEARCH_CRITERIA_KEY);
            
            if (searchCriteria == null) {
                Bundle extras = getIntent().getExtras();
                searchCriteria = extras == null ? null : extras.getString(SEARCH_CRITERIA_KEY);
            }
            mSearchCriteria = searchCriteria;
                        
            if ((searchCriteria != null) && (searchCriteria.length() > 0)) {
                uri = WikiNote.Notes.SEARCH_URI.buildUpon().appendPath(searchCriteria).build();
                setTitle(getResources().getString(R.string.search_results, searchCriteria));
            } else {
                uri = WikiNote.Notes.CONTENT_URI;
                setTitle(getResources().getString(R.string.recent_changes));
            }
        } else {
            // get the search criteria from the url, and set the title
            if (uri.getPathSegments().size() == 3) {
                String searchCriteria = uri.getLastPathSegment();
                mSearchCriteria = searchCriteria;
            } else {
                // no search criteria, so just go with the List title
                setTitle(getResources().getString(R.string.recent_changes));
            }
        }
        
        // Do the query
        Cursor c = managedQuery(uri, PROJECTION, null, null, WikiNote.Notes.DEFAULT_SORT_ORDER);
        mCursor = c;
        
        // Bind the results of the search into the list
        ListAdapter adapter = new SimpleCursorAdapter(this,
                android.R.layout.simple_list_item_1, mCursor,
                new String[] {WikiNote.Notes.TITLE}, new int[] {android.R.id.text1});
        setListAdapter(adapter);
        
        // use the menu shortcut keys as default key bindings for the entire activity
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
    }

    /**
     * Override the onListItemClick to open the wiki note to view when it is
     * selected from the list.
     */
    @Override
    protected void onListItemClick(ListView list, View view, int position, long id) {
        Cursor c = mCursor;
        c.moveToPosition(position);
        String title = c.getString(c.getColumnIndexOrThrow(WikiNote.Notes.TITLE));
        
        // Create the URI of the note we want to view based on the title
        Uri uri = Uri.withAppendedPath(WikiNote.Notes.CONTENT_URI, title);
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(i);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SEARCH_CRITERIA_KEY, mSearchCriteria);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        
        menu.add(0, WikiNotes.HOME_ID, 0, R.string.menu_home).setShortcut('#', 'h');
        menu.add(0, WikiNotes.SEARCH_ID, 0, R.string.menu_search).setShortcut('*', 's');
        menu.add(0, WikiNotes.LIST_ID, 0, R.string.menu_list);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case WikiNotes.HOME_ID:
            goHome();
            return true;
        case WikiNotes.SEARCH_ID:
            searchNotes();
            return true;
        case WikiNotes.LIST_ID:
            listNotes();
            return true;
        default:
            return false;
        }
    }

    private void listNotes() {
        Intent i = new Intent(Intent.ACTION_VIEW, WikiNote.Notes.CONTENT_URI);
        startActivity(i);
    }
    
    private void goHome() {
        Uri startPageURL = Uri.withAppendedPath(WikiNote.Notes.CONTENT_URI, 
                getResources().getString(WikiNotes.DEFAULT_NOTE));
        Intent startPage = new Intent(Intent.ACTION_VIEW, startPageURL);
        startActivity(startPage);
    }

    private void searchNotes() {
        Intent i = new Intent(this, WikiSearch.class);
        startActivity(i);
    }
}
