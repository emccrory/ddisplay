package gov.fnal.ppd.signage.comm;

import gov.fnal.ppd.signage.Channel;
import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.channel.PlainURLChannel;
import gov.fnal.ppd.signage.xml.ChangeChannel;
import gov.fnal.ppd.signage.xml.ChangeChannelList;
import gov.fnal.ppd.signage.xml.ChannelSpec;
import gov.fnal.ppd.signage.xml.Pong;

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
 * DC stands for Display Communications
 * 
 * This is the protocol for the server that is controlling a display.
 * 
 * @author Elliott McCrory, Fermilab
 */
public class DCProtocol {
	// private enum State {
	// WAITING, READY, DISPLAYING
	// };

	private Object			theMessage;
	private Object			theReply;
	private List<Display>	listeners		= new ArrayList<Display>();
	protected boolean		keepRunning		= false;
	private Thread			changerThread	= null;
	protected long			SHORT_INTERVAL	= 100l;

	public Object getTheMessage() {
		return theMessage;
	}

	public Object getTheReply() {
		return theReply;
	}

	public void addListener(Display listener) {
		if (listener != null && !listeners.contains(listener))
			listeners.add(listener);
		assert (listeners.size() <= 1);
	}

	public Display removeListener(Display remove) {
		if (listeners.remove(remove))
			return remove;
		return null;
	}

	public boolean processInput(DDMessage message) {

		/**
		 * There are two agents in this conversation (client) -- the machine that tells the displays what to show asynchronous
		 * message from a human, or from a channel script. This client receives a "ready" message, and the it can send a URL to the
		 * server -- the machine that controls a display (server). This server waits for a URL from the client. When it gets it and
		 * displays the URL successfully, it replies, "Ready".
		 * 
		 * -- There can (should?) also be a protocol that allows the client to ask, "How are you doing?" To which the server replies
		 * with the URL it is showing.
		 */

		try {
			System.out.println(getClass().getSimpleName() + ".processInput(): received '" + message + "'");

			if (message != null && message.getMessage() != null) {
				theMessage = message.getMessage();
				// System.out.println(getClass().getSimpleName() + ".processInput(): theMessage is of type "
				// + theMessage.getClass().getSimpleName());

				if (theMessage instanceof Pong) {
					// For the client (the channel changer), this is a statement of what the server (the Display) is doing.
					// For the server (the display), this should never happen.
					theReply = null;
					informListenersPong();
				} else {
					if (theMessage instanceof ChangeChannelList) {
						informListenersForever();
					} else if (theMessage instanceof ChangeChannel) {
						informListeners();
					} else if (theMessage instanceof ChannelSpec) {
						checkChanger();
						informListeners((ChannelSpec) theMessage);
					} else {
						System.out.println("The message is of type " + theMessage.getClass().getCanonicalName()
								+ ".  We will assume they meant it to be 'Pong'");
					}

					Pong p = new Pong();
					if (listeners.get(0) != null) {
						// ASSUME that there is one and only one listener here.
						p.setDisplayNum(listeners.get(0).getNumber());
						ChannelSpec spec = new ChannelSpec();
						spec.setUri(listeners.get(0).getContent().getURI());
						spec.setCategory(listeners.get(0).getContent().getCategory());
						spec.setName(listeners.get(0).getContent().getName());
						p.setChannelSpec(spec);
					} else {
						System.err.println(getClass().getSimpleName() + ".processInput(): Hmm.  No listeners!?");
					}
					theReply = p;
				}
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

	private void informListenersPong() {
		Pong p = (Pong) theMessage;
		ChannelSpec spec = p.getChannelSpec();
		System.out.println(getClass().getSimpleName() + ": Propagating the pong with this ChannelSpec: " + spec.getName() + ", "
				+ spec.getTime() + ", " + spec.getCategory() + " (" + spec.getClass().getSimpleName() + ")");
		informListeners(spec);
	}

	private void informListenersForever() {
		checkChanger();
		keepRunning = true;

		final ChannelSpec[] specs = ((ChangeChannelList) theMessage).getChannelSpec();
		changerThread = new Thread("ChannelChanger") {
			public void run() {
				System.out.println(DCProtocol.class.getSimpleName()
						+ ": Looks like we are going to be changing the channel automatically.  There are " + specs.length
						+ " channels in the loop -- " + (new Date()));
				while (keepRunning) {
					for (ChannelSpec spec : specs) {
						long sleepTime = spec.getTime();
						System.out.println(DCProtocol.class.getSimpleName() + ": Channging to " + spec.getName() + " -- "
								+ (new Date()));

						informListeners(spec);
						try {
							for (long i = 0; i < sleepTime && keepRunning; i += SHORT_INTERVAL)
								sleep(SHORT_INTERVAL);
							System.out.println(DCProtocol.class.getSimpleName() + ": Changing the channel now!");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
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
			for (Display L : listeners) {
				System.out.println(getClass().getSimpleName() + ": Telling a " + L.getClass().getSimpleName() + " to change to "
						+ c);
				L.setContent(c);
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void checkChanger() {
		keepRunning = false;
		while (changerThread != null && changerThread.isAlive()) {
			try {
				// Give the thread time to stop
				Thread.sleep(2 * SHORT_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		changerThread = null;
	}

}