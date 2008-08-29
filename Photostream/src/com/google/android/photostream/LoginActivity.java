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

package com.google.android.photostream;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Selection;
import android.text.Spannable;

/**
 * Activity used to login the user. The activity asks for the user name and then starts
 * the PhotostreamActivity upong successful login. If the login is unsuccessful, an error
 * message is displayed.
 *
 * This activity is also used to create Home shortcuts. When the intent
 * {@link Intent#ACTION_CREATE_SHORTCUT} is used to start this activity, sucessful login
 * returns a shortcut Intent to Home instead of proceeding to PhotostreamActivity.
 *
 * The shortcut Intent contains the real name of the user, his buddy icon, the action
 * {@link android.content.Intent#ACTION_VIEW} and the URI flickr://photos/nsid.
 */
public class LoginActivity extends Activity implements View.OnClickListener {
    private static final String PREFERENCE_USERNAME = "flickr.username";
    private static final String DEFAULT_USERNAME = "romainguy";

    private boolean mCreateShortcut;

    private TextView mUsername;
    private ViewAnimator mSwitcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If the activity was started with the "create shortcut" action, we
        // remember this to change the behavior upon successful login
        if (Intent.ACTION_CREATE_SHORTCUT.equals(getIntent().getAction())) {
            mCreateShortcut = true;
        }

        setContentView(R.layout.screen_login);
        setupViews();
    }

    private void setupViews() {
        mUsername = (TextView) findViewById(R.id.input_username);
        mUsername.setOnClickListener(this);

        mSwitcher = (ViewAnimator) findViewById(R.id.switcher_login);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.input_username:
                onLogin();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Saves the entered user name so that the user doesn't have to
        // enter it every time
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREFERENCE_USERNAME, mUsername.getText().toString());
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        final String username = preferences.getString(PREFERENCE_USERNAME, DEFAULT_USERNAME);
        mUsername.setText(username);
        Selection.setSelection((Spannable) mUsername.getText(), username.length());

        if (mSwitcher.getDisplayedChild() != 0) {
            mSwitcher.setDisplayedChild(0);
        }
    }

    private void onLogin() {
        // When the user enters his user name, we need to find his NSID to proceed to
        // PhotostreamActivity. However, if we are creating a shortcut, we need to
        // retrieve the user info to get his real name and his buddy icon.
        if (!mCreateShortcut) {
            new FindUserTask().execute(mUsername.getText().toString());
        } else {
            new FindUserInfoTask().execute(mUsername.getText().toString());
        }
    }

    private void onError() {
        mSwitcher.showPrevious();
        mUsername.setError(getString(R.string.screen_login_error));
    }

    private void onShowPhotostream(Flickr.User user) {
        PhotostreamActivity.show(this, user);
    }

    /**
     * Creates the shortcut Intent to send back to Home. The intent is a view action
     * to a flickr://photos/nsid URI, with a title (real name or user name) and a
     * custom icon (the user's buddy icon.)
     *
     * @param shortcut The buddy icon and user information to use to create the
     *                 shortcut.
     */
    private void onCreateShortcut(UserShortcut shortcut) {
        final Uri uri = Uri.parse(String.format("%s://%s/%s", Flickr.URI_SCHEME,
                Flickr.URI_PHOTOS_AUTHORITY, shortcut.mUserInfo.getId()));

        final Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, uri);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Sets the custom shortcut's title to the real name of the user. If no
        // real name was found, use the user name instead.
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        String name = shortcut.mUserInfo.getRealName();
        if (name == null || name.length() == 0) {
            name = shortcut.mUserInfo.getUserName();
        }
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);

        // Sets the custom shortcut icon to the user's buddy icon. If no buddy
        // icon was found, use a default local buddy icon instead.
        final Bitmap icon = shortcut.mBuddyIcon;
        if (icon != null) {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        } else {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.default_buddyicon));
        }

        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Background task used to load the user's NSID. The task begins by showing the
     * progress bar, then loads the user NSID from the network and finally open
     * PhotostreamActivity.
     */
    private class FindUserTask extends UserTask<String, Void, Flickr.User> {
        @Override
        public void begin() {
            mSwitcher.showNext();
        }

        public Flickr.User doInBackground(String... params) {
            return Flickr.get().findByUserName(params[0]);
        }

        @Override
        public void end(Flickr.User user) {
            if (user != null) {
                onShowPhotostream(user);
            } else {
                onError();
            }
        }
    }

    /**
     * Background task used to load the user's information. The task begins by showing
     * the progress bar, then loads the user NSID and information from the network and
     * creates a shortcut intent and send it back to Home.
     */
    private class FindUserInfoTask extends UserTask<String, Void, UserShortcut> {
        @Override
        public void begin() {
            mSwitcher.showNext();
        }

        public UserShortcut doInBackground(String... params) {
            Flickr.User user = Flickr.get().findByUserName(params[0]);
            if (user == null) return null;

            Flickr.UserInfo info = Flickr.get().getUserInfo(user);
            if (info == null) return null;

            UserShortcut shortcut = new UserShortcut();
            shortcut.mUserInfo = info;
            shortcut.mBuddyIcon = info.loadBuddyIcon();
            return shortcut;
        }

        @Override
        public void end(UserShortcut shortcut) {
            if (shortcut != null) {
                onCreateShortcut(shortcut);
            } else {
                onError();
            }
        }
    }

    private static class UserShortcut {
        Bitmap mBuddyIcon;
        Flickr.UserInfo mUserInfo;
    }
}
