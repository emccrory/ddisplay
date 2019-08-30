package gov.fnal.ppd.dd.chat.original;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Modified from GossipServer to handle multiple connections at once.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */

public class GossipServerA {
	public static int	threadNumber	= 0;
	public static int	counter			= 0;

	public static void main(String[] args) throws Exception {
		// - This try-block added for completeness, Aug 2019
		try (ServerSocket sersock = new ServerSocket(3000)) {
			System.out.println("Server  ready for chatting");
			List<Socket> sockets = new ArrayList<Socket>();
			// - Accept a connection on this socket. Every client will have its own connection, so this should be threaded in the
			// - real world.
			while (true) {
				final Socket sock = sersock.accept();
				sockets.add(sock);
				final int thisThread = threadNumber;
				new Thread("Connection" + threadNumber++) {

					@Override
					public void run() {
						int limit = 1000;
						System.out.println("Starting thread #" + thisThread + " to read from client");
						try {
							InputStream istream = sock.getInputStream();
							BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));
							String receiveMessage;
							while (limit-- > 0) {
								System.out.print(thisThread + ": Reading line " + counter++ + " -- [");
								if ((receiveMessage = receiveRead.readLine()) != null) {
									System.out.print(receiveMessage);
								} else {
									System.out.print("NULL");
								}
								System.out.println("]");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		/**
		 * <p>
		 * The conclusion here is as follows.
		 * </p>
		 * <p>
		 * When you are reading ASCII text, we read it line by line. There is no a priori way to know that the message has been
		 * fully sent. I think we will have to invent one. For example, a message could have a form like this:
		 * </p>
		 * 
		 * <pre>
		 * Line 1: BEGIN [A random long integer]
		 * Line 2: \<?xml version="1.0"?\>
		 * line 3: \<firstXMLTag\>
		 * ...
		 * Line N-1: \</firstXMLTag\>
		 * Line N: END [The same random long integer]
		 * </pre>
		 */
	}
}