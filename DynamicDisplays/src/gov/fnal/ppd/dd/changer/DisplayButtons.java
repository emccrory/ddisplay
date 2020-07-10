package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.FONT_SIZE;
import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.userHasDoneSomething;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.shortDate;
import static gov.fnal.ppd.dd.util.specific.PackageUtilities.getDisplayID;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
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

import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.util.guiUtils.DisplayColorSliderUI;
import gov.fnal.ppd.dd.util.specific.DisplayButtonGroup;

/**
 * Create the widget of the available displays for the Dynamic Displays "Channel Selector"
 * 
 * @author Elliott McCrory, Fermilab AD, 2012-20
 */
public class DisplayButtons extends JPanel {

	/**
	 * Make a JSlider with a tool-tip that is determined from where the mouse is. Put all the functionality for this class in here.
	 * 
	 * @author Elliott McCrory, Fermilab AD, 2012-20
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

			setFont(getFont().deriveFont((SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : FONT_SIZE / 2)));
			// setFont(new Font("Arial", Font.BOLD, (SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : theFS)));
			setToolTipText("initializing...");

			Color[] colorArray = new Color[displayList.size()];
			String[] labels = new String[displayList.size()];
			for (int i = 0; i < colorArray.length; i++) {
				colorArray[i] = displayList.get(i).getPreferredHighlightColor();
				labels[i] = displayList.get(i).getVirtualDisplayNumber() + ": " + displayList.get(i).getLocation() + " ("
						+ displayList.get(i).getDBDisplayNumber() + ")";
			}

			BasicSliderUI sliderUI = new DisplayColorSliderUI(this, colorArray, labels);
			setUI(sliderUI);

			addChangeListener(new ChangeListener() {

				public void stateChanged(ChangeEvent e) {
					Display disp = displayList.get(getValue());
					listener.actionPerformed(new ActionEvent(disp, getValue(), getDisplayID(disp), System.currentTimeMillis(),
							java.awt.event.MouseEvent.BUTTON1_MASK | java.awt.event.MouseEvent.BUTTON1_DOWN_MASK));
				}
			});

			if (displayList.size() < 26) {
				setMajorTickSpacing(1);
				setFont(getFont().deriveFont((SHOW_IN_WINDOW ? (int) WINDOW_FONT_SIZE : FONT_SIZE / 2)));
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
			int size = displayList.size();
			int index = size * x / rect.height;
			Display d = displayList.get(index);
			return "<html><b>" + d + "</b> " + d.getDescription() + "</html>";
		}
	}

	private static List<JComponent>	buttonList				= new ArrayList<JComponent>();
	// private static List<Display> displayList;
	// static final int INSET_SIZE = 5;
	private static final int		MAXIMUM_DISPLAY_BUTTONS	= 20;
	private static final long		serialVersionUID		= 4096502469001848381L;
	protected static final Color	sliderBG				= new Color(0xe0e0e0);
	static final float				WINDOW_FONT_SIZE		= 18.0f;

	/**
	 * Make this false if this is some special application, one that does not have display buttons (for some reason)
	 */
	public static boolean			normalOperations		= true;

	/**
	 * Find this Display in the list and then set the ToolTip for it based on the status of the Display.
	 * 
	 * @param disp
	 *            The Display to use for this tool tip
	 */
	public static void setToolTip(final Display disp) {
		if (buttonList == null)
			return;
		// Note: This does not work with the Slider selector. 

		if (buttonList.size() == 0) {
			if (normalOperations)
				println(DisplayButtons.class, ".setToolTip() for display " + disp.getDBDisplayNumber()
						+ " -- nothing in buttonList.  Are we still initializing?");
			return;
		}

		synchronized (displayList) {
			int index = displayList.indexOf(disp);

			String toolTip = "<html><b>Display:</b> " + disp.getVirtualDisplayNumber();
			toolTip += " (index=" + disp.getDBDisplayNumber() + ", num=" + disp.getVirtualDisplayNumber() + ")";
			toolTip += " -- " + disp.getDescription();
			toolTip += "<br /><b>Last status update:</b> " + shortDate();
			toolTip += "<br />Press to select this display.";
			toolTip += "</p></html>";
			buttonList.get(index).setToolTipText(toolTip);
		}
	}

	private Container		buttonBox;

	private ActionListener	listener;

	/**
	 * @param cat
	 *            The type of Display to allow control of, here
	 * @param listener
	 */
	public DisplayButtons(final ActionListener listener) {
		super(new BorderLayout());

		this.listener = listener;

		// displayList = DisplayListFactory.getInstance(cat, ChannelSelector.getLocationCode());
		println(getClass(), "Number of displays: " + displayList.size());

		if (displayList.size() <= MAXIMUM_DISPLAY_BUTTONS)
			makeScreenGrid();
		else {
			if (SHOW_IN_WINDOW)
				makeCompactScreenGrid();
			else
				makeScreenGridSlider();
		}
		if (SHOW_IN_WINDOW)
			setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		else
			setBorder(BorderFactory.createEmptyBorder(10, 5, 5, 5));
	}

