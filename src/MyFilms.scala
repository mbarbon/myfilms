package org.barbon.myfilms;

import _root_.android.app.ListActivity;
import _root_.android.database.Cursor;
import _root_.android.content.{Context, Intent};
import _root_.android.os.Bundle;
import _root_.android.view.{LayoutInflater, Menu, MenuItem, View, ViewGroup};
import _root_.android.widget.{ListView, SimpleCursorAdapter, Toast};

import scrape.Trovacinema;

class MyFilmsCursorAdapter(context : Context, layout : Int, c : Cursor,
                           from : Array[String], to : Array[Int])
        extends SimpleCursorAdapter(context, layout, c, from, to) {
    val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE).asInstanceOf[LayoutInflater];

    override def getItemViewType(position : Int) : Int = {
        val item = getItem(position).asInstanceOf[Cursor];
        println("getItemViewType", position, item.getInt(3) == 0);
        return if (item.getInt(3) == 0) 0 else 1;
    }

    override def newView(context : Context, cursor : Cursor, parent : ViewGroup) : View = {
        val layout = if (cursor.getInt(3) == 0)
                         R.layout.movie_item
                     else
                         R.layout.movie_item_hidden;
        println("newView", layout);
        return inflater.inflate(layout, parent, false);
    }

    override def getViewTypeCount() : Int = {
        println("getViewTypeCount");
        return 2;
    }
}

class MyFilms extends ListActivity {
    var trovacinema : Trovacinema = null;
    var movies : Movies = null;
    var adapter : SimpleCursorAdapter = null;

    // lifecycle

    protected override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState);

        movies = Movies.getInstance(this);
        trovacinema = new Trovacinema(movies);

        adapter = new MyFilmsCursorAdapter(
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
        val id = if (success)
                     R.string.update_film_list_success
                 else
                     R.string.update_film_list_failure;

        Toast.makeText(this, id, Toast.LENGTH_SHORT).show;

        adapter.getCursor.requery;
    }
}
