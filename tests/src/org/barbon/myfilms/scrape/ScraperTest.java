package org.barbon.myfilms.scrape;

import android.content.Context;

import android.database.Cursor;

import android.test.InstrumentationTestCase;

import java.io.InputStream;
import java.io.InputStreamReader;

import org.barbon.myfilms.Movies;

import org.barbon.myfilms.tests.R;

class ScraperTest extends InstrumentationTestCase {
    private static final String listUrl = "http://trovacinema.repubblica.it/programmazione-cinema/citta/firenze/fi/film";
    private boolean complete;

    private class TestFetchTask extends ListFetchTask {
        public TestFetchTask(scala.Function1 callback, Movies movies) {
            super(callback, movies);
        }

        @Override
        public scala.Tuple3<String, String, String> downloadUrl(String url) {
            try {
                return doDownloadUrl(url);
            }
            catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        public scala.Tuple3<String, String, String> doDownloadUrl(String url)
                throws Throwable {
            Context context = getInstrumentation().getContext();
            InputStream stream = context.getResources()
                .openRawResource(R.raw.trovacinema_results);
            InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
            StringBuffer builder = new StringBuffer();
            int res;

            while ((res = reader.read()) != -1) {
                builder.append((char) res);
            }

            return new scala.Tuple3(url, "UTF-8", builder.toString());
        }
    }

    private class OperationComplete
            extends scala.runtime.AbstractFunction1<Boolean, Void> {
        @Override
        public Void apply(Boolean status) {
            complete = true;

            return null;
        }
    }

    public void testUpdateMovieList() throws Throwable {
        Context target = getInstrumentation().getTargetContext();
        Movies db = new Movies(target, "test_movies");
        final ListFetchTask task = new TestFetchTask(
            new OperationComplete(), db);

        class UiTask implements Runnable {
            @Override
            public void run() {
                task.execute(listUrl);
            }
        }

        UiTask uiTask = new UiTask();

        complete = false;
        runTestOnUiThread(uiTask);

        while (complete)
            Thread.sleep(500);

        Cursor movies = db.getMovies();

        assertEquals(25, movies.getCount());
    }
}
