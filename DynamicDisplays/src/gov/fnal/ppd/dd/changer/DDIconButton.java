package gov.fnal.ppd.dd.changer;

import java.awt.Insets;

import javax.swing.Icon;

import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;

public class DDIconButton extends DDButton {

	private static final long	serialVersionUID	= -8377645551707373769L;

	public DDIconButton(Channel channel, Display display, int maxLen, Icon icon) {
		super(channel, display, maxLen / 2);
		setIcon(icon);
		setVerticalTextPosition(BOTTOM);
		setHorizontalTextPosition(CENTER);
		setMargin(new Insets(5, 5, 5, 5));
	}

}