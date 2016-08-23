package util;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import main.MyLogger;
import POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import POGOProtos.Data.Capture.CaptureProbabilityOuterClass.CaptureProbability;
import POGOProtos.Enums.ActivityTypeOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass;
import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Inventory.Item.ItemAwardOuterClass.ItemAward;
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;

import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.api.map.pokemon.encounter.EncounterResult;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

public class TextUtils {
	
	public static String toCoordString(double coord) {
		return String.format(Locale.ENGLISH, "%.10f", coord);
	}
	
	public static String toCoordString(double lat, double lon) {
		return "( "+ toCoordString(lat) + " , " + toCoordString(lon) + " )" ;
	}
	
	public static String secondsToETA( long secondsTotal ) {
		long hours   = TimeUnit.SECONDS.toHours(secondsTotal);
		long minutes = TimeUnit.SECONDS.toMinutes(secondsTotal) - TimeUnit.HOURS.toMinutes( TimeUnit.SECONDS.toHours(secondsTotal) );
		long seconds = secondsTotal                             - TimeUnit.MINUTES.toSeconds( TimeUnit.SECONDS.toMinutes(secondsTotal) );
		String str;
		if ( hours > 0 ) {
			str = String.format("%02d:%02d:%02ds", hours, minutes, seconds );
		} else if ( minutes >0 ) {
			str = String.format("%02d:%02ds", minutes, seconds );
		} else {
			str = String.format("%02ds", seconds );
		}
		return str;
	}
	
	public static void copy(String string) {
		StringSelection stringSelection = new StringSelection(string);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
	}

	public static String toString(Gym gym, boolean details) throws LoginFailedException, RemoteServerException {
		String main = gym.getId() + " " + gym.getLatitude() + ", " + gym.getLongitude();
		if ( details ) {
			main += "\n  guard: " + String.format("%04d", gym.getGuardPokemonCp()) + " - " + toString( gym.getGuardPokemonId() );
			for (PokemonData pd : gym.getDefendingPokemon()) {
				main += "\n   > " + String.format("%04d ", pd.getCp()) + toString( pd.getPokemonId() );
			}
		}
		return main;
	}
	
	public static String toString(EggPokemon egg) throws LoginFailedException, RemoteServerException {
		return "isIncubate = " + egg.isIncubate() + " " + egg.getEggKmWalked() + "/" + egg.getEggKmWalkedTarget() + "km";
	}
	
	public static String toString(Pokestop ps, Map<?,?> mapPsLure) throws LoginFailedException, RemoteServerException {
		return ps.getId() + "\n( " + ps.getLatitude() + ", " + ps.getLongitude() + " ) "
				+ "\n  date = " + new Date( ps.getCooldownCompleteTimestampMs() )
				+ "\n  inRange = " + ps.inRange() + ", canLoot = " + ps.canLoot()
				+ "\n  hasLure = " + mapPsLure.get(ps);
	}
	
	public static String toString(PokemonId pokemonId) {
		return String.format( "%03d", pokemonId.getNumber() ) + " - " + pokemonId.getValueDescriptor().getName();
	}
	
	public static void printPokemonsCatchables(List<CatchablePokemon> listCatch, String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		System.out.println(prefix +"===== printPokemonsCatchables =====");
		System.out.println(prefix +"listCatch = " + (listCatch == null ? null : listCatch.size()));
		for (CatchablePokemon cp : listCatch) {
			System.out.println(prefix +"\t"+ TextUtils.toString(cp.getPokemonId())  + "  >  " + cp.getEncounterId() );
			System.out.println(prefix +"\t\texpire on " + new Date(cp.getExpirationTimestampMs()) );
		}
	}

	public static void printPokemonsNearby(List<NearbyPokemon> listNearby, String prefix) {
		if ( prefix == null ) {
			prefix = "";
		}
		
		System.out.println(prefix + "===== printPokemonsNearby =====");
		System.out.println(prefix +"listNearby = " + (listNearby == null ? null : listNearby.size()));
		for (NearbyPokemon np : listNearby) {
			System.out.println( prefix +"\t" + TextUtils.toString(np.getPokemonId()) + " , " + np.getDistanceInMeters() + "m away" );
		}
	}
	
	public static void printGyms(List<Gym> list, boolean details) throws LoginFailedException, RemoteServerException {
		System.out.println( "===== printGyms =====");
		System.out.println("listGyms = " + (list == null ? null : list.size()));
		for (Gym gym : list) {
			System.out.println( TextUtils.toString( gym, details ) );
		}
	}
	
	public static void printLootResult(PokestopLootResult loot) {
		MyLogger.LOGGER.info("[loot]xp awarded = " + loot.getExperience());
		for (ItemAward ia : loot.getItemsAwarded()) {
			MyLogger.LOGGER.info( "\t" +  ia.getItemId().name() + " x" + ia.getItemCount() );
		}
	}
	
