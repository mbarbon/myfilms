package org.barbon.myfilms;

import _root_.android.content.{Context, ContentValues};
import _root_.android.database.Cursor;
import _root_.android.database.sqlite.{SQLiteDatabase, SQLiteOpenHelper};

import _root_.java.lang.{Long => JLong};

import scala.collection.LinearSeq;

object Movies {
    val VERSION : Int = 2;

    val CREATE_MOVIES_TABLE : String = """
        CREATE TABLE movie (
            id INTEGER PRIMARY KEY,
            title TEXT NOT NULL,
            url TEXT NOT NULL,
            has_projections INTEGER NOT NULL,
            hidden INTEGER NOT NULL,
            UNIQUE (url)
        )
    """;

    val CREATE_TROVACINEMA_PROJECTIONS_TABLE : String = """
        CREATE TABLE tc_projection (
            id INTEGER PRIMARY KEY,
            movie_id INTEGER NOT NULL,
            theater TEXT NOT NULL,
            times TEXT NOT NULL,
            FOREIGN KEY (movie_id) REFERENCES movie(id)
                ON DELETE CASCADE
        )
    """;

    val CREATE_FILMUP_CARD_TABLE : String = """
        CREATE TABLE fu_card (
            id INTEGER PRIMARY KEY,
            movie_id INTEGER NOT NULL,
            url TEXT NOT NULL,
            review_url TEXT,
            FOREIGN KEY (movie_id) REFERENCES movie(id)
                ON DELETE CASCADE
        )
    """;

    val CREATE_FILMUP_REVIEW_TABLE : String = """
        CREATE TABLE fu_review (
            id INTEGER PRIMARY KEY,
            movie_id INTEGER NOT NULL,
            text TEXT NOT NULL,
            FOREIGN KEY (movie_id) REFERENCES movie(id)
                ON DELETE CASCADE
        )
    """;

    val MOVIE_TITLE = "title";
    val PROJECTION_THEATER = "theater";
    val PROJECTION_HOURS = "times";
    val FILMUP_CARD_URL = "url";
    val FILMUP_REVIEW_URL = "review_url";

    // singleton handling

    private var theInstance : Movies = null;

    def getInstance(context : Context) : Movies = {
        if (theInstance != null)
            return theInstance;

        theInstance = new Movies(context, "movies");

        return theInstance;
    }
}

case class MovieProjection(theater : String, hours : String);

class Movies (context : Context, name : String) {
    private val openHelper = new MoviesOpenHelper(context, name);

    private def getDatabase() : SQLiteDatabase =
        openHelper.getWritableDatabase;

    def getMovie(movieId : JLong) : ContentValues = {
        val db = getDatabase;
        val cursor = db.rawQuery("SELECT url, title, hidden" +
                                 "    FROM movie WHERE id = ?",
                                 Array(movieId.toString));

        if (!cursor.moveToNext)
            return null;

        val values = new ContentValues;

        values.put("id", movieId);
        values.put("url", cursor.getString(0));
        values.put("title", cursor.getString(1));
        values.put("hidden", cursor.getInt(2) != 0);

        return values;
    }

    def getOrCreateMovie(title : String, url : String) : JLong = {
        val db = getDatabase;
        val cursor = db.rawQuery("SELECT id AS _id FROM movie WHERE url = ?",
                                 Array(url));
        var id : JLong = -1;

        if (!cursor.moveToNext) {
            val values = new ContentValues;

            values.put("title", title);
            values.put("url", url);
            values.put("has_projections", false);

            id = db.insert("movie", null, values);
        }
        else
            id = cursor.getLong(0);

        cursor.close;

        return id;
    }

    def setMovieHidden(movieId : JLong, hidden : Boolean) {
        val db = getDatabase;
        val movie = new ContentValues;

        movie.put("hidden", hidden);

        db.update("movie", movie, "id = ?",
                  Array(movieId.toString));
    }

