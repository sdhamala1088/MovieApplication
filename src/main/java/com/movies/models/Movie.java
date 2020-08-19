package com.movies.models;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class Movie {
	
	String name;
	Director director;
	// Date releaseDate; - To do String to Date 
	String releaseDate;
	List<Artist> artists = new ArrayList<Artist>();
	List<String> genreList = new ArrayList<String>();
	String quote;
	int rank;
	Path imgPath; // To do - look up img type store in DB  
	Path themeSong; // To do - look up mp3 type store in DB
	
	public Movie() {
	}
	
	@Autowired
	public Movie(String name, Director director, String releaseDate, 
			List<Artist> artists, List<String> genreList) {
		this.name = name;
		this.director = director;
		this.releaseDate = releaseDate;
		this.artists = artists;
		this.genreList = genreList;
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

	public List<String> getGenreList() {
		return genreList;
	}

	public void setGenreList(List<String> genreList) {
		this.genreList = genreList;
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

	public Path getImgPath() {
		return imgPath;
	}

	public void setImgPath(Path imgPath) {
		this.imgPath = imgPath;
	}

	public Path getThemeSong() {
		return themeSong;
	}

	public void setThemeSong(Path themeSong) {
		this.themeSong = themeSong;
	}

	public String getName() {
		return name;
	}
	
}