	public static void printMine(List<Pokemon> pokemons, String[] args) throws LoginFailedException, RemoteServerException {
		System.out.println("===== printMine =====");
		System.out.println("pokemons = " + (pokemons == null ? null : pokemons.size()));
		
		// order
		if ( args.length > 1) {
			final String sortType = args[1];
			if ( "cp".equals( sortType ) ) {
				Collections.sort(pokemons, new Comparator<Pokemon>() {

					@Override
					public int compare(Pokemon p1, Pokemon p2) {
						return p2.getCp() - p1.getCp();
					}
				});
				
				for (Pokemon pokemon : pokemons) {
					System.out.println("  cp: " + String.format("%03d", pokemon.getCp()) + " " + TextUtils.toString( pokemon.getPokemonId() ));
				}
				
				return;
			}
			
			if ("evolve".equals(sortType)) {
				HashMap<Pokemon, Integer[]> map = new HashMap<>();
				for (Pokemon p : pokemons) {
					int toEvolve = p.getCandiesToEvolve();
					int candFactor;
					if ( toEvolve != 0 ) {
						candFactor = (int) (p.getCandy() * 100 / (float) toEvolve);
					} else {
						candFactor = -1;
					}
					map.put(p, new Integer[]{ candFactor, p.getCandy(), toEvolve  });
				}
				
				Collections.sort(pokemons, new Comparator<Pokemon>() {

					@Override
					public int compare(Pokemon p1, Pokemon p2) {
						int can2 = map.get( p2 )[0];
						int can1 = map.get( p1 )[0];
						return can2 - can1;
					}
				});
				
				for (Pokemon pokemon : pokemons) {
					Integer ints[] = map.get(pokemon);
					System.out.println("  factor: " + String.format("%03d", ints[0])
						+ " , " + String.format("%3d", ints[1])
						+ " / " + String.format("%3d", ints[2])
						+ "  > " + TextUtils.toString( pokemon.getPokemonId() ));
				}
				
				return;
			}
			
		}
		
		// default order:
		HashMap<PokemonId, List<Pokemon>> map = new HashMap<>();
		List<PokemonId> listPokeIds = new ArrayList<PokemonIdOuterClass.PokemonId>();
		List<Pokemon> list;
		for (Pokemon pokemon : pokemons) {
			list = map.get(pokemon.getPokemonId());
			if (list == null) {
				list = new ArrayList<Pokemon>();
				map.put(pokemon.getPokemonId(), list);
				listPokeIds.add( pokemon.getPokemonId() );
			}
			
			list.add( pokemon );
		}
		
		Collections.sort(listPokeIds, new Comparator<PokemonId>() {

			@Override
			public int compare(PokemonId p1, PokemonId p2) {
				return p1.getNumber() - p2.getNumber();
			}
		});
		
		
		for (PokemonId pid : listPokeIds) {
			list = map.get(pid);
			Collections.sort(list, new Comparator<Pokemon>() {
				
				@Override
				public int compare(Pokemon p1, Pokemon p2) {
					return p2.getCp() - p1.getCp();
				}
			});
			
			System.out.println( TextUtils.toString( pid ) + " x" + list.size() );
			System.out.println("  candy to evolve : " + list.get(0).getCandy() + " / " + list.get( 0 ).getCandiesToEvolve());
			int count = 1;
			for (Pokemon pokemon : list) {
				System.out.println("  #"+count+" cp =" + pokemon.getCp() + ", cpmulti = " + pokemon.getCpMultiplier() + ", id =" + pokemon.getId());
				count++;
			}
		}
		
	}
	
	public static void printEncounterProba(EncounterResult ep) {
		System.out.println("\tCP = " + ep.getPokemonData().getCp());
		CaptureProbability cp = ep.getCaptureProbability();
		for (int i = 0; i < cp.getCaptureProbabilityCount(); i++) {
			float captureProbability = cp.getCaptureProbability(i);
			ItemId pokeballType = cp.getPokeballType(i);
			
			System.out.println("\t" + pokeballType.name() + " > " + captureProbability);
		}
	}

	public static void printCatchResult(CatchResult result) {
		List<Integer> stardustList = result.getStardustList();
        List<Integer> candyList = result.getCandyList();
        List<ActivityTypeOuterClass.ActivityType> activityTypeList = result.getActivityTypeList();
        List<Integer> xpList = result.getXpList();

//        System.out.println( "candyList    = " + (candyList == null ? "null" : candyList.size()));
//        System.out.println( "stardustList = " + (stardustList == null ? "null" : stardustList.size()));
//        System.out.println( "activityTypeList = " + (activityTypeList == null ? "null" : activityTypeList.size()));
//        System.out.println( "xpList = " + (xpList == null ? "null" : xpList.size()));

        ActivityTypeOuterClass.ActivityType type;
        int totalXp = 0;
        int totalStardust = 0;
        int totalCandy = 0;
        int xp;

        for (int i = 0; i < activityTypeList.size(); i++) {
            type = activityTypeList.get(i);
            xp = xpList.get(i);

            totalXp += xp;
            totalStardust += stardustList.get(i);
            totalCandy += candyList.get(i);

            System.out.println(type.name() + " " + xp + " XP" );

        }
        System.out.println("-------------------------------");
        System.out.println( "Stardust: " + totalStardust );
        System.out.println( "Candy:    " + totalCandy );
        System.out.println( "TOTAL:    " + totalXp + " XP" );
	}
	
}
