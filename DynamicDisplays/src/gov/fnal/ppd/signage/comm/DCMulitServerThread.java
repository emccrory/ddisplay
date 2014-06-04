package gov.fnal.ppd.signage.comm;

/*
 * Copyright (c) 1995, 2013, Oracle and/or its affiliates. All rights reserved.
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

import gov.fnal.ppd.signage.Display;
import gov.fnal.ppd.signage.xml.MyXMLMarshaller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * DC stands for Display Communications
 * 
 * A multi-threaded server --test code.
 * 
 * @deprecated
 * 
 * @author Elliott McCrory, Fermilab
 */
public class DCMulitServerThread extends Thread {
	public static final int		DEFAULT_BUFFER_SIZE		= 64000;
	protected static final long	STATUS_MESSGE_PERIOD	= 60000l;
	private Socket				socket					= null;
	private DCProtocol			dcp						= new DCProtocol();
	private PrintWriter			out;
	private boolean				running					= true;
	// private URLChangeListener myChangeListener = new URLChangeListener() {
	//
	// @Override
	// public boolean changeURL(URL newURL) {
	// // This is where the server (that is, the display) deals with messages
	// System.out.println("Received a new URL: '" + newURL + "'");
	// dcp.setToReadyState();
	// DDMessage outMessage = new DDMessage(dcp.processInput(null), false);
	// out.println(outMessage.getOutputMessage());
	// System.out.println("--DEBUG1-- Sent '" + outMessage.getOutputMessage()
	// + "'");
	// return true;
	// }
	// };
	private BufferedReader		in;
	private boolean				debug					= false;

	/**
	 * @param socket
	 */
	public DCMulitServerThread(final Socket socket) {
		super(DCMulitServerThread.class.getSimpleName());
		this.socket = socket;
	}

	public void run() {
		try (final PrintWriter o = new PrintWriter(socket.getOutputStream(), true);
				final BufferedReader ii = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
			this.out = o;
			this.in = ii;
			DDMessage inputLine;

			// ------------------------------------------------------------------------------------------
			// This block of code would issue an asynchronous "Pong" message at a regular interval.
			// The problem is that there is not a good way for a "server" to issue unsolicited messages
			// to a "client" over a TCP/IP socket--the client needs to be listening. This can work
			// if/when we go to a message server.
			//
			// new Thread(getClass().getSimpleName() + "StatusThread") {
			// public void run() {
			// Pong pong = new Pong();
			// ChannelSpec spec = new ChannelSpec();
			// pong.setChannelSpec(spec);
			// while (running) {
			// try {
			// sleep(STATUS_MESSGE_PERIOD);
			//
			// spec.setContent(myDisplay.getContent());
			//
			// synchronized (out) {
			// String xmlMessage = MyXMLMarshaller.getXML(pong);
			// out.println(xmlMessage);
			//
			// System.out.println(getClass().getSimpleName() + " --Asynchronous Status Update-- Sent '"
			// + xmlMessage + "'");
			//
			// // TODO -- Make the client (channel changer) listen for this!
			//
			// }
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// } catch (JAXBException e) {
			// e.printStackTrace();
			// }
			//
			// }
			// }
			// }.start();
			// ------------------------------------------------------------------------------------------

			while (running) {
				try {
					// Read the entire XML file that is coming in over several lines
					char[] cbuf = new char[DEFAULT_BUFFER_SIZE];
					int numRead = in.read(cbuf, 0, DEFAULT_BUFFER_SIZE);
					if (numRead < 0) {
						// This seems to mean that the input stream was closed. Dang it!
						System.out.println(getClass().getSimpleName() + " --DEBUG5-- I think the client has died.");
						break;
					}
					String line = new String(cbuf);
					if (debug)
						System.out.println("[[\n" + line + "\n]]");
					inputLine = new DDMessage(line);

					// if (inputLine.getURL() == null) {
					// System.out.println(getClass().getSimpleName() + " --DEBUG-- Got a blank line. Leaving now.");
					// break;
					// }
					if (dcp.processInput(inputLine)) {
						// We now have the received message object. Send a reply, if warranted.
						Object mes = dcp.getTheReply();
						if (mes != null) {
							synchronized (out) {
								String xmlMessage = MyXMLMarshaller.getXML(mes);
								out.println(xmlMessage);
								System.out.println(getClass().getSimpleName() + " --Normal Reply-- Sent '" + xmlMessage + "'");
							}
						}
					} else {
						System.err.println(getClass().getSimpleName() + ": Processing the input failed");
					}
				} catch (SocketException e) {
					// This exception occurs when the connection to the Channel Selector is broken.
					System.err.println("Received " + e.getClass().getSimpleName() + " (" + e.getLocalizedMessage()
							+ "), indicating connection to the Channel Changer has been lost.  Will attempt to re-connect.");
					e.printStackTrace();
				} catch (Exception e3) {
					System.err.println("Unexpected connection.  This client may need to be restarted");
					e3.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			// dcp.removeListener(myChangeListener);
			in.close();
			synchronized (out) {
				out.close();
			}
			socket.close();
			sleep(5000);
		} catch (Exception e2) {
			System.err.println("Compounded exception within an exception.  This client may need to be restarted");
			e2.printStackTrace();
		}
	}

	/**
	 * @param listener
	 *            The listener to add to the socket connection
	 */
	public void addListener(Display listener) {
		// myDisplay = listener;
		dcp.addListener(listener);
	}

}