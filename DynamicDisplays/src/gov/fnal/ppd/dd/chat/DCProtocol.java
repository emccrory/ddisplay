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
import static gov.fnal.ppd.dd.util.Util.println;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import gov.fnal.ppd.dd.changer.DisplayChangeEvent;
import gov.fnal.ppd.dd.changer.DisplayChangeEvent.DisplayChangeType;
import gov.fnal.ppd.dd.channel.ChannelInList;
import gov.fnal.ppd.dd.channel.ChannelInListImpl;
import gov.fnal.ppd.dd.channel.ChannelListHolder;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.channel.ConcreteChannelListHolder;
import gov.fnal.ppd.dd.channel.MapOfChannels;
import gov.fnal.ppd.dd.emergency.EmergCommunicationImpl;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.signage.Display;
import gov.fnal.ppd.dd.signage.EmergencyCommunication;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.AreYouAliveMessage;
import gov.fnal.ppd.dd.xml.ChangeChannel;
import gov.fnal.ppd.dd.xml.ChangeChannelByNumber;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.ChangeChannelReply;
import gov.fnal.ppd.dd.xml.ChannelSpec;
import gov.fnal.ppd.dd.xml.EmergencyMessXML;
import gov.fnal.ppd.dd.xml.ErrorMessage;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;

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

	private static final long		MESSAGE_AGE_IS_TOO_OLD	= 180000L;					// Three minutes
	private Object					theReply;
	private List<Display>			listeners				= new ArrayList<Display>();
	// protected boolean keepRunning = false;
	private Thread					changerThread			= null;
	protected long					SHORT_INTERVAL			= 100l;
	private String					errorMessageText;

	private static MapOfChannels	moc						= new MapOfChannels();

	/**
	 * @return The reply that is relevant
	 */
	public Object getTheReply() {
		return theReply;
	}

	/**
	 * @return The error message text that was just generated.
	 */
	public String getErrorMessageText() {
		return errorMessageText;
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
	 * @throws ErrorProcessingMessage
	 */
	public boolean processInput(final MessageCarrierXML message) throws ErrorProcessingMessage {

		if (tooOld(message.getTimeStamp())) {
			// Message rejected as too old to matter!

			System.err.println("An old message has been received: [" + message + ", date=" + new Date(message.getTimeStamp())
					+ "]\n Ignoring it.");

			errorMessageText = "The message just received by the display is too old to be trusted." + "\nMessage time="
					+ new Date(message.getTimeStamp()) + " which is " + (System.currentTimeMillis() - message.getTimeStamp())
					+ " msec old.  The time on the display is " + new Date()
					+ "\n\nEither this is an attempt to spoof a message (which would be very bad), or the clocks on the display and your PC are out of sync."
					+ "\n\nThis message has been ignored.";

			return false;
		}
		MessagingDataXML body = message.getMessageValue();
		if (body instanceof ErrorMessage) {
			errorHandler(message);
		} else if (body instanceof EmergencyMessXML) {
			try {
				return processEmergencyMessage((EmergencyMessXML) body);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return processInput(body);
		}
		return true;
	}

	private static boolean tooOld(long timeStamp) {
		return (System.currentTimeMillis() - timeStamp) > MESSAGE_AGE_IS_TOO_OLD;
	}

	/**
	 * <p>
	 * There are two agents in this conversation
	 * <ul>
	 * <li>(client) -- the machine that tells the displays what to show asynchronous message from a human, or from a channel script.
	 * This client receives a "ready" message, and the it can send a URL to the server</li>
	 * <li>(server) -- the machine that controls a display. This server waits for a URL from the client. When it gets it and
	 * displays the URL successfully, it replies, "Ready".</li>
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
	public boolean processInput(final MessagingDataXML message) {
		errorMessageText = null;
		try {
			// println(getClass(), ".processInput(): processing '" + message + "'");

			// TODO -- Verify the IP address of the sender
			// String ip = carrier.getIPAddress();
			// if ( ip.equals(getIPAddressOf(messageFromField) ) everything is OK

			if (message instanceof ChangeChannelList) {
				// disableListThread();
				// informListenersForever();
				createChannelListAndInform((ChangeChannelList) message);
			} else if (message instanceof ChangeChannel) {
				// disableListThread();
				informListeners((ChangeChannel) message);
			} else if (message instanceof ChangeChannelByNumber) {
				// Verify checksum
				ChangeChannelByNumber ccbn = (ChangeChannelByNumber) message;
				int num = ccbn.getChannelNumber();
				SignageContent sc = moc.get(num);
				if (sc == null || sc.getChecksum() != ccbn.getChecksum()) {
					if (sc != null)
						println(getClass(), "Channel checksum from expected channel (" + sc.getChecksum()
								+ ") does not match the checksum in the XML (" + ccbn.getChecksum() + ").  Will reload channels.");
					moc.resetChannelList();
					sc = moc.get(num);
					if (sc == null || sc.getChecksum() != ccbn.getChecksum()) {
						// CHECKSUM FAILED!
						if (sc == null) {
							errorMessageText = " Channel number " + num + " does not exist.  Content on display not changed.";
							println(getClass(), errorMessageText);
						} else {
							errorMessageText = " Channel list checksums, for the database and for the transmitted list, do not match.  Content on display not changed.";
							println(getClass(), errorMessageText);
						}
						return false;
					}
				}

				// Checksum match between the XML message and the database's URL. We're good to go!

				informListeners(moc.get(num));

				// } else if (message instanceof ChannelSpec) {
				// // disableListThread();
				// informListeners((ChannelSpec) message);
			} else if (message instanceof AreYouAliveMessage) {
				; // Now what??
			} else if (message instanceof ChangeChannelReply) {
				theReply = message;
				informListeners((ChangeChannelReply) message);
			} else {
				println(getClass(), "The message is of type " + message.getClass().getCanonicalName()
						+ ".  We will assume they meant it to be 'ChangeChannelReply'");
				ChangeChannelReply p = new ChangeChannelReply();
				p.setDisplayNum(listeners.get(0).getDBDisplayNumber());
				ChannelSpec spec = new ChannelSpec(listeners.get(0).getContent());
				p.setChannelSpec(spec);
				theReply = p;
				informListeners(p);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * <p>
	 * There are two agents in this conversation
	 * <ul>
	 * <li>(client) -- the machine that tells the displays what to show asynchronous message from a human, or from a channel script.
	 * </li>
	 * <li>(server) -- the machine that controls a display.</li>
	 * </ul>
	 * </p>
	 * 
	 * @param message
	 *            The emergency message to process
	 * @return Was the processing successful?
	 */
	private boolean processEmergencyMessage(EmergencyMessXML message) {
		try {
			println(getClass(), ".processEmergencyMessage(): processing ...");

			if (message != null) {
				EmergencyMessage em = new EmergencyMessage();
				em.setFootnote(message.getFootnote());
				em.setHeadline(message.getHeadline());
				em.setSeverity(message.getSeverity());
				em.setMessage(message.getMessage());
				em.setDwellTime(message.getDwellTime());
				em.setIpAddress(message.getIpAddress());
				em.setTimestamp(message.getTimestamp());

				if (message.getSeverity() != Severity.REMOVE)
					println(getClass(), ".processEmergencyMessage(): Emergency message is " + em);

				EmergencyCommunication ec = new EmergCommunicationImpl();
				ec.setMessage(em);
				informListeners(ec);
			}

		} catch (NullPointerException e) {
			System.err.println("It looks like we may have lost the connection to the Display");
			e.printStackTrace();
			return false;
		}
		return true;

	}

	private void createChannelListAndInform(ChangeChannelList mess) {
		final ChannelSpec[] specs = mess.getChannelSpec();
		if (specs.length == 0) {
			println(DCProtocol.class, " -- Empty list received. Abort.");
			return;
		}
		if (specs.length == 1) {
			// There is only one channel in this list. Hmmm (shouldn't happen). Don't send this over and over to the client. The
			// timeout in the channel itself will cause it to refresh from within the client.
			informListeners(specs[0]);
		} else {
			long dwellTime = 5000;
			ChannelListHolder channelList = new ConcreteChannelListHolder();

			for (ChannelSpec spec : specs) {
				// NOTE : Lists of lists are not supported
				ChannelInList c = new ChannelInListImpl(spec.getContent());
				channelList.channelAdd(c);
			}
			ChannelPlayList playList = new ChannelPlayList( channelList, dwellTime);
			playList.setChannelNumber(mess.getChannelNumber());
			for (Display L : listeners) {
				L.setContent(playList);
			}
		}
	}

	private void informListeners(final ChangeChannel message) {
		assert (changerThread == null);
		informListeners(((ChangeChannel) message).getChannelSpec());
	}

	private void informListeners(final EmergencyCommunication spec) {
		try {
			for (Display L : listeners) {
				// println(getClass().getSimpleName(), + " Telling a "+L.getClass().getSimpleName()+" to change to ["+c+"]");
				L.setContent(spec);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void informListeners(final SignageContent c) {
		for (Display L : listeners) {
			L.setContent(c);
		}
	}

	private void informListeners(final ChannelSpec spec) {
		SignageContent c = spec.getContent();
		informListeners(c);
	}

	private void informListeners(final ChangeChannelReply reply) {
		DisplayChangeEvent e = new DisplayChangeEvent(reply,0, DisplayChangeType.CHANGE_COMPLETED);
		for (Display L : listeners) {
			L.actionPerformed(e);
		}
	}

	/**
	 * An error has been encountered: Deal with it.
	 * 
	 * @param message
	 *            The message to consider
	 */
	public void errorHandler(final MessageCarrierXML message) {
		// println(DCProtocol.class, " $$$ Error handler, message is to '" + message.getTo() + "', from '" + message.getFrom() +
		// "'");
		for (Display L : listeners) {
			if (L.getMessagingName().equals(message.getMessageRecipient())
					|| L.getMessagingName().equals(message.getMessageOriginator())
					|| message.getMessageOriginator().equals(SPECIAL_SERVER_MESSAGE_USERNAME)) {
				// println(DCProtocol.class, " $$$ Sending to " + L);
				ErrorMessage o = (ErrorMessage) message.getMessageValue();
				L.errorHandler(o.getErrorMessageText());
			} else
				; // println(DCProtocol.class, " $$$ SKIPPING " + L + ", messaging name=" + L.getMessagingName());
		}
	}
}