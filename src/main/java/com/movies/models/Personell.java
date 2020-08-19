package com.movies.models;

public abstract class Personell {
	
	private String name;
	
	public Personell() {
	}
	
	public Personell(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
