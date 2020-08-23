package com.movies.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import com.google.gson.JsonObject;
import com.movies.models.Artist;
import com.movies.models.Director;
import com.movies.models.Movie;
import com.movies.repositories.MovieRepository;

@Service
public class MovieService {

	@Autowired
	private MovieRepository movieRepository;
	
	@Value("${images.location}")
	String imagesDirectory;

	/**
	 * Get movie by name from localDb, if not present from Omdb
	 * 
	 * @param name
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public Movie getMovieByName(String name) throws URISyntaxException, IOException {

		Movie movie = movieRepository.getMovieFromLocalDb(name);
		if (movie == null) {
			JsonObject omdbObject = movieRepository.getMovieFromOmdb(name);
			movie = mapOmdbJsonIntoMovieEntity(omdbObject);
			if (movie != null) {
				movieRepository.insertMovie(movie);
				return movie;
			}
		} 
		return movie;
	}

	/**
	 * Map Json returned from Omdb into Movie entity
	 * @param omdbJsonObject
	 * @return
	 * @throws IOException 
	 */
	private Movie mapOmdbJsonIntoMovieEntity(JsonObject omdbJsonObject) throws IOException {
		// To do - store document name capitalized on each work
		String movieName = omdbJsonObject.get("Title").getAsString().toLowerCase();
		String directorName = omdbJsonObject.get("Director").getAsString();
		String releasedDate = omdbJsonObject.get("Released").getAsString();
		String imagePath = saveImageToServer(omdbJsonObject.get("Poster").getAsString(), movieName);
		List<String> artistsString = Arrays.asList(omdbJsonObject.get("Actors").getAsString().split(","));
		List<String> genres = Arrays.asList(omdbJsonObject.get("Genre").getAsString().split(","));

		Director director = new Director(directorName);
		List<Artist> artists = artistsString.stream().map(x -> new Artist(x)).collect(Collectors.toList());
		Movie movie = new Movie(movieName, director, releasedDate, artists, genres, imagePath);
		return movie;
	}

	private String saveImageToServer(String httpUrl, String movieName) throws IOException {
		if (httpUrl.isBlank()) {
			return null;
		}
		String movieImageServerLocation = imagesDirectory + movieName + ".jpg";
		try(InputStream in = new URL(httpUrl).openStream()){
			Files.copy(in, Paths.get(movieImageServerLocation));
		}
		return movieImageServerLocation;
	}

	/**
	 * Get all movies in the local database
	 * @return
	 * @throws IOException 
	 */
	public List<Movie> getAllMovies() throws IOException {
		return movieRepository.getAllMovies();
	}

	public Movie addQuoteToMovie(String quote, String movieName) throws IOException {
		return movieRepository.updateMovie(quote, movieName);
	}
}
