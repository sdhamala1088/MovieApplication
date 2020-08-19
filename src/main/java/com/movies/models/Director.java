package com.movies.models;

public class Director extends Personell{
	
	public Director() {
		super();
	}

	public Director(String name) {
		super(name);
	}
	
	// Comparator interface equals overridden  
	@Override
	public boolean equals(Object directorObject) {
		
		if (directorObject == this) {
			return true;
		}
		if (!(directorObject instanceof Director)) { 
            return false; 
        } 
		
	// compareTo return int not boolean -- :|
		Director comparedDirector = (Director) directorObject;
		return super.getName().compareToIgnoreCase(comparedDirector.getName()) == 0;
	}

}
