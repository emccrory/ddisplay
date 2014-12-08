package gov.fnal.ppd.dd.util;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Generate a set of colors that are (in theory) as distinct as possible from each other.
 * 
 * This is a work in progress. When the number of colors get only slightly large (8), it is quite possible to sort the colors so
 * that there are very similar colors right next to each other. A final scheme for distinguishing colors will probably have to
 * include some words that one can associate with the color for unambiguous differences.
 * 
 * @author Elliott McCrory, Fermilab AD, 2012-14
 */
public class HSBColor extends Color {

	private static int			numColors			= 10;
	private static int			numBrightness		= 1;

	private static float		hueFactor			= 1.0f / numColors;

	private static final int	map[]				= { 2, 5, 4, 7, 0, 3, 6, 1 };

	private static float		saturation			= 0.8f;
	private static float		brightness			= 0.69f;

	private static final long	serialVersionUID	= -861252317070417732L;

	/**
	 * Sort the colors in this instance so that adjacent colors are as close as possible
	 */
	public static boolean		smooth				= true;

	/**
	 * @return The number of colors in this set
	 */
	public static int getNumColors() {
		return numColors;
	}

	private static final int rotate(int index) {
		if (smooth)
			return index;
		int base = (index & 0x7);
		return (index & 0xfffffff8) | map[base];
	}

	/**
	 * @param numColors
	 *            The number of colors for this set
	 */
	public static void setNumColors(final int numColors) {
		assert (numColors >= 1);
		if (numColors > 100) {
			// This kludge is not so great. It breaks the "numColors" attribute and only adds a little distinctiveness.
			HSBColor.numColors = (numColors / 2);
			hueFactor = 1.0f / (numColors / 2);
			numBrightness = 2;
		} else {
			HSBColor.numColors = numColors;
			hueFactor = 1.0f / numColors;
		}
	}

	/**
	 * @param index
	 *            The color number to create, based on the previously determined number of colors in the set
	 */
	public HSBColor(final int index) {
		super(numBrightness == 1 ? Color.HSBtoRGB(hueFactor * rotate(index), saturation, brightness) : Color.HSBtoRGB(hueFactor
				* rotate(index % numColors), saturation, (index > numColors ? 1.0f : 0.6f)));
	}

	/**
	 * @param args
	 *            One argument expected
	 */
	public static void main(final String[] args) {
		int nc = Integer.parseInt(args[0]);
		if (nc > 8)
			HSBColor.setNumColors(nc / 2);
		else
			HSBColor.setNumColors(nc);
		Box b = Box.createVerticalBox();
		int index = 0;
		for (int i = 0; i < nc; i++) {
			if (nc > 8)
				index = i / 2;

			Color hsb;
			if (nc > 8 && (i % 2) != 0)
				hsb = new HSBColor(index).brighter();
			else
				hsb = new HSBColor(index);
			System.out.println(i + ": " + hsb);
			int rgb = hsb.getRGB();
			JLabel lab = new JLabel((i < 10 ? "0" : "") + i + " " + hsb + " ["
					+ Integer.toHexString(0x00FFFFFF & rgb).toUpperCase() + "]");
			lab.setBorder(BorderFactory.createLineBorder(hsb, 20));
			b.add(lab);
		}
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(b);
		f.pack();
		f.setVisible(true);

	}
	
	/* from a website called "16 distinct colors", http://prestopnik.com/warfish/colors, I steal these 16 "distinct" colors
	 * #262626 -- Almost black
	 * #0000bd -- Blue
	 * #5d0016 -- Crimson/purple
	 * #f20019 -- Red
	 * #827800 -- green/gold
	 * #8f00c7 -- Purple
	 * #0086fe -- half-light blue
	 * #008000 -- green
	 * #00fefe -- Carolina blue
	 * #f368fe -- Magenta
	 * #fe8420 -- Brown/orange
	 * #70fe00 -- Lime
	 * #fefe00 -- Yellow
	 * #fed38b -- Beige
	 * #a0d681 -- pale green
	 * #ffffff -- white 
	 */
}
