package com.google.android.wikinotes;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;

import com.google.android.wikinotes.db.WikiNote;

public class WikiActivityHelper {
	public static final int ACTIVITY_EDIT = 1;
	public static final int ACTIVITY_DELETE = 2;
	public static final int ACTIVITY_LIST = 3;
	public static final int ACTIVITY_SEARCH = 4;

	private Cursor mCursor;
	private Activity mContext;
	
	public WikiActivityHelper(Activity context, Cursor cursor) {
		mCursor = cursor;
		mContext = context;
	}

	/**
	 * If a list of notes is requested, fire the WikiNotes list Content URI and
	 * let the WikiNotesList activity handle it.
	 */
	protected void listNotes() {
		Intent i = new Intent(Intent.ACTION_VIEW, WikiNote.Notes.ALL_NOTES_URI);
		mContext.startActivity(i);
	}

	/**
	 * If requested, go back to the start page wikinote by requesting an intent
	 * with the default start page URI
	 */
	protected void goHome() {
		Uri startPageURL = Uri.withAppendedPath(WikiNote.Notes.ALL_NOTES_URI,
				mContext.getResources().getString(R.string.start_page));
		Intent startPage = new Intent(Intent.ACTION_VIEW, startPageURL);
		mContext.startActivity(startPage);
	}

	/**
	 * Create an intent to start the WikiNoteEditor using the current title and
	 * body information (if any).
	 */
	protected void editNote(String mNoteName) {
		// This intent could use the android.intent.action.EDIT for a wiki note
		// to invoke, but instead I wanted to demonstrate the mechanism for
		// invoking
		// an intent on a known class within the same application directly. Note
		// also that the title and body of the note to edit are passed in using
		// the extras bundle.
		Intent i = new Intent(mContext, WikiNoteEditor.class);
		i.putExtra(WikiNote.Notes.TITLE, mNoteName);
		Cursor c = mCursor;
		i.putExtra(WikiNote.Notes.BODY, c.getString(c
				.getColumnIndexOrThrow(WikiNote.Notes.BODY)));
		mContext.startActivityForResult(i, ACTIVITY_EDIT);
	}

	/**
	 * If requested, delete the current note. The user is prompted to confirm
	 * this operation with a dialog, and if they choose to go ahead with the
	 * deletion, the current activity is finish()ed once the data has been
	 * removed, so that android naturally backtracks to the previous activity.
	 */
	protected void deleteNote() {
		new AlertDialog.Builder(mContext).setTitle(
				mContext.getResources().getString(R.string.delete_title)).setMessage(
				R.string.delete_message).setPositiveButton(R.string.yes_button,
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int arg1) {
						Cursor c = mCursor;
						Uri noteUri = ContentUris.withAppendedId(
								WikiNote.Notes.ALL_NOTES_URI, c.getInt(0));
						mContext.getContentResolver().delete(noteUri, null, null);
						mContext.setResult(mContext.RESULT_OK);
						mContext.finish();
					}
				}).setNegativeButton(R.string.no_button, null).show();
	}

}
