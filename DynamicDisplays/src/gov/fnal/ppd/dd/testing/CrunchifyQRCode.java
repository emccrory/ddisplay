package gov.fnal.ppd.dd.testing;

/**
 * <p>
 * Create QR codes on the fly. UNUSED and UNTESTED
 * </p>
 * 
 * <p>
 * The purpose of this test was to try and implement a way to show the user of a display in the system what URL was showing. This
 * would be useful for some content.
 * </p>
 * 
 * <p>
 * *TODO*: Figure out a way to use this on the ChannelSelector GUI. Maybe we can add it to the channel button, or maybe there can be
 * a way to launch a new dialog box that shows this (that is, "Please give me the URL as a QR code for a channel button."). Or maybe
 * it can be part of the Display screen in some special situation (e.g., "Identify")
 * </p>
 * 
 * @author Crunchify.com Retrieved: 4/10/2015
 */

public class CrunchifyQRCode {

	private static int sizeOfQRCode = 200;

	/**
	 * @return the current size of the QR code that will be rendered
	 */
	public static int getSizeOfQRCode() {
		return sizeOfQRCode;
	}

	/**
	 * @param sizeOfQRCode
	 */
	public static void setSizeOfQRCode(final int sizeOfQRCode) {
		if (sizeOfQRCode > 0)
			CrunchifyQRCode.sizeOfQRCode = sizeOfQRCode;
	}

	// Tutorial: http://zxing.org/w/docs/javadoc/index.html

	/**
	 * test program
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		// writeQRCode("http://Crunchify.com/", "CrunchifyQR.png");
		// String url = "http://mccrory.fnal.gov/XOC/kenburns/portfolioDisplay.php?exp=DUNE-LBNF";
		// try {
		// BufferedImage image = createQRCodeImage(url);
		// JFrame f = new JFrame("test");
		// f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//
		// ImageIcon imageIcon = new ImageIcon(image);
		// JLabel jLabel = new JLabel();
		// jLabel.setIcon(imageIcon);
		// JLabel caption = new JLabel(url);
		// Box b = Box.createVerticalBox();
		// b.add(jLabel);
		// b.add(caption);
		// f.getContentPane().add(b, BorderLayout.CENTER);
		//
		// f.pack();
		// f.setVisible(true);
		//
		// } catch (WriterException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Create a QR Code for this code text and write it to a PNG file
	 * 
	 * @param myCodeText
	 *            The text to encode into the QR Code
	 * @param filePath
	 *            The path of the file to write. The extension ".png" is assumed
	 */
	public static void writeQRCode(final String myCodeText, final String filePath) {
		// String fileType = "png";
		// File myFile = new File(filePath);
		// try {
		//
		// RenderedImage image = createQRCodeImage(myCodeText);
		//
		// ImageIO.write(image, fileType, myFile);
		// } catch (WriterException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// System.out.println("\n\nYou have successfully created QR Code: " + myFile.getAbsolutePath());
	}

	/**
	 * Create a QR Code for this code text and return an image suitable for viewing
	 * 
	 * @param myCodeText
	 *            The text to encode into the QR Code
	 * @return the image of the QR Code
	 * @throws WriterException
	 *             When the creation of the QR code fails.
	 */
	// public static BufferedImage createQRCodeImage(final String myCodeText) throws WriterException {
	// Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
	// hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
	// QRCodeWriter qrCodeWriter = new QRCodeWriter();
	// BitMatrix byteMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, sizeOfQRCode, sizeOfQRCode, hintMap);
	// int CrunchifyWidth = byteMatrix.getWidth();
	// BufferedImage image = new BufferedImage(CrunchifyWidth, CrunchifyWidth, BufferedImage.TYPE_INT_RGB);
	// image.createGraphics();
	//
	// Graphics2D graphics = (Graphics2D) image.getGraphics();
	// graphics.setColor(Color.WHITE);
	// graphics.fillRect(0, 0, CrunchifyWidth, CrunchifyWidth);
	// graphics.setColor(Color.BLACK);
	//
	// for (int i = 0; i < CrunchifyWidth; i++) {
	// for (int j = 0; j < CrunchifyWidth; j++) {
	// if (byteMatrix.get(i, j)) {
	// graphics.fillRect(i, j, 1, 1);
	// }
	// }
	// }
	//
	// return image;
	// }
}