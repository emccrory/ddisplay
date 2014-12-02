package gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.dd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.dd.GlobalVariables.displayList;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.chat.MessageCarrier;
import gov.fnal.ppd.dd.chat.MessagingClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
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

			catchSleep(2000); // Wait long enough for all the messages to come in.

			for (int i = 0; i < displayList.size(); i++)
				if (lastAliveList == null || lastAliveList[i] != aliveList[i])
					alive.setDisplayIsAlive(displayList.get(i).getNumber(), aliveList[i]);
		}
	}

	private void login() {
		try {
			long ran = new Date().getTime() % 1000L;
			final String myName = InetAddress.getLocalHost().getCanonicalHostName() + "_" + ran;

			System.out.println("\n**Starting messaging client named '" + myName + "'");
			client = new MessagingClient(MESSAGING_SERVER_NAME, MESSAGING_SERVER_PORT, myName) {
				@Override
				public void displayIncomingMessage(final MessageCarrier msg) {
					String clientName = msg.getFrom();
					if (clientName.toLowerCase().contains("façade"))
						return;
					for (int i = 0; i < displayList.size(); i++)
						if (getRootName(displayList.get(i).getMessagingName()).equals(clientName))
							aliveList[i] = true;
				}

				private String getRootName(String n) {
					return n.substring(0, n.indexOf(" -- "));
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
