/*
 * Copyright (C) 2015 Teddy Rodriguez (TROD)
 *   email: cia.123trod@gmail.com
 *   github: TROD-123
 *
 * For Udacity's Android Developer Nanodegree
 * P1-2: Popular Movies
 *
 * Currently for educational purposes only.
 */

package com.thirdarm.popularmovies.API;

import com.thirdarm.popularmovies.MoviePostersFragment;
import com.thirdarm.popularmovies.constant.URL;
import com.thirdarm.popularmovies.model.MovieDB;
import com.thirdarm.popularmovies.model.Results;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by TROD on 20150910.
 *
 * Class used to call APIService with specified information
 */
public class TMDB {

    private final String LOG_TAG = "Movies/TMDB";

    // API information
    private String API_KEY;
    private String LANGUAGE;
    private int PAGE;
    private APIService api;

    // movie results
    private List<Results.MovieDBResult> results;
    private int[] movieIDs;
    private ArrayList<MovieDB> movies = new ArrayList<>();


    public TMDB(String key, String language_code, int page) {
        API_KEY = key;
        LANGUAGE = language_code;
        PAGE = page;
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(URL.BASE)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        api = retrofit.create(APIService.class);
    }

    /** Fetches results */
    public ArrayList<MovieDB> getResults(Call<Results> response) {
        clear();
        try {
            results = response.execute().body().getMovieDBResults();
            movieIDs = new int[results.size()];
            for (int i = 0; i < movieIDs.length; i++) {
                movieIDs[i] = results.get(i).getId();
            }
            for (int id : movieIDs) {
                movies.add(getMovieDetails(id));
            }
            return movies;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Generates results for /discover
     *
     * @param sort method to sort discover results
     * @return list of movies
     */
    public ArrayList<MovieDB> discover(String sort) {
        return getResults(api.discover(API_KEY, sort, LANGUAGE, PAGE));
    }

    /**
     * Generates results for /movie/{category}
     *
     * @param category the category of movies to display
     * @return list of movies
     */
    public ArrayList<MovieDB> getResults(String category) {
        return getResults(api.getResults(category, API_KEY, LANGUAGE, PAGE));
    }

    /**
     * Generates specific MovieDB object
     *
     * @param id the TMDB movie id
     * @return a movie object
     */
    public MovieDB getMovieDetails(int id) {
        Call<MovieDB> response = api.getMovieDetails(id, API_KEY, LANGUAGE, "images,releases,trailers");
        try {
            final MovieDB movie = response.execute().body();
            if (movie != null) {
                // Post loading status to main UI thread
                MoviePostersFragment.sProgressStatus.post(new Runnable() {
                    @Override
                    public void run() {
                        MoviePostersFragment.sProgressStatus.setText("Added " + movie.getTitle());
                    }
                });
                return movie;
            } else {
                return getMovieDetails(id);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Clears results and movies lists before reloading grid view */
    public void clear() {
        if (results != null && movies != null && movieIDs != null){
            results = null;
            movies = new ArrayList<>();
            movieIDs = null;
        }
    }
}