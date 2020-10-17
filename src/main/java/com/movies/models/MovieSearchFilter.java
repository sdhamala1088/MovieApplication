package com.movies.models;

import java.util.ArrayList;
import java.util.List;

public class MovieSearchFilter {
	
	List<Director> directorList = new ArrayList<>();
	List<String> genres = new ArrayList<>();
	List<Artist> artists = new ArrayList<>();
	List<String> languages = new ArrayList<>();
	RangeYear rangeYear = new RangeYear();
	
	public List<Director> getDirectorList() {
		return directorList;
	}
	public List<String> getLanguages() {
		return languages;
	}
	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}
	public void setDirectorList(List<Director> directorList) {
		this.directorList = directorList;
	}
	public List<String> getGenres() {
		return genres;
	}
	public void setGenres(List<String> genres) {
		this.genres = genres;
	}
	public List<Artist> getArtists() {
		return artists;
	}
	public void setArtists(List<Artist> artists) {
		this.artists = artists;
	}
	public RangeYear getRangeYear() {
		return this.rangeYear;
	}
	
	public static class RangeYear {
		static final String FIELD = Movie.MovieVariables.YEAR.toString();
		int from;
		int to;
		public int getFromYear() {
			return from;
		}
		public void setFromYear(int from) {
			this.from = from;
		}
		public int getToYear() {
			return to;
		}
		public void setToYear(int to) {
			this.to = to;
		}
		public static String getField() {
			return FIELD;
		}
	}

}
