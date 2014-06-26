package gov.fnal.ppd.signage.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Hashtable;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Implement a JSlider. This is used when there are too many Displays to show on a ChannelSelector as individual buttons.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class DisplayColorSliderUI extends BasicSliderUI {

	private Color				backgroundColor	= new Color(0xeeeeee);

	private static final Color	myWhite			= new Color(255, 255, 255); // , 200);
	private static final Color	myGray			= new Color(182, 182, 182); // , 200);

	private int					WIDTH			= 60;
	private Color[]				colors;

	/**
	 * Test this complicated bit of eye candy
	 * 
	 * @param args
	 *            Do not expect any arguments
	 */
	public static final void main(final String[] args) {
		JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 20, 3);

		System.out.println(Color.white + ", " + Color.gray.brighter() + ", " + myWhite + ", " + myGray);
		slider.setUI(new DisplayColorSliderUI(slider,
				new Color[] { Color.red, Color.white, Color.orange, Color.white, Color.blue }, new int[] { 1, 5, 9, 22, 23 }));

		slider.setSnapToTicks(true);
		slider.setPaintTicks(true);
		slider.setPaintLabels(true);
		slider.setAlignmentY(JSlider.RIGHT_ALIGNMENT);
		slider.setInverted(true);
		slider.setOpaque(true);
		slider.setFont(new Font("Arial", Font.BOLD, 20));

		JFrame f = new JFrame("Test Slider");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setContentPane(slider);
		f.pack();
		f.setVisible(true);

	}

	/**
	 * @param slider
	 *            The JSlider to use
	 * @param c
	 *            The list of colors to put onto this JSlider
	 * @param labels
	 *            The labels (Display descriptions, presumably) for the individual elements in the JSlider
	 */
	public DisplayColorSliderUI(final JSlider slider, final Color[] c, final int[] labels) {
		super(slider);
		assert (c != null);
		colors = c;
		backgroundColor = slider.getBackground();
		Hashtable<Integer, JComponent> dict = new Hashtable<Integer, JComponent>();
		for (Integer I = 0; I < labels.length; I++)
			dict.put(I, new JLabel("" + labels[I]));
		slider.setLabelTable(dict);
	}

	protected Dimension getThumbSize() {
		return new Dimension(WIDTH, 40);
	}

	@Override
	public void paintThumb(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Point p = thumbRect.getLocation();
		int w = thumbRect.getSize().width;
		int h = thumbRect.getSize().height;

		g2d.setPaint(Color.black);

		Polygon homePlateOuter = new Polygon( //
				new int[] { p.x + 2, p.x + w / 2, p.x + w - 1, p.x + w / 2, p.x + 2 }, //
				new int[] { p.y, p.y, p.y + h / 2, p.y + h, p.y + h }, //
				5);

		g2d.fill(homePlateOuter);

		Polygon homePlateInner = new Polygon( //
				new int[] { p.x + 4, p.x - 1 + w / 2, p.x - 3 + w, p.x - 1 + w / 2, p.x + 4 }, //
				new int[] { p.y + 2, p.y + 2, p.y + h / 2, p.y + h - 2, p.y + h - 2 }, //
				5);
		Paint pp = new GradientPaint(p.x, p.y, myWhite, p.x, p.y + h, myGray);
		g2d.setPaint(pp);
		g2d.fill(homePlateInner);

	}

	@Override
	public void paintTrack(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		Rectangle t = trackRect;
		// t.width = WIDTH + 10;

		// Split the rectangle into 2*colors.length little rectangles
		// The first one and the last one have the first and last colors.
		// The second and third one has the second color.
		// The fourth and fifth one have the third color.
		// etc.
		double inc = (double) t.height / (double) (colors.length - 1);
		g2d.setPaint(colors[0]);
		g2d.fillRect(t.x, t.y, t.width, (int) Math.round(t.y + inc / 2.0));
		g2d.setPaint(Color.black);
		g2d.drawLine(t.x, (int) Math.round(t.y + inc / 2.0), t.x + t.width - 1, (int) Math.round(t.y + inc / 2.0));
		int ymax;
		for (int i = 1; i < colors.length - 1; i++) {
			ymax = (int) Math.round(t.y + (i + 1) * inc - inc / 2.0);
			g2d.setPaint(colors[i]);
			g2d.fillRect(t.x, 1 + (int) Math.round(t.y + i * inc - inc / 2.0), t.width, ymax);
			g2d.setPaint(Color.black);
			g2d.drawLine(t.x, ymax, t.x + t.width - 1, ymax);
		}
		ymax = (int) Math.round(t.y + (colors.length - 1) * inc - inc / 2.0);
		g2d.setPaint(colors[colors.length - 1]);
		g2d.fillRect(t.x, 1 + ymax, t.width, (int) Math.round(ymax + inc / 2.0));
		g2d.setPaint(backgroundColor);
		g2d.fillRect(t.x, 1 + (int) Math.round(ymax + inc / 2.0), t.width, (int) Math.round(ymax + inc));
		// g2d.setPaint(Color.black);
		// g2d.drawLine(t.x, (int) Math.round(ymax + inc), t.x + t.width - 1,
		// (int) Math.round(ymax + inc));
	}
}
