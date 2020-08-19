package com.movies.controllers;

import java.io.IOException;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movies.models.Movie;
import com.movies.services.MovieService;

@RestController
public class MovieController {
	
	@Autowired
	private MovieService movieService;
	
	private static ObjectMapper objectMapper = new ObjectMapper();

		// get a movie data by it's name
		@GetMapping("/movies/{name}")
		public ResponseEntity<Movie> getMovie(@PathVariable String name, HttpServletResponse response) throws IOException, URISyntaxException {
			Movie movie = movieService.getMovieByName(name);
			if (movie == null) {
				return ResponseEntity.notFound().build();
			} else {
				return ResponseEntity.ok(movie);
			}
		}
}
