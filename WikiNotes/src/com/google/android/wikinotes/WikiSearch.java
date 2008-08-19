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

import com.google.android.wikinotes.db.WikiNote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * An activity to show a search dialog consisting of a textedit field to
 * enter search terms into, and a pair of buttons, one to search the bodies
 * and the other to search the titles of notes.
 */
public class WikiSearch extends Activity {
    
    private static final String KEY_SEARCH_TEXT = "wikiSearchText";
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.wiki_search);
        setTitle(getResources().getString(R.string.search_title));

        // do we have anything in the icicle from a previous lifecycle event?
        if (icicle != null) {
            String searchString = icicle.getString(KEY_SEARCH_TEXT);
            if ((searchString != null) && (searchString.length() > 0)) {
                ((EditText) findViewById(R.id.body_search)).setText(searchString);
            }
        }
        
        ((Button) findViewById(R.id.body_search_button)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // search the body of notes for the text specified
                String searchText = ((EditText) findViewById(R.id.body_search)).
                        getText().toString();
                if (!TextUtils.isEmpty(searchText)) {
                    Uri bodySearchURI = Uri.withAppendedPath(WikiNote.Notes.SEARCH_URI, searchText);
                    Intent i = new Intent(Intent.ACTION_VIEW, bodySearchURI);
                    startActivity(i);
                    // finish is called to skip this activity in the backstack
                    finish();
                }
            }
        });
        
        ((Button) findViewById(R.id.title_search_button)).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // search the title of notes for the text specified
                String searchText = ((EditText) findViewById(R.id.body_search)).getText().toString();
                if (!TextUtils.isEmpty(searchText)) {
                    Uri titleSearchURI = Uri.withAppendedPath(
                            WikiNote.Notes.TITLE_SEARCH_URI, searchText);
                    Intent i = new Intent(Intent.ACTION_VIEW, titleSearchURI);
                    startActivity(i);
                    // finish is called to skip this activity in the backstack
                    finish();
                }
            }
        });        
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SEARCH_TEXT, 
                ((EditText) findViewById(R.id.body_search)).getText().toString());
    }
}
