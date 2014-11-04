package gov.fnal.ppd.signage.util;

import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_NAME;
import static gov.fnal.ppd.GlobalVariables.MESSAGING_SERVER_PORT;
import static gov.fnal.ppd.GlobalVariables.displayList;
import gov.fnal.ppd.chat.MessageCarrier;
import gov.fnal.ppd.chat.MessagingClient;

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
			try {
				sleep(sleepTime);
				sleepTime = 10000;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			lastAliveList = aliveList;
			aliveList = new boolean[displayList.size()];
			for (int i = 0; i < displayList.size(); i++) {
				aliveList[i] = false;
			}
			client.sendMessage(MessageCarrier.getWhoIsIn(client.getName()));

			try {
				sleep(2000); // Wait long enough for all the messages to come in.
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for (int i = 0; i < displayList.size(); i++) {
				if (lastAliveList == null || lastAliveList[i] != aliveList[i])
					alive.setDisplayIsAlive(displayList.get(i).getNumber(), aliveList[i]);
			}
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
					System.out.println("A client named '" + clientName + "' is alive.  Do we care?");
					for (int i = 0; i < displayList.size(); i++) {
						System.out.println("How about " + displayList.get(i).getMessagingName());
						if (getRootName(displayList.get(i).getMessagingName()).equals(clientName)) {
							System.out.println("A client named '" + clientName + "' is a Display I know about!");
							// setDisplayIsAlive(D.getNumber(), true);
							aliveList[i] = true;
						}
					}
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
