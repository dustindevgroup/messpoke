package main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.api.gym.Gym;
import com.pokegoapi.api.inventory.EggIncubator;
import com.pokegoapi.api.inventory.Item;
import com.pokegoapi.api.inventory.ItemBag;
import com.pokegoapi.api.inventory.Pokeball;
import com.pokegoapi.api.map.fort.Pokestop;
import com.pokegoapi.api.map.pokemon.CatchResult;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;
import com.pokegoapi.api.map.pokemon.NearbyPokemon;
import com.pokegoapi.api.pokemon.EggPokemon;
import com.pokegoapi.api.pokemon.Pokemon;
import com.pokegoapi.api.settings.CatchOptions;
import com.pokegoapi.auth.GoogleUserCredentialProvider;
import com.pokegoapi.exceptions.LoginFailedException;
import com.pokegoapi.exceptions.NoSuchItemException;
import com.pokegoapi.exceptions.RemoteServerException;

import POGOProtos.Inventory.Item.ItemIdOuterClass.ItemId;
import POGOProtos.Networking.Responses.ReleasePokemonResponseOuterClass.ReleasePokemonResponse.Result;
import okhttp3.OkHttpClient;
import travel.Traveller;
import util.FileUtils;
import util.KeyboardUtils;
import util.TextUtils;
import util.TokenJson;


public class PokeBO {

	private String tokenRefresh = null;
	private PokemonGo GO;
	private double latitude;
	private double longitude;
	private boolean isLogged = false;
	private boolean isScanner;
	
	private static PokeBO BO_DEFAULT;
	private static PokeBO BO_SCANNER;
	private static TokenJson JSON_TOKEN;
	
	static {
		JSON_TOKEN = FileUtils.readTokens();
	}
	
	private PokeBO(String refresh, boolean isScanner) {
		this.tokenRefresh = refresh;
		this.isScanner = isScanner;
	}
	
	public static PokeBO getDefault() {
		if ( BO_DEFAULT == null ) {
			BO_DEFAULT = new PokeBO( JSON_TOKEN.getDefaultToken(), false );
		}
		
		return BO_DEFAULT;
	}
	
	public static PokeBO getScanner() throws Exception {
		if ( BO_SCANNER == null ) {
			BO_SCANNER = new PokeBO( JSON_TOKEN.getScannerToken(), true );
			BO_SCANNER.doLogin();
		}
		
		return BO_SCANNER;
	}
	
	public void doLogin() throws Exception {
		if (isLogged) {
			return;
		}
		
		OkHttpClient httpClient = new OkHttpClient();

		/** 
		* Google: 
		* You will need to redirect your user to GoogleUserCredentialProvider.LOGIN_URL
		* Afer this, the user must signin on google and get the token that will be show to him.
		* This token will need to be put as argument to login.
		*/
		GoogleUserCredentialProvider provider = new GoogleUserCredentialProvider(httpClient);
		if (tokenRefresh != null) {
			provider = new GoogleUserCredentialProvider( httpClient, tokenRefresh );
		} else {
			// in this url, you will get a code for the google account that is
			// logged
			System.out.println("Please go to :\n" + GoogleUserCredentialProvider.LOGIN_URL);
			System.out.println("Enter code:");

			// Ask the user to enter it in the standart input
			String access = KeyboardUtils.readString();

			// we should be able to login with this token
			provider.login(access);

			this.tokenRefresh = provider.getRefreshToken();
			System.out.println("REFRESH_TOKEN = " + this.tokenRefresh);

			if (this.tokenRefresh == null) {
				throw new Exception("login failed, try again");
			}

			if (this.isScanner) {
				JSON_TOKEN.setScannerToken(this.tokenRefresh);
			} else {
				JSON_TOKEN.setDefaultToken(this.tokenRefresh);
			}

			FileUtils.writeTokens(JSON_TOKEN);
		}

		GO = new PokemonGo(provider, httpClient);
		isLogged = true;
	}
	
	public List<NearbyPokemon> listNearby() throws LoginFailedException, RemoteServerException {
		return GO.getMap().getNearbyPokemon();
	}

	public void setLocation(double latitude, double longitude, boolean fakeMoves, boolean write) {
		if ( fakeMoves ) {
			double coords[] = Traveller.goCloserTo(latitude, longitude);
			latitude = coords[0];
			longitude = coords[1];
		}
		
		MyLogger.LOGGER.warn("setLocation: " + latitude + " , " + longitude);
		
		this.latitude = latitude;
		this.longitude = longitude;
		
		GO.setLocation(latitude, longitude, 1.0);
		if ( write ) {
			FileUtils.writeLastLocation( latitude, longitude );
		}
	}

