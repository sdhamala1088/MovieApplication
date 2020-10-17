package com.movies.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.movies.models.MoviesAndAggregations;
import com.movies.models.Movie;
import com.movies.models.MovieSearchFilter;
import com.movies.services.MovieService;

@RestController
public class MovieController {

	@Autowired
	private MovieService movieService;

	// get a movie data by it's name
	@GetMapping("/movies/{movieName}")
	public ResponseEntity<Movie> getMovie(@PathVariable String movieName, HttpServletResponse response)
			throws IOException, URISyntaxException {
		Movie movie = movieService.getMovieByName(movieName);
		return (movie == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(movie);
	}

	@GetMapping("/movies/prefixSearch/{moviePrefix}")
	public ResponseEntity<List<Movie>> getMovies(@PathVariable String moviePrefix, HttpServletResponse response)
			throws IOException, URISyntaxException {
		List<Movie> movies = movieService.getMoviesByPrefix(moviePrefix);
		return (movies.isEmpty()) ? ResponseEntity.notFound().build() : ResponseEntity.ok(movies);
	}

	// get all movies in local database
	@GetMapping("/movies")
	public ResponseEntity<List<Movie>> getAllMovies() throws IOException {
		List<Movie> allMovies = movieService.getAllMovies();
		return ResponseEntity.ok(allMovies);
	}

	// get ranked movies & aggregations in local database
	@GetMapping("/movies/ranked")
	public ResponseEntity<MoviesAndAggregations> getRankedMoviesAndAggregations() throws IOException {
		MoviesAndAggregations rankedMoviesAndAggregations = movieService.getRankedMoviesAndAggregations();
		return ResponseEntity.ok(rankedMoviesAndAggregations);
	}
	
	@PostMapping(value = "/movies/filtered", consumes = "application/json")
	public ResponseEntity<List<Movie>> getFilteredMovies(@RequestBody MovieSearchFilter movieSearchFilter) throws IOException {
		List<Movie> filteredMovies = movieService.getFilteredMovies(movieSearchFilter);
		return ResponseEntity.ok(filteredMovies);
	}

	// post quote for a given movie
	@PostMapping(path = "/movies/{movieName}", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Movie> addQuote(@PathVariable(name = "movieName", required = true) String movieName,
			@RequestParam("quote") String quote) throws IOException {
		Movie movie = movieService.addQuoteToMovie(quote, movieName);
		return (movie == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(movie);
	}

	// post ranking to a given movie
	@PostMapping(path = "/movies/{movieName}/{rank}", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Movie> addRanking(@PathVariable(name = "movieName", required = true) String movieName,
			@PathVariable(name = "rank", required = true) int ranking) throws IOException {
		Movie movie = movieService.addRankingToMovie(ranking, movieName);
		return (movie == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(movie);
	}

	// add theme soundtrack or image to the server
	@PostMapping(path = "movies/{movieName}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Movie> addMovieThemeSong(@PathVariable(name = "movieName", required = true) String movieName,
			@RequestParam("file") MultipartFile file) throws IOException {
		Movie movie = movieService.addMovieFile(file, movieName);
		return (movie == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(movie);
	}
}
