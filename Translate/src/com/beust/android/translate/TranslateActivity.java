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

package com.beust.android.translate;

import com.google.android.collect.Lists;

import com.beust.android.translate.Languages.Language;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/**
 * Main activity for the Translate application.
 *
 * @author Cedric Beust
 * @author Daniel Rall
 */
public class TranslateActivity extends Activity implements OnClickListener {
    static final String TAG = "Translate";
    private EditText mOutput;
    private EditText mInput;
    private Button mFrom;
    private Button mTo;
    private Button mTranslate;
    private Handler mHandler = new Handler();
    private ProgressBar mProgressBar;
    private TextView mStatusView;
    
    // true if changing a language should automatically trigger a translation
    private boolean mDoTranslate = true;

    // The history of all the previous translations
    private History mHistory;
    
    // Dialog id's
    private static final int LANGUAGE_DIALOG_ID = 1;
    private static final int ABOUT_DIALOG_ID = 2;

    // Saved preferences
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String INPUT = "input";
    private static final String OUTPUT = "output";
    
    // Default language pair if no saved preferences are found
    private static final String DEFAULT_FROM = Language.ENGLISH.getShortName();
    private static final String DEFAULT_TO = Language.GERMAN.getShortName();

    private Button mLatestButton;

    private OnClickListener mClickListener = new OnClickListener() {
        public void onClick(View v) {
            mLatestButton = (Button) v;
            showDialog(LANGUAGE_DIALOG_ID);
        }
    };

    // Translation service handle.
    private ITranslate mTranslateService;

