package main;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.ItemBag;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.fort.PokestopLootResult;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.api.map.pokemon.encounter.EncounterResult;
import com.pokegoapi.api.player.PlayerProfile.Currency;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.exceptions.InvalidCurrencyException;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Enums.PokemonIdOuterClass.PokemonId;
import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.UpgradePokemonResponseOuterClass.UpgradePokemonResponse.Result;
import command.ArgsFinder;
import command.ArgsTravel;
import travel.OnTravelListener;
import travel.TravelType;
import travel.Traveller;
import travel.Traveller.OnSpiralTravelListener;
import travel.Traveller.TravelStatus;
import util.AudioUtils;
import util.FileUtils;
import util.KeyboardUtils;
import util.TextUtils;
import util.TimeUtils;
import util.TimeUtils.WaitListener;


public class Main {
	
	public static final String BASE_URL = "http://maps.google.com/maps/api/staticmap?zoom=16&size=800x800&maptype=roadmap";
	
	private static List<Pokestop> listPokestops;
	private static Pokestop lastPokestop;

	private static HashMap<PokemonId,List<Pokemon>> mapMine;
	private static HashMap<Pokestop, Boolean> mapPsLure;
	private static Profile profile;
	private static List<Gym> listGyms;
	private static WaitListener waitListener = new WaitListener();

