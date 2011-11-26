package org.barbon.myfilms;

import _root_.android.app.{Activity, ListActivity}
import _root_.android.content.{ContentValues, Intent};
import _root_.android.os.Bundle;
import _root_.android.view.{View, ViewGroup}
import _root_.android.webkit.WebView;
import _root_.android.widget.{BaseAdapter, ListView, TextView};

import _root_.java.lang.{Long => JLong};

import scrape.FilmUp;

class MyReview extends Activity with ActivityHelper {
    import helpers._

    var movieId : JLong = -1;

    protected override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.review_page);

        val movies = Movies.getInstance(this);
        movieId = getIntent.getLongExtra("movieId", -1);

        (movies.getFilmUpReview(movieId), movies.getFilmUpCard(movieId)) match {
            case (null, null) => searchReview(true)
            case (null, card) => loadData
            case (reviewText, _) => setReviewText(reviewText)
        }

        findButton(R.id.search_review).setOnClickListener {
            () => searchReview(false);
        };
    }

    // event handlers

    def searchReview(replace : Boolean) {
        val intent = new Intent(this, classOf[SearchReviews]);

        intent.putExtra("movieId", movieId);

        startActivity(intent);

        if (replace)
            finish;
    }

    // implementation

    def loadData() {
        val filmup = new FilmUp(Movies.getInstance(this));

        setLoading(true);
        filmup.loadCard(movieId, cardLoaded _);
    }

    def cardLoaded(success : Boolean) {
        if (!success) {
            setLoading(false);

            return;
        }

        val filmup = new FilmUp(Movies.getInstance(this));

        filmup.loadReview(movieId, reviewLoaded _);
    }

    def reviewLoaded(success : Boolean) {
        setLoading(false);

        if (!success)
            return;

        val reviewText = Movies.getInstance(this).getFilmUpReview(movieId);

        setReviewText(reviewText);
    }

    def setLoading(loading : Boolean) {
        if (loading) {
            findView[View](R.id.movie_review).setVisibility(View.GONE);
            findView[View](R.id.load_progress).setVisibility(View.VISIBLE);
        } else {
            findView[View](R.id.movie_review).setVisibility(View.VISIBLE);
            findView[View](R.id.load_progress).setVisibility(View.GONE);
        }
    }

    def setReviewText(text : String) {
        val view = findView[WebView](R.id.movie_review);

        view.loadDataWithBaseURL(null, text, "text/html", "utf-8", null);
    }
}

class SearchReviews extends ListActivity with ActivityHelper {
    import helpers._

    type Item = (String, String);

    var movieId : JLong = -1;

    protected override def onCreate(savedInstanceState : Bundle) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_review);

        movieId = getIntent.getLongExtra("movieId", -1);

        val movies = Movies.getInstance(this);
        val movie = movies.getMovie(movieId);
        val title = movie.getAsString(Movies.MOVIE_TITLE);

        findEditText(R.id.search_terms).setText(title);
        findButton(R.id.search_review).setOnClickListener(searchReview _);
        searchReview;
    }

    // event handlers

    def searchReview() {
        val movies = Movies.getInstance(this);
        val filmup = new FilmUp(movies);
        val text = findEditText(R.id.search_terms).getText.toString;

        setListAdapter(null);
        filmup.search(text, gotCardList _);
    }

    override def onListItemClick(l : ListView, v : View,
                                 position : Int, id : Long) {
        val item = getListView.getAdapter.getItem(position).asInstanceOf[Item];
        val movies = Movies.getInstance(this);

        movies.setFilmUpCardUrl(movieId, item._2);
        movies.setFilmUpReview(movieId, null);

        // got back to review page
        val intent = new Intent(this, classOf[MyReview]);

        intent.putExtra("movieId", movieId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(intent);

        finish;
    }

    // implementation

    private def gotCardList(success : Boolean, results : Seq[Item]) {
        val adapter = new BaseAdapter {
            override def getCount() : Int = results.length;
            override def getItem(position : Int) = results(position);
            override def getItemId(position : Int) = position;
            override def isEmpty() = false;

            override def getView(position : Int, old : View, parent : ViewGroup) : View = {
                var view : View = old;

                if (view == null) {
                    val inflater = getLayoutInflater;

                    view = inflater.inflate(R.layout.movie_item, null);
                }

                val text = view.findViewById(R.id.movie_title)
                     .asInstanceOf[TextView];

                text.setText(results(position)._1);

                return view;
            }
        };

        setListAdapter(adapter);
    }
}
