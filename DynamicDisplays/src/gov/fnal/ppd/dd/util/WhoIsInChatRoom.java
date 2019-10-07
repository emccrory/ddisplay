/*
 * WhoIsInChatRoom
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.GlobalVariables.getMessagingServerName;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import gov.fnal.ppd.dd.chat.MessagingClient;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.YesIAmAliveMessage;

/**
 * Helper class to keep track of who is in the chat room. Used by the top-level GUI(s) to say if a Display is available.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class WhoIsInChatRoom extends Thread {

	private MessagingClient	client;
	private boolean[]		aliveList		= null;
	@SuppressWarnings("unused")
	private boolean[]		lastAliveList	= null;
	private DisplayKeeper	alive;
	private boolean			debug			= false;

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
			catchSleep(sleepTime);
			sleepTime = 10000;

			lastAliveList = aliveList;
			aliveList = new boolean[displayList.size()];
			for (int i = 0; i < displayList.size(); i++)
				aliveList[i] = false;

			if (debug)
				println(getClass(), "Sending query to server, " + getMessagingServerName() + ":" + MESSAGING_SERVER_PORT
						+ ", under the name " + client.getName());

			client.sendMessage(MessageCarrierXML.getWhoIsIn(client.getName()));

			catchSleep(2000); // Wait long enough for all the messages to come in. This is almost always enough. In fact, less than
								// one second is almost always enough. But there is the occasional time when even 2 seconds is not
								// enough

			synchronized (aliveList) {
				for (int i = 0; i < displayList.size(); i++)
					// if (lastAliveList == null || lastAliveList[i] != aliveList[i])
					alive.setDisplayIsAlive(displayList.get(i).getDBDisplayNumber(), aliveList[i]);
			}
		}
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
