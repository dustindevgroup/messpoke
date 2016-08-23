package travel;

import travel.Traveller.TravelStatus;

public interface OnTravelListener {
	
	public boolean onTravelling(TravelStatus ts);
	public void onTravelDone();

}
