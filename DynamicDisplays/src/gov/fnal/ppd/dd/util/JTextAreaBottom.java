package gov.fnal.ppd.dd.util;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * A simple extension to the JTextArea class which tried to have the bottom of the area visible at all times.
 * 
 * 10/2014: Limit the size of the object to 10MB so that this cannot lead to a memory problem.
 * 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class JTextAreaBottom extends JTextArea {

	private static final long	serialVersionUID	= 6409040285176732876L;
	private int maxLength = 10 * 1024 * 1024; // 10MB default limit

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
		if (getDocument().getLength() > maxLength) {
			// Note that this is a rough maximum
			try {
				Document doc = getDocument();
				int shift = doc.getLength()/3; // Remove a third of the existing text
				String newText = doc.getText(shift, doc.getLength() - shift);
				setText(newText);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		super.append(text);
		setCaretPosition(getCaretPosition() + text.length());
	}

	/**
	 * @return The number of characters that may be placed into the JTextArea
	 */
	public int getMaxLength() {
		return maxLength;
	}

	/**
	 * Set the number of characters that may be placed into the JTextArea
	 * 
	 * @param maxLength
	 */
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}
}
