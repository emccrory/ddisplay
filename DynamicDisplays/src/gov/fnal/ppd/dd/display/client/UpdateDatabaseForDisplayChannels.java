package gov.fnal.ppd.dd.display.client;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

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
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

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
				((ChangeChannelList) cc).setContent(getContent());
			} else {
				cc = new ChangeChannel();
				((ChangeChannel) cc).setDisplayNumber(getVirtualDisplayNumber());
				((ChangeChannel) cc).setScreenNumber(getScreenNumber());
				((ChangeChannel) cc).setContent(content);
			}
			MessageCarrierXML mcx = new MessageCarrierXML("Display" + displayID, "Database", cc);
			mcx.setTimeStamp(150000000000L);  // We make a common date here so SQL can match identical content

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
		println(getClass(), "getPreferredHighlightColor()");
		return null;
	}

	@Override
	public int getDBDisplayNumber() {
		println(getClass(), "getDBDisplayNumber()");
		return displayID;
	}

	@Override
	public int getVirtualDisplayNumber() {
		println(getClass(), "getVirtualDisplayNumber()");
		return displayID;
	}

	@Override
	public void setDBDisplayNumber(int d) {
		println(getClass(), "setDBDisplayNumber()");
		displayID = d;
	}

	@Override
	public void setVirtualDisplayNumber(int v) {
		println(getClass(), "setVirtualDisplayNumber(" + v + ")");
	}

	@Override
	public int getScreenNumber() {
		println(getClass(), "getScreenNumber()");
		return 0;
	}

	@Override
	public InetAddress getIPAddress() {
		println(getClass(), "getIPAddress()");
		return null;
	}

	@Override
	public void addListener(ActionListener L) {
		println(getClass(), "addListener()");
	}

	@Override
	public String getDescription() {
		println(getClass(), "getDescription()");
		return "Dummy display for listening";
	}

	@Override
	public String getLocation() {
		println(getClass(), "getLocation()");
		return "Dummy display (location) for listening";
	}

	@Override
	public String getStatus() {
		println(getClass(), "getStatus()");
		return "Hunky dorie";
	}

	@Override
	public String getMessagingName() {
		println(getClass(), "getMessagingName()");
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
