package gov.fnal.ppd.dd.util.specific;

import static gov.fnal.ppd.dd.GlobalVariables.ONE_BILLION;
import static gov.fnal.ppd.dd.GlobalVariables.SINGLE_IMAGE_DISPLAY;
import static gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities.println;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.display.client.DisplayControllerMessagingAbstract;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.nonguiUtils.GeneralUtilities;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.messages.ChangeChannelList;

public class PackageUtilities {
	/**
	 * The default URL for a Display
	 */
	public static final String	MY_URL	= "https://dynamicdisplays.fnal.gov/standby.html";
	/**
	 * The name that is associated with the default URL for a Display
	 */
	public static final String	MY_NAME	= "unspecified content";

	private PackageUtilities() {
		// Do not instantiate
	}

	/**
	 * @param url
	 *            The URL to show in this empty channel (which can be null)
	 * @return An empty channel
	 */
	public static SignageContent makeEmptyChannel(String url) {
		try {
			return new ChannelImpl(MY_NAME, "This is a default channel", new URI(url == null ? MY_URL : url), 0, 0);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * @param d
	 *            The display to identify
	 * @return A unique string to identify this display.
	 */
	public static String getDisplayID(final Display d) {
		return "Display " + d.getVirtualDisplayNumber() + "/" + d.getDBDisplayNumber();

	}

	/**
	 * @param content
	 *            The Content object to be turned into a string
	 * @return the content converted to a DB-ready string
	 */
	public static String convertContentToDBReadyString(final SignageContent theContent) {
		if (theContent == null)
			return null;

		String blob = null;

		String xmlDocument = "";
		try {
			if (theContent instanceof ChannelPlayList) {
				ChangeChannelList ccl = new ChangeChannelList(); // This is the XML class
				ccl.setContent((ChannelPlayList) theContent);
				xmlDocument = MyXMLMarshaller.getXML(ccl);
			} else {
				ChannelSpec cs = new ChannelSpec(theContent); // This is the XML class
				xmlDocument = MyXMLMarshaller.getXML(cs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// blob = xmlDocument.replace("'", "").replace("\"", "");
		// I think we need to remove all the tick marks and the quote marks from the body of the XML specification of the
		// channel.
		// This global removal of them breaks things downstream.
		blob = xmlDocument;

		return blob;
	}

	private static HashMap<Integer, Channel> alreadyRetrieved = new HashMap<Integer, Channel>();

	/**
	 * @param channelNumber
	 *            The channel number to look up
	 * @return The channel that is specified by this channel number
	 */
	public static SignageContent getChannelFromNumber(int channelNumber) {
		if (alreadyRetrieved.containsKey(channelNumber))
			return new ChannelImpl(alreadyRetrieved.get(channelNumber));

		Channel retval = null;
		try {
			retval = new ChannelImpl("Fermilab", ChannelClassification.MISCELLANEOUS, "Fermilab", new URI("https://www.fnal.gov"),
					0, 360000L);
		} catch (URISyntaxException e2) {
			e2.printStackTrace();
		}

		if (channelNumber == 0) {
			// Using the default, Fermilab home page!
			println(GeneralUtilities.class, " Using the Fermilab home page as the default channel!");
			return retval;
		} else if (channelNumber < 0) {
			// The default channel is a list of channels. Build it!

			// This is the old query that got all the channels, but ignored the portfolio entries
			// String query = "select Channel.Number as Number,Dwell,Name,Description,URL,SequenceNumber,Sound FROM
			// Channel,ChannelList where ListNumber=" + (-channelNumber) + " and Channel.Number=ChannelList.Number ORDER BY SequenceNumber";

			String query = "SELECT Number,Dwell,SequenceNumber from ChannelList where ListNumber=" + (-channelNumber);
			
			println(DisplayControllerMessagingAbstract.class, " -- Getting default channel list: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			ChannelListHolder channelList = new ConcreteChannelListHolder();

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (!rs.first()) {
							return retval;
						}
						do {
							int chanNum = rs.getInt("Number");
							int dwell = rs.getInt("Dwell");
							int seq = rs.getInt("SequenceNumber");

							SignageContent content = getChannelFromNumber(chanNum); // Recursive call - will this work??
							ChannelInList c = new ChannelInListImpl(content.getName(), ChannelClassification.MISCELLANEOUS,
									content.getDescription(), content.getURI(), chanNum, dwell);
							c.setSequenceNumber(seq);
							c.setCode(content.getCode());
							channelList.channelAdd(c);
						} while (rs.next());

						retval = new ChannelPlayList(channelList, 60000L);
						alreadyRetrieved.put(channelNumber, retval);
						stmt.close();
						rs.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		} else if (channelNumber < ONE_BILLION) {
			// Channel number is positive and "small": this is a single channel.
			//
			// WARNING -- We assume here that the automatically-generated "channelNumber" in the database is incremented, as
			// opposed to chosen randomly. It is possible that some future MySQL implementation will do random index number
			// generation, although this is unlikely. Something to be aware of!
			//
			String query = "SELECT URL,DwellTime,Description,Name,Sound FROM Channel WHERE Number=" + channelNumber;
			// println(DisplayControllerMessagingAbstract.class, " -- Getting default channel: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							String url = rs.getString("URL");
							int dwell = rs.getInt("DwellTime");
							String desc = rs.getString("Description");
							String name = rs.getString("Name");
							int specialCode = rs.getInt("Sound");

							retval = new ChannelImpl(name, ChannelClassification.MISCELLANEOUS, desc, new URI(url), channelNumber,
									dwell);
							retval.setCode(specialCode);

							alreadyRetrieved.put(channelNumber, retval);

							stmt.close();
							rs.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		} else {
			// This is an image. Look up that image number and then make the URL that shows this image.
			int portfolioID = channelNumber - ONE_BILLION;
			String query = "SELECT Filename,Description FROM Portfolio where PortfolioID=" + portfolioID
					+ " AND Approval='Approved'";
			// println(DisplayControllerMessagingAbstract.class, " -- Getting a portfolio image: [" + query + "]");
			Connection connection;
			try {
				connection = ConnectionToDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				return null;
			}

			synchronized (connection) {
				try (Statement stmt = connection.createStatement();) {
					try (ResultSet rs = stmt.executeQuery(query);) {
						if (rs.first()) { // Move to first returned row (there should only be one)
							String desc = rs.getString("Description");
							String filename = rs.getString("Filename");

							String url = SINGLE_IMAGE_DISPLAY + URLEncoder.encode(filename, "UTF-8") + "&caption="
									+ URLEncoder.encode(desc, "UTF-8");

							String[] array = filename.split("/", -1);
							retval = new ChannelImpl(array[array.length - 1], ChannelClassification.IMAGE, desc, new URI(url),
									channelNumber, 0L);
							alreadyRetrieved.put(channelNumber, retval);

							stmt.close();
							rs.close();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (SQLException ex) {
					System.err.println("It is likely that the DB server is down.  We'll try again later.");
					ex.printStackTrace();
				}
			}
		}

		return retval;
	}

}
