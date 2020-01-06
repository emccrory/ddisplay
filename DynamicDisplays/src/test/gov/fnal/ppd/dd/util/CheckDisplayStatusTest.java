package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;

import org.junit.Test;

import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.CheckDisplayStatus;

public class CheckDisplayStatusTest {

	static {
		credentialsSetup();
	}

	public static final class SetTextClass {
		private static String text = null;

		public void setText(String t) {
			text = t;
			System.out.println("Text changed to [" + text + "]");
		}
	}

	@Test
	public void testRun() {
		CheckDisplayStatus cds = new CheckDisplayStatus(new Display() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

			}

			@Override
			public SignageContent getContent() {
				return null;
			}

			@Override
			public SignageContent setContent(SignageContent c) {
				return null;
			}

			@Override
			public Color getPreferredHighlightColor() {
				return Color.RED;
			}

			@Override
			public int getDBDisplayNumber() {
				return 1; // This ASSUMES that the system has a display with a DB id of 1
			}

			@Override
			public int getVirtualDisplayNumber() {
				return 1; // A reasonable assumption that the system has a display (virtual number) 1
			}

			@Override
			public void setDBDisplayNumber(int d) {

			}

			@Override
			public void setVirtualDisplayNumber(int v) {

			}

			@Override
			public int getScreenNumber() {
				return 0;
			}

			@Override
			public InetAddress getIPAddress() {
				return null;
			}

			@Override
			public void addListener(ActionListener L) {

			}

			@Override
			public String getDescription() {
				return "Testing";
			}

			@Override
			public String getLocation() {
				return "Not relevant";
			}

			@Override
			public String getStatus() {
				return "OK";
			}

			@Override
			public String getMessagingName() {
				return "Not relevant";
			}

			@Override
			public void disconnect() {

			}

			@Override
			public void errorHandler(String message) {

			}

		}, new SetTextClass());
		cds.start();
		// In the Fermilab system, the first display responds in about 14 seconds after this test is launched. Usually, we see two
		// responses in 20 seconds.  But it seems to not work at all for maven, even if we wait for 40 seconds.
		catchSleep(20000);
		assertNotNull(SetTextClass.text);
	}

}
