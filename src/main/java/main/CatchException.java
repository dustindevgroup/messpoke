package main;

import com.pokegoapi.api.map.pokemon.CatchResult;

public class CatchException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private CatchResult catchResult;

	public CatchException(CatchResult cr) {
		this.catchResult = cr;
	}

	public CatchResult getCatchResult() {
		return catchResult;
	}

}
