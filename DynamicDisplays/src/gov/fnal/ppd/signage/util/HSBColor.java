package gov.fnal.ppd.signage.util;

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

	public static float			saturation			= 0.8f;
	public static float			brightness			= 0.7f;

	private static final long	serialVersionUID	= -861252317070417732L;

	/**
	 * Sort the colors in this instance so that adjacent colors are as close as possible
	 */
	public static boolean		smooth				= true;

	public static int getNumColors() {
		return numColors;
	}

	private static final int rotate(int index) {
		if (smooth)
			return index;
		int base = (index & 0x7);
		return (index & 0xfffffff8) | map[base];
	}

	public static void setNumColors(int numColors) {
		assert (numColors >= 1);
		if (numColors > 100) {
			// This kludge is not so great.  It breaks the "numColors" attribute and only adds a little distinctiveness. 
			HSBColor.numColors = (numColors / 2);
			hueFactor = 1.0f / (numColors / 2);
			numBrightness = 2;
		} else {
			HSBColor.numColors = numColors;
			hueFactor = 1.0f / numColors;
		}
	}

	public HSBColor(int index) {
		super(numBrightness == 1 ? Color.HSBtoRGB(hueFactor * rotate(index), saturation, brightness) : Color.HSBtoRGB(hueFactor
				* rotate(index % numColors), saturation, (index > numColors ? 1.0f : 0.6f)));
	}

	public static void main(final String[] args) {
		int nc = Integer.parseInt(args[0]);
		HSBColor.setNumColors(nc);
		Box b = Box.createVerticalBox();
		for (int i = 0; i < nc; i++) {
			HSBColor hsb = new HSBColor(i);
			System.out.println(i + ": " + hsb);
			JLabel lab = new JLabel((i < 10 ? "0" : "") + i + " " + hsb);
			lab.setBorder(BorderFactory.createLineBorder(hsb, 20));
			b.add(lab);
		}
		JFrame f = new JFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(b);
		f.pack();
		f.setVisible(true);

	}
}
