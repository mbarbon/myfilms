package org.barbon.myfilms.scrape;

import _root_.android.net.http.AndroidHttpClient;

import _root_.java.io.InputStream;
import _root_.java.io.ByteArrayOutputStream;

import _root_.java.lang.{Boolean => JBoolean};

import _root_.org.apache.http.util.EntityUtils;
import _root_.org.apache.http.client.methods.HttpGet;

import _root_.org.jsoup.Jsoup;

import org.barbon.myfilms.Movies;

import scala.collection.mutable.MutableList;

import scala.collection.JavaConversions._;

abstract class ScraperTask(val callback : ScraperTask#CompletionCallback)
        extends ScraperTaskHelper {
    type CompletionCallback = Boolean => Unit;

    protected def downloadUrl(url : String) : (String, String, String) = {
        val client = AndroidHttpClient.newInstance("MyFilms/1.0");
        var encoding : String = null;
        var totalSize : Long = 0;
        var content : InputStream = null;

        try {
            val response = client.execute(new HttpGet(url));
            val entity = response.getEntity;

            content = entity.getContent;
            totalSize = entity.getContentLength;
            encoding = EntityUtils.getContentCharSet(entity);

            // TODO handle redirects
            // http://stackoverflow.com/questions/1456987/httpclient-4-how-to-capture-last-redirect-url/1457173#1457173
        } catch {
            case e => {
                e.printStackTrace;
                client.close;

                return (null, null, null);
            }
        }

        var success = true;
        val buffer = new Array[Byte](1024);
        val out = new ByteArrayOutputStream;

        try {
            var size : Int = -1;

            do {
                size = content.read(buffer);

                if (size != -1)
                    out.write(buffer, 0, size);
            } while (size != -1)

            if (totalSize > 0 && out.size != totalSize)
                return (null, null, null);
        }
        catch {
            case e => {
                e.printStackTrace;

                return (null, null, null);
            }
        }
        finally {
            client.close;
        }

        return (url, encoding, out.toString(encoding));
    }

    protected override def onPostExecute(res : JBoolean) {
        if (callback != null)
            callback(res == true);
    }
}

class ListFetchTask(callback : ScraperTask#CompletionCallback,
                    private val movies : Movies)
        extends ScraperTask(callback) {
    protected override def doInBackground(url : String) : JBoolean = {
        try {
            return updateMovieList(url);
        } catch {
            case e => {
                e.printStackTrace;

                return false;
            }
        }
    }

    private def updateMovieList(url : String) : Boolean = {
        val (baseUrl, encoding, content) = downloadUrl(url);

        if (content == null)
            return false;

        val doc = Jsoup.parse(content, baseUrl);

        movies.deleteProjections;

        for (movie <- doc.select("div.searchRes-group")) {
            var link = movie.select("a.filmName").first;

            if (link != null) {
                var title = link.text;
                var url = link.attr("abs:href");
                var movieId = movies.getOrCreateMovie(title, url);
                var projections = new MutableList[(String, String)];

                for (projection <- movie.select("div.resultLineFilm")) {
                    var theater = projection.select("p.cineName").first.text;
                    var hours = projection.select("span.res-hours").first.text;
                    projections += ((theater, hours));
                }

                movies.setProjections(movieId, projections);
            }
        }

        return true;
    }
}

object Trovacinema {
    val URL : String = "http://trovacinema.repubblica.it/" +
        "programmazione-cinema/citta/firenze/fi/film";
}

class Trovacinema(private val movies : Movies) {
    def fetchList(callback : ScraperTask#CompletionCallback) {
        var task = new ListFetchTask(callback, movies);

        task.execute(Trovacinema.URL);
    }
}