	public List<CatchablePokemon> listCatchables() throws LoginFailedException, RemoteServerException {
		List<CatchablePokemon> catchablePokemon = GO.getMap().getCatchablePokemon();
		Collections.sort(catchablePokemon, new Comparator<CatchablePokemon>() {

			@Override
			public int compare(CatchablePokemon cp1, CatchablePokemon cp2) {
				return (int) (cp1.getExpirationTimestampMs() - cp2.getExpirationTimestampMs());
			}
		});
		return catchablePokemon;
	}

	public String toStringLocation() {
		return TextUtils.toCoordString( latitude, longitude );
	}
	
	public  double[] getLocation() {
		return new double[] { latitude, longitude };
	}
	
	public  List<Pokemon> listMyPokemons() throws LoginFailedException, RemoteServerException {
		return GO.getInventories().getPokebank().getPokemons();
	}
	
	public  List<Pokestop> listPokestops() throws LoginFailedException, RemoteServerException {
		return new ArrayList<Pokestop>(GO.getMap().getMapObjects().getPokestops());
	}

	public  List<Item> listItems() throws LoginFailedException, RemoteServerException {
		return new ArrayList<Item>( GO.getInventories().getItemBag().getItems() );
	}

	public  void removePokeball(ItemId pokeball) throws RemoteServerException, LoginFailedException {
		System.out.println("removePokeball : " + pokeball.name() + " -1");
		GO.getInventories().getItemBag().removeItem(pokeball, 1);
	}

	public String getPokeBallCount() throws LoginFailedException, RemoteServerException {
		ItemBag itemBag = GO.getInventories().getItemBag();
		return itemBag.getItem( ItemId.ITEM_POKE_BALL ).getCount() + ", "
				+ itemBag.getItem( ItemId.ITEM_GREAT_BALL ).getCount() + ", "
				+ itemBag.getItem( ItemId.ITEM_ULTRA_BALL ).getCount() + ", "
				+itemBag.getItem( ItemId.ITEM_MASTER_BALL ).getCount();
	}
	
	public ItemBag getItemBag() throws LoginFailedException, RemoteServerException {
		return GO.getInventories().getItemBag();
	}

	public  Item getLure() throws LoginFailedException, RemoteServerException {
		return GO.getInventories().getItemBag().getItem(ItemId.ITEM_TROY_DISK);
	}

	public  void transfer(Pokemon pokemon) throws LoginFailedException, RemoteServerException {
		MyLogger.LOGGER.info( "[transfer] pokemon = " + TextUtils.toString( pokemon.getPokemonId() ) );
		MyLogger.LOGGER.info( "  cp = "+ pokemon.getCp()+ " , id= " + pokemon.getId() );
		Result transferPokemon = pokemon.transferPokemon();
		MyLogger.LOGGER.info( "    result.name() = " + transferPokemon.name() );
	}

	public  Set<EggPokemon> getEggs() throws LoginFailedException, RemoteServerException {
		return GO.getInventories().getHatchery().getEggs();
	}
	
	public  List<EggIncubator> getIncubators() throws LoginFailedException, RemoteServerException {
		return GO.getInventories().getIncubators();
	}

	public Profile getProfile() throws LoginFailedException, RemoteServerException {
		Profile p = new Profile();
		
		p.player = GO.getPlayerProfile();
		p.stats = p.player.getStats();
		p.data = p.player.getPlayerData();
		
		return p;
	}
	
	public List<Gym> getGyms() throws LoginFailedException, RemoteServerException {
		return GO.getMap().getGyms();
	}

	public CatchResult catchPokemon(CatchablePokemon cp, boolean best) throws LoginFailedException, RemoteServerException, NoSuchItemException {
		CatchOptions co = new CatchOptions(GO);
		co.useBestBall(best);
		return cp.catchPokemon(co);
	}

	public CatchResult catchPokemon(CatchablePokemon cp, Pokeball pokeball) throws LoginFailedException, RemoteServerException, NoSuchItemException {
		CatchOptions co = new CatchOptions(GO);
		co.usePokeball(pokeball);
		return cp.catchPokemon(co);
	}

	public void removeItem(ItemId itemId, int count) throws RemoteServerException, LoginFailedException {
		GO.getInventories().getItemBag().removeItem(itemId, count);
	}

	public static boolean isRare(ItemId itemId) {
		return ItemId.ITEM_TROY_DISK == itemId
				|| ItemId.ITEM_INCENSE_COOL == itemId
				|| ItemId.ITEM_INCENSE_FLORAL == itemId
				|| ItemId.ITEM_INCENSE_ORDINARY == itemId
				|| ItemId.ITEM_INCENSE_SPICY == itemId;
	}
	
	public static boolean isPokeball(ItemId itemId) {
		return ItemId.ITEM_POKE_BALL == itemId
				|| ItemId.ITEM_GREAT_BALL == itemId
				|| ItemId.ITEM_ULTRA_BALL == itemId
				|| ItemId.ITEM_MASTER_BALL == itemId;
	}

}