    def setProjections(movieId : JLong, projections : Seq[MovieProjection]) {
        val db = getDatabase;
        val projection = new ContentValues;

        db.delete("tc_projection", "movie_id = ?",
                  Array(movieId.toString));

        for (MovieProjection(theater, hours) <- projections) {
            projection.put("movie_id", movieId);
            projection.put("theater", theater);
            projection.put("times", hours);

            db.insert("tc_projection", null, projection);
        }

        val movie = new ContentValues;

        movie.put("has_projections", true);

        db.update("movie", movie, "id = ?", Array(movieId.toString));
    }

    def deleteProjections() {
        val db = getDatabase;
        val movie = new ContentValues;

        movie.put("has_projections", false);

        db.update("movie", movie, null, null);
        db.delete("tc_projection", null, null);
    }

    def setFilmUpCardUrl(movieId : JLong, cardUrl : String) {
        val db = getDatabase;
        val card = new ContentValues;

        db.delete("fu_card", "movie_id = ?",
                  Array(movieId.toString));

        card.put("url", cardUrl);
        card.put("movie_id", movieId);

        db.insert("fu_card", null, card);
    }

    def setFilmUpReviewUrl(movieId : JLong, reviewUrl : String) {
        val db = getDatabase;
        val review = new ContentValues;

        review.put("review_url", reviewUrl);

        db.update("fu_card", review, "movie_id = ?",
                  Array(movieId.toString));
    }

    def setFilmUpReview(movieId : JLong, reviewText : String) {
        val db = getDatabase;

        db.delete("fu_review", "movie_id = ?",
                  Array(movieId.toString));

        if (reviewText == null)
            return;

        val review = new ContentValues;

        review.put("text", reviewText);
        review.put("movie_id", movieId);

        db.insert("fu_review", null, review);
    }

    def getMovies() : Cursor = {
        val db = getDatabase;

        return db.rawQuery(
            "SELECT id AS _id, title, url, hidden" +
            "    FROM movie" +
            "    WHERE has_projections = 1" +
            "    ORDER BY hidden ASC", null);
    }

    def getProjections(movieId : JLong) : Cursor = {
        val db = getDatabase;

        return db.rawQuery(
            "SELECT id AS _id, theater, times" +
            "    FROM tc_projection" +
            "    WHERE movie_id = ?",
            Array(movieId.toString));
    }


    def getFilmUpReview(movieId : JLong) : String = {
        val db = getDatabase;
        val cursor = db.rawQuery(
            "SELECT id AS _id, text" +
            "    FROM fu_review" +
            "    WHERE movie_id = ?",
            Array(movieId.toString));
        var review : String = null;

        if (cursor.moveToNext)
            review = cursor.getString(1);

        cursor.close;

        return if (review == "") null else review;
    }

    def getFilmUpCard(movieId : JLong) : ContentValues = {
        val db = getDatabase;
        val cursor = db.rawQuery(
            "SELECT id AS _id, url, review_url" +
            "    FROM fu_card" +
            "    WHERE movie_id = ?",
            Array(movieId.toString));
        var card : ContentValues = null;

        if (cursor.moveToNext) {
            card = new ContentValues;

            card.put("movie_id", movieId);
            card.put("url", cursor.getString(1));
            card.put("review_url", cursor.getString(2));
        }

        cursor.close;

        return card;
    }

    private class MoviesOpenHelper(context : Context, name : String)
            extends SQLiteOpenHelper(context, name, null, Movies.VERSION) {
        override def onOpen(db : SQLiteDatabase) {
            super.onOpen(db);

            // Enable foreign key constraints
            if (!db.isReadOnly)
                db.execSQL("PRAGMA foreign_keys=ON");
        }

        override def onCreate(db : SQLiteDatabase) {
            db.execSQL(Movies.CREATE_MOVIES_TABLE);
            db.execSQL(Movies.CREATE_TROVACINEMA_PROJECTIONS_TABLE);
            db.execSQL(Movies.CREATE_FILMUP_CARD_TABLE);
            db.execSQL(Movies.CREATE_FILMUP_REVIEW_TABLE);
        }

        override def onUpgrade(db : SQLiteDatabase, from : Int, to : Int) {
            if (from < 2 && 2 <= to)
                upgrade1To2(db);
        }

        private def upgrade1To2(db : SQLiteDatabase) {
            db.beginTransaction();

            try {
                db.execSQL(
                    "ALTER TABLE movie" +
                    "    ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0");
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }
}
