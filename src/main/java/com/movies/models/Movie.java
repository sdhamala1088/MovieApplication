package com.movies.models;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Movie {
	
	public enum MovieVariables {
		NAME("name"),
		QUOTE("quote"),
		IMG_PATH("imgPath"),
		THEME_SONG_PATH("themeSongPath"),
		RANK("rank"),
		YEAR("year"),
		DIRECTOR("director"),
		GENRE("genre"),
		ARTIST("artist"),
		LANGUAGE("language");

		private final String varibleName;
		
		private MovieVariables(String varibleName) {
			this.varibleName = varibleName;
		}	
		public String getValue() {
			return varibleName;
		}
	}

	@JsonIgnore
	private int id = 0;
	String name;
	Director director;
	String releaseDate;
	int year;
	@JsonProperty("artist")
	List<Artist> artists = new ArrayList<Artist>();
	@JsonProperty("genre")
	List<String> genres = new ArrayList<String>();
	@JsonProperty("language")
	List<String> languages = new ArrayList<String>();
	String quote;
	int rank;
	String imgPath;
	String themeSongPath;
	List<Rating> ratings = new ArrayList<>();
	
	public Movie() {
		id++;
	}
	
	public static class Rating {
		@JsonAlias("Source")
		String source;
		@JsonAlias("Value")
		String value;
		public String getSource() {
			return source;
		}
		public void setSource(String source) {
			this.source = source;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String value) {
			this.value = value;
		}
	}

	@Autowired
	public Movie(String name, Director director, String releaseDate, int year, List<Artist> artists,
			List<String> genres, List<String> languages, String imgPath, List<Rating> ratings) {
		super();
		this.name = name;
		this.director = director;
		this.releaseDate = releaseDate;
		this.year = year;
		this.artists = artists;
		this.genres = genres;
		this.languages = languages;
		this.imgPath = imgPath;
		this.ratings = ratings;
	}

	public int getId() {
		return id;
	}

	public String getQuote() {
		return quote;
	}

	public void setQuote(String quote) {
		this.quote = quote;
	}

	public Director getDirector() {
		return director;
	}

	public void setDirector(Director director) {
		this.director = director;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public List<Artist> getArtists() {
		return artists;
	}

	public void setArtists(List<Artist> artists) {
		this.artists = artists;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getImgPath() {
		return imgPath;
	}

	public void setImgPath(String imgPath) {
		this.imgPath = imgPath;
	}
	
	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public String getThemeSongPath() {
		return themeSongPath;
	}

	public void setThemeSongPath(String themeSongPath) {
		this.themeSongPath = themeSongPath;
	}
	
	public List<String> getGenres() {
		return genres;
	}

	public void setGenres(List<String> genres) {
		this.genres = genres;
	}

	public List<String> getLanguages() {
		return languages;
	}

	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	public List<Rating> getRatings() {
		return ratings;
	}

	public void setRatings(List<Rating> ratings) {
		this.ratings = ratings;
	}

}
