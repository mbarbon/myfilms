package org.barbon.myfilms;

import _root_.android.app.ListActivity;

import _root_.android.content.Intent;

import _root_.android.os.Bundle;

import _root_.android.view.Menu;
import _root_.android.view.MenuItem;
import _root_.android.view.View;

import _root_.android.widget.ListView;
import _root_.android.widget.SimpleCursorAdapter;
import _root_.android.widget.Toast;

import scrape.Trovacinema;

class MyFilms extends ListActivity {
    var trovacinema : Trovacinema = null;
    var movies : Movies = null;
    var adapter : SimpleCursorAdapter = null;

    // lifecycle

    protected override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState);

        movies = Movies.getInstance(this);
        trovacinema = new Trovacinema(movies);

        adapter = new SimpleCursorAdapter(
            this, R.layout.movie_item, null,
            Array(Movies.MOVIE_TITLE),
            Array(R.id.movie_title));

        setListAdapter(adapter);
    }

    protected override def onStart() {
        super.onStart;

        adapter.changeCursor(movies.getMovies);
    }

    protected override def onResume() {
        super.onResume;

        if (adapter.getCursor != null)
            adapter.getCursor.requery;
    }

    // event handlers

    override def onCreateOptionsMenu(menu : Menu) : Boolean = {
        val inflater = getMenuInflater;

        inflater.inflate(R.menu.myfilms, menu);

        return true;
    }

    override def onOptionsItemSelected(item : MenuItem) : Boolean = {
        item.getItemId match {
            case R.id.update_film_list => {
                trovacinema.fetchList(this.movieListUpdated);
                true
            }
            case _ => super.onOptionsItemSelected(item);
        }
    }

    override def onListItemClick(l : ListView, v : View, position : Int,
                                 movieId : Long) {
        val intent = new Intent(this, classOf[MyProjections]);

        intent.putExtra("movieId", movieId);

        startActivity(intent);
    }

    // implementation

    private def movieListUpdated(success : Boolean) {
        var id = if (success)
                     R.string.update_film_list_success
                 else
                     R.string.update_film_list_failure;

        Toast.makeText(this, id, Toast.LENGTH_SHORT).show;

        adapter.getCursor.requery;
    }
}
