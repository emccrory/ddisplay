package gov.fnal.ppd.dd.changer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * A play class to implement a keyboard across all architectures. This code is almost there, but there is plenty more to do to make
 * it work like a real keyboard. Is it worth it??
 * 
 * TODO - either put a text box on the keyboard to show what is being typed, or allow the calling program to see each and every
 * letter to it can fill in the thing that it needs input for. But then that puts the "backspace" key into their hands. Ugh.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class Keyboard extends JPanel implements ActionListener {

	private static final long serialVersionUID = 4961744485969760570L;
	private static String		lowerCase	= "`1234567890-=qwertyuiop[]\\asdfghjkl;'  zxcvbnm,./   ";
	private static String		upperCase	= "~!@#$%^&*()_+QWERTYUIOP{}|ASDFGHJKL:\"  ZXCVBNM<>?    ";
	private String[]			special		= { "BS", "DEL", "ACCEPT", "SHIFT" };
	private int[]				widths		= { 2, 2, 2, 4 };
	private List<Component>		buttons		= new ArrayList<Component>();
	private boolean				isLowerCase	= true;
	private ActionListener		theListener	= null;
	protected static boolean	done;

	public Keyboard() {
		super(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		int row = 0;
		int index = 0;
		gbc.gridy = gbc.gridx = 0;
		gbc.gridwidth = 1;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.insets = new Insets(4, 4, 4, 4);
		for (int i = 0; i < lowerCase.length(); i++) {
			String c = lowerCase.substring(i, i + 1);
			if (c.equals(" ")) {
				buttons.add(Box.createRigidArea(new Dimension(2, 2)));
			} else {
				buttons.add(new ASpecialButton(c));
			}
		}

		for (int i = 0; index < lowerCase.length(); i++) {
			if (i % 14 == 13) {
				gbc.gridwidth = widths[row];
				add(new ASpecialButton(special[row]), gbc);
				gbc.gridwidth = 1;
				gbc.gridy++;
				gbc.gridx = 0;
				row++;
			} else {
				add(buttons.get(index), gbc);
				gbc.gridx++;
				index++;
			}
		}
		gbc.gridwidth = widths[row];
		add(new ASpecialButton(special[row++]), gbc);
		gbc.gridx = 1;
		gbc.gridy++;
		gbc.gridwidth = 11;
		add(new ASpecialButton("SPACE"), gbc);

	}

	private class ASpecialButton extends JButton {
		private static final long serialVersionUID = 4945602285858242692L;

		public ASpecialButton(String text) {
			super(text);
			addActionListener(Keyboard.this);
			setFont(new Font("Courier", Font.PLAIN, 36));
			setMargin(new Insets(30, 10, 5, 10));
		}
	}

	static JFrame myFrame = new JFrame();

	public static void launchKeyboard(final ActionListener receiver) {
		final Keyboard keyboard = new Keyboard();
		final List<String> retval = new ArrayList<String>();
		keyboard.setListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				String letter = arg0.getActionCommand();
				if (letter.length() == 1) {
					retval.add(letter);
				} else if (letter.equals("ACCEPT")) {
					String r = "";
					for (String S : retval)
						r += S;
					receiver.actionPerformed(new ActionEvent(this, (int) Math.random() * 32767, r));
					myFrame.setVisible(false);
					myFrame = null;
				} else if (letter.equals("SHIFT")) {
					keyboard.isLowerCase = !keyboard.isLowerCase;
					String L = (keyboard.isLowerCase ? lowerCase : upperCase);

					for (int i = 0; i < L.length(); i++) {
						String c = L.substring(i, i + 1);
						if (c.equals(" "))
							continue;
						else {
							((ASpecialButton) keyboard.buttons.get(i)).setText(c);
						}
					}

				} else if (letter.equals("SPACE")) {
					retval.add(" ");
				} else if (letter.equals("BS") || letter.equals("DEL")) {
					retval.remove(retval.size() - 1);
				}
			}

		});
		myFrame.add(keyboard);
		myFrame.pack();
		myFrame.setVisible(true);
		myFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

	}

	private void setListener(ActionListener actionListener) {
		theListener = actionListener;
	}

	public static void main(String[] args) {
		launchKeyboard(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println(e.getActionCommand());
				System.exit(0);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (theListener != null) {
			theListener.actionPerformed(arg0);
			return;
		}
		String letter = arg0.getActionCommand();
		if (letter.length() == 1) {
			System.out.print(letter);
		} else if (letter.equals("ACCEPT")) {
			System.out.println();
		} else if (letter.equals("SHIFT")) {
			isLowerCase = !isLowerCase;
			String L = (isLowerCase ? lowerCase : upperCase);

			for (int i = 0; i < L.length(); i++) {
				String c = L.substring(i, i + 1);
				if (c.equals(" "))
					continue;
				else {
					((ASpecialButton) buttons.get(i)).setText(c);
				}
			}

		} else if (letter.equals("SPACE")) {
			System.out.print(" ");
		} else if (letter.equals("BS")) {
			// TODO implement this key
			System.out.println("\"" + letter + "\"");
		} else if (letter.equals("DEL")) {
			// TODO implement this key
			System.out.println("\"" + letter + "\"");
		}
	}

}
