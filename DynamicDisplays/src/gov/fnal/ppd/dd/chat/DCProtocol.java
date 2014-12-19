package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import gov.fnal.ppd.dd.channel.PlainURLChannel;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.EncodedCarrier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * <p>
 * This is the protocol for the messages between a Channel Changer and a Display.
 * </p>
 * <p>
 * <em>DC
 * </p>
 * stands for <em>Display Communications</p</p>
 * 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copyright 2014
 * 
 */
public class DCProtocol {
	// private enum State {
	// WAITING, READY, DISPLAYING
	// };

	private Object			theMessage;
	private Object			theReply;
	private List<Display>	listeners			= new ArrayList<Display>();
	protected boolean		keepRunning			= false;
	private Thread			changerThread		= null;
	protected long			SHORT_INTERVAL		= 100l;
	private long			lastServerHeartbeat	= 0L;

	/**
	 * @return The message just received
	 */
	public Object getTheMessage() {
		return theMessage;
	}

	/**
	 * @return The reply that is relevant
	 */
	public Object getTheReply() {
		return theReply;
	}

	/**
	 * @param listener
	 */
	public void addListener(final Display listener) {
		if (listener != null && !listeners.contains(listener))
			listeners.add(listener);
		assert (listeners.size() <= 1);
	}

	/**
	 * @param remove
	 * @return The Display that was just removed as a listener
	 */
	public Display removeListener(final Display remove) {
		if (listeners.remove(remove))
			return remove;
		return null;
	}

	/**
	 * @param message
	 * @param myDisplayNumber
	 * @return Was this message understood?
	 */
	public boolean processInput(final MessageCarrier message, final int myDisplayNumber) {
		String body = message.getMessage();
		switch (message.getType()) {
		case MESSAGE:
			String xmlDocument = body.substring(body.indexOf("<?xml"));
			DDMessage myMessage = new DDMessage(xmlDocument);
			return processInput(myMessage, myDisplayNumber);

		case WHOISIN:
		case LOGIN:
		case LOGOUT:
			// Not relevant to a client (only a server cares about these message types)
			break;

		case ISALIVE:
			// We are being asked, "Are we alive right now?". respond with, "Yes, I am alive" message
			// FIXME -- At this time (11/2014), we should not really see this soor of message down here. It should be
			// handled directly by the messaging client that receives it (and knows where to send the response).
			break;

		case AMALIVE:
			// We are being told that such-and-such a client is alive right now.
			break;

		}
		return true;
	}

