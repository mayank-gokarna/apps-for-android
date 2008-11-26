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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.google.android.wikinotes.db.WikiNote;

public class WikiActivityHelper {
    public static final int ACTIVITY_EDIT = 1;
    public static final int ACTIVITY_DELETE = 2;
    public static final int ACTIVITY_LIST = 3;
    public static final int ACTIVITY_SEARCH = 4;

    private Activity mContext;

    public WikiActivityHelper(Activity context) {
	mContext = context;
    }

    /**
     * If a list of notes is requested, fire the WikiNotes list Content URI and
     * let the WikiNotesList activity handle it.
     */
    public void listNotes() {
	Intent i = new Intent(Intent.ACTION_VIEW, WikiNote.Notes.ALL_NOTES_URI);
	mContext.startActivity(i);
    }

    /**
     * If requested, go back to the start page wikinote by requesting an intent
     * with the default start page URI
     */
    public void goHome() {
	Uri startPageURL =
	        Uri.withAppendedPath(WikiNote.Notes.ALL_NOTES_URI, mContext
	                .getResources().getString(R.string.start_page));
	Intent startPage = new Intent(Intent.ACTION_VIEW, startPageURL);
	mContext.startActivity(startPage);
    }

    /**
     * Create an intent to start the WikiNoteEditor using the current title and
     * body information (if any).
     */
    public void editNote(String mNoteName, Cursor cursor) {
	// This intent could use the android.intent.action.EDIT for a wiki note
	// to invoke, but instead I wanted to demonstrate the mechanism for
	// invoking
	// an intent on a known class within the same application directly. Note
	// also that the title and body of the note to edit are passed in using
	// the extras bundle.
	Intent i = new Intent(mContext, WikiNoteEditor.class);
	i.putExtra(WikiNote.Notes.TITLE, mNoteName);
	String body;
	if (cursor != null) {
	    body =
		    cursor.getString(cursor
		            .getColumnIndexOrThrow(WikiNote.Notes.BODY));
	} else {
	    body = "";
	}
	i.putExtra(WikiNote.Notes.BODY, body);
	mContext.startActivityForResult(i, ACTIVITY_EDIT);
    }

    /**
     * If requested, delete the current note. The user is prompted to confirm
     * this operation with a dialog, and if they choose to go ahead with the
     * deletion, the current activity is finish()ed once the data has been
     * removed, so that android naturally backtracks to the previous activity.
     */
    public void deleteNote(final Cursor cursor) {
	new AlertDialog.Builder(mContext)
	        .setTitle(
	                  mContext.getResources()
	                          .getString(R.string.delete_title))
	        .setMessage(R.string.delete_message)
	        .setPositiveButton(R.string.yes_button, new OnClickListener() {
		    public void onClick(DialogInterface dialog, int arg1) {
		        Uri noteUri =
		                ContentUris
		                        .withAppendedId(
		                                        WikiNote.Notes.ALL_NOTES_URI,
		                                        cursor.getInt(0));
		        mContext.getContentResolver().delete(noteUri, null,
		                                             null);
		        mContext.setResult(Activity.RESULT_OK);
		        mContext.finish();
		    }
	        }).setNegativeButton(R.string.no_button, null).show();
    }
    
    private void showOutcomeDialog(int titleId, String msg) {
    	new AlertDialog.Builder(mContext)
        .setCancelable(false)
    	.setTitle(
                  mContext.getResources()
                          .getString(titleId))
        .setMessage(msg).setPositiveButton(R.string.export_dismiss_button, null).create().show();
    }
    
    public void exportNotes() {
    	if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
    		showOutcomeDialog(R.string.export_failure_title, mContext.getResources().getString(R.string.export_failure_missing_sd));
    		return;
    	}
    	
    	Cursor c =
	        mContext.managedQuery(WikiNote.Notes.ALL_NOTES_URI, WikiNote.WIKI_EXPORT_PROJECTION, null, null,
	                WikiNote.Notes.DEFAULT_SORT_ORDER);
    	boolean dataAvailable = c.moveToFirst();
    	StringBuffer sb = new StringBuffer();
    	String title, body, created, modified;
    	int cnt = 0;
    	sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<wiki-notes>");
    	while (dataAvailable) {
    		title = c.getString(c.getColumnIndexOrThrow("title"));
    		body = c.getString(c.getColumnIndexOrThrow("body"));
    		modified = c.getString(c.getColumnIndexOrThrow("modified"));
    		created = c.getString(c.getColumnIndexOrThrow("created"));
    		if (!"".equals(title) && !"".equals(body) && title != null && body != null) {
    			sb.append("\n").append("<note>\n\t<title><![CDATA[").append(title).append("]]></title>\n\t<body><![CDATA[");
    			sb.append(body).append("]]></body>\n\t<created>").append(created).append("</created>\n\t");
    			sb.append("<modified>").append(modified).append("</modified>\n</note>");
    			cnt++;
    		}
    		dataAvailable = c.moveToNext();
    	}
    	sb.append("\n</wiki-notes>\n");

