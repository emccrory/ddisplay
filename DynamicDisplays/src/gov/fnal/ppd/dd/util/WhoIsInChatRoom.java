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
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessagingClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * Helper class to keep track of who is in the chat room. Used by the top-level GUI(s) to say if a Display is available.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class WhoIsInChatRoom extends Thread {

	private MessagingClient	client;
	private boolean[]		aliveList		= null;
	private boolean[]		lastAliveList	= null;
	private DisplayKeeper	alive;

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

			client.sendMessage(MessageCarrier.getWhoIsIn(client.getName()));

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
			final String myName = InetAddress.getLocalHost().getCanonicalHostName() + "_" + ran;

			System.out.println("\n**Starting messaging client named '" + myName + "' for listening to who is in the system");
			client = new MessagingClient(getMessagingServerName(), MESSAGING_SERVER_PORT, myName) {
				@Override
				public void receiveIncomingMessage(final MessageCarrier msg) {
					String clientName = msg.getFrom();
					// System.out.println("WhoIsInChatRoom: see a message from [" + clientName + "]");
					if (clientName.toLowerCase().contains("façade"))
						return;
					System.out.println(new Date() + " WhoIsInChatRoom: see a message from [" + clientName + "]");
					synchronized (aliveList) {
						for (int i = 0; i < displayList.size(); i++)
							if (displayList.get(i).getMessagingName().equals(clientName)
									|| clientName.startsWith(displayList.get(i).getMessagingName())) {
								aliveList[i] = true;
								System.out.println(new Date() + " WhoIsInChatRoom: Display no. "
										+ displayList.get(i).getDBDisplayNumber() + " is alive");
							}
					}
				}
			};

			// start the Client
			if (!client.start())
				return;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

}