	/**
	 * <p>
	 * There are two agents in this conversation
	 * <ul>
	 * <li>(client) -- the machine that tells the displays what to show asynchronous message from a human, or from a channel script.
	 * This client receives a "ready" message, and the it can send a URL to the server</li>
	 * <li>
	 * <server) -- the machine that controls a display. This server waits for a URL from the client. When it gets it and displays
	 * the URL successfully, it replies, "Ready".</li>
	 * </ul>
	 * </p>
	 * 
	 * <p>
	 * Maybe there should also be a protocol that allows the client to ask, "How are you doing?" To which the server replies with
	 * the URL it is showing.
	 * </p>
	 * 
	 * @param message
	 *            The message to process
	 * @param myDisplayNumber
	 * @return Was the processing successful?
	 */
	public boolean processInput(final DDMessage message, int myDisplayNumber) {
		try {
			System.out.println(getClass().getSimpleName() + ".processInput(): processing '" + message + "'");

			if (message != null && message.getMessage() != null) {
				theMessage = message.getMessage();
				// System.out.println(getClass().getSimpleName() + ".processInput(): theMessage is of type "
				// + theMessage.getClass().getSimpleName());

				// if (theMessage instanceof Pong) {
				// // For the client (the channel changer), this is a statement of what the server (the Display) is doing.
				// // For the server (the display), this should never happen.
				// theReply = null;
				// informListenersPong();
				// } else {

				if (theMessage instanceof EncodedCarrier) {
					// Every message *should be* an instance of EncodedCarrier.
					EncodedCarrier carrier = (EncodedCarrier) theMessage;
					if (carrier.howOld() > 30000L) {
						long old = carrier.howOld();
						System.err.println("An old message has been received: [" + carrier + ", date=" + carrier.getDate()
								+ "], which is " + old + " msec old\n Ignoring it.");
						return false;
					}
				}

				if (theMessage instanceof ChangeChannelList) {
					informListenersForever();
				} else if (theMessage instanceof ChangeChannel) {
					if (((ChangeChannel) theMessage).getDisplayNumber() == myDisplayNumber)
						informListeners();
					else
						System.out.println(DCProtocol.class.getSimpleName() + ".processInput(): Got display="
								+ ((ChangeChannel) theMessage).getDisplayNumber() + ", but I am " + myDisplayNumber);
				} else if (theMessage instanceof ChannelSpec) {
					checkChanger();
					informListeners((ChannelSpec) theMessage);
					// } else if (theMessage instanceof HeartBeat) {
					// // System.out.println("Got a server heartbeat with timestamp = " + ((HeartBeat)
					// theMessage).getTimeStamp());
					// theReply = null;
					// lastServerHeartbeat = ((HeartBeat) theMessage).getTimeStamp();
					// return true;
				} else {
					System.out.println("The message is of type " + theMessage.getClass().getCanonicalName()
							+ ".  We will assume they meant it to be 'ChangeChannelReply'");
				}

				ChangeChannelReply p = new ChangeChannelReply();
				if (listeners.size() > 0 && listeners.get(0) != null) {
					// ASSUME that there is one and only one listener here and it is the physical display
					p.setDisplayNum(listeners.get(0).getNumber());
					ChannelSpec spec = new ChannelSpec();
					spec.setUri(listeners.get(0).getContent().getURI());
					spec.setCategory(listeners.get(0).getContent().getCategory());
					spec.setName(listeners.get(0).getContent().getName());
					spec.setTime(listeners.get(0).getContent().getTime());
					p.setChannelSpec(spec);
				} else {
					// System.err.println(getClass().getSimpleName() + ".processInput(): No listeners for a 'Pong' message");
					;
				}
				theReply = p;
				// }
			} else {
				System.err.println(getClass().getSimpleName() + ".processInput(): Hmm.  message problems: " + message
						+ (message != null ? message.getMessage() : ""));
				return false;
			}
		} catch (NullPointerException e) {
			System.err.println("It looks like we may have lost the connection to the Display");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void informListeners() {
		checkChanger();
		informListeners(((ChangeChannel) theMessage).getChannelSpec());
	}

	// private void informListenersPong() {
	// ChangeChannelReply p = (ChangeChannelReply) theMessage;
	// ChannelSpec spec = p.getChannelSpec();
	// System.out.println(getClass().getSimpleName() + ": Propagating the ChangeChannelReply with this ChannelSpec: " + spec.getName() + ", "
	// + spec.getTime() + ", " + spec.getCategory() + " (" + spec.getClass().getSimpleName() + ")");
	// informListeners(spec);
	// }

	private void informListenersForever() {
		checkChanger();
		if (listeners == null || listeners.size() == 0) {
			System.out.println("No listeners, so exiting from informListenersForever()");
			return;
		}
		keepRunning = true;
		final ChannelSpec[] specs = ((ChangeChannelList) theMessage).getChannelSpec();
		changerThread = new Thread("ChannelChanger") {
			public void run() {
				System.out.println(DCProtocol.class.getSimpleName()
						+ ": We will be changing the channel automatically based on the list we just received.\n\tThere are "
						+ specs.length + " channels in the list, and the first dwell time is " + specs[0].getTime() / 1000
						+ " secs -- " + (new Date()));
				while (keepRunning) {
					for (ChannelSpec spec : specs) {
						long sleepTime = spec.getTime();
						// System.out.println(DCProtocol.class.getSimpleName() + ": Channging to [" + spec.getName() + "] -- "
						// + (new Date()));

						informListeners(spec);
						for (long i = 0; i < sleepTime && keepRunning; i += SHORT_INTERVAL)
							catchSleep(SHORT_INTERVAL);
						// System.out.println(DCProtocol.class.getSimpleName() + ": Changing the channel now!");

						if (!keepRunning)
							break;
					}

				}
				System.out.println(DCProtocol.class.getSimpleName() + ": Channel list has exited -- " + (new Date()));
			}
		};
		changerThread.start();
	}

	private void informListeners(ChannelSpec spec) {
		try {
			URL url = spec.getUri().toURL();
			Channel c = new PlainURLChannel(url);
			c.setCategory(spec.getCategory());
			c.setName(spec.getName());
			c.setTime(spec.getTime());
			for (Display L : listeners) {
				// System.out.println(getClass().getSimpleName() + ": Telling a " + L.getClass().getSimpleName() + " to change to ["
				// + c + "]");
				L.setContent(c);
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkChanger() {
		// Hmmm. This seems to have been neutered (11/2014)
		keepRunning = false;
		while (changerThread != null && changerThread.isAlive()) {
			catchSleep(2 * SHORT_INTERVAL);
		}
		changerThread = null;
	}

	/**
	 * @return The timestamp of the last time we got a heart beat message from the server.
	 */
	public long getLastServerHeartbeat() {
		return lastServerHeartbeat;
	}
}