    	FileWriter fw = null;
    	File f = null;
    	try {
	    	f = new File(Environment.getExternalStorageDirectory() + "/" + EXPORT_DIR);
	    	f.mkdirs();
	    	String fileName = String.format(EXPORT_FILENAME, Calendar.getInstance());
	    	f =  new File(Environment.getExternalStorageDirectory() + "/" + EXPORT_DIR + "/" + fileName);
	    	if (!f.createNewFile()) {
	    		showOutcomeDialog(R.string.export_failure_title, mContext.getResources().getString(R.string.export_failure_io_error));
	    		return;
	    	}
	    	
	    	fw = new FileWriter(f);
	    	fw.write(sb.toString());
    	} catch(IOException e) {
    		showOutcomeDialog(R.string.export_failure_title, mContext.getResources().getString(R.string.export_failure_io_error));
    		return;
    	} finally {
    		if (fw != null) {
    			try {
    				fw.close();
    			} catch (IOException ex) { }
    		}
    	}

    	if (f == null) {
    		showOutcomeDialog(R.string.export_failure_title, mContext.getResources().getString(R.string.export_failure_io_error));
    		return;
    	}
    	showOutcomeDialog(R.string.export_success_title, String.format(mContext.getResources().getString(R.string.export_success), cnt, f.getName()));
    }
    
    public void doImport() {
    	mContext.getContentResolver().delete(WikiNote.Notes.ALL_NOTES_URI, null, null);
    	int cnt = 0;
    	FileReader fr = null;
    	ArrayList<StringBuffer[]> records = new ArrayList<StringBuffer[]>();
    	try {
			fr = new FileReader(new File(Environment.getExternalStorageDirectory() + "/" + IMPORT_FILENAME));
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(false);
			XmlPullParser xpp = factory.newPullParser();
			xpp.setInput(fr);
			int eventType = xpp.getEventType();
			StringBuffer[] cur = new StringBuffer[4];
			int curIdx = -1;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				StringBuffer[] x = cur;
				if (eventType == XmlPullParser.START_TAG && "note".equals(xpp.getName())) {
					if (cur[0] != null && cur[1] != null && !"".equals(cur[0]) && !"".equals(cur[1])) {
						records.add(cur);
					}
					cur = new StringBuffer[4];
					curIdx = -1;
				} else if (eventType == XmlPullParser.START_TAG && "title".equals(xpp.getName())) {
					curIdx = 0;
				} else if (eventType == XmlPullParser.START_TAG && "body".equals(xpp.getName())) {
					curIdx = 1;
				} else if (eventType == XmlPullParser.START_TAG && "created".equals(xpp.getName())) {
					curIdx = 2;
				} else if (eventType == XmlPullParser.START_TAG && "modified".equals(xpp.getName())) {
					curIdx = 3;
				} else if (eventType == XmlPullParser.TEXT && curIdx > -1) {
					if (cur[curIdx] == null) {
						cur[curIdx] = new StringBuffer();
					}
					cur[curIdx].append(xpp.getText());
				}
				eventType = xpp.next();
			}
			if (cur[0] != null && cur[1] != null && !"".equals(cur[0]) && !"".equals(cur[1])) {
				records.add(cur);
			}			
			
			for (StringBuffer[] record : records) {
				Uri uri =
				    Uri.withAppendedPath(WikiNote.Notes.ALL_NOTES_URI, record[0].toString());
				ContentValues values = new ContentValues();
				values.put("title", record[0].toString().trim());
				values.put("body", record[1].toString().trim());
				values.put("created", Long.parseLong(record[2].toString().trim()));
				values.put("modified", Long.parseLong(record[3].toString().trim()));
				if (mContext.getContentResolver().insert(uri, values) != null) {
					cnt++;
				}
			}
		} catch (FileNotFoundException e) {
			return;
		} catch (XmlPullParserException e) {
			return;
		} catch (IOException e) {
			return;
		} finally {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) { }
			}
		}
    	showOutcomeDialog(R.string.import_success_title, String.format(mContext.getResources().getString(R.string.import_success), cnt));
    }
    
    public void importNotes() {
    	new AlertDialog.Builder(mContext)
        .setCancelable(true)
    	.setTitle(
                  mContext.getResources()
                          .getString(R.string.import_confirm_title))
        .setMessage(R.string.import_confirm_body).setPositiveButton(R.string.import_confirm_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface arg0, int arg1) {
				doImport();
			}        	
        }).setNegativeButton(R.string.import_cancel_button, null).create().show();
    }
    
    private static final String EXPORT_DIR = "WikiNotes";
    private static final String EXPORT_FILENAME = "WikiNotes_%1$tY-%1$tm-%1$td_%1$tH-%1$tM-%1$tS.xml";
    private static final String IMPORT_FILENAME = "WikiNotes-import.xml";
}
