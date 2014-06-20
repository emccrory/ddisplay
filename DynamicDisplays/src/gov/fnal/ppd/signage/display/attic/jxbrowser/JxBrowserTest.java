package gov.fnal.ppd.signage.display.attic.jxbrowser;


public class JxBrowserTest {
//	private Browser			browser;
//	private Color			borderColor		= new Color(200, 255, 200);
//	private AtomicInteger	splashCountdown	= new AtomicInteger(0);
//	private Thread			borderThread;
//	protected String		lastURL			= "http://www.fnal.gov";
//	protected String		nowShowing;
//	private int				screenNumber;
//
//	public JxBrowserTest(int screenNumber, int xOffset, int yOffset) {
//		this.screenNumber = screenNumber;
//
//		if (screenNumber < 0)
//			screenNumber = 0; // Modify the argument, not the attribute
//
//		System.out.println("Operating system is '" + OS + "'; browser type is '" + getBrowserType() + "'");
//
//		browser = BrowserFactory.createBrowser(getBrowserType());
//
//		browser.navigate(lastURL);
//
//		JPanel panel = new JPanel(new BorderLayout());
//		panel.setBorder(BorderFactory.createLineBorder(borderColor, 5));
//		panel.add(browser.getComponent(), BorderLayout.CENTER);
//
//		JFrame frame = new JFrame("JxBrowser Test");
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.add(panel, BorderLayout.CENTER);
//
//		frame.setUndecorated(true);
//		frame.setLocationRelativeTo(null);
//
//		Rectangle bounds = ScreenLayoutInterpreter.getBounds(screenNumber);
//		frame.setSize(bounds.width, bounds.height);
//		frame.setLocation((int) bounds.getX(), (int) bounds.getY());
//
//		frame.setVisible(true);
//
//		identifyMe();
//	}
//
//	/**
//	 * Put up a colorful splash screen that identifies this Display
//	 * 
//	 * Copied and modified from ADynamicDisplay.java
//	 */
//	private void identifyMe() {
//		String colorString = Integer.toHexString(getPreferredHighlightColor().getRGB()).substring(2);
//		// String style1 = "font-size:500%;font-weight:bold;background-color:white;border-style:solid;border-width:25px;border-color:white;";
//		String style2 = "font-size:350%;background-color:white;border-style:solid;border-width:25px;border-color:white;";
//		String style3 = "font-size:150%;background-color:white;border-style:solid;border-width:25px;border-color:white;";
//
//		String cntt = "<html><body style='margin:200;background-color:#" + colorString + ";'>\n" + "<p align='center' " + "style='"
//				+ style2 + "'>This is a test on architecture='" + OS + "'<br>";
//
//		cntt += " Screen number " + screenNumber;
//
//		cntt += "<p style='" + style3 + "'>Next URL='" + lastURL + "' will be shown ";
//
//		final String endContent = "</p></body></html>";
//
//		// nowShowing = "Self identification splash screen";
//
//		System.out.println(cntt);
//		splashCountdown.set(10);
//		final String content = cntt;
//		borderThread = new Thread() {
//			public void run() {
//				// Turn off the border in a while
//				try {
//					while (splashCountdown.decrementAndGet() > 0) {
//						synchronized (browser) {
//							if (splashCountdown.get() != 1)
//								browser.setContent(content + "in about " + splashCountdown + " seconds.</em>" + endContent);
//							else
//								browser.setContent(content + "in a second.</em>" + endContent);
//						}
//						sleep(999);
//					}
//					browser.setContent(content + "NOW!</em>" + endContent);
//					sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//
//				synchronized (browser) {
//					browser.navigate(lastURL);
//				}
//				nowShowing = lastURL;
//				// displayHasChanged();
//			}
//
//		};
//		borderThread.start();
//		// updateMyStatus();
//	}
//
//	private Color getPreferredHighlightColor() {
//		return borderColor;
//	}
//
//	public static void main(String[] args) {
//		int screen = 0;
//		int xOff = 0, yOff = 0;
//		if (args.length == 1)
//			screen = Integer.parseInt(args[0]);
//		if (args.length == 2) {
//			screen = -1;
//			xOff = Integer.parseInt(args[0]);
//			yOff = Integer.parseInt(args[1]);
//		}
//		new JxBrowserTest(screen, xOff, yOff);
//	}
}
