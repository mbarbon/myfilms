package org.barbon.myfilms;

import _root_.android.app.ListActivity;
import _root_.android.content.Intent;
import _root_.android.os.Bundle;
import _root_.android.view.{Menu, MenuItem, View};
import _root_.android.widget.{Toast, SimpleCursorAdapter};

import _root_.java.lang.{Long => JLong};

class MyProjections extends ListActivity with ActivityHelper {
    import helpers._

    var adapter : SimpleCursorAdapter = null;
    var movies : Movies = null;
    var movieId : JLong = -1;

    protected override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_page);

        movies = Movies.getInstance(this);
        movieId = getIntent.getLongExtra("movieId", -1);

        adapter = new SimpleCursorAdapter(
            this, R.layout.projection_item, null,
            Array(Movies.PROJECTION_THEATER, Movies.PROJECTION_HOURS),
            Array(R.id.movie_theater, R.id.movie_hours));

        setListAdapter(adapter);

        var movie = movies.getMovie(movieId);

        findButton(R.id.display_review).setOnClickListener(showReview _);
        findButton(R.id.display_plot).setOnClickListener(showPlot _);
        findButton(R.id.hide).setOnClickListener(hideMovie _);
        findButton(R.id.show).setOnClickListener(showMovie _);

        if (movie.getAsBoolean("hidden"))
            findButton(R.id.hide).setVisibility(View.GONE);
        else
            findButton(R.id.show).setVisibility(View.GONE);
    }

    protected override def onStart() {
        super.onStart;

        adapter.changeCursor(movies.getProjections(movieId));
    }

    protected override def onResume() {
        super.onResume;

        if (adapter.getCursor != null)
            adapter.getCursor.requery;
    }

    // event handlers

    private def showReview() {
        val intent = new Intent(this, classOf[MyReview]);

        intent.putExtra("movieId", movieId);

        startActivity(intent);
    }

    private def showPlot() {
        val intent = new Intent(this, classOf[MyPlot]);

        intent.putExtra("movieId", movieId);

        startActivity(intent);
    }

    private def showMovie() {
        findButton(R.id.show).setVisibility(View.GONE);
        findButton(R.id.hide).setVisibility(View.VISIBLE);
        movies.setMovieHidden(movieId, false);
    }

    private def hideMovie() {
        findButton(R.id.show).setVisibility(View.VISIBLE);
        findButton(R.id.hide).setVisibility(View.GONE);
        movies.setMovieHidden(movieId, true);
    }
}
