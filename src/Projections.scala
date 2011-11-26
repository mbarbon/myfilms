package org.barbon.myfilms;

import _root_.android.app.ListActivity;
import _root_.android.content.Intent;
import _root_.android.os.Bundle;
import _root_.android.view.{Menu, MenuItem};
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

        findButton(R.id.display_review).setOnClickListener(showReview _);
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
}
