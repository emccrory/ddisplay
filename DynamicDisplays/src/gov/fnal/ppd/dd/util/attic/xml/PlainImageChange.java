package gov.fnal.ppd.dd.util.attic.xml;

import gov.fnal.ppd.dd.xml.EncodedCarrier;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PlainImageChange extends EncodedCarrier {

	private int	displayNum;

	@XmlElement
	public int getDisplayNum() {
		return displayNum;
	}

	@XmlElement
	public String getImageType() {
		return "jpg";
	}

	@XmlElement
	public String getImage() {
		try {
			URL url = new URL("http://mccrory.fnal.gov/images/MugOfElliott.JPG"); // This is a 10660
																					// byte image
			return wrapText(x(url, getImageType()), 100);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setDisplayNum(int d) {
		displayNum = d;
	}

	private String x(URL url, String type) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
		BufferedImage img = ImageIO.read(url);
		ImageIO.write(img, type, baos);
		baos.flush();

		// return Base64.encode(baos.toByteArray());
		return DatatypeConverter.printBase64Binary(baos.toByteArray());
	}

	private BufferedImage y(String base64String) throws IOException {
		byte [] bytearray = DatatypeConverter.parseBase64Binary(base64String);
		// byte [] bytearray = Base64.decode(base64String);

		return ImageIO.read(new ByteArrayInputStream(bytearray));
	}

	static String wrapText(String text, int len) {
		if (text == null)
			return new String();

		if (len <= 0 || text.length() <= len)
			return text;

		char[] chars = text.toCharArray();
		String ret = "";

		for (int i = 0; i < chars.length; i++) {
			ret += chars[i];

			if ((i % len) == (len - 1))
				ret += "\n";
		}

		return ret;
	}

}
