package gov.fnal.ppd.signage.changer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class MyRectangle extends JPanel {

	private static final long serialVersionUID = -6361828025007738524L;

	private static final String colorClassName = Color.class.getCanonicalName();

	private static final int PADD = 0;

	private int width, height;

	private double x = 25, y = 3;

	private Color color;

	boolean inside = false;

	private Color myBorder;

	private String tip;

	private MyRectangle( Color c ) {
		super();
		width = 80;
		height = 30;
		x = y = 5;
		color = c;
		setSize(new Dimension(width + PADD, height + PADD));
		setMinimumSize(new Dimension(width + PADD, height + PADD));
		setPreferredSize(new Dimension(width + PADD, height + PADD));
	}

	public MyRectangle( int w, int h, Color c ) {
		super();
		width = w;
		height = h;
		int grey = (c.getRed() + c.getBlue() + c.getGreen()) / 3;
		if (grey < 64)
			myBorder = Color.white;
		else
			myBorder = Color.black;

		color = c;
		setSize(new Dimension(width + PADD, height + PADD));
		setMinimumSize(new Dimension(width + PADD, height + PADD));
		setPreferredSize(new Dimension(width + PADD, height + PADD));
		setToolTipText(c.toString().replace(colorClassName, "") + " (CTL-Click for details)");
		addMouseListener(new MouseListener() {
			public void mouseReleased( MouseEvent arg0 ) {
				if (arg0.isControlDown()) {
					Box b = Box.createHorizontalBox();
					b.add(new MyRectangle(color));
					b.add(new JLabel(color.toString().replace(colorClassName, "")));
					b.add(Box.createGlue());
					b.setOpaque(true);
					b.setBackground(Color.white);
					if (tip != null) {
						Box vb = Box.createVerticalBox();
						vb.add(b);
						vb.add(Box.createRigidArea(new Dimension(10, 10)));
						JLabel lab = new JLabel(tip);
						lab.setAlignmentX(JLabel.CENTER_ALIGNMENT);
						vb.add(lab);
						b = vb;
					}
					JOptionPane.showMessageDialog(null, b, (tip != null ? tip : "Color Map"),
							JOptionPane.PLAIN_MESSAGE);
				}
			}

			public void mousePressed( MouseEvent arg0 ) {}

			public void mouseExited( MouseEvent arg0 ) {
				inside = false;
				repaint();
			}

			public void mouseEntered( MouseEvent arg0 ) {
				inside = true;
				repaint();
			}

			public void mouseClicked( MouseEvent arg0 ) {}

		});
	}

	public MyRectangle( double x, double y, int w, int h, Color c, String tip ) {
		this(w, h, c);
		this.x = x;
		this.y = y;
		this.tip = tip;
	}

	@Override
	public void paint( Graphics g ) {
		Graphics2D g2 = (Graphics2D) g;

		g2.setPaint(color);
		g2.fill(new Rectangle2D.Double(x, y, width, height));
		if (inside)
			g2.setPaint(myBorder);
		else
			g2.setPaint(color);
		g2.draw(new Rectangle2D.Double(x, y, width, height));
	}
}
