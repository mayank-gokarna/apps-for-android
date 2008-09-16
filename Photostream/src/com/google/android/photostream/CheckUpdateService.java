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

import android.app.Service;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.content.Intent;
import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.net.Uri;

import java.util.Date;

/**
 * CheckUpdateService checks every 24 hours if updates have been made to the photostreams
 * of the current contacts. This service simply polls an RSS feed and compares the
 * modification timestamp with the one stored in the database.
 */
public class CheckUpdateService extends Service {
    // Check interval: every 24 hours
    private static long UPDATES_CHECK_INTERVAL = 24 * 60 * 60 * 1000;

    private CheckForUpdatesTask mTask;

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        (mTask = new CheckForUpdatesTask()).execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTask != null && mTask.getStatus() == UserTask.Status.RUNNING) {
            mTask.cancel(true);
        }
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    static void schedule(Context context) {
        final Intent intent = new Intent(context, CheckUpdateService.class);
        final PendingIntent pending = PendingIntent.getService(context, 0, intent, 0);

        final AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pending);
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                UPDATES_CHECK_INTERVAL, pending);
    }

    private class CheckForUpdatesTask extends UserTask<Void, Object, Void> {
        public Void doInBackground(Void... params) {
            final UserDatabase helper = new UserDatabase(CheckUpdateService.this);
            final SQLiteDatabase database = helper.getWritableDatabase();

            Cursor cursor = null;
            try {
                cursor = database.query(UserDatabase.TABLE_USERS,
                        new String[] { UserDatabase._ID, UserDatabase.COLUMN_NSID,
                        UserDatabase.COLUMN_REALNAME, UserDatabase.COLUMN_LAST_UPDATE },
                        null, null, null, null, null);

                int idIndex = cursor.getColumnIndexOrThrow(UserDatabase._ID);
                int realNameIndex = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_REALNAME);
                int nsidIndex = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_NSID);
                int lastUpdateIndex = cursor.getColumnIndexOrThrow(UserDatabase.COLUMN_LAST_UPDATE);

                final Flickr flickr = Flickr.get();

                while (!isCancelled() && cursor.moveToNext()) {
                    Date lastUpdate = new Date(cursor.getLong(lastUpdateIndex));
                    final String nsid = cursor.getString(nsidIndex);

                    if (flickr.hasUpdates(Flickr.User.fromId(nsid), lastUpdate)) {
                        publishProgress(nsid, cursor.getString(realNameIndex),
                                cursor.getInt(idIndex));
                    }
                }

                final ContentValues values = new ContentValues();
                values.put(UserDatabase.COLUMN_LAST_UPDATE, System.currentTimeMillis());

                database.update(UserDatabase.TABLE_USERS, values, null, null);
            } finally {
                if (cursor != null) cursor.close();
                database.close();
            }

            return null;
        }

        @Override
        public void onProgressUpdate(Object... values) {
            final Uri uri = Uri.parse(String.format("%s://%s/%s", Flickr.URI_SCHEME,
                    Flickr.URI_PHOTOS_AUTHORITY, values[0]));
            final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            Notification notification = new Notification(R.drawable.stat_notify,
                    getString(R.string.notification_new_photos, values[1]),
                    System.currentTimeMillis());
            notification.defaults = Notification.DEFAULT_ALL;
            notification.setLatestEventInfo(CheckUpdateService.this,
                    getString(R.string.notification_title),
                    getString(R.string.notification_contact_has_new_photos, values[1]),
                    PendingIntent.getActivity(CheckUpdateService.this, 0, intent, 0));

            NotificationManager manager = (NotificationManager)
                    getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify((Integer) values[2], notification);
        }

        @Override
        public void onPostExecute(Void aVoid) {
            stopSelf();
        }
    }
}
