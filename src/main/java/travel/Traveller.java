package travel;

import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import main.PokeBO;
import util.TextUtils;

public class Traveller {
	
	public static class TravelStatus {
		
		public double[] origin;
		public double[] destination;
		public long timeStarted;
		public long etaSeconds;
		public float progress;
		public TravelType travelType;
		
	}

	private static final long STEP_TRAVEL = 5000;
	
	public static void travelTo(PokeBO pokeBO, double latitude, double longitude, TravelType tt, OnTravelListener listener ) {
		System.out.println("[travel] travelTo " + TextUtils.toCoordString(latitude, longitude));
		System.out.println("[travel] from     " + pokeBO.toStringLocation() );
		
		final TravelStatus ts = new TravelStatus();
		ts.origin = pokeBO.getLocation();
		ts.destination = new double[]{ latitude, longitude };
		ts.progress = 0;
		ts.travelType = tt;
		ts.timeStarted = new Date().getTime();
		ts.etaSeconds = calculateETA(ts.origin, ts.destination, ts.travelType);
		
		System.out.println("[travel] ETA : " + TextUtils.secondsToETA(ts.etaSeconds));
		
		while( keepTravelling(ts, listener, pokeBO) ) {
			// keep
		}
		
	}
	
	private static boolean keepTravelling(TravelStatus ts, OnTravelListener listener, PokeBO pokeBO) {
		try {
			Thread.sleep( STEP_TRAVEL );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		long secondsPassed = (new Date().getTime() - ts.timeStarted) / 1000;
		if ( secondsPassed == 0 ) {
            secondsPassed = 1;
        }
		ts.progress = secondsPassed / (float) ts.etaSeconds;
		
		final double nextCoords[] = calculateProgressPoint(ts.origin, ts.destination, ts.progress);
		final double distance = distance(nextCoords[0], nextCoords[1], ts.destination[0], ts.destination[1]);
		
		System.out.println( "\tdistance = " + distance);
		
		
		double newCoords[];
		if ( distance <= 5 || ts.progress >= 1 ) {
			newCoords = goCloserTo(ts.destination[0], ts.destination[1]);
			listener.onTravelDone();
			return false;
		}
		
		newCoords = goCloserTo(nextCoords[0], nextCoords[1]);
		pokeBO.setLocation(newCoords[0], newCoords[1], false, true);
		
		return listener.onTravelling(ts);
	}

	public static double fixCoordPrecision(double coord) {
        return Double.valueOf( String.format(Locale.ENGLISH, "%.8f", coord) );
    }
	
	public static double[] goCloserTo(double latitude, double longitude) {
        double variation = (new Random().nextInt( 4 ) + 1) / 100000d;
        latitude = latitude + ( new Random().nextBoolean() ? -variation : variation );
        longitude = longitude + ( new Random().nextBoolean() ? -variation : variation );

        latitude = Traveller.fixCoordPrecision( latitude );
        longitude = Traveller.fixCoordPrecision( longitude );

        return new double[] { latitude, longitude };
    }
	
	public static long calculateETA(double[] current, double[] dest, TravelType travelType) {
        float speedVariation = (new Random().nextInt(30) + 1) / 100f;
        speedVariation *= new Random().nextBoolean() ? -1 : 1;

        float actualMediumSpeed = travelType.getSpeed() + speedVariation;

        double result = distance( current[0], current[1], dest[0], dest[1] );

        long eta = new Float( result / actualMediumSpeed ).longValue();
        return eta;
    }
	
	/*
	 * Calculate distance between two points in latitude and longitude taking
	 * into account height difference. If you are not interested in height
	 * difference pass 0.0. Uses Haversine method as its base.
	 * 
	 * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
	 * el2 End altitude in meters
	 * @returns Distance in Meters
	 */
	public static double distance(double lat1, double lon1,
			double lat2, double lon2) {
		 double el1 = 1;
		 double el2 = 1;

	    final int R = 6371; // Radius of the earth

	    Double latDistance = Math.toRadians(lat2 - lat1);
	    Double lonDistance = Math.toRadians(lon2 - lon1);
	    Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
	            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
	    Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	    double distance = R * c * 1000; // convert to meters

	    double height = el1 - el2;

	    distance = Math.pow(distance, 2) + Math.pow(height, 2);

	    return Math.sqrt(distance);
	}

    public static double[] calculateProgressPoint( double[] origin, double[] dest, float ratio ) {
        ratio = Float.valueOf( String.format(Locale.ENGLISH, "%.2f", ratio) );

        // var x3 = x1 + (x2 - x1) * ratio;
        double result[] = new double[2];
        result[0]  = origin[0] + (dest[0] - origin[0]) * ratio;
        result[1] = origin[1] + (dest[1] - origin[1]) * ratio;

        result[0] = Traveller.fixCoordPrecision(result[0]);
        result[1] = Traveller.fixCoordPrecision(result[1]);

//        System.out.println("calculateProgressPoint result = " + TextUtils.toCoordString(result[0], result[1]));

        if ( Math.abs( result[1] ) > 180
                || Math.abs( result[0] ) > 90) {
            return null;
        }

        return result;
    }
    
    /**
     * Vincenty direct calculation.
     *
     * @private
     * @param   {number} distance - Distance along bearing in meters.
     * @param   {number} initialBearing - Initial bearing in degrees from north.
     * @returns (Object} Object including point (destination point), finalBearing.
     * @throws  {Error}  If formula failed to converge.
     */
    
    //DATUM : WGS-84	a = 6 378 137 m (±2 m)	b ≈ 6 356 752.314245 m	f ≈ 1 / 298.257223563
    static double datumA = 6378137;
    static double datumB = 6356752.314245;
    static double datumF = 1 / 298.257223563;
    
    public static double[] findPoint(float distance, float initialBearing, double[] origin) throws Exception
    {
		double φ1 = Math.toRadians( origin[0] ), λ1 = Math.toRadians( origin[1] );
		double α1 = Math.toRadians(initialBearing);//initialBearing.toRadians();
		double s = distance;
		
		double a = datumA, b = datumB, f = datumF;
		
		double sinα1 = Math.sin(α1);
		double cosα1 = Math.cos(α1);
		
		double tanU1 = (1-f) * Math.tan(φ1), cosU1 = 1 / Math.sqrt((1 + tanU1*tanU1)), sinU1 = tanU1 * cosU1;
		double σ1 = Math.atan2(tanU1, cosα1);
		double sinα = cosU1 * sinα1;
		double cosSqα = 1 - sinα*sinα;
		double uSq = cosSqα * (a*a - b*b) / (b*b);
		double A = 1 + uSq/16384*(4096+uSq*(-768+uSq*(320-175*uSq)));
		double B = uSq/1024 * (256+uSq*(-128+uSq*(74-47*uSq)));
		
		double cos2σM, sinσ, cosσ, Δσ;
		
		double σ = s / (b*A), σʹ, iterations = 0;
		do {
		    cos2σM = Math.cos(2*σ1 + σ);
		    sinσ = Math.sin(σ);
		    cosσ = Math.cos(σ);
		    Δσ = B*sinσ*(cos2σM+B/4*(cosσ*(-1+2*cos2σM*cos2σM)-
		        B/6*cos2σM*(-3+4*sinσ*sinσ)*(-3+4*cos2σM*cos2σM)));
		    σʹ = σ;
		    σ = s / (b*A) + Δσ;
		} while (Math.abs(σ-σʹ) > 1e-12 && ++iterations<200);
		
		if (iterations>=200) throw new Exception("Formula failed to converge"); // not possible?
		
		double x = sinU1*sinσ - cosU1*cosσ*cosα1;
		double φ2 = Math.atan2(sinU1*cosσ + cosU1*sinσ*cosα1, (1-f)*Math.sqrt(sinα*sinα + x*x));
		double λ = Math.atan2(sinσ*sinα1, cosU1*cosσ - sinU1*sinσ*cosα1);
		double C = f/16*cosSqα*(4+f*(4-3*cosSqα));
		double L = λ - (1-C) * f * sinα *
		    (σ + C*sinσ*(cos2σM+C*cosσ*(-1+2*cos2σM*cos2σM)));
		double λ2 = (λ1+L+3*Math.PI)%(2*Math.PI) - Math.PI;  // normalise to -180..+180
		
		double α2 = Math.atan2(sinα, -x);
		α2 = (α2 + 2*Math.PI) % (2*Math.PI); // normalise to 0..360
		
//		return {
//		    point:        new LatLon(φ2.toDegrees(), λ2.toDegrees(), this.datum),
//		    finalBearing: α2.toDegrees(),
//		};
		
		return new double[]{
				Math.toDegrees( φ2 ), Math.toDegrees( λ2 )
		};
    }
    
    public static void spiralTravel(float range, int totalPoints, TravelType travelType, double origin[], OnSpiralTravelListener listener) throws Exception {
    	float direcoes[]= { 0, 90, 180, 270  };
    	int indexDirecao = 0;
    	
    	int counterPoints = 1;
    	
    	int sameDirectionStep = 1;
    	
    	double latLng[] = origin;
    	double latLngAnt[] = latLng;
    	
//    	String url = Main.BASE_URL;
    	
//    	int contador = 0;
    	
//    	url += "&markers=color:blue|label:"+contador+"|"+latLng[0]+","+latLng[1];
    	
    	do {
    		
    		for (int i = 0; i < 2; i++) {
    			for (int j = 0; j < sameDirectionStep; j++) {
//    				System.out.println( indexDirecao + "\t" + direcoes[indexDirecao] + "\t" + stepMax + "\t" + j );
//    				url += "&markers=color:blue|label:"+contador+"|"+latLng[0]+","+latLng[1];
    				
    				latLng = findPoint( range, direcoes[indexDirecao], latLngAnt);
    				
    				System.out.println( counterPoints + "\t" + Arrays.toString( latLng ) );
    				
    				latLngAnt = latLng;
    				
//    				contador++;
    				
//    				Thread.sleep( (long) ((range / travelType.getSpeed()) * 1000) );
    				
    				listener.onNewPoint( latLng );
				}
    			
				indexDirecao = (indexDirecao + 1) % direcoes.length;
				if ( indexDirecao % 2 == 0 ) {
					sameDirectionStep++;
				}
				
				counterPoints++;
			}
    		
    	} while (counterPoints < totalPoints);
    	
    	listener.onDoneTravel(latLng);
    	
//    	System.out.println( url );
    }
    
    public static interface OnSpiralTravelListener {
    	
    	void onNewPoint(double[] latLng);
    	void onDoneTravel(double[] latLng);
    	
    }
    
}
