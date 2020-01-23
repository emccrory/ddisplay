/*
 * GetColorsFromDatabase
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.DATABASE_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.UnmarshalException;

import gov.fnal.ppd.dd.CredentialsNotFoundException;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

/**
 * Look at the ContentHistory table in the Dynamic Displays database.
 * 
 * This stand-alone, non-GUI program shows you the URLs that have been sent to the various displays.
 * 
 * The use-case is to determine if the tab containing the "most popular" channels for a display should be updated.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class SummarizeContentHistory {

	private static final Comparator<Three>	cThree	= new Comparator<Three>() {
														@Override
														public int compare(Three o1, Three o2) {
															if (o1.equals(o2))
																return 0;
															if (o1.displayID == o2.displayID)
																return ((Integer) o2.count).compareTo((Integer) o1.count);				// Descending
															return ((Integer) o1.displayID).compareTo(o2.displayID);					// Ascending
														}
													};

	private List<Three>						all		= new ArrayList<Three>();
	private int								cut;

	private static class Three {
		public MessagingDataXML	chan	= null;
		public int					displayID, virtualDisplayID;
		public int					count	= 0;

		public Three(int dbID, int c, int vDID, MessagingDataXML x) {
			displayID = dbID;
			virtualDisplayID = vDID;
			count = c;
			chan = x;
		}

		public String getURL() {
			if (chan instanceof ChangeChannelList)
				// This is probably OK, but it is not exactly right
				return ((ChangeChannelList) chan).getChannelSpec()[0].getURI().toString();
			else if (chan != null)
				return ((ChangeChannel) chan).getChannelSpec().getURI().toString();
			return null;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((chan == null) ? 0 : chan.hashCode());
			result = prime * result + displayID;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Three other = (Three) obj;
			if (chan == null) {
				if (other.chan != null)
					return false;
			} else if (!chan.equals(other.chan))
				return false;

			if (displayID != other.displayID)
				return false;
			return true;
		}

		public String toString() {
			if (listLength() == 0)
				return count + "\t" + displayID + " | " + virtualDisplayID + "\t" + getURL();

			// return count + "\t" + displayID + " | " + virtualDisplayID + "\t" + Arrays.toString(((ChangeChannelList)
			// chan).getChannelSpec());
			return count + "\t" + displayID + " | " + virtualDisplayID + "\t" + chan;
		}

		public int listLength() {
			if (chan != null && chan instanceof ChangeChannelList) {
				return ((ChangeChannelList) chan).getChannelSpec().length;
			}
			return 0;
		}

		public static String getHeader() {
			return "Count\tDisp\tURL";
		}
	}

	public static void main(String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		selectorSetup();

		int cut = 0;
		if (args.length > 0)
			cut = Integer.parseInt(args[0]);
		SummarizeContentHistory s = new SummarizeContentHistory(cut, "2019/04/01");
		System.out.println(s);
		System.exit(0);
	}

	// ------------------------------------------------------------------------------------------------------------------------------

	public SummarizeContentHistory() {
		this(1);
	}

	public SummarizeContentHistory(int cut) {
		this.cut = cut;
		get("select ContentHistory.DisplayID as DisplayID,ContentSpecification,COUNT(*),VirtualDisplayNumber "
				+ "FROM ContentHistory,Display WHERE ContentHistory.DisplayID=Display.DisplayID");
	}

	public SummarizeContentHistory(int cut, int displayID) {
		this.cut = cut;
		get("select ContentHistory.DisplayID as DisplayID,ContentSpecification,COUNT(*),VirtualDisplayNumber "
				+ "FROM ContentHistory,Display WHERE DisplayID=" + displayID
				+ " AND ContentHistory.DisplayID=Display.DisplayID GROUP BY ContentSpecification");
	}

	public SummarizeContentHistory(int cut, String since) {
		this.cut = cut;
		get("select ContentHistory.DisplayID as DisplayID,ContentSpecification,COUNT(*),VirtualDisplayNumber "
				+ "FROM ContentHistory,Display WHERE IntitiateTimeStamp>'" + since + "'"
				+ " AND ContentHistory.DisplayID=Display.DisplayID GROUP BY ContentSpecification");
	}

	public SummarizeContentHistory(int cut, int displayID, String since) {
		this.cut = cut;
		get("select ContentHistory.DisplayID as DisplayID,ContentSpecification,COUNT(*),VirtualDisplayNumber "
				+ "FROM ContentHistory,Display WHERE DisplayID=" + displayID
				+ " AND ContentHistory.DisplayID=Display.DisplayID AND IntitiateTimeStamp>'" + since + "'"
				+ " GROUP BY ContentSpecification");
	}

	// ------------------------------------------------------------------------------------------------------------------------------

	private void get(String query) {
		System.out.println(query);
		int unmarshallExceptions = 0, classCastExceptions = 0, oldChangeChannel = 0;
		try {
			Connection connection = null;
			try {
				connection = ConnectionToDatabase.getDbConnection();
			} catch (DatabaseNotVisibleException e1) {
				e1.printStackTrace();
				System.err.println("\nNo connection to the Signage/Displays database.");
				System.exit(-1);
				return;
			}

			synchronized (connection) {
				// Use ARM to simplify these try blocks.
				try (Statement stmt = connection.createStatement(); ResultSet rs1 = stmt.executeQuery("USE " + DATABASE_NAME)) {
					try (ResultSet rs2 = stmt.executeQuery(query);) {
						if (rs2.first()) {
							do {
								int displayID = rs2.getInt("DisplayID");
								int vDID = rs2.getInt("VirtualDisplayNumber");
								String rawMessage = rs2.getString("ContentSpecification");
								int count = rs2.getInt("COUNT(*)");
								if (count < cut)
									continue;

								try {
									if (rawMessage.contains("messageCarrierXML")) {
										MessageCarrierXML content = (MessageCarrierXML) MyXMLMarshaller
												.unmarshall(MessageCarrierXML.class, rawMessage);
										all.add(new Three(displayID, count, vDID, content.getMessageValue()));
									} else if ( rawMessage.contains("changeChannelList")) {
										ChangeChannelList theList = (ChangeChannelList) MyXMLMarshaller
												.unmarshall(ChangeChannelList.class, rawMessage);
										all.add(new Three(displayID, count, vDID, theList));
									} else if ( rawMessage.contains("changeChannel")) {
										// Why doesn't this work? Must be some subtle change in the object structure
										// ChangeChannel theChannel = (ChangeChannel) MyXMLMarshaller.unmarshall(ChangeChannel.class, rawMessage);
										// all.add(new Three(displayID, count, vDID, theChannel));
										oldChangeChannel++;
									} else {
										System.out.println("\nOTHER\n" + rawMessage);
									}
								} catch (UnmarshalException e1) {
									unmarshallExceptions++;
									e1.printStackTrace();
									if (unmarshallExceptions > 10)
										System.exit(0);
								} catch (ClassCastException e2) {
									classCastExceptions++;
								}
							} while (rs2.next());
						} else {
							new Exception().printStackTrace();
							System.exit(-1);
						}
					}
				} catch (SQLException e) {
					System.err.println(query);
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Collections.sort(all, cThree);
		System.out.println("Old change channels: " + oldChangeChannel + ", Unmarshal exceptions: " + unmarshallExceptions + ", class cast exceptions: " + classCastExceptions);
	}

	public List<String> getAllURLs() {
		List<String> retval = new ArrayList<String>();
		for (Three T : all) {
			retval.add(T.getURL());
		}
		return retval;
	}

	public String toString() {
		String retval = "";

		System.out.println("\n\n" + Three.getHeader());
		for (Three T : all) {
			retval += T + "\n";
		}
		return retval;
	}
}
