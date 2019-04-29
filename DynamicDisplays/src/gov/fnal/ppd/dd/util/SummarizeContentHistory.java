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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.fnal.ppd.dd.chat.ErrorProcessingMessage;
import gov.fnal.ppd.dd.db.ConnectionToDatabase;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.EncodedCarrier;
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

	private static final String				CHANGE_CHANNEL		= "<changeChannel ";
	private static final String				CHANGE_CHANNEL_LIST	= "<changeChannelList ";

	private static final Comparator<Two>	cTwo				= new Comparator<Two>() {
																	@Override
																	public int compare(Two o1, Two o2) {
																		if (o1.equals(o2))
																			return 0;
																		return ((Integer) o1.displayID).compareTo(o2.displayID);
																	}
																};

	private List<Two>						all					= new ArrayList<Two>();
	private int								cut;

	private static class Two {
		public EncodedCarrier	chan	= null;
		public int				displayID;

		public Two(int d, EncodedCarrier x) {
			displayID = d;
			chan = x;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Two other = (Two) obj;
			if (chan == null) {
				if (other.chan != null)
					return false;
			} else if (!chan.equals(other.chan))
				return false;
			if (displayID != other.displayID)
				return false;
			return true;
		}

		public String getURL() {
			if (chan instanceof ChangeChannel)
				return ((ChangeChannel) chan).getChannelSpec().getURI().toString();
			else if (chan instanceof ChangeChannelList)
				return ((ChangeChannelList) chan).getChannelSpec()[0].getURI().toString();
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

		public String toString() {
			return "" + displayID + "\t" + getURL();
		}

	}

	public static void main(String[] args) {
		credentialsSetup();
		selectorSetup();

		int cut = 0;
		if (args.length > 0)
			cut = Integer.parseInt(args[0]);
		SummarizeContentHistory s = new SummarizeContentHistory(cut, 1, "2019/04/01");
		System.out.println(s);
		System.exit(0);
	}

	// ------------------------------------------------------------------------------------------------------------------------------

	public SummarizeContentHistory() {
		this(1);
	}

	public SummarizeContentHistory(int cut) {
		this.cut = cut;
		get("select DisplayID,ContentSpecification FROM ContentHistory");
	}

	public SummarizeContentHistory(int cut, int displayID) {
		this.cut = cut;
		get("select DisplayID,ContentSpecification FROM ContentHistory WHERE DisplayID=" + displayID);
	}

	public SummarizeContentHistory(int cut, String since) {
		this.cut = cut;
		get("select DisplayID,ContentSpecification FROM ContentHistory WHERE IntitiateTimeStamp>'" + since + "'");
	}

	public SummarizeContentHistory(int cut, int displayID, String since) {
		this.cut = cut;
		get("select DisplayID,ContentSpecification FROM ContentHistory WHERE DisplayID=" + displayID + " AND IntitiateTimeStamp>'"
				+ since + "'");
	}

	// ------------------------------------------------------------------------------------------------------------------------------

	private Map<Two, Integer> count(List<Two> all) {
		Map<Two, Integer> retval = new HashMap<Two, Integer>();
		for (Two T : all) {
			if (retval.containsKey(T)) {
				Integer I = retval.get(T);
				retval.replace(T, I, I + 1);
			} else
				retval.put(T, new Integer(1));
		}
		return retval;
	}

	private void get(String query) {
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
								String rawMessage = rs2.getString("ContentSpecification");

								if (rawMessage.contains(CHANGE_CHANNEL)) {
									all.add(new Two(displayID,
											(ChangeChannel) MyXMLMarshaller.unmarshall(ChangeChannel.class, rawMessage)));
								} else if (rawMessage.contains(CHANGE_CHANNEL_LIST)) {
									all.add(new Two(displayID,
											(ChangeChannelList) MyXMLMarshaller.unmarshall(ChangeChannelList.class, rawMessage)));
								} else {
									throw new ErrorProcessingMessage(
											"Unknown XML data type within this XML document: [Unrecognized XML message received.");
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

		Collections.sort(all, cTwo);
	}

	public List<String> getAllURLs() {
		List<String> retval = new ArrayList<String>();
		for (Two T : all) {
			retval.add(T.getURL());
		}
		return retval;
	}

	public String toString() {
		String retval = "";
		Map<Two, Integer> allCount = count(all);

		System.out.println("\n\nCount\tDisp#\tURL");
		for (Two T : allCount.keySet()) {
			if (allCount.get(T) > cut)
				retval += allCount.get(T) + "\t" + T + "\n";
		}
		return retval;
	}
}
