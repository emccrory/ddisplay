package gov.fnal.ppd.signage.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * A listener to attach to a Component to help the caller select a JCardLayout card when clicked.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class SimpleMouseListener implements MouseListener {
	private DisplayCardActivator	listener;

	public SimpleMouseListener(DisplayCardActivator listener) {
		this.listener = listener;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// Interesting!!
		listener.activateCard();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// Not interesting
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// Not interesting
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// Not interesting
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// Not interesting
	}

}
