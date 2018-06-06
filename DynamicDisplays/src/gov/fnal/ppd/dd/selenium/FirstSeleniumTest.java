package gov.fnal.ppd.dd.selenium;

import gov.fnal.ppd.dd.selenium.config.PropertiesFile;

import java.awt.Rectangle;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class FirstSeleniumTest {
	private static String				browser;
	private static WebDriver			driver;
	private static JavascriptExecutor	jse;

	private static String				colorCode		= "33ff33";
	private static Rectangle			bounds			= new Rectangle(1920, 1080);

	private static String				baseLocation	= System.getProperty("user.dir");

	public static void main(String[] args) {
		System.out.println("set browser");
		setBrowser();
		System.out.println("browser config");
		setBrowserConfig();
		System.out.println("run test");
		runTest();
	}

	public static void setBrowser() {
		browser = PropertiesFile.getProperty("browser");
	}

	public static void setBrowserConfig() {
		if (browser.contains("Firefox")) {
			System.setProperty("webdriver.gecko.driver", baseLocation + "/lib/selenium/geckodriver"); // add .exe if Windows
			System.out.println("start firefoxdriver");
			driver = new FirefoxDriver();
			System.out.println("returned from creating firefoxdriver");
		} else if (browser.contains("Chrome")) {
			System.setProperty("webdriver.chrome.driver", baseLocation + "/lib/selenium/chromedriver"); // add .exe if Windows
			driver = new ChromeDriver();
		}
		System.out.println("create link to the JavascriptExecutor");
		jse = (JavascriptExecutor) driver;
	}

	public static void runTest() {
		long sleep = Long.parseLong(PropertiesFile.getProperty("sleep"));

		// Set the Script Timeout to 100 milliseconds
		System.out.println("set timeout");
		driver.manage().timeouts().setScriptTimeout(100, TimeUnit.MILLISECONDS);

		System.out.println("first url");
		driver.get("http://dynamicdisplays.fnal.gov/border7.php");
		System.out.println("fullscreen");
		driver.manage().window().fullscreen();
		catchSleep(100);
		driver.navigate().refresh();

		// Change to a new URL
		setURL("http://www-bd.fnal.gov/notifyservlet/www?project=HD&refresh=on&infolinks=none");
		catchSleep(sleep);

		// Change to a new URL
		setURL("https://www.youtube.com/embed/sBDCIOERPNw?autoplay=1&cc_load_policy=1");
		catchSleep(sleep);

		// Do an "Identify"
		identify();
		catchSleep(15000);
		removeIdentify();
		catchSleep(sleep);

		// All done.
		driver.quit();
	}

	public static void send(String command) {
		WebElement element = driver.findElement(By.id("iframe"));
		command += "return 1;\n";
		jse.executeScript(command, new Object[] { element });
	}

	public static void setURL(String url) {
		send("document.getElementById('iframe').src='" + url + "';\n");
	}

	public static void identify() {
		String s = "document.getElementById('numeral').style.opacity=0.8;\n";
		s += "document.getElementById('numeral').style.font='bold 750px sans-serif';\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 0 0 18px black, 16px 16px 2px #"
				+ colorCode + "';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'background-color: #" + colorCode
				+ "; padding:0; margin: 100;' );\n";

		s += "document.getElementById('iframe').style.width=" + (bounds.width - 220) + ";\n";
		s += "document.getElementById('iframe').style.height=" + (bounds.height - 208) + ";\n";

		s += "document.getElementById('colorName').innerHTML = '42 #" + colorCode + "';\n";
		send(s);
	}

	private static void removeIdentify() {
		String s = "document.getElementById('numeral').style.opacity=0.4;\n";
		s += "document.getElementById('numeral').style.font='bold 120px sans-serif';\n";
		s += "document.getElementsByTagName('body')[0].setAttribute('style', 'padding:0; margin: 0;');\n";
		s += "document.getElementById('numeral').style.textShadow='0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 0 0 4px black, 6px 6px 2px #"
				+ colorCode + "';\n";
		s += "document.getElementById('iframe').style.width=" + (bounds.width - 4) + ";\n";
		s += "document.getElementById('iframe').style.height=" + (bounds.height - 6) + ";\n";

		s += "document.getElementById('colorName').innerHTML = '';\n";
		send(s);
	}

	public static void catchSleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
