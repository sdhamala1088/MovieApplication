package com.movies.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoviesAndAggregations {
	
	List<Movie> movies = new ArrayList<>();
	List<Facets> facets = new ArrayList<>();
	
	public List<Movie> getMovies() {
		return movies;
	}
	public void setMovies(List<Movie> movies) {
		this.movies = movies;
	}
	public List<Facets> getFacets() {
		return facets;
	}
	public void setAggregations(List<Facets> facets) {
		this.facets = facets;
	}
	public void addAggregation(Facets facets) {
		this.facets.add(facets);
	}
	
	public static class Facets {
		String name;
		List<Map<String, Long>> keyDocCounts = new ArrayList<>();
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public List<Map<String, Long>> getKeyDocCounts() {
			return keyDocCounts;
		}
		public void setKeyDocCounts(List<Map<String, Long>> keyDocCounts) {
			this.keyDocCounts = keyDocCounts;
		}
		public void addKeyDocCount(Map<String, Long> keyDocCount) {
			this.keyDocCounts.add(keyDocCount);
		}
	}
}
