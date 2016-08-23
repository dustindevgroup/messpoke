package command;

import travel.TravelType;

public class ArgsTravel {
	
	public TravelType travelType = TravelType.WALK;
	public double latitude;
	public double longitude;
	public boolean pausable = false;
	public boolean uniqueOnly = false;
	public boolean listOnly = false;
	
	public ArgsTravel(String[] args) {
		this.travelType = TravelType.getString( args[1] );
		this.latitude = Double.valueOf( args[2].replace(",", "") );
		this.longitude = Double.valueOf( args[3] );
		
		if ( args.length > 4 ) {
			String lastArgs = "";
			for (int i = 4; i < args.length; i++) {
				lastArgs += args[i] + " ";
			}
			
			if ( lastArgs.contains( "unique" ) ) {
				this.uniqueOnly = true;
			} else if ( lastArgs.contains( "list" ) ) {
				this.listOnly = true;
			} else if ( lastArgs.contains( "pause" ) ) {
				this.pausable = true;
			}
		}
	}
	
}
