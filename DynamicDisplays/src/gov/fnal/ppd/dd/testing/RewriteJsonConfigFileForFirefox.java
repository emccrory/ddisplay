package gov.fnal.ppd.dd.testing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.json.JSONObject;

public class RewriteJsonConfigFileForFirefox {
	private static boolean	debug	= Boolean.getBoolean("JSON.config.debug");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ( args.length != 5 ) {
			System.out.println("Usage: java RewriteJsonConfigFileForFirefox /path/to/mozilla/firefox/xulstore.json <xpos> <ypos> <width> <height>");
			System.exit(-1);
		}
		int requestedPositionX = Integer.parseInt(args[1]);
		int requestedPositionY = Integer.parseInt(args[2]);
		int requestedWidth = Integer.parseInt(args[3]);
		int requestedHeight = Integer.parseInt(args[4]);
		try {
			String fileString = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
			if (debug)
				System.out.println("Contents (Java 7 with character encoding ) : " + fileString);

			JSONObject obj = new JSONObject(fileString);
			if (debug)
				System.out.println("JSON Names: " + Arrays.toString(JSONObject.getNames(obj)));

			JSONObject next = obj.getJSONObject("chrome://browser/content/browser.xul");
			if (debug)
				System.out.println("JSON Names from chrome thing: " + Arrays.toString(JSONObject.getNames(next)));

			JSONObject mainWindow = next.getJSONObject("main-window");
			if (debug)
				System.out.println("JSON main thing: " + Arrays.toString(JSONObject.getNames(mainWindow)));

			if (debug) {
				String[] expectedFields = { "height", "width", "screenY", "sizemode", "screenX" };
				for (String f : expectedFields) {
					String result = mainWindow.getString(f);
					System.out.println(f + ": " + result);
				}
			}
			
			mainWindow.put("screenX", "" + requestedPositionX);
			mainWindow.put("screenY", "" + requestedPositionY);
			mainWindow.put("height", "" + requestedHeight);
			mainWindow.put("width", "" + requestedWidth);

			System.out.println(obj);

		} catch (Exception e) {
			System.err.println("Inpu file is " + args[0]);
			e.printStackTrace();
		}
	}

}
