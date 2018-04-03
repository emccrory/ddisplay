package gov.fnal.ppd.dd.channel.list;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;
import gov.fnal.ppd.dd.changer.ConnectionToDynamicDisplaysDatabase;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Database utility to read all the lists of channels from the database
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2018
 * 
 */
public class AllChannelLists extends HashMap<String, ChannelPlayList> {

	private static final long	serialVersionUID	= 5785358969009813539L;

	/**
	 * read the channel lists from the database and hold them in this here HashMap.
	 */
	public AllChannelLists() {
		readTheChannelLists();
	}

	private void readTheChannelLists() {
		Map<String, ArrayList<SignageContent>> allChannelLists = new HashMap<String, ArrayList<SignageContent>>();
		clear();

		try {

			Connection connection = ConnectionToDynamicDisplaysDatabase.getDbConnection();
			String query = "SELECT ChannelList.ListNumber AS ListNumber,Number,Dwell,ChangeTime,SequenceNumber,ListName,ListAuthor"
					+ " FROM ChannelList,ChannelListName WHERE ChannelList.ListNumber=ChannelListName.ListNumber ORDER BY SequenceNumber ASC";

			synchronized (connection) {
				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
					try (ResultSet rs2 = stmt.executeQuery(query)) {
						if (rs2.first())
							do {
								String listName = rs2.getString("ListName");
								String listAuthor = rs2.getString("ListAuthor");
								// Special case -- abbreviate the code author's name here
								if (listAuthor.toLowerCase().contains("mccrory"))
									listAuthor = "EM";
								// int listNumber = rs2.getInt("ListNumber");
								long dwell = rs2.getLong("Dwell");
								int sequence = rs2.getInt("SequenceNumber");
								int chanNumber = rs2.getInt("Number");

								String fullName = listName + " (" + listAuthor + ")";
								if (!allChannelLists.containsKey(fullName)) {
									ArrayList<SignageContent> list = new ArrayList<SignageContent>(sequence + 1);
									allChannelLists.put(fullName, list);
								}
								Channel chan = (Channel) getChannelFromNumber(chanNumber);
								ChannelInList cih = new ChannelInList(chan, sequence, dwell);

								allChannelLists.get(fullName).add(cih);

							} while (rs2.next());
						else {
							// Oops. no first element!?
							throw new Exception("No channel lists in the save/restore database!");
						}
					} catch (SQLException e) {
						System.err.println(query);
						e.printStackTrace();
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}

			}
			for (String channelListName : allChannelLists.keySet()) {
				ChannelPlayList theList = new ChannelPlayList(allChannelLists.get(channelListName), 60000000);
				theList.setDescription(allChannelLists.get(channelListName).toString());
				put(channelListName, theList);
				// println(ExistingChannelLists.class, ": added list of length " + theList.getChannels().size() + " called '" +
				// channelListName + "'");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
