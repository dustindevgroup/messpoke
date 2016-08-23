package util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class FileUtils {
	
	private static final String FILE_LAST_LOCATION = "lastlocation.txt";
	private static final String FILE_TOKENS = "tokens.json";

	private static Path getDataPath(String filename) {
		return Paths.get( "data" , filename);
	}
	
	public static void writeLastLocation(double latitude, double longitude) {
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream( getDataPath(FILE_LAST_LOCATION).toFile() ), "utf-8"))) {
			writer.write(latitude + ", " + longitude);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static double[] readLastLocation() {
		try {
			byte[] encoded = Files.readAllBytes( getDataPath(FILE_LAST_LOCATION) );
			String str[] = new String(encoded, "utf-8").split(",");
			return new double[] { Double.valueOf( str[0] ), Double.valueOf( str[1] ) };
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<Double[]> readFinderLocations() throws IOException {
		List<Double[]> list = new ArrayList<>();
		List<String> lines = Files.readAllLines( getDataPath( "finder.txt" ), Charset.forName( "utf-8" ));

		for (String line : lines) {
			String str[] = line.replaceAll(" ",	 "").split(",");
			double lat = Double.valueOf( str[0] );
			double lon = Double.valueOf( str[1] );
			list.add( new Double[]{ lat, lon } );
		}
		
		return list;
	}
	
	public static TokenJson readTokens() {
		TokenJson tokenJson = new TokenJson();
		
		try {
			byte[] encoded = Files.readAllBytes( getDataPath(FILE_TOKENS) );
			JSONObject jsonObj = new JSONObject( new String(encoded, "utf-8") );
			
			tokenJson.setDefaultToken( jsonObj.optString("defaultToken", null) );
			tokenJson.setScannerToken( jsonObj.optString("scannerToken", null) );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return tokenJson;
	}
	
	public static void writeTokens(TokenJson tokenJson) {
		try (Writer writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream( getDataPath(FILE_TOKENS).toFile() ), "utf-8"))) {
			
			JSONObject jsonObj = new JSONObject(tokenJson);
			writer.write( jsonObj.toString() );
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
