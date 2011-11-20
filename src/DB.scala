package org.barbon.myfilms;

import _root_.android.content.Context;
import _root_.android.content.ContentValues;

import _root_.android.database.Cursor;

import _root_.android.database.sqlite.SQLiteDatabase;
import _root_.android.database.sqlite.SQLiteOpenHelper;

import _root_.java.lang.{Long => JLong};

import scala.collection.LinearSeq;

object Movies {
    val VERSION : Int = 1;

    val CREATE_MOVIES_TABLE : String = """
        CREATE TABLE movie (
            id INTEGER PRIMARY KEY,
            title TEXT NOT NULL,
            url TEXT NOT NULL,
            has_projections INTEGER NOT NULL,
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
            FOREIGN KEY (movie_id) REFERENCES movie(id)
                ON DELETE CASCADE
        )
    """;

    val CREATE_FILMUP_REVIEW_TABLE : String = """
        CREATE TABLE fu_review (
            id INTEGER PRIMARY KEY,
            movie_id INTEGER NOT NULL,
            FOREIGN KEY (movie_id) REFERENCES movie(id)
                ON DELETE CASCADE
        )
    """;

    val MOVIE_TITLE = "title";
    val PROJECTION_THEATER = "theater";
    val PROJECTION_HOURS = "times";

    // singleton handling

    private var theInstance : Movies = null;

    def getInstance(context : Context) : Movies = {
        if (theInstance != null)
            return theInstance;

        theInstance = new Movies(context, "movies");

        return theInstance;
    }
}

class Movies (context : Context, name : String) {
    private val openHelper = new MoviesOpenHelper(context, name);

    private def getDatabase() : SQLiteDatabase =
        openHelper.getWritableDatabase;

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

    def setProjections(movieId : JLong, projections : LinearSeq[(String, String)]) {
        val db = getDatabase;
        val projection = new ContentValues;

        db.delete("tc_projection", "movie_id = ?",
                  Array(movieId.toString));

        for ((theater, hours) <- projections) {
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

    def getMovies() : Cursor = {
        val db = getDatabase;

        return db.rawQuery("SELECT id AS _id, title, url FROM movie", null);
    }

    def getProjections(movieId : JLong) : Cursor = {
        val db = getDatabase;

        return db.rawQuery(
            "SELECT id AS _id, theater, times" +
            "    FROM tc_projection" +
            "    WHERE movie_id = ?",
            Array(movieId.toString));
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
            throw new _root_.java.lang.UnsupportedOperationException;
        }
    }
}
