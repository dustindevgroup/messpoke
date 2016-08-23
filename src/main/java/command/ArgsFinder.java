package command;

import java.util.ArrayList;
import java.util.List;

import travel.TravelType;

public class ArgsFinder {
	
	public int maxCircles = 2;
	public List<Integer> numbers = new ArrayList<>();
	public TravelType travelType = TravelType.getString( "r" );
	
	public ArgsFinder( String args[] ) {
		// finder id1,id2 maxCircles
		if ( args.length > 1 ) {
			
			// 
			String strIds[] = args[1].split( "," );
			for (String strId : strIds) {
				numbers.add( Integer.valueOf( strId ) );
			}
			
			// 
			if ( args.length > 2 ) {
				maxCircles = Integer.valueOf( args[2] );
			}
		}
		
	}
	

}
