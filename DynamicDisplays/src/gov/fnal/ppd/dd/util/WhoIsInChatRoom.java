/*
 * WhoIsInChatRoom
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.ONE_SECOND;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.util.GeneralUtilities.catchSleep;
import static gov.fnal.ppd.dd.util.GeneralUtilities.println;
import static gov.fnal.ppd.dd.util.GeneralUtilities.printlnErr;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import javax.xml.bind.JAXBException;

import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.YesIAmAliveMessage;

/**
 * Helper class to keep track of who is in the chat room. Used by the top-level GUI(s) to say if a Display is available.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class WhoIsInChatRoom extends Thread {

	private static final boolean	debug					= PropertiesFile.getBooleanProperty("WhoIsInChatRoomVerbose", false);
	private static final long		WAIT_TIME_INCREMENT		= 100L;
	private static final int		PATIENCE_VALUE			= 20;
	private static final long		ABSOLUTE_MAX_WAIT_TIME	= 5 * ONE_SECOND;

	private MessagingClient			client;
	private boolean[]				aliveList				= null;
	@SuppressWarnings("unused")
	private boolean[]				lastAliveList			= null;
	private DisplayKeeper			alive;
	private long					timeToWaitForResponses	= 2 * ONE_SECOND;
	private long					minimumWaitTime			= ONE_SECOND;
	private int						numTriesAtMinimum		= 0;

	/**
	 * @param alive
	 */
	public WhoIsInChatRoom(final DisplayKeeper alive) {
		super(WhoIsInChatRoom.class.getSimpleName());
		this.alive = alive;
	}

	public void run() {
		login();
		long sleepTime = 1000;
		while (true) {
			// Note that the total cycle time of this loop is sleepTime + timeToWaitForResponses. We want this cycle time to be as
			// short as possible so the user can see all the living displays as quickly as possible
			catchSleep(sleepTime);
			sleepTime = 10 * ONE_SECOND;

			lastAliveList = aliveList;
			aliveList = new boolean[displayList.size()];
			for (int i = 0; i < displayList.size(); i++)
				aliveList[i] = false;

			if (debug)
				println(getClass(), "Sending query to server, " + getMessagingServerName() + ":" + MESSAGING_SERVER_PORT
						+ ", under the name " + client.getName());

			MessageCarrierXML m = MessageCarrierXML.getWhoIsIn(client.getName(), getMessagingServerName());

			if (debug) {
				String theMessageString = "exception";
				try {
					theMessageString = MyXMLMarshaller.getXML(m);
				} catch (JAXBException e) {
					e.printStackTrace();
				}
				println(getClass(), "Message to send: " + m + "\n\t" + theMessageString);
			}
			client.sendMessage(m);

			catchSleep(timeToWaitForResponses);
			// Wait long enough for all the messages to come in. One second is almost always enough. But there is the occasional
			// time when even 2 seconds is not enough. 3 seconds seems always to be enough.

			synchronized (aliveList) {
				int numAlive = 0;
				for (int i = 0; i < displayList.size(); i++) {
					// if (lastAliveList == null || lastAliveList[i] != aliveList[i])

					// Note that this call will end up calling the "setEnable()" method for all of the channel buttons that this
					// display owns, so it does a lot. So that we are not wasting time and energy setting a button to be enabled
					// when it is already enabled, the cycle time on this thread should be as long as possible.
					
					alive.setDisplayIsAlive(displayList.get(i).getDBDisplayNumber(), aliveList[i]);
					numAlive += (aliveList[i] ? 1 : 0);
				}
				if (debug) {
					println(getClass(), "numAlive=" + numAlive + " out of " + aliveList.length);
				}
				setTimeToWaitForResponses(numAlive == aliveList.length);
			}
		}
	}

	private void setTimeToWaitForResponses(boolean didTheyAllRespond) {
		// The default that seems to work most of the time is 2 seconds. But occasionally, this gets delayed, so we have to lengthen
		// this time. I made this method so I could try to be smart about it. If we are always getting all of them to respond, the
		// we can reduce the wait time. If we do not see all of the responses, then we should increase the wait time until we DO see
		// them all.

		// On my Linux test server, wait times as low as 1300 ms work well, and 1900 works virtually all the time

		// On a Windows PC, we're seeing that 2500 works most of the time.

		if (didTheyAllRespond) {
			timeToWaitForResponses -= WAIT_TIME_INCREMENT;

			if (timeToWaitForResponses < minimumWaitTime)
				timeToWaitForResponses = minimumWaitTime;
			if (timeToWaitForResponses == minimumWaitTime) {
				if (numTriesAtMinimum++ >= PATIENCE_VALUE) {
					numTriesAtMinimum = 0;
					// We sat at this wait time for "PATIENCE_VALUE" cycles, so let's decrement the minimum wait time a bit.
					minimumWaitTime -= WAIT_TIME_INCREMENT;
				}
			}
		} else {
			// The last wait time was too long; reset the minimum.
			minimumWaitTime = timeToWaitForResponses + WAIT_TIME_INCREMENT;
			numTriesAtMinimum = 0;
			timeToWaitForResponses += (PATIENCE_VALUE / 4) * WAIT_TIME_INCREMENT;
		}

		// This is relevant since we often have a controller that cannot actually see all of its displays (that is, one of the
		// displays is powered down, or the network is screwed up). It would be nice to actually KNOW if a display is down so the
		// wait times can be set more intelligently. But, alas, ...
		if (timeToWaitForResponses > ABSOLUTE_MAX_WAIT_TIME)
			timeToWaitForResponses = ABSOLUTE_MAX_WAIT_TIME;

		if (debug)
			println(getClass(), "timeToWaitForResponses = " + timeToWaitForResponses);
	}

	private void login() {
		try {
			long ran = new Date().getTime() % 1000L;
			final String myName = InetAddress.getLocalHost().getCanonicalHostName() + "_WHO_" + ran;

			println(getClass(), "**Starting messaging client named '" + myName + "' for listening to who is in the system");
			client = new MessagingClient(getMessagingServerName(), MESSAGING_SERVER_PORT, myName) {
				@Override
				public void receiveIncomingMessage(final MessageCarrierXML msg) {
					if (debug)
						println(WhoIsInChatRoom.class, "See a message from [" + msg.getMessageOriginator() + "]");

					if (msg.getMessageValue() instanceof YesIAmAliveMessage) {
						YesIAmAliveMessage mess = (YesIAmAliveMessage) msg.getMessageValue();
						String clientName = mess.getClient().getName();
						synchronized (aliveList) {
							for (int i = 0; i < displayList.size(); i++)
								if (displayList.get(i).getMessagingName().equals(clientName)
										|| clientName.startsWith(displayList.get(i).getMessagingName())) {
									aliveList[i] = true;
									if (debug)
										println(WhoIsInChatRoom.class,
												"Display no. " + displayList.get(i).getDBDisplayNumber() + " is alive");
								} else if (debug) {
									println(WhoIsInChatRoom.class,
											"Display no. " + displayList.get(i).getDBDisplayNumber() + " is NOT alive, I think");
								}
						}
					}
				}
			};

			// start the Client
			if (!client.start()) {
				printlnErr(getClass(), "Problem starting messaging client");
				return;
			}
			println(getClass(), "Started messaging client");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
