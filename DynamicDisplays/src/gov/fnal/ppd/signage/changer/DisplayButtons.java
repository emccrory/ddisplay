package gov.fnal.ppd.signage.changer;

import static gov.fnal.ppd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.signage.util.Util.shortDate;
import gov.fnal.ppd.ChannelSelector;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.util.DisplayButtonGroup;
import gov.fnal.ppd.signage.util.DisplayColorSliderUI;

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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

/**
 * Create the widget of the available displays for the XOC "Channel Changer"
 * 
 * @author Elliott McCrory, Fermilab AD, 2012
 */
public class DisplayButtons extends JPanel {

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

			BasicSliderUI sliderUI = new DisplayColorSliderUI(this, colorArray, labels);
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

	private static List<DDButton>	buttonList				= new ArrayList<DDButton>();
	private static List<Display>	displays;
	static final int				INSET_SIZE				= 6;
	static final float				LOCAL_FONT_SIZE			= 38.0f;
	private static final int		MAXIMUM_DISPLAY_BUTTONS	= 20;
	private static final long		serialVersionUID		= 4096502469001848381L;
	protected static final Color	sliderBG				= new Color(0xe0e0e0);
	static final float				WINDOW_FONT_SIZE		= 14.0f;

	/**
	 * Find this Display in the list and then set the ToolTip for it based on the status of the Display.
	 * 
	 * @param disp
	 *            The Display to use for this tool tip
	 */
	public static void setToolTip(final Display disp) {
		int index = 0;
		for (Display d : displays) {
			if (d == disp) {
				String toolTip = "<html><b>Display:</b> " + disp.getNumber() + " -- " + disp.getDescription();
				toolTip += "<br /><b>Last status update:</b> " + shortDate();
				toolTip += "<br />Press to select this display.";
				toolTip += "</p></html>";
				// button.setToolTipText(toolTip);
				buttonList.get(index).setToolTipText(toolTip);
			}
			index++;
		}
	}

	private Box				buttonBox;

	private ActionListener	listener;

	private int				theFS	= (FONT_SIZE > 20.0f ? 20 : (int) FONT_SIZE);

	/**
	 * @param cat
	 *            The type of Display to allow control of, here
	 * @param listener
	 */
	public DisplayButtons(final SignageType cat, final ActionListener listener) {
		super(new BorderLayout());
		this.listener = listener;

		displays = DisplayListFactory.getInstance(cat, ChannelSelector.getLocationCode());

		System.out.println("Number of displays: " + displays.size());

		if (displays.size() <= MAXIMUM_DISPLAY_BUTTONS)
			makeScreenGrid();
		else
			makeScreenGridSlider();
		if (SHOW_IN_WINDOW)
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		else
			setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
	}

	private void makeScreenGrid() {
		buttonBox = Box.createVerticalBox();

		DisplayButtonGroup bg = new DisplayButtonGroup();
		int is = displays.size() > 10 ? 2 * INSET_SIZE / 3 : INSET_SIZE;
		float fs = displays.size() > 10 ? 0.6f * LOCAL_FONT_SIZE - 2 * (displays.size() - 10) : LOCAL_FONT_SIZE;
		if (SHOW_IN_WINDOW)
			fs = WINDOW_FONT_SIZE;

		int rigidHeight = INSET_SIZE + (11 - displays.size());
		if (rigidHeight <= 0)
			rigidHeight = 1;
		buttonBox.add(Box.createVerticalGlue());
		for (int i = 0; i < displays.size(); i++) {
			final Display disp = displays.get(i);
			final DDButton button = new DDButton(disp);
			buttonList.add(button);

			button.setFont(button.getFont().deriveFont(fs));
			if (!SHOW_IN_WINDOW)
				button.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
			button.setSelected(i == 0);
			bg.add(button);

			final int fi = i;
			button.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					// AbstractButton b = (AbstractButton) e.getSource();
					listener.actionPerformed(new ActionEvent(disp, fi, disp.toString(), e.getWhen(), e.getModifiers()));

					// Gets updated when the button is clicked
					setToolTip(disp);
				}
			});

			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xcccccc)),
					BorderFactory.createLineBorder(disp.getPreferredHighlightColor(), is)));
			p.add(button, BorderLayout.CENTER);
			buttonBox.add(p);
		}
		if (displays.size() > 1) {
			setOpaque(true);
			setBackground(Color.black);

			JPanel outerPanel = new JPanel(new BorderLayout());
			outerPanel.setOpaque(true);
			outerPanel.setBackground(Color.black);

			int blackBand = (SHOW_IN_WINDOW ? 2 : 20);
			outerPanel.add(Box.createRigidArea(new Dimension(blackBand, blackBand)), BorderLayout.WEST);
			outerPanel.add(buttonBox, BorderLayout.CENTER);
			outerPanel.add(Box.createRigidArea(new Dimension(blackBand, blackBand)), BorderLayout.EAST);

			add(outerPanel, BorderLayout.CENTER);
		}
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
	 * @param display
	 *            The Display number for which to reset the ToolTipText
	 */
	public void resetToolTip(final Display display) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setToolTip(display);
				System.out.println("Writing tool tip text for display '" + display + "'");
			}
		});
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

	/**
	 * @return The first display in the internal list
	 */
	public Display getFirstDisplay() {
		return displays.get(0);
	}

}
