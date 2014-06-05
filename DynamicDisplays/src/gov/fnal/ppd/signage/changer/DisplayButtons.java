package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.ChannelSelector.FONT_SIZE;
import static gov.fnal.ppd.ChannelSelector.SHOW_IN_WINDOW;
import static gov.fnal.ppd.signage.util.Util.shortDate;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.util.MyButtonGroup;
import gov.fnal.ppd.signage.util.MyColorSliderUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Create the widget of the available displays for the XOC "Channel Changer"
 * 
 * @author Elliott McCrory, Fermilab AD, 2012
 */
public class DisplayButtons extends JPanel {

	int								theFS				= (FONT_SIZE > 20.0f ? 20 : (int) FONT_SIZE);
	private static final long		serialVersionUID	= 4096502469001848381L;
	static final int				INSET_SIZE			= 6;
	static final float				LOCAL_FONT_SIZE		= 38.0f;
	static final float				WINDOW_FONT_SIZE	= 14.0f;
	protected static final Color	sliderBG			= new Color(0xe0e0e0);
	private Box						buttonBox;
	private DisplayList				displays;
	private ActionListener			listener;
	private List<MyButton>			buttonList			= new ArrayList<MyButton>();

	/**
	 * Make a JSlider with a tool-tip that is determined from where the mouse is. Put all the functionality for this class in here.
	 * 
	 * @author Elliott McCrory, Fermilab AD, 2012-2014
	 */
	private class MyJSlider extends JSlider {
		private static final long	serialVersionUID	= 6272217247978609017L;

