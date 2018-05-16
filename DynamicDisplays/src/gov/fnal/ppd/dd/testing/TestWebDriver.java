package gov.fnal.ppd.dd.testing;

// This is not working because there is an unresolved dependency within this org.openqa.selenium.internal package, class Killable.  
// Web sites have instructions for resolving it, but I cannot figure out what they are saying (these solutions use Maven XML).

// May, 2018

//import org.openqa.selenium.WebDriver;
//import org.openqa.selenium.firefox.FirefoxDriver;
//
////comment the above line and uncomment below line to use Chrome
////import org.openqa.selenium.chrome.ChromeDriver;

public class TestWebDriver {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
//		// declaration and instantiation of objects/variables
//		System.setProperty("webdriver.firefox.marionette", "lib/geckodriver");
//		WebDriver driver = new FirefoxDriver();
//		// comment the above 2 lines and uncomment below 2 lines to use Chrome
//		// System.setProperty("webdriver.chrome.driver","G:\\chromedriver.exe");
//		// WebDriver driver = new ChromeDriver();
//
//		String baseUrl = "http://demo.guru99.com/test/newtours/";
//		String expectedTitle = "Welcome: Mercury Tours";
//		String actualTitle = "";
//
//		// launch Fire fox and direct it to the Base URL
//		driver.get(baseUrl);
//
//		// get the actual value of the title
//		actualTitle = driver.getTitle();
//
//		/*
//		 * compare the actual title of the page with the expected one and print the result as "Passed" or "Failed"
//		 */
//		if (actualTitle.contentEquals(expectedTitle)) {
//			System.out.println("Test Passed!");
//		} else {
//			System.out.println("Test Failed");
//		}
//
//		// close Fire fox
//		driver.close();
//
	}

}