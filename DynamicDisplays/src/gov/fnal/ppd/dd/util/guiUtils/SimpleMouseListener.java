/*
 * SimpleMouseListener
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util.guiUtils;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import gov.fnal.ppd.dd.interfaces.DisplayCardActivator;

/**
 * A listener to attach to a Component to help the caller select a JCardLayout card when clicked.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SimpleMouseListener implements MouseListener {
	private DisplayCardActivator	listener;

	/**
	 * @param listener
	 */
	public SimpleMouseListener(final DisplayCardActivator listener) {
		this.listener = listener;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// Interesting!!
		listener.activateCard(true);
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
