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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movies.models.Movie;
import com.movies.services.MovieService;

@RestController
public class MovieController {

	@Autowired
	private MovieService movieService;

	private static ObjectMapper objectMapper = new ObjectMapper();

	// get a movie data by it's name
	@GetMapping("/movies/{movieName}")
	public ResponseEntity<Movie> getMovie(@PathVariable String movieName, HttpServletResponse response)
			throws IOException, URISyntaxException {
		Movie movie = movieService.getMovieByName(movieName);
		if (movie == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(movie);
		}
	}

	// get all movies in local database
	@GetMapping("/movies")
	public ResponseEntity<List<Movie>> getAllMovies() throws IOException {
		List<Movie> allMovies = movieService.getAllMovies();
		return ResponseEntity.ok(allMovies);
	}
	
	// post quote for a given movie
	@PostMapping(path = "/movies/{movieName}/{quote}", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<Movie> addQuote(@PathVariable (name = "movieName", required = true) String movieName, 
			@PathVariable (name = "quote", required = true) String quote) throws IOException{
		Movie movie = movieService.addQuoteToMovie(quote, movieName);
		if (movie == null) {
			return ResponseEntity.notFound().build();
		} else {
			return ResponseEntity.ok(movie);
		}
	}
	
	// add image to the server
//	@PostMapping("/movies/{movieName}")
//	public ResponseEntity<Movie> handleFileUpload(@RequestParam("file") MultipartFile files) {
//		return null;
//		
//		
//	}
	
	
	
	
	
}
