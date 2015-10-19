/*
 * DCProtocol
 *
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
 * 
 * Adaptations to this class:
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.chat.MessagingServer.SPECIAL_SERVER_MESSAGE_USERNAME;
import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.println;
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

/**
 * <p>
 * This is the protocol for the messages between a Channel Changer and a Display.
 * </p>
 * <p>
 * <em>DC</em> stands for <em>Display Communications</em>
 * </p>
 * 
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class DCProtocol {
	// private enum State {
	// WAITING, READY, DISPLAYING
	// };

	private static final long	MESSAGE_AGE_IS_TOO_OLD	= 180000L; // Three minutes
	private Object				theMessage;
	private Object				theReply;
	private List<Display>		listeners				= new ArrayList<Display>();
	protected boolean			keepRunning				= false;
	private Thread				changerThread			= null;
	protected long				SHORT_INTERVAL			= 100l;

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
	 * @return Was this message understood?
	 */
	public boolean processInput(final MessageCarrier message) {
		String body = message.getMessage();
		switch (message.getType()) {
		case MESSAGE:
			String xmlDocument = body.substring(body.indexOf("<?xml"));
			DDMessage myMessage = new DDMessage(xmlDocument);
			return processInput(myMessage);

		case WHOISIN:
		case LOGIN:
		case LOGOUT:
			// Not relevant to a client (only a server cares about these message types)
			break;

		case ERROR:
			errorHandler(message);
			break;

		case ISALIVE:
			// We are being asked, "Are we alive right now?". respond with, "Yes, I am alive" message
			// FIXME -- At this time (11/2014), we should not really see this sort of message down here. It should be
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
	 * @return Was the processing successful?
	 */
	public boolean processInput(final DDMessage message) {
		try {
			println(getClass(), ".processInput(): processing '" + message + "'");

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
					if (carrier.howOld() > MESSAGE_AGE_IS_TOO_OLD) {
						long old = carrier.howOld();
						System.err.println("An old message has been received: [" + carrier + ", date=" + carrier.getDate()
								+ "], which is " + old + " msec old\n Ignoring it.");
						return false;
					}
					// TODO -- Verify the IP address of the sender
					// String ip = carrier.getIPAddress();
					// if ( ip.equals(getIPAddressOf(messageFromField) ) everything is OK
				}

				if (theMessage instanceof ChangeChannelList) {
					disableListThread();
					informListenersForever();
				} else if (theMessage instanceof ChangeChannel) {
					disableListThread();
					informListeners();
				} else if (theMessage instanceof ChannelSpec) {
					disableListThread();
					informListeners((ChannelSpec) theMessage);
				} else {
					println(getClass(), "The message is of type " + theMessage.getClass().getCanonicalName()
							+ ".  We will assume they meant it to be 'ChangeChannelReply'");
				}

				ChangeChannelReply p = new ChangeChannelReply();
				if (listeners.size() > 0 && listeners.get(0) != null) {
					// ASSUME that there is one and only one listener here and it is the physical display
					p.setDisplayNum(listeners.get(0).getDBDisplayNumber());
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
		assert (changerThread == null);
		informListeners(((ChangeChannel) theMessage).getChannelSpec());
	}

	// private void informListenersPong() {
	// ChangeChannelReply p = (ChangeChannelReply) theMessage;
	// ChannelSpec spec = p.getChannelSpec();
	// System.out.println(getClass().getSimpleName() + ": Propagating the ChangeChannelReply with this ChannelSpec: " +
	// spec.getName() + ", "
	// + spec.getTime() + ", " + spec.getCategory() + " (" + spec.getClass().getSimpleName() + ")");
	// informListeners(spec);
	// }

	/**
	 * This is where the channel list is played on a display.
	 */
	private void informListenersForever() {
		assert (changerThread == null);

		// TODO -- Alternate way to do this, which is possibly more elegant: Change the way we interpret the time field in the
		// channel so that it will look for "the next channel" when that time expires. Otherwise, it refreshes this channel.

		// FIXME -- In a world where a temporary channel is played on a Display, it cannot know that the permanent channel
		// that it had was a list. That is because the handling of the list is here in the Protocol portion. The handling
		// of the playing of the list, then, needs to be down in the client that is actually talking to the browser. This
		// solution is very elegant, so it will be hard to change it.

		// These two tags seem to be compatible.

		// HOWEVER,
		//
		// There is a glitch in this because a channel can have a refresh time that is different from the dwell time in a list.
		// For example, the FESS folks like to refresh a channel every 15 seconds or so. But they may want this to be in a
		// list with this fast-refresh channel being on the screen for several minutes (15 or so refreshes). Thus, combining
		// these two functionalities would (might?) not work right.

		if (listeners == null || listeners.size() == 0) {
			println(getClass(), "No listeners, so exiting from informListenersForever()");
			return;
		}
		keepRunning = true;
		final ChangeChannelList channelListSpec = ((ChangeChannelList) theMessage);
		final ChannelSpec[] specs = channelListSpec.getChannelSpec();
		if (specs.length == 0) {
			println(DCProtocol.class, " -- Empty list received. Abort.");
			return;
		}
		if (specs.length == 1) {
			// There is only one channel in this list. Hmmm. Don't to send this over and over to the client. The timeout in the
			// channel itself will cause it to refresh from within the client.
			informListeners(specs[0]);
		} else {
			changerThread = new Thread("ChannelChanger") {
				public void run() {
					println(DCProtocol.class,
							": We will be changing the channel automatically based on the list we just received.\n"
									+ "\t\t\tThere are " + specs.length + " channels in the list -- " + (new Date()));

					// long sleepTime = channelListSpec.getTime();
					while (keepRunning) {
						for (ChannelSpec spec : specs) {
							// long st = (spec.getTime() > sleepTime ? spec.getTime() : sleepTime);

							// ASSUME spec.getTime() is always non-zero. The way the code is today, this is an appropriate
							// assumption.
							informListeners(spec);
							for (long sleepTime = spec.getTime(); sleepTime > 0 && keepRunning; sleepTime -= SHORT_INTERVAL)
								catchSleep(Math.min(sleepTime, SHORT_INTERVAL));

							if (!keepRunning)
								break;
						}

					}
					println(DCProtocol.class, ": Channel list has exited -- " + (new Date()));
				}
			};
			changerThread.start();
		}
	}

	private void informListeners(ChannelSpec spec) {
		try {
			URL url = spec.getUri().toURL();
			Channel c = new PlainURLChannel(url);
			c.setCategory(spec.getCategory());
			c.setName(spec.getName());
			c.setTime(spec.getTime());
			c.setFrameNumber(spec.getFrameNumber());
			c.setExpiration(spec.getExpiration());
			for (Display L : listeners) {
				// println(getClass().getSimpleName(), + " Telling a "+L.getClass().getSimpleName()+" to change to ["+c+"]");
				L.setContent(c);
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void disableListThread() {
		keepRunning = false;
		while (changerThread != null && changerThread.isAlive()) {
			catchSleep(2 * SHORT_INTERVAL);
		}
		changerThread = null;
	}

	/**
	 * An error has been encountered: Deal with it.
	 * 
	 * @param message
	 *            The message to consider
	 */
	public void errorHandler(final MessageCarrier message) {
		// println(DCProtocol.class, " $$$ Error handler, message is to '" + message.getTo() + "', from '" + message.getFrom() +
		// "'");
		for (Display L : listeners) {
			if (L.getMessagingName().equals(message.getTo()) || message.getFrom().equals(SPECIAL_SERVER_MESSAGE_USERNAME)) {
				// println(DCProtocol.class, " $$$ Sending to " + L);
				L.errorHandler(message.getMessage());
			} else
				;// println(DCProtocol.class, " $$$ SKIPPING " + L);

		}

	}
}