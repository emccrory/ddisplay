package gov.fnal.ppd.dd.util.attic.jxbrowser;


/**
 * Using the commercial tool JxBrowser, implement this test web display on the same display as the channel selector
 * 
 * @author Elliott McCrory, Fermilab (2013)
 * @deprecated
 */
public class DisplayAsJxBrowser { // extends DisplayImpl {
//	private static int		x				= 0;
//	private static int		y				= 0;
//	private JFrame			f;
//	private Browser			browserPortal	= BrowserFactory.createBrowser(BrowserType.Mozilla);
//	private JLabel			dNum, cName, cNum, cDescr, errorMessage;
//	public static boolean	webTesting;
//
//	static {
//		BrowserType s = BrowserFactory.getDefaultBrowserType();
//		System.out.println("Default browser type: " + s.toString());
//	}
//
//	public DisplayAsJxBrowser(String ipName, int displayID, int screenNumber, String location, Color color, SignageType type) {
//		super(ipName, displayID, screenNumber, location, color, type);
//		initComponents();
//	}
//	
//	private void initComponents() {
//		JPanel p = new JPanel(new BorderLayout());
//		if (getContent() == null)
//			throw new IllegalArgumentException("No content defined!");
//
//		// p.setOpaque(true);
//		// p.setBackground(getPreferredHighlightColor());
//		dNum = new JLabel("Display " + getNumber() + " (" + getCategory() + ")");
//		dNum.setFont(new Font("SansSerif", Font.BOLD, 20));
//		cName = new JLabel((this.getContent() != null ? this.getContent().getName() + ": " + getContent().getURI().toString()
//				: "n/a"));
//		cName.setFont(new Font("SansSerif", Font.BOLD, 15));
//
//		errorMessage = new JLabel("");
//		errorMessage.setFont(new Font("SansSerif", Font.BOLD, 15));
//
//		browserPortal.navigate(getContent().getURI().toString());
//
//		Box b = Box.createVerticalBox();
//		b.add(dNum);
//		b.add(cName);
//		b.add(errorMessage);
//
//		if (this.getContent() instanceof Channel) {
//			cNum = new JLabel("Channel " + (this.getContent() != null ? ((Channel) this.getContent()).getNumber() : "n/a"));
//			b.add(cNum);
//			cDescr = new JLabel((this.getContent() != null ? ((Channel) this.getContent()).getDescription() : "n/a"));
//			cDescr.setFont(new Font("Serif", Font.ITALIC, 10));
//			b.add(cDescr);
//		}
//
//		if (webTesting) {
//			Box bb = Box.createVerticalBox();
//			bb.add(b);
//			bb.add(createWebBox());
//			bb.setBorder(BorderFactory.createLineBorder(Color.blue));
//			bb.setPreferredSize(new Dimension(820, 420));
//			p.add(bb, BorderLayout.CENTER);
//		} else
//			p.add(b, BorderLayout.CENTER);
//
//		JLabel color = new JLabel(("" + getPreferredHighlightColor()).replace(getPreferredHighlightColor().getClass()
//				.getCanonicalName(), ""));
//		color.setOpaque(true);
//		color.setBackground(getPreferredHighlightColor());
//		p.add(color, BorderLayout.SOUTH);
//
//		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(getPreferredHighlightColor(), 10),
//				BorderFactory.createEmptyBorder(5, 5, 5, 5)));
//
//		f = new JFrame("Place holder for Display " + getNumber());
//		if (webTesting)
//			f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//		else
//			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		f.setContentPane(p);
//		f.pack();
//
//		f.setLocation(x, y);
//		x += 180;
//		if (x > 900) {
//			x = 0;
//			y += 135;
//		}
//
//		f.setVisible(true);
//	}
//
//	private Component createWebBox() {
//		browserPortal.getComponent().setSize(600, 400);
//		JPanel p = new JPanel(new BorderLayout());
//		p.add(browserPortal.getComponent(), BorderLayout.CENTER);
//		p.setPreferredSize(new Dimension(600, 400));
//		p.setBorder(BorderFactory.createLineBorder(Color.black, 5));
//		return p;
//	}
//
//	protected void localSetContent() {
//		if (webTesting) {
//			browserPortal.navigate(getContent().getURI().toString());
//			System.out.println("Displaying '" + getContent().getURI().toString() + "'");
//		} else {
//			System.out.println("Not in the right mode of webTesting, '" + getContent().getURI().toString() + "'");
//		}
//		cName.setText(this.getContent().getName());
//		if (this.getContent() instanceof Channel) {
//			cNum.setText("Channel " + ((Channel) this.getContent()).getNumber() + ": " + getContent().getURI().toString());
//		} else {
//			cNum.setText("Generic Signage Content");
//		}
//		cDescr.setText((this.getContent()).getDescription());
//		errorMessage.setText("");
//	}
//
//	public void actionPerformed(ActionEvent e) {
//		super.actionPerformed(e);
//
//		localSetContent();
//	}
//
//	protected void error(String string) {
//		errorMessage.setText(string);
//	}
//
//	public final static void main(String[] args) {
//		webTesting = true;
//		ChannelCatalog list = ChannelCatalogFactory.getInstance();
//		DisplayAsJxBrowser display = new DisplayAsJxBrowser("IPName", 0, 0, "Location", Color.green, SignageType.XOC);
//
//		Map<String, SignageContent> pub = list.getPublicChannels();
//		for (String key : pub.keySet()) {
//			display.setContent(pub.get(key));
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//			}
//		}
//	}
}
