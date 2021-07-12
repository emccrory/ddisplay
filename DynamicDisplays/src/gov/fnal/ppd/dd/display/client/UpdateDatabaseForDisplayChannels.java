package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.printlnErr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.xml.bind.JAXBException;

import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.messages.ChangeChannel;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelList;

/**
 * <p>
 * An implementation of the Display interface that is suitable for pushing into the database. This is used to update the database
 * table that keeps track of the channels that a Display has been showing.
 * </p>
 * <p>
 * The part that is unique to this implementation is that the channel time stamp is fixed (1500000000: July 14, 2017 2:40:00 AM GMT)
 * In the class that looks at these, this makes comparing channels easier.
 * </p>
 * <p>
 * See SummarizeContentHistory
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class UpdateDatabaseForDisplayChannels implements Display {

	private int				displayID;
	private SignageContent	content;

	public UpdateDatabaseForDisplayChannels(int displayID) {
		this.displayID = displayID;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		println(getClass(), "ActionPerformed()");
	}

	@Override
	public SignageContent getContent() {
		return content;
	}

	@Override
	public SignageContent setContent(SignageContent c) {
		if (this.content == null || !this.content.equals(c)) {
			println(getClass(), "**CONTENT CHANGED** to " + c.getName());
			this.content = c;
			new Thread("CommonChannelDBUpdate") {
				public void run() {
					updateDatabase();
				}
			}.start();
		}
		return c;
	}

	private void updateDatabase() {
		catchSleep(10);

		try {
			MessagingDataXML cc = null;
			if (content instanceof ChannelPlayList) {
				cc = new ChangeChannelList();
				((ChangeChannelList) cc).setDisplayNumber(getVirtualDisplayNumber());
				((ChangeChannelList) cc).setScreenNumber(getScreenNumber());
				((ChangeChannelList) cc).setChannelNumber(((ChannelPlayList) content).getNumber());
				((ChangeChannelList) cc).setContent(getContent());
			} else {
				cc = new ChangeChannel();
				((ChangeChannel) cc).setDisplayNumber(getVirtualDisplayNumber());
				((ChangeChannel) cc).setScreenNumber(getScreenNumber());
				((ChangeChannel) cc).setChannelNumber(((Channel) content).getNumber());
				((ChangeChannel) cc).setContent(content);
			}
			MessageCarrierXML mcx = new MessageCarrierXML("Display" + displayID, "Database", cc);
			mcx.setTimeStamp(150000000000L); // We make a common date here so SQL can match identical content

			String xmlDocument = MyXMLMarshaller.getXML(mcx);
			println(getClass(), "New content is " + xmlDocument.length() + " bytes long.");
			if (xmlDocument.length() > 32767) {
				printlnErr(getClass(), "This XML document length, " + xmlDocument.length() + ", is too long for the database");
			} else {
				Connection connection;
				try {
					connection = ConnectionToDatabase.getDbConnection();
					String statementString = "";
					synchronized (connection) {
						try (Statement stmt = connection.createStatement();
								ResultSet result = stmt.executeQuery("USE " + DATABASE_NAME);) {
							statementString = "INSERT INTO ContentHistory VALUES(NULL, " + displayID + ", CURRENT_TIMESTAMP, '"
									+ xmlDocument + "')";
							@SuppressWarnings("unused")
							int numRows = stmt.executeUpdate(statementString);
							stmt.close();
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		// The SQL query that will read which Channels are the most prolific for this Display is:

		// SELECT ContentSpecification,COUNT(*) FROM ContentHistory WHERE DisplayID=46 GROUP BY ContentSpecification

	}

	protected final synchronized void updateMyStatus() {

	}

	@Override
	public Color getPreferredHighlightColor() {
		return null;
	}

	@Override
	public int getDBDisplayNumber() {
		return displayID;
	}

	@Override
	public int getVirtualDisplayNumber() {
		// This is routinely called
		return displayID;
	}

	@Override
	public void setDBDisplayNumber(int d) {
		displayID = d;
	}

	@Override
	public void setVirtualDisplayNumber(int v) {
	}

	@Override
	public int getScreenNumber() {
		// This is routinely called
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
		return "Dummy display for listening";
	}

	@Override
	public String getLocation() {
		return "Dummy display (location) for listening";
	}

	@Override
	public String getStatus() {
		return "Hunky dorie";
	}

	@Override
	public String getMessagingName() {
		return "DummyName";
	}

	@Override
	public void disconnect() {
		println(getClass(), "disconnect()");
	}

	@Override
	public void errorHandler(String message) {
		println(getClass(), "errorHandler()");
	}

}
