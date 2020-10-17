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
import org.apache.commons.io.FilenameUtils;
import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.movies.models.Artist;
import com.movies.models.Director;
import com.movies.models.MoviesAndAggregations;
import com.movies.models.Movie;
import com.movies.models.Movie.Rating;
import com.movies.models.MovieSearchFilter;
import com.movies.repositories.MovieRepository;

@Service
public class MovieService {

	@Autowired
	private MovieRepository movieRepository;

	@Value("${images.location}")
	String imagesDirectory;

	@Value("${music.location}")
	String musicDirectory;
	
	private static ObjectMapper ObjectMapper = new ObjectMapper();
	private static final String JPG = "jpg";
	private static final String MP3 = "mp3";
	private static final String DOT = ".";

	/**
	 * Get movie by name from localDb, if not present, from Omdb
	 * 
	 * @param name
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public Movie getMovieByName(String name) throws URISyntaxException, IOException {

		Movie movie = movieRepository.getMovieFromLocalDb(name);
		if (movie == null) {
			movie = getMovieFromOmdb(name);
		}
		return movie;
	}

	/**
	 * Get movies for movie prefix by name from localDb, if not present from Omdb
	 * 
	 * @param name
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public List<Movie> getMoviesByPrefix(String name) throws URISyntaxException, IOException {

		MoviesAndAggregations moviesAndAggregations = movieRepository.getMoviesByPrefix(name);
		return moviesAndAggregations.getMovies();
	}
	
	private Movie getMovieFromOmdb(String name) throws IOException, ClientProtocolException, URISyntaxException {
		Movie movie;
		JsonObject omdbObject = movieRepository.getJsonObjectFromOmdb(name);
		movie = mapOmdbJsonIntoMovieEntity(omdbObject);
		if (movie != null) {
			movieRepository.insertMovie(movie);
		}
		return movie;
	}
	
	/**
	 * Get all movies in the local database
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<Movie> getAllMovies() throws IOException {
		return movieRepository.getAllMovies();
	}
	
	public MoviesAndAggregations getRankedMoviesAndAggregations() throws IOException {
		return movieRepository.getRankedMoviesAndAggregations();
	}

	/**
	 * Map Json returned from Omdb into Movie entity
	 * 
	 * @param omdbJsonObject
	 * @return
	 * @throws IOException
	 */
	private Movie mapOmdbJsonIntoMovieEntity(JsonObject omdbJsonObject) throws IOException {
		String movieName = jsonElementToString(omdbJsonObject.get("Title"));
		String directorName = jsonElementToString(omdbJsonObject.get("Director"));
		String releasedDate = jsonElementToString(omdbJsonObject.get("Released"));
		JsonElement yearJsonElement = omdbJsonObject.get("Year");
		int year = yearJsonElement.isJsonNull() ? 1 : yearJsonElement.getAsInt();
		String imagePath = saveImageToServer(jsonElementToString(omdbJsonObject.get("Poster")), movieName);
		String artistsString = jsonElementToString(omdbJsonObject.get("Actors"));
		String genresString = jsonElementToString(omdbJsonObject.get("Genre"));
		String languagesString = jsonElementToString(omdbJsonObject.get("Language"));
		List<Rating> ratings = Arrays.asList(ObjectMapper.readValue
				(omdbJsonObject.get("Ratings").toString(), Movie.Rating[].class));
		List<Artist> artists = null;
		List<String> genresList = null;
		List<String> languageList = null;

		if (artistsString != null) {
			List<String> artistsList = Arrays.asList(artistsString.split(", "));
			artists = artistsList.stream().map(x -> new Artist(x)).collect(Collectors.toList());
		}
		genresList = splitString(genresString, genresList);
		languageList = splitString(languagesString, languageList);
		Director director = new Director(directorName);
		Movie movie = new Movie(movieName, director, releasedDate, year, artists, genresList, languageList, imagePath, ratings);
		return movie;
	}

	private List<String> splitString(String genresString, List<String> genresList) {
		if (genresString != null) {
			genresList = Arrays.asList(genresString.split(", "));
		}
		return genresList;
	}

	private static String jsonElementToString(JsonElement jsonElement) {
		if (jsonElement.isJsonNull()) {
			return null;
		}
		return jsonElement.getAsString();
	}

	private String saveImageToServer(String httpUrl, String movieName) throws IOException {
		if (httpUrl == null || httpUrl.isBlank()) {
			return null;
		}
		String movieImageLocation = imagesDirectory + movieName + DOT + JPG;
		try (InputStream in = new URL(httpUrl).openStream()) {
			Files.copy(in, Paths.get(movieImageLocation));
		} catch (Exception e) {
			movieImageLocation = imagesDirectory + cleanFileName(movieName) + DOT + JPG;
		}
		return movieImageLocation;
	}

	public Movie addQuoteToMovie(String quote, String movieName) throws IOException {
		return movieRepository.updateMovie(Movie.MovieVariables.QUOTE.getValue(), quote, movieName);
	}

	public Movie addRankingToMovie(int ranking, String movieName) throws IOException {
		return movieRepository.updateMovie(Movie.MovieVariables.RANK.getValue(), ranking, movieName);
	}

	public Movie addMovieFile(MultipartFile file, String movieName) throws IOException {
		String extension = FilenameUtils.getExtension(file.getOriginalFilename());
		if (extension.equals(MP3)) {
			return addMP3File(movieName, file);
		} else if (extension.equals(JPG)) {
			return addImageFile(movieName, file);
		} else {
			return null;
		}
	}

	public Movie addImageFile(String movieName, MultipartFile file) throws IOException {
		String movieImageLocation = imagesDirectory + movieName + JPG;
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, Paths.get(movieImageLocation));
		} catch (Exception e) {
			movieImageLocation = imagesDirectory + cleanFileName(movieName) + DOT + JPG;
		}
		return movieRepository.updateMovie(Movie.MovieVariables.IMG_PATH.getValue(), movieImageLocation, movieName);
	}

	public Movie addMP3File(String movieName, MultipartFile file) throws IOException {
		String movieThemeSongLocation = musicDirectory + movieName + MP3;
		try (InputStream in = file.getInputStream()) {
			Files.copy(in, Paths.get(movieThemeSongLocation));
		} catch (Exception e) {
			movieThemeSongLocation = musicDirectory + cleanFileName(movieName) + DOT + MP3;
		}
		return movieRepository.updateMovie(Movie.MovieVariables.THEME_SONG_PATH.getValue(), movieThemeSongLocation,
				movieName);
	}

	private String cleanFileName(String fileName) {

		int[] illegalChars = { 34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
				20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47 };

		Arrays.sort(illegalChars);
		StringBuilder cleanName = new StringBuilder();
		int len = fileName.codePointCount(0, fileName.length());
		for (int i = 0; i < len; i++) {
			int c = fileName.codePointAt(i);
			if (Arrays.binarySearch(illegalChars, c) < 0) {
				cleanName.appendCodePoint(c);
			}
		}
		return cleanName.toString();
	}

	public List<Movie> getFilteredMovies(MovieSearchFilter movieSearchFilter) throws IOException {
		return movieRepository.getFilteredMovies(movieSearchFilter);
	}
}
