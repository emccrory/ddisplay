package gov.fnal.ppd.chat;

import javax.swing.JTextArea;
import javax.swing.text.Document;

/**
 * A simple extension to the JTextArea class which tried to have the bottom of the area visible at all times.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class JTextAreaBottom extends JTextArea {

	private static final long	serialVersionUID	= 6409040285176732876L;

	/**
	 * Link to the JTextArea constructor
	 */
	public JTextAreaBottom() {
		super();
	}

	/**
	 * Link to the JTextArea constructor
	 * 
	 * @param doc
	 * @param text
	 * @param rows
	 * @param columns
	 */
	public JTextAreaBottom(final Document doc, final String text, final int rows, final int columns) {
		super(doc, text, rows, columns);
	}

	/**
	 * Link to the JTextArea constructor
	 * 
	 * @param doc
	 */
	public JTextAreaBottom(final Document doc) {
		super(doc);
	}

	/**
	 * Link to the JTextArea constructor
	 * 
	 * @param rows
	 * @param columns
	 */
	public JTextAreaBottom(final int rows, final int columns) {
		super(rows, columns);
	}

	/**
	 * Link to the JTextArea constructor
	 * 
	 * @param text
	 * @param rows
	 * @param columns
	 */
	public JTextAreaBottom(final String text, final int rows, final int columns) {
		super(text, rows, columns);
	}

	/**
	 * Link to the JTextArea constructor
	 * 
	 * @param text
	 */
	public JTextAreaBottom(final String text) {
		super(text);
	}

	@Override
	public void append(String text) {
		super.append(text);
		setCaretPosition(getCaretPosition() + text.length());
	}
}
