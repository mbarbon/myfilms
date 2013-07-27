package org.barbon.myfilms.scrape;

import _root_.android.net.http.AndroidHttpClient;

import _root_.java.io.{InputStream, ByteArrayInputStream,
                       ByteArrayOutputStream}
import _root_.java.lang.{Boolean => JBoolean, Long => JLong};
import _root_.java.net.URLEncoder;
import _root_.java.util.regex.Pattern;

import _root_.org.apache.http.util.EntityUtils;
import _root_.org.apache.http.client.methods.HttpGet;

import _root_.org.jsoup.Jsoup;
import _root_.org.jsoup.nodes.{Document, Element};

import org.barbon.myfilms.{Movies, MovieProjection};

import scala.collection.mutable.{MutableList, ArrayBuffer};
import scala.collection.JavaConversions._;

abstract class ScraperTask extends ScraperTaskHelper {
    type CompletionCallback = Boolean => Unit;

    protected def downloadUrl(url : String, defaultEncoding : String = null)
            : (String, String, Document) = {
        val client = AndroidHttpClient.newInstance("MyFilms/1.0");
        var encoding : String = null;
        var totalSize : Long = 0;
        var content : InputStream = null;

        try {
            val response = client.execute(new HttpGet(url));
            val entity = response.getEntity;

            content = entity.getContent;
            totalSize = entity.getContentLength;
            encoding = EntityUtils.getContentCharSet(entity) match {
                case null => defaultEncoding
                case e    => e
            };

            // TODO handle redirects
            // http://stackoverflow.com/questions/1456987/httpclient-4-how-to-capture-last-redirect-url/1457173#1457173
        } catch {
            case e : Throwable => {
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
            case e : Throwable => {
                e.printStackTrace;

                return (null, null, null);
            }
        }
        finally {
            client.close;
        }

        val doc = Jsoup.parse(new ByteArrayInputStream(out.toByteArray),
                              encoding, url);

        return (url, encoding, doc);
    }
}

class ListFetchTask(private val callback : ScraperTask#CompletionCallback,
                    private val movies : Movies)
        extends ScraperTask {
    protected override def doInBackground(url : String) : JBoolean = {
        try {
            return updateMovieList(url);
        } catch {
            case e : Throwable => {
                e.printStackTrace;

                return false;
            }
        }
    }

    private def updateMovieList(url : String) : Boolean = {
        val (baseUrl, encoding, doc) = downloadUrl(url);

        if (doc == null)
            return false;

        movies.deleteProjections;

        for (movie <- doc.select("div.searchRes-group")) {
            val link = movie.select("a.filmName").first;

            if (link != null) {
                val title = link.text;
                val url = link.attr("abs:href");
                val movieId = movies.getOrCreateMovie(title, url);
                val projections = new MutableList[MovieProjection];

                for (projection <- movie.select("div.resultLineFilm")) {
                    val theater = projection.select("p.cineName").first;
                    val hours = projection.select("span.res-hours").first;

                    if (theater != null && hours != null)
                        projections += MovieProjection(theater.text, hours.text);
                }

                movies.setProjections(movieId, projections);
            }
        }

        return true;
    }

    protected override def onPostExecute(res : JBoolean) {
        if (callback != null)
            callback(res == true);
    }
}

class SearchTask(private val callback : SearchTask#SearchCallback)
        extends ScraperTask {
    type SearchCallback = (Boolean, Seq[FilmUp.SearchItem]) => Unit;

    val cards = new ArrayBuffer[FilmUp.SearchItem](0);

    protected override def doInBackground(url : String) : JBoolean = {
        try {
            return getMovieList(url);
        } catch {
            case e : Throwable => {
                e.printStackTrace;

                return false;
            }
        }
    }

    private def getMovieList(url : String) : Boolean = {
        val (baseUrl, encoding, doc) = downloadUrl(url);

        if (doc == null)
            return false;

        val title = Pattern.compile("\\sscheda:\\s+(.*?)\\s*$",
                                    Pattern.CASE_INSENSITIVE);
        val year = Pattern.compile("anno:\\s+(\\d+)\\s+genere:",
                                   Pattern.CASE_INSENSITIVE);

        for (item <- doc.select("dl")) {
            val link = item.select("dt a.filmup").first;
            val details = item.select("dd table small").first;
            val titleMatcher = title.matcher(link.text);
            val yearMatcher = year.matcher(details.text);

            if (titleMatcher.find && yearMatcher.find)
                cards += FilmUp.SearchItem(titleMatcher.group(1),
                                           link.attr("abs:href"),
                                           yearMatcher.group(1));
        }

        return true;
    }

    protected override def onPostExecute(res : JBoolean) {
        if (callback != null)
            callback(res == true, cards);
    }
}

class CardTask(private val callback : ScraperTask#CompletionCallback,
               private val movies : Movies,
               private val movieId : JLong)
        extends ScraperTask {
    protected override def doInBackground(url : String) : JBoolean = {
        try {
            return getCardData(url);
        } catch {
            case e : Throwable => {
                e.printStackTrace;

                return false;
            }
        }
    }

    private def getCardData(url : String) : Boolean = {
        val (baseUrl, encoding, doc) = downloadUrl(url, "ISO-8859-1");

        if (doc == null)
            return false;

        val reviewUrl = doc.select("a.filmup:matchesOwn(^Recensione$)") match {
            case null => null;
            case link => link.attr("abs:href");
        }

        movies.setFilmUpReviewUrl(movieId, reviewUrl);

        return reviewUrl != null;
    }

    protected override def onPostExecute(res : JBoolean) {
        if (callback != null)
            callback(res == true);
    }
}

class ReviewTask(private val callback : ScraperTask#CompletionCallback,
                 private val movies : Movies,
                 private val movieId : JLong)
        extends ScraperTask {
    protected override def doInBackground(url : String) : JBoolean = {
        try {
            return getReviewData(url);
        } catch {
            case e : Throwable => {
                e.printStackTrace;

                return false;
            }
        }
    }

    private def getReviewData(url : String) : Boolean = {
        val (baseUrl, encoding, doc) = downloadUrl(url);

        if (doc == null)
            return false;

        val review = doc.select("td font[size=2]").first match {
            case null => null;
            case font => cleanupReview(font);
        }

        movies.setFilmUpReview(movieId, review);

        return review != null;
    }

    private def cleanupReview(html : Element) : String = {
        for (node <- html.select("a.filmup, a.filmup ~ *"))
            node.remove;

        val matcher = Pattern.compile("[\u007f-\uffff]").matcher(html.html);
        val res = new StringBuffer;

        while (matcher.find) {
            val char = matcher.group(0)(0) toInt;
            val hex = char toHexString;

            matcher.appendReplacement(res, "&#x" + hex + ";");
        }

        matcher.appendTail(res);

        return res.toString;
    }

    protected override def onPostExecute(res : JBoolean) {
        if (callback != null)
            callback(res == true);
    }
}

object Trovacinema {
    val URL : String = "http://trovacinema.repubblica.it/" +
        "programmazione-cinema/citta/firenze/fi/film";
}

class Trovacinema(private val movies : Movies) {
    def fetchList(callback : ScraperTask#CompletionCallback) {
        val task = new ListFetchTask(callback, movies);

        task.execute(Trovacinema.URL);
    }
}

object FilmUp {
    val URL : String = "http://filmup.leonardo.it/cgi-bin/search.cgi"
    val SearchParams : String = "?ps=10&fmt=long&ul=%25%2Fsc_%25&x=29&y=6&m=all&wf=0020&wm=wrd&sy=0";

    case class SearchItem(title : String, url : String, year : String);
}

class FilmUp(private val movies : Movies) {
    // public interface

    def search(title : String, callback : SearchTask#SearchCallback) {
        val task = new SearchTask(callback);

        task.execute(searchUrl(title));
    }

    def loadCard(movieId : JLong, callback : ScraperTask#CompletionCallback) {
        val task = new CardTask(callback, movies, movieId);
        val url = movies.getFilmUpCard(movieId)
                      .getAsString(Movies.FILMUP_CARD_URL);

        task.execute(url);
    }

    def loadReview(movieId : JLong, callback : ScraperTask#CompletionCallback) {
        val task = new ReviewTask(callback, movies, movieId);
        val url = movies.getFilmUpCard(movieId)
                      .getAsString(Movies.FILMUP_REVIEW_URL);

        task.execute(url);
    }

    // implementation

    private def searchUrl(title : String) : String = {
        val url = FilmUp.URL + FilmUp.SearchParams + "&q=" + URLEncoder.encode(title, "UTF-8");

        return url;
    }
}
