package gov.fnal.ppd.signage.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class SimpleMouseListener implements MouseListener {
	private DisplayCardActivator	listener;

	public SimpleMouseListener(DisplayCardActivator listener) {
		this.listener = listener;
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// Interesting!!
		listener.activateCard();
		System.out.println("Mouse Clicked");
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// Not interesting
		System.out.println("Mouse Entered");
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// Not interesting
		System.out.println("Mouse Exited");
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// Not interesting
		System.out.println("Mouse Pressed");
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// Not interesting
		System.out.println("Mouse Released");
	}

}