	public static void main(String[] args) {
		MyLogger.init();
		MyLogger.LOGGER.info( "START session" );
		
		try {
			startLoop();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		MyLogger.LOGGER.info( "END session" );
	}
	
	private static void startLoop() throws Exception {
		PokeBO.getDefault().doLogin();
		
		LinkedList<String> listCmds = new LinkedList<String>();
		listCmds.add( "l last" );
		
		String answer = null;
		do {
			try {
				// multiple cmds:
				if ( listCmds.size() == 0 ) {
					System.out.println("commands:");
					String[] cmds = new String[]{
							"refresh (r) ",
							"local (l) [now] [last]",
							"mine [cp]",
							"pokestops (ps) [r]",
							"inv (i) [rm] [index] [count]",
							"travel (t) type lat, lon [unique] [pause] [list]",
							"trans [number]",
							"egg",
							"prof",
							"scanner [step] [points]",
							"gym",
							"finder poke,id,s [max]",
							"powerup",
							"loginrefresh",
							"exit"
					};
					for (int i = 0; i < cmds.length; i++) {
						System.out.println( "\t"+ cmds[i] );
					}
					
					AudioUtils.beepSound();
					
					answer = KeyboardUtils.readString();
					if (answer.contains("|")) {
						String[] arrayCmds = answer.split("|");
						for (String cmd : arrayCmds) {
							listCmds.addLast( cmd.trim() );
						}
					}
				} else {
					answer = listCmds.pollFirst();
				}
				
				
				String[] args = answer.split(" ");
				
				switch (args[0]) {
					case "refresh":
					case "r":
						cmdRefresh(args);
						break;
						
					case "local":
					case "l":
						cmdLocal(args);
						break;
						
					case "mine":
						cmdMine(args, false);
						break;
						
					case "pokestops":
					case "ps":
						cmdPokestops(args);
						break;
						
					case "items":
					case "i":
						cmdItems(args);
						break;
						
					case "travel":
					case "t":
						cmdTravel(args);
						break;
						
					case "trans":
						cmdTransfer(args);
						break;
						
					case "egg":
						cmdEgg(args);
						break;
						
					case "prof":
						cmdProfile(args);
						break;
						
					case "scanner":
						new Thread(new Runnable() {
							
							@Override
							public void run() {
								try {
									cmdScanner(args);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}).start();
						break;
						
					case "gym":
						cmdGym(args);
						break;
	
					case "finder":
						cmdPokeFinder(args);
						break;
					
					case "powerup":
						cmdPowerUp(args);
						break;
						
					case "loginrefresh":
						cmdLoginRefresh();
						break;
						
					default:
						break;
				}
			} catch (LoginFailedException | RemoteServerException e) {
				e.printStackTrace();
			} catch(Exception e) {
				e.printStackTrace();
			}
			
		} while( !"exit".equals(answer) );
			
		KeyboardUtils.close();
	}

	private static void cmdLoginRefresh() throws Exception {
		PokeBO.getDefault().refreshLogin();
		cmdLocal( new String[]{ "l", "last" } );
	}

	private static void cmdPowerUp(String[] args) throws LoginFailedException, RemoteServerException, InvalidCurrencyException {
		List<Pokemon> list = chooseMineList(args);
		
		System.out.println( "choose by ID or INDEX (i2):" );
		String answer = KeyboardUtils.readString();
		
		
		Pokemon selected = null;
		if ( answer.contains("i") ) {
			int index = Integer.valueOf( answer.replace("i", "") );
			selected = list.get(index);
		} else {
			long id = Long.valueOf( answer );
			
			for (Pokemon p : list) {
				if ( id == p.getId() ) {
					selected = p;
					break;
				}
			}
		}
		
		if ( selected == null ) {
			return;
		}
		
		if ( profile == null ) {
			cmdProfile(new String[]{ "prof" });
		}
		
		System.out.println( "STARDUST = " + profile.player.getCurrency(Currency.STARDUST) );
		System.out.println( "CANDY    = " + selected.getCandy() );
		
		System.out.println( "\tselected.getCandyCostsForPowerup() = " + selected.getCandyCostsForPowerup() );
		System.out.println( "\tselected.getStardustCostsForPowerup() = " + selected.getStardustCostsForPowerup() );
		
		System.out.println( "power it up? (yes)" );
		if ( ! "yes".equals( KeyboardUtils.readString() ) ) {
			return;
		}
		
		Result powerUp = selected.powerUp();
		System.out.println( "powerupresult = " + powerUp.name());
	}

	private static void cmdGym(String[] args) throws Exception {
		System.out.println("===== cmdGym =====");
		if ( args.length >= 2 ) {
			if ( "r".equals( args[1] ) ) {
				listGyms = PokeBO.getDefault().getGyms();
			}
			
			if ("scanner".equals( args[1] )) {
				
				final PokeBO scanner = PokeBO.getScanner();
				
				if ( args.length > 2 ) {
					final double lat = Double.valueOf( args[2].replace(",", "") );
					final double lon = Double.valueOf( args[3] );
					scanner.setLocation(lat, lon, false, false);
				}
				
				TextUtils.printGyms( scanner.getGyms(), true);
				return;
			}
		}
		
		if ( listGyms == null ) {
			listGyms = PokeBO.getDefault().getGyms();
		}
		
		TextUtils.printGyms(listGyms, true);
		
	}

	private static void cmdScanner(String[] args) throws Exception {
		
		final List<CatchablePokemon> catchables = new ArrayList<CatchablePokemon>();
		
		double origin[] = PokeBO.getDefault().getLocation();
		final PokeBO scanner = PokeBO.getScanner();
		scanner.setLocation(origin[0], origin[1], false, false);
		
		float range = 30f;
		int pointsCounter = 20;
		if ( args.length > 1 ) {
			range = Float.valueOf( args[1] );
			if ( args.length > 2 ) {
				pointsCounter = Integer.valueOf( args[2] );
			}
		}
		
		Traveller.spiralTravel(range, pointsCounter, TravelType.DRIVE, origin, new OnSpiralTravelListener() {
			
			@Override
			public void onNewPoint(double[] latLng) {
				try {
					
					scanner.setLocation(latLng[0], latLng[1], false, false);
					
					scanner.listNearby();
					
					List<CatchablePokemon> list = scanner.listCatchables();
					List<CatchablePokemon> aux = new ArrayList<CatchablePokemon>();
					
					for (CatchablePokemon cpNew : list) {
						for (CatchablePokemon cp : catchables) {
							if ( cp.getEncounterId() ==  cpNew.getEncounterId() ) {
								aux.add(cp);
							}
						}
					}
					
					list.removeAll(aux);
					
					System.out.println( "[scanner] onNewPoint list = " + list.size() );
					
					if ( ! list.isEmpty() ) {
						catchables.addAll( list );
					}
					
				} catch( Exception e ) {
					e.printStackTrace();
				}
				
			}
			
			@Override
			public void onDoneTravel(double[] latLng) {
				System.err.println("[scanner] onDoneSpiralTravel");
				
//				String url = Main.BASE_URL;
				
				System.out.println( "[scanner] onDoneSpiralTravel list = " + catchables.size() );
				
				for (CatchablePokemon cp : catchables) {
					System.out.println( cp.getPokemonId().name()+"\t"+cp.getLatitude()+", "+cp.getLongitude() + "\t" +  new Date(cp.getExpirationTimestampMs()) );
//					url += "&markers=color:blue|label:"+cp.getPokemonId().name()+"|"+cp.getLatitude()+","+cp.getLongitude();
				}
//				TextUtils.copy( url );
//				System.err.println("[scanner] " +url );
				
			}
		});
		
	}

	private static void cmdProfile(String[] args) throws LoginFailedException, RemoteServerException, InvalidCurrencyException {
		System.out.println("===== cmdProfile =====");
		// prof [r]efresh
		if ( args.length > 1 ) {
			if ( "r".equals( args[1] ) ) {
				profile = null;
			}
		}
		
		if ( profile == null ) {
			profile = PokeBO.getDefault().getProfile();
		}
		
		System.out.println(profile.data.getUsername() + " lv = " +profile.stats.getLevel() );
		System.out.println("    xp = " +profile.stats.getExperience() );
		System.out.println("    km walked = " + profile.stats.getKmWalked());
		System.out.println("-------------------" );
		System.out.println("    stardust : " + profile.player.getCurrency(
				Currency.STARDUST) );
		System.out.println("===================" );
		
		
	}

	private static void cmdEgg(String[] splitted) throws LoginFailedException, RemoteServerException {
		System.out.println("===== cmdEgg =====");
		Set<EggPokemon> eggs = PokeBO.getDefault().getEggs();
		System.out.println("listItems = " + (eggs == null ? null : eggs.size()));
		
		for (EggPokemon eggPokemon : eggs) {
			System.out.println("\t"+TextUtils.toString( eggPokemon ));
		}
	}

	private static void cmdTravel(String[] args) {
		System.out.println("cmdTravel args = " + Arrays.toString( args ));
		
		ArgsTravel argsTravel = new ArgsTravel(args);
		
		final List<Integer> listPokeNumbers = new ArrayList<>(); 
		if (argsTravel.listOnly) {
			// TODO load list
		}
		
		Traveller.travelTo(PokeBO.getDefault(), argsTravel.latitude, argsTravel.longitude, argsTravel.travelType, new OnTravelListener() {

			@Override
			public boolean onTravelling(TravelStatus ts) {
				System.out.println("onTravelling progress = " + ts.progress  );
				try {
					List<NearbyPokemon> listNearby = PokeBO.getDefault().listNearby();
					TextUtils.printPokemonsNearby( listNearby, "\t\t" );
					
					List<CatchablePokemon> listCatch = PokeBO.getDefault().listCatchables();
					TextUtils.printPokemonsCatchables( listCatch, "\t\t" );
					if ( listCatch.size() > 0 ) {
						
						PokemonId pid;
						
						if ( argsTravel.uniqueOnly ) {
							if ( mapMine == null ) {
								cmdMine(new String[]{ "mine" }, false);
							}
							
							for (CatchablePokemon catchablePokemon : listCatch) {
								pid = catchablePokemon.getPokemonId();
								
								if ( mapMine.get( pid ) == null ) {
									AudioUtils.beepSound(2);
									System.out.println( "FOUND a " + TextUtils.toString(pid) );
									return false;
								}
							}
							
							return true;
						} else if ( argsTravel.listOnly ) {
							
							for (CatchablePokemon cp : listCatch) {
								pid = cp.getPokemonId();
								if ( listPokeNumbers.contains( pid.getNumber() ) ) {
									AudioUtils.beepSound(2);
									System.out.println( "FOUND a " + TextUtils.toString(pid) );
									return false;
								}
							}
							
						} else if ( argsTravel.pausable ) {
							AudioUtils.beepSound(2);
							System.out.println("stop travel? (y)");
							if ( "y".equals(KeyboardUtils.readString()) ) {
								return false;
							}
						}
						
					}
				} catch (LoginFailedException e) {
					e.printStackTrace();
				} catch (RemoteServerException e) {
					e.printStackTrace();
				}
				
				return true;
			}

			@Override
			public void onTravelDone() {
				System.out.println("onTravelDone");
				AudioUtils.beepSound(3);
			}
		});
	}

	private static void cmdItems(String[] args) throws LoginFailedException, RemoteServerException {
		System.out.println("===== cmdItems =====");
		List<Item> listItems = PokeBO.getDefault().listItems();
		System.out.println("listItems = " + (listItems == null ? null : listItems.size()));
		
		Collections.sort(listItems, new Comparator<Item>() {

			@Override
			public int compare(Item i1, Item i2) {
				ItemId id1 = i1.getItemId();
				ItemId id2 = i2.getItemId();
				// rare
				if ( PokeBO.isRare(id1) || PokeBO.isRare(id2) ) {
					if ( PokeBO.isRare(id1) ) {
						return -1;
					}
					
					if ( PokeBO.isRare(id2) ) {
						return 1;
					}
						
					return id1.getNumber() - id2.getNumber();
				}
				
				// pokeballs
				if ( PokeBO.isPokeball( id1 ) || PokeBO.isPokeball(id2)  )  {
					if ( PokeBO.isPokeball(id1) ) {
						return -1;
					}
					
					if ( PokeBO.isPokeball(id2) ) {
						return 1;
					}
						
					return id1.getNumber() - id2.getNumber();
				}
				
				return id1.getNumber() - id2.getNumber();
			}
		});
		
		int index = 0;
		int totalCount = 0;
		for (Item item : listItems) {
			System.out.println( "\t" + String.format("%02d", index) + "  x"
								+ String.format("%02d", item.getCount()) + "  " + item.getItemId().name() );
			index++;
			
			totalCount += item.getCount();
		}
		
		System.out.println( "   TOTAL count: " + totalCount );
		
		if ( args.length > 1 ) {
			if ( "rm".equals( args[1] ) ) {
				index = 0;
				if ( args.length > 2 ) {
					index = Integer.valueOf(args[2]);
				} else {
					System.out.println( "choose by index:" );
					index = Integer.valueOf( KeyboardUtils.readString() );
				}
				
				int count = 1;
				if ( args.length > 3 ) {
					count = Integer.valueOf(args[3]);
				} else {
					System.out.println( "how many to remove?" );
					count = Integer.valueOf( KeyboardUtils.readString() );
				}
				ItemId itemId = listItems.get(index).getItemId();
				System.out.println( "removing "+ count + " " + itemId.name() +". Are you sure? (yes)" );
				
				if ( "yes".equals( KeyboardUtils.readString() ) ) {
					PokeBO.getDefault().removeItem( itemId, count );
				}
				
			}
		}
	}

	private static void cmdPokestops(String args[]) throws Exception {
		System.out.println("===== cmdPokestops =====");
		// ps [r]fresh
		
		if (listPokestops == null) {
			refreshPokestops();
		}
		
		if ( args.length > 1 ) {
			if ( "last".equals( args[1] ) ) {
				if ( lastPokestop != null ) {
					checkAndLoot(lastPokestop);
					return;
				}
			} else if ( "r".equals( args[1] ) ) {
				refreshPokestops();
			}
		}
		
		String url = BASE_URL;
		for (int i = 0; i < listPokestops.size(); i++) {
			Pokestop ps = listPokestops.get(i);
			url +=  qParameter(ps, mapPsLure.get(ps));
		}
		
		System.out.println(url);
		
		for (Pokestop ps : listPokestops) {
			if ( checkAndLoot(ps) ) {
				break;
			}
		}
	}

	private static void refreshPokestops() throws LoginFailedException, RemoteServerException {
		listPokestops = PokeBO.getDefault().listPokestops();
		
		if ( mapPsLure == null ) {
			mapPsLure = new HashMap<Pokestop,Boolean>();
		}
		
		mapPsLure.clear();
		for (Pokestop pokestop : listPokestops) {
			mapPsLure.put(pokestop, pokestop.hasLure());
		}
		
		System.out.println("listPokestops = " + (listPokestops == null ? null : listPokestops.size()));
	}

	private static String qParameter(Pokestop ps, boolean hasLure) {
		// Hello%20world@53,-2
		// title@latitude,longitude
		//http://maps.google.com/maps/api/staticmap?center=Brooklyn+Bridge,New+York,NY&zoom=14&size=512x512&maptype=roadmap
		//&markers=color:blue|label:S|40.702147,-74.015794&markers=color:green|label:G|40.711614,-74.012318
		//		&markers=color:red|color:red|label:C|40.718217,-73.998284
		//color:blue|label:S|40.702147,-74.015794
		String color;
		if ( hasLure ) {
			color = "green";
		} else if ( ps.canLoot() ) {
			color = "blue";
		} else {
			color = "red";
		}
		
		return "&markers=color:"+color+"|label:"+ps.getId() + "|" + ps.getLatitude() + "," + ps.getLongitude();
	}

	private static boolean checkAndLoot(Pokestop ps) throws Exception {
		
		String qParameter = qParameter(ps,mapPsLure.get(ps));
		System.out.println("[pokestop] " + TextUtils.toString(ps, mapPsLure));
		System.out.println("\t" + BASE_URL + qParameter);
		
		if ( ps.inRange() ) {
			System.out.println(ps.getId() + " - what to do ? (y) (inloop) (lure)");
			AudioUtils.beepSound();
			String answer = KeyboardUtils.readString();
			
			if ( "y".equals( answer ) ) {
				if ( ps.canLoot() ){
					return cmdLoot(ps, false);
				}
			} else if ( "inloop".equals( answer ) ) {
				return cmdLoot(ps, true);
			} else if ( "lure".equals( answer ) ) {
				return cmdLure( ps );
			}
		}
		
		return false;
	}

	private static boolean cmdLure(Pokestop ps) throws Exception {
		
		System.out.println( "cmdLure ps.inRangeForLuredPokemon() = " + ps.inRangeForLuredPokemon() );
		if ( ps.inRangeForLuredPokemon() ) {
			
			if ( ! ps.hasLure() ) {
				// try to add lure
				Item lure = PokeBO.getDefault().getLure();
				System.out.println( "lure = " + lure );
				System.out.println( "ps.hasLure() = " + ps.hasLure() );
				if ( lure != null ) {
					ps.addModifier(lure.getItemId());
					System.out.println( "ps addLure = " + ps.hasLure() );
				}
			}
		}
		
		System.out.println( "start catchLoop? (y)" );
		String answer = KeyboardUtils.readString();
		if ( "y".equals( answer ) ) {
			while (true) {
				cmdRefresh(new String[]{"r"});
				TimeUtils.delay( (new Random().nextInt( 20 ) + 1) );
			}
		}
		
		return false;
	}

	private static boolean cmdLoot(Pokestop ps, boolean loop) throws Exception {
		lastPokestop = ps;
		
		System.out.println( "cmdLoot = " + ps.getId() );
		System.out.println( "\tps.canLoot() = " + ps.canLoot() );
		System.out.println( "\tps.inRange() = " + ps.inRange() );
		
		if ( ps.canLoot() ) {
			PokestopLootResult loot = ps.loot();
			MyLogger.LOGGER.info("loot.getResult() = " + loot.getResult().name());
			
			if ( !loot.wasSuccessful() ) {
				if ( POGOProtos.Networking.Responses.FortSearchResponseOuterClass.FortSearchResponse.Result.INVENTORY_FULL == loot.getResult() ) {
					AudioUtils.beepSound(4);
					throw new LootException(loot.getResult().name());
				}
				
				AudioUtils.beepSound(3);
				
				return false;
			}
			
			AudioUtils.beepSound();
			
			TextUtils.printLootResult( loot );
			
			MyLogger.LOGGER.info( "next loot available on: " + new Date( ps.getCooldownCompleteTimestampMs() ) );
		}
		
		if ( loop ) {
			long now = new Date().getTime();
			long diff = ps.getCooldownCompleteTimestampMs() - now;
			long wait =  diff +
					( new Random().nextInt(30) + 25 ) * 1000;
			System.out.println("waiting for pokestop available, " + TextUtils.secondsToETA( wait / 1000));
			System.out.println( "  next loot on: " + new Date( now + wait ) );
			
			TimeUtils.waitOrCancel(waitListener, wait, waitListener);
			
			if (waitListener.consumeInterrupted()) {
				throw new Exception( "user interrupted" );
			}
			
			return cmdLoot(ps, true);
		}
		
		return true;
		
	}

	private static void cmdMine(String args[], boolean mute) throws LoginFailedException, RemoteServerException {
		List<Pokemon> pokemons = PokeBO.getDefault().listMyPokemons();
		fillMineMap(pokemons);
		
		if (! mute)  {
			TextUtils.printMine( pokemons, args );
		}
	}
	
	private static void cmdTransfer(String args[]) throws LoginFailedException, RemoteServerException {
		System.out.println("cmdTransfer args = " + Arrays.toString( args ));

		List<Pokemon> list = chooseMineList(args);
		
		System.out.println( "choose by ID or INDEX (i2) , 'WEAK' or 'ALL':" );
		String answer = KeyboardUtils.readString();
		
		/// WEAK only
		if ("weak".equals(answer)) {
			Pokemon maxP = null;
			for (Pokemon pokemon : list) {
				if ( maxP == null || maxP.getCp() <= pokemon.getCp() ) {
					maxP = pokemon;
				}
			}
			
			System.out.println( "maxP    = " + maxP );
			System.out.println( "maxP.id = " + maxP.getId() );
			
			for (Pokemon pokemon : list) {
				if ( maxP.getId() != pokemon.getId() ) {
					TimeUtils.delay(8, 20);
					PokeBO.getDefault().transfer(pokemon);
				}
			}
			return;
		}
		
		/// ALL
		if ("all".equals(answer)) {
			System.out.println( "transfering all, sure? (yes)");
			if ( !"yes".equals( KeyboardUtils.readString() ) ) {
				return;
			}
			
			for (Pokemon pokemon : list) {
				TimeUtils.delay(8, 13);
				PokeBO.getDefault().transfer(pokemon);
			}
			return;
		}
		
		
		Long id = null;
		Integer index = null;
		
		if ( answer.startsWith("i") ) {
			index = Integer.valueOf( answer.replace("i", "") ) - 1;
		} else {
			id = Long.valueOf(answer);
		}
		
		Pokemon selected = null;
		if ( id != null ) {
			for (Pokemon pokemon : list) {
				if (pokemon.getId() == id) {
					selected = pokemon;
					break;
				}
			}
		} else {
			selected = list.get( index );
		}
		
		System.out.println( "selected = " + selected );
		if ( selected != null ) {
			PokeBO.getDefault().transfer(selected);
		}
		
		cmdMine( new String[]{ "mine" }, true );
		
	}

	private static List<Pokemon> chooseMineList(String[] args) throws LoginFailedException, RemoteServerException {
		final int number;
		if ( args.length > 1 ) {
			number = Integer.valueOf( args[1] );
		} else {
			cmdMine( new String[]{ "mine" }, false );
			System.out.println( "digit a number:" );
			number = Integer.valueOf( KeyboardUtils.readString() );
		}
		
		if ( mapMine == null ) {
			cmdMine( new String[]{ "mine" }, true );
		}
		
		List<Pokemon> list = null;
		for (PokemonId pid : mapMine.keySet()) {
			if (pid.getNumber() == number) {
				list = mapMine.get(pid);
				break;
			}
		}
		
		System.out.println( "list = " + list );
		if ( list != null ) {
			TextUtils.printMine(list, new String[]{"mine"});
		}
		return list;
	}

	private static void fillMineMap(List<Pokemon> pokemons) {
		if ( mapMine == null ) {
			mapMine =  new HashMap<PokemonId, List<Pokemon>>();
		}
		mapMine.clear();
		for (Pokemon p : pokemons) {
			List<Pokemon> list = mapMine.get(p.getPokemonId());
			if ( list == null ) {
				list = new ArrayList<Pokemon>();
				mapMine.put(p.getPokemonId(), list);
			}
			list.add(p);
		}
	}

	private static void cmdLocal(String[] args) throws Exception {
		// args[0] == local || l
		// l lat, lon
		// l lat lon sleepTime
		System.out.println( "cmdLocal args = " + Arrays.toString( args ) );
		
		String coords[];
		if ( args.length != 1 ) {
			if ( "now".equals( args[1] ) ) {
				System.out.println( "CURRENT location = " + PokeBO.getDefault().toStringLocation() );
				String url = BASE_URL + "&markers=color:blue|label:L|" + PokeBO.getDefault().getLocation()[0]+","+PokeBO.getDefault().getLocation()[1];
				TextUtils.copy( url );
				return;
			}
			
			if ("last".equals(args[1])) {
				double last[] = FileUtils.readLastLocation();
				if ( last != null ) {
					PokeBO.getDefault().setLocation(last[0], last[1], true, true);
					System.out.println( "loaded last location : " + TextUtils.toCoordString( last[0], last[1] ) );
					return;
				}
			}
		}
			
		System.out.println("choose a location (lat.tude, long.tude):");
		
		String answer = KeyboardUtils.readString();
		coords = answer.split(",");
		
		System.out.println( "cmdLocal coords = " + Arrays.toString( coords ) );
		
		double lat = Double.valueOf( coords[0] );
		double lon = Double.valueOf( coords[1] );
		
		PokeBO.getDefault().setLocation(lat, lon, false, true);
		
		if ( "r".equals(args[args.length - 1]) ) {
			cmdRefresh(new String[]{"r"});
		}
		
	}

	private static void cmdRefresh(String[] args) throws Exception {
		MyLogger.LOGGER.info("refresh pokemons on " + PokeBO.getDefault().toStringLocation());
		
		List<NearbyPokemon> listNearby = PokeBO.getDefault().listNearby();
		TextUtils.printPokemonsNearby( listNearby, null );
		
		List<CatchablePokemon> listCatch = PokeBO.getDefault().listCatchables();
		TextUtils.printPokemonsCatchables( listCatch, null );
		
		if ( args.length > 1 ) {
			return;
		}
		
		String answer;
		System.out.println( "POKEBALL COUNT = " + PokeBO.getDefault().getPokeBallCount() );
		
		for (CatchablePokemon cp : listCatch) {
			System.out.println("Catch - " + TextUtils.toString( cp.getPokemonId() ) + " ? (y) or (try) or (loseN)");
			
			AudioUtils.beepSound(2);
			
			answer = KeyboardUtils.readString();
			final boolean tryCatch = answer.contains("try");
			final boolean bestBall = answer.contains("best");
			final boolean loseSome = answer.startsWith("lose");
			
			
			if ( "y".equals(answer)
					|| tryCatch
					|| bestBall
					|| loseSome ) {
				EncounterResult encounterPokemon = cp.encounterPokemon();
				boolean wasSuccessful = encounterPokemon.wasSuccessful();
				MyLogger.LOGGER.info("EncounterResult, "+ TextUtils.toString( cp.getPokemonId() ) +" = " + encounterPokemon.getStatus().name() );
				TextUtils.printEncounterProba(encounterPokemon);
				
				if ( !wasSuccessful ) {
					break;
				}
				
				int loseN = 0;
				
				if (loseSome) {
					String strN = answer.replaceAll("lose|best", "").trim();
					loseN = Integer.valueOf( strN );
				}
				
				try {
					while ( catchPokemon( cp, encounterPokemon, tryCatch, bestBall, loseN ) ) {
						// keep trying
					}
				} catch (CatchException e) {
					e.printStackTrace();
				}
				
				break;
			}
		}
		
	}
	
	private static boolean catchPokemon(CatchablePokemon cp, EncounterResult encounterPokemon, boolean tryCatch, boolean best, int loseN) throws Exception {
		TimeUtils.delay(10, 24);
		
		System.out.println( "tryCatch = " + tryCatch );
		System.out.println( "\tPOKEBALL COUNT = " + PokeBO.getDefault().getPokeBallCount() );
		
		
		Pokeball pokeball = null;
		ItemId itemPokeball = null;
		
		ItemBag ib = PokeBO.getDefault().getItemBag();
		if ( ib.getItem( ItemId.ITEM_POKE_BALL ).getCount() > 0 ) {
			itemPokeball = ItemId.ITEM_POKE_BALL;
			pokeball = Pokeball.POKEBALL;
		} else if ( ib.getItem( ItemId.ITEM_GREAT_BALL ).getCount() > 0 ) {
			itemPokeball = ItemId.ITEM_GREAT_BALL;
			pokeball = Pokeball.GREATBALL;
		} else if( ib.getItem( ItemId.ITEM_ULTRA_BALL ).getCount() > 0  ) {
			itemPokeball = ItemId.ITEM_ULTRA_BALL;
			pokeball = Pokeball.ULTRABALL;
//		} else if( ib.getItem( ItemId.ITEM_MASTER_BALL ).getCount() > 0  ) {
//			itemPokeball = ItemId.ITEM_MASTER_BALL;
//			pokeball = Pokeball.MASTERBALL;
		} else {
			throw new Exception("No pokeballs left");
		}
		
		if (loseN > 0) {
			for (int i = 0; i < loseN; i++) {
				TimeUtils.delay(8, 19);
				PokeBO.getDefault().removePokeball( itemPokeball );
			}
		} else if (tryCatch) {
			float proba = encounterPokemon.getCaptureProbability().getCaptureProbability(0);
			float result = (new Random().nextInt(100) + 1) / 100f;
			System.out.println( "proba     = " + proba );
			System.out.println( "   result = " + result );
			if ( result > proba ) {
				MyLogger.LOGGER.info( "remove pokeball" );
				PokeBO.getDefault().removePokeball( itemPokeball );
				return true;
			}
		}
		
		CatchResult cr = null;
		
		if (best) {
			cr = PokeBO.getDefault().catchPokemon( cp, best );
		} else {
			cr = PokeBO.getDefault().catchPokemon( cp, pokeball );
		}
		
		System.out.println("\tcatchResult isFailed = " + cr.isFailed());
		MyLogger.LOGGER.info("\t"+cr.getStatus().toString());
		
		if ( !cr.isFailed() ) {
			// TODO ACTIVITY_POKEDEX_ENTRY_NEW
			MyLogger.LOGGER.info("\tCAPTURED: " + TextUtils.toString( cp.getPokemonId() ) );
			TextUtils.printCatchResult(cr);
			TimeUtils.delay( 10, 25 );
			
			AudioUtils.beepSound( 5 );
			return false;
		}
		
		throw new CatchException(cr);
	}
	
	private static void onFinderPause(ArgsFinder argFinder) {
		try {
			List<NearbyPokemon> listNearby = PokeBO.getDefault().listNearby();
			TextUtils.printPokemonsNearby(listNearby, "\t");

			List<CatchablePokemon> listCatch = PokeBO.getDefault().listCatchables();
			TextUtils.printPokemonsCatchables(listCatch, "\t");

			for (CatchablePokemon cp : listCatch) {
				System.out.println("[pokefinder] " + TextUtils.toString(cp.getPokemonId()));

				if (argFinder.numbers.contains(cp.getPokemonId().getNumber())) {

					EncounterResult encounterPokemon = cp.encounterPokemon();
					boolean wasSuccessful = encounterPokemon.wasSuccessful();
					MyLogger.LOGGER.info("EncounterResult, " + TextUtils.toString(cp.getPokemonId()) + " = "
							+ encounterPokemon.getStatus().name());
					TextUtils.printEncounterProba(encounterPokemon);

					if (!wasSuccessful) {
						continue;
					}

					catchPokemon(cp, encounterPokemon, false, true, 0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void cmdPokeFinder(String args[]) throws Exception {
		System.out.println("===== cmdPokeFinder =====");
		
		final ArgsFinder argFinder = new ArgsFinder( args );
		List<Double[]> points = FileUtils.readFinderLocations();
		for (int i = 0; i < argFinder.maxCircles; i++) {
			for (Double[] doubles : points) {
				Traveller.travelTo(PokeBO.getDefault(), doubles[0], doubles[1], argFinder.travelType, new OnTravelListener() {
					
					@Override
					public boolean onTravelling(TravelStatus ts) {
						System.out.println("[pokefinder] onTravelling " + PokeBO.getDefault().toStringLocation());
						onFinderPause( argFinder );
						return true;
					}
					
					@Override
					public void onTravelDone() {
						System.out.println("[pokefinder] onTravelDone " + PokeBO.getDefault().toStringLocation());
						onFinderPause( argFinder );
					}
				});
			}
		}
		
	}
	
}