		public MyJSlider(int vertical, int min, int max, int val) {
			super(vertical, min, max, val);
			setSnapToTicks(true);
			setPaintTicks(true);
			setPaintLabels(true);
			setAlignmentY(RIGHT_ALIGNMENT);
			setInverted(true);
			setBackground(sliderBG);
			setOpaque(true);

			setFont(getFont().deriveFont((SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : theFS)));
			// setFont(new Font("Arial", Font.BOLD, (SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : theFS)));
			setToolTipText("initializing...");

			Color[] colorArray = new Color[displays.size()];
			int[] labels = new int[displays.size()];
			for (int i = 0; i < colorArray.length; i++) {
				colorArray[i] = displays.get(i).getPreferredHighlightColor();
				labels[i] = displays.get(i).getNumber();
			}

			BasicSliderUI sliderUI = new MyColorSliderUI(this, colorArray, labels);
			setUI(sliderUI);

			addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					Display disp = displays.get(getValue());
					// FIXME The listener is part of the surrounding class--I don't like this
					listener.actionPerformed(new ActionEvent(disp, getValue(), disp.toString(), System.currentTimeMillis(),
							java.awt.event.MouseEvent.BUTTON1_MASK | java.awt.event.MouseEvent.BUTTON1_DOWN_MASK));
				}
			});

			if (displays.size() < 26) {
				setMajorTickSpacing(1);
				setFont(getFont().deriveFont((SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : theFS)));
				// setFont(new Font("Courier", Font.PLAIN, (SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : theFS)));
			} else {
				setMajorTickSpacing(10);
				setMinorTickSpacing(1);
			}

			setAlignmentX(SwingConstants.LEFT);
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			int x = e.getY();
			Rectangle rect = getBounds();
			int size = displays.size();
			int index = size * x / rect.height;
			Display d = displays.get(index);
			return "<html><b>" + d + "</b> " + d.getDescription() + "</html>";
		}
	}

	/**
	 * @param cats
	 *            The types of signage here (NOT USED at this time!)
	 * @param listener
	 */
	public DisplayButtons(final SignageType[] cats, final ActionListener listener) {
		super(new BorderLayout());
		this.listener = listener;

		displays = DisplayListFactory.getInstance();

		if (displays.size() <= 20)
			makeScreenGrid();
		else
			makeScreenGridSlider();
		if (SHOW_IN_WINDOW)
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		else
			setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));
	}

	private void makeScreenGrid() {
		buttonBox = Box.createVerticalBox();

		MyButtonGroup bg = new MyButtonGroup();
		int is = displays.size() > 10 ? 2 * INSET_SIZE / 3 : INSET_SIZE;
		float fs = displays.size() > 10 ? 3f * LOCAL_FONT_SIZE / 4f - 2 * (displays.size() - 10) : LOCAL_FONT_SIZE;
		if (SHOW_IN_WINDOW)
			fs = WINDOW_FONT_SIZE;

		int rigidHeight = INSET_SIZE + (11 - displays.size());
		if (rigidHeight <= 0)
			rigidHeight = 1;
		buttonBox.add(Box.createHorizontalGlue());
		for (int i = 0; i < displays.size(); i++) {
			final Display disp = displays.get(i);
			final MyButton button = new MyButton(disp);
			buttonList.add(button);

			button.setFont(button.getFont().deriveFont(fs));
			if (!SHOW_IN_WINDOW)
				button.setMargin(new Insets(is, is, is, is));
			button.setSelected(i == 0);
			bg.add(button);

			final int fi = i;
			button.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					listener.actionPerformed(new ActionEvent(disp, fi, disp.toString(), e.getWhen(), e.getModifiers()));

					String toolTip = "<html><b>Display:</b> " + disp.getNumber() + " -- " + disp.getDescription() + " ("
							+ disp.getIPAddress() + ")";
					toolTip += "<br><b>Active channel:</b> " + disp.getContent();
					toolTip += "<br /><b>Last status update:</b> " + shortDate();
					toolTip += "<br />Press to select this display.";
					toolTip += "</p></html>";
					button.setToolTipText(toolTip);
				}
			});

			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createLineBorder(disp.getPreferredHighlightColor(), is));
			p.add(button, BorderLayout.CENTER);
			buttonBox.add(p);
			if (!SHOW_IN_WINDOW)
				buttonBox.add(Box.createRigidArea(new Dimension(5, rigidHeight)));
		}
		final Color g = new Color(0xaaaaaa);
		buttonBox.add(Box.createHorizontalGlue());
		buttonBox.setOpaque(true);
		buttonBox.setBackground(g);
		setOpaque(true);
		setBackground(g);
		add(buttonBox, BorderLayout.CENTER);
	}

	private void makeScreenGridSlider() {
		buttonBox = Box.createVerticalBox();

		JLabel lab = new JLabel("\u25BC Display \u25BC");
		lab.setAlignmentX(SwingConstants.LEFT);
		buttonBox.add(lab);
		buttonBox.add(new MyJSlider(SwingConstants.VERTICAL, 0, displays.size() - 1, 0));
		buttonBox.add(Box.createRigidArea(new Dimension(10, 10)));

		buttonBox.setAlignmentX(JComponent.TOP_ALIGNMENT);
		add(buttonBox, BorderLayout.CENTER);
	}

	/**
	 * Re-write the ToolTipsText for this Display button
	 * 
	 * @param displayNum
	 *            The Display number for which to reset the ToolTipText
	 */
	public void resetToolTip(final int displayNum) {
		new Thread() {
			public void run() {
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int index = 0;
				for (Display d : displays) {
					if (d.getNumber() == displayNum) {
						String toolTip = "<html><b>Display</b> " + displays.get(index).getNumber() + " -- "
								+ displays.get(index).getDescription();
						toolTip += "<br><b>Active channel:</b> " + displays.get(index).getContent() + " ("
								+ displays.get(index).getIPAddress() + ")";
						toolTip += "<br />Press to select this display.";
						toolTip += "<br /><b>Last status update:</b> " + shortDate();
						toolTip += "</p></html>";

						buttonList.get(index).setToolTipText(toolTip);
					}
					index++;
				}
			}
		}.start();
	}

	/**
	 * @param displayNum
	 *            The Display number of concern here
	 * @param alive
	 *            Is this Display alive?
	 */
	public void setIsAlive(final int displayNum, final boolean alive) {
		new Thread("SetAliveDisplay" + displayNum) {
			public void run() {
				try {
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				int index = 0;
				for (Display d : displays) {
					if (d.getNumber() == displayNum) {
						buttonList.get(index).setEnabled(alive);
					}
					index++;
				}
			}
		}.start();

	}
}