	private void makeScreenGrid() {
		buttonBox = Box.createVerticalBox();

		DisplayButtonGroup bg = new DisplayButtonGroup();
		// int is = displayList.size() > 10 ? 2 * INSET_SIZE / 3 : INSET_SIZE;
		// float fs = displayList.size() > 10 ? LOCAL_FONT_SIZE - (displayList.size() - 10)/2 : LOCAL_FONT_SIZE;
		float fs = FONT_SIZE; // In small atom tablet, full-screen: 24.0f
		if (SHOW_IN_WINDOW) {
			fs = WINDOW_FONT_SIZE;
		}
		fs = Math.max(8.0f, Math.min(FONT_SIZE, fs - (displayList.size() - 5))); // In small atom tablet, full-screen w 11 Disps:
																					// 19.0f
		// println(getClass(), "** Font size is " + fs);

		// int rigidHeight = INSET_SIZE + (11 - displayList.size());
		// if (rigidHeight <= 0)
		// rigidHeight = 1;
		buttonBox.add(Box.createVerticalGlue());
		synchronized (buttonList) {
			for (int i = 0; i < displayList.size(); i++) {
				final Display disp = displayList.get(i);
				final DDButton button = new DDButton(disp);
				buttonList.add(button);

				button.setFont(button.getFont().deriveFont(fs));
				// if (!SHOW_IN_WINDOW)
				// button.setMargin(new Insets(INSET_SIZE, INSET_SIZE, INSET_SIZE, INSET_SIZE));
				button.setSelected(i == 0);
				bg.add(button);

				final int fi = i;
				button.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						// AbstractButton b = (AbstractButton) e.getSource();
						listener.actionPerformed(new ActionEvent(disp, fi, getDisplayID(disp), e.getWhen(), e.getModifiers()));

						// Gets updated when the button is clicked
						setToolTip(disp);
						userHasDoneSomething();
					}
				});

				JPanel p = new JPanel(new BorderLayout());
				p.add(button, BorderLayout.CENTER);
				buttonBox.add(p);
				// buttonBox.add(button);
			}

			if (displayList.size() > 1) {
				setOpaque(true);
				setBackground(Color.black);

				JPanel outerPanel = new JPanel(new BorderLayout());
				outerPanel.setOpaque(true);
				outerPanel.setBackground(Color.black);

				int blackBand = (SHOW_IN_WINDOW ? 2 : 10);
				outerPanel.add(Box.createRigidArea(new Dimension(blackBand, blackBand)), BorderLayout.WEST);
				outerPanel.add(buttonBox, BorderLayout.CENTER);
				outerPanel.add(Box.createRigidArea(new Dimension(blackBand, blackBand)), BorderLayout.EAST);

				add(outerPanel, BorderLayout.CENTER);
			}
		}
	}

	/**
	 * There are too many displays to show them all as buttons.  So we created this slider, based on JSlider,
	 * to allow a user to select from that plethora of channels more easily.  Realistically, this is likely
	 * not to be used: It is kind of ugly, and a smart user can handle a LOT of display buttons, and (finally)
	 * no Dynamic Display location is likely to have nearly that many displays.  One could imagine, however,
	 * that a controller that could change all the displays on site would have that many buttons.
	 */
	private void makeScreenGridSlider() {
		buttonBox = Box.createVerticalBox();

		JLabel lab = new JLabel("\u25BC Display \u25BC");
		lab.setAlignmentX(SwingConstants.LEFT);
		buttonBox.add(lab);
		buttonBox.add(new MyJSlider(SwingConstants.VERTICAL, 0, displayList.size() - 1, 0));
		buttonBox.add(Box.createRigidArea(new Dimension(10, 10)));

		((Box) buttonBox).setAlignmentX(JComponent.TOP_ALIGNMENT);
		add(buttonBox, BorderLayout.CENTER);
		buttonList = null;
	}

	private void makeCompactScreenGrid() {
		buttonBox = new JPanel(new GridLayout(0, 2));

		DisplayButtonGroup bg = new DisplayButtonGroup();

		float fs = 12.0f;
		if (displayList.size() > 60)
			fs = 10.0f;
		else if (displayList.size() > 40)
			fs = 11.0f;

		synchronized (buttonList) {
			for (int i = 0; i < displayList.size(); i++) {
				final Display disp = displayList.get(i);
				final DDButton button = new DDButton(disp);
				button.setText("Disp " + disp.getVirtualDisplayNumber() + " | " + disp.getDBDisplayNumber());
				buttonList.add(button);
				button.setFont(button.getFont().deriveFont(fs));
				button.setSelected(i == 0);
				bg.add(button);

				final int fi = i;
				button.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						listener.actionPerformed(new ActionEvent(disp, fi, getDisplayID(disp), e.getWhen(), e.getModifiers()));
						setToolTip(disp);
						userHasDoneSomething();
					}
				});

				JPanel p = new JPanel(new BorderLayout());
				p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xcccccc)),
						BorderFactory.createLineBorder(disp.getPreferredHighlightColor(), 2)));
				p.add(button, BorderLayout.CENTER);
				buttonBox.add(p);
			}

			if (displayList.size() > 1) {
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
				// System.out.println("Writing tool tip text for display '" + display + "'");
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
		if (buttonList == null || buttonList.size() == 0)
			return;

		new Thread("SetAliveDisplay" + displayNum) {
			public void run() {
				catchSleep(100);
				int index = 0;
				synchronized (buttonList) {
					for (Display d : displayList) {
						if (d.getDBDisplayNumber() == displayNum) {
							if (buttonList.size() > index)
								buttonList.get(index).setEnabled(alive);
							else
								System.err.println(DisplayButtons.class.getSimpleName() + " -- DisplayNum=" + displayNum
										+ ", index=" + index + ", but size of buttonList is only " + buttonList.size());
						}
						index++;
					}
				}
			}
		}.start();
	}

	/**
	 * @return The first display in the internal list
	 */
	public Display getFirstDisplay() {
		return displayList.get(0);
	}

}
