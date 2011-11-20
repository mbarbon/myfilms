package org.barbon.myfilms.scrape;

import android.os.AsyncTask;

// for some reason, doInBackground is not overridden correctly; this
// avoids the problem by replacing it with a non-varargs version
public abstract class ScraperTaskHelper
        extends AsyncTask<String, Void, Boolean> {
    protected Boolean doInBackground(String... urls) {
        return doInBackground(urls[0]);
    }

    abstract protected Boolean doInBackground(String url);
}