    // ServiceConnection implementation for translation.
    private ServiceConnection mTranslateConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTranslateService = ITranslate.Stub.asInterface(service);
            /* TODO(dlr): Register a callback to assure we don't lose our svc.
            try {
                mTranslateervice.registerCallback(mTranslateCallback);
            } catch (RemoteException e) {
                log("Failed to establish Translate service connection: " + e);
                return;
            }
            */
            if (mTranslateService != null) {
                mTranslate.setEnabled(true);
            } else {
                mTranslate.setEnabled(false);
                mStatusView.setText(getString(R.string.error));
                log("Unable to acquire TranslateService");
            }
        }

        public void onServiceDisconnected(ComponentName name) {
            mTranslate.setEnabled(false);
            mTranslateService = null;
        }
    };

    // Dictionary
    private static byte[] mWordBuffer;
    private static int mWordCount;
    private static ArrayList<Integer> mWordIndices;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.translate_activity);
        mInput = (EditText) findViewById(R.id.input);
        mOutput = (EditText) findViewById(R.id.translation);
        mFrom = (Button) findViewById(R.id.from);
        mTo = (Button) findViewById(R.id.to);
        mTranslate = (Button) findViewById(R.id.translate);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mStatusView = (TextView) findViewById(R.id.status);
        
        //
        // Install the language adapters on both the From and To spinners.
        //
        mFrom.setOnClickListener(mClickListener);
        mTo.setOnClickListener(mClickListener);

        mHistory = new History(getPrefs(this));

        mTranslate.setOnClickListener(this);
        mInput.selectAll();

        connectToTranslateService();
    }
    
    private void connectToTranslateService() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        bindService(intent, mTranslateConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = getPrefs(this);
        mDoTranslate = false;

        //
        // See if we have any saved preference for From
        //
        Language from = Language.findLanguageByShortName(prefs.getString(FROM, DEFAULT_FROM));
        updateButton(mFrom, from);

        //
        // See if we have any saved preference for To
        //
        //
        Language to = Language.findLanguageByShortName(prefs.getString(TO, DEFAULT_TO));
        updateButton(mTo, to);
        
        //
        // Restore input and output, if any
        //
        mInput.setText(prefs.getString(INPUT, ""));
        setOutputText(prefs.getString(OUTPUT, ""));
        mDoTranslate = true;
    }
    
    private void setOutputText(String string) {
        log("Setting output to " + string);
        mOutput.setText(new Entities().unescape(string));
    }

    private void updateButton(Button button, Language language) {
        language.configureButton(this, button);
        maybeTranslate();
    }

    /**
     * Launch the translation if the input text field is not empty.
     */
    private void maybeTranslate() {
        if (mDoTranslate && !TextUtils.isEmpty(mInput.getText().toString())) {
            doTranslate();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();

        //
        // Save the content of our views to the shared preferences
        //
        Editor edit = getPrefs(this).edit();
        mHistory.saveHistory(edit);
        String f = ((Language) mFrom.getTag()).getShortName();
        String t = ((Language) mTo.getTag()).getShortName();
        String input = mInput.getText().toString();
        String output = mOutput.getText().toString();
        savePreferences(edit, f, t, input, output);
        edit.commit();
    }
    
    static void savePreferences(Editor edit, String from, String to, String input, String output) {
        log("Saving preferences " + from + " " + to + " " + input + " " + output);
        edit.putString(FROM, from);
        edit.putString(TO, to);
        edit.putString(INPUT, input);
        edit.putString(OUTPUT, output);
    }
    
    static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }
    
    public void onClick(View v) {
        maybeTranslate();
    }
    
    private void doTranslate() {
        mStatusView.setText(R.string.retrieving_translation);
        mHandler.post(new Runnable() {
            public void run() {
                mProgressBar.setVisibility(View.VISIBLE);
                String result = "";
                try {
                    Language from = (Language) mFrom.getTag();
                    Language to = (Language) mTo.getTag();
                    String fromShortName = from.getShortName();
                    String toShortName = to.getShortName();
                    String input = mInput.getText().toString();
                    log("Translating from " + fromShortName + " to " + toShortName);
                    result = mTranslateService.translate(input, fromShortName, toShortName);
                    if (result == null) {
                        throw new Exception(getString(R.string.translation_failed));
                    }
                    mHistory.addHistoryRecord(from, to, input, result);
                    mStatusView.setText(R.string.found_translation);
                    setOutputText(result);
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mInput.selectAll();
                } catch (Exception e) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    mStatusView.setText("Error:" + e.getMessage());
                }
            }
        });
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d) {
        if (id == LANGUAGE_DIALOG_ID) {
            boolean from = mLatestButton == mFrom;
            ((LanguageDialog) d).setFrom(from);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == LANGUAGE_DIALOG_ID) {
            return new LanguageDialog(this);
        } else if (id == ABOUT_DIALOG_ID) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.about_title);
            builder.setMessage(getString(R.string.about_message));
            builder.setIcon(R.drawable.babelfish);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setNeutralButton(R.string.send_email,
                    new android.content.DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:cedric@beust.com"));
                            startActivity(intent);
                        }
                    });
            builder.setCancelable(true);
            return builder.create();
        }
        return null;
    }
    
    /**
     * Pick a random word and set it as the input word.
     */
    public void selectRandomWord() {
        BufferedReader fr = null;
        try {
            GZIPInputStream is =
                    new GZIPInputStream(getResources().openRawResource(R.raw.dictionary));
            if (mWordBuffer == null) {
                mWordBuffer = new byte[601000];
                int n = is.read(mWordBuffer, 0, mWordBuffer.length);
                int current = n;
                while (n != -1) {
                    n = is.read(mWordBuffer, current, mWordBuffer.length - current);
                    current += n;
                }
                is.close();
                mWordCount = 0;
                mWordIndices = Lists.newArrayList();
                for (int i = 0; i < mWordBuffer.length; i++) {
                    if (mWordBuffer[i] == '\n') {
                        mWordCount++;
                        mWordIndices.add(i);
                    }
                }
                log("Found " + mWordCount + " words");
            }

            int randomWordIndex = (int) (System.currentTimeMillis() % (mWordCount - 1));
            log("Random word index:" + randomWordIndex + " wordCount:" + mWordCount);
            int start = mWordIndices.get(randomWordIndex);
            int end = mWordIndices.get(randomWordIndex + 1);
            byte[] b = new byte[end - start - 2];
            System.arraycopy(mWordBuffer, start + 1, b, 0, (end - start - 2)); 
            String randomWord = new String(b);
            mInput.setText(randomWord);
            updateButton(mFrom, Language.findLanguageByShortName(Language.ENGLISH.getShortName()));
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        
    }

    public void setNewLanguage(Language language, boolean from) {
        updateButton(from ? mFrom : mTo, language);
        maybeTranslate();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.translate_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case R.id.about:
            showDialog(ABOUT_DIALOG_ID);
            break;

        case R.id.show_history:
            showHistory();
            break;
        
        case R.id.random_word:
            selectRandomWord();
            break;
        }

        return true;
    }
    
    private void showHistory() {
        startActivity(new Intent(this, HistoryActivity.class));
    }
    
    private static void log(String s) {
        Log.d(TAG, "[TranslateActivity] " + s);
    }

}
