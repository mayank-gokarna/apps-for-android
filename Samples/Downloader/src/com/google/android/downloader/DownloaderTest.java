package com.google.android.downloader;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class DownloaderTest extends Activity {

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! DownloaderActivity.ensureDownloaded(this,
                getString(R.string.app_name), FILE_CONFIG_URL,
                CONFIG_VERSION, DATA_PATH, USER_AGENT)) {
            return;
        }
        setContentView(R.layout.main);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        boolean handled = true;
        int id = item.getItemId();

        if (id == R.id.menu_main_download_again) {
            downloadAgain();
        } else {
            handled = false;
        }

        if (!handled) {
            handled = super.onOptionsItemSelected(item);
        }
        return handled;
    }

    private void downloadAgain() {
        DownloaderActivity.deleteData(DATA_PATH);
        startActivity(getIntent());
        finish();
    }

    private final static String FILE_CONFIG_URL =
        "http://jack.palevich.googlepages.com/download.config";
    private final static String CONFIG_VERSION = "1.0";
    private final static String DATA_PATH = "/sdcard/data/downloadTest";
    private final static String USER_AGENT = "MyApp Downloader";
}
