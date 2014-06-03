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

import static gov.fnal.ppd.signage.comm.DCMulitServerThread.DEFAULT_BUFFER_SIZE;
import gov.fnal.ppd.signage.Display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * DC stands for Display Communications
 * 
 * Class that connects with a single Display server and, asynchronously, tells it what web page to show now.
 * 
 * @author Elliott McCrory, Fermilab
 */
public class DCClient {
	private Socket			kkSocket;
	private PrintWriter		out			= null;
	private BufferedReader	in			= null;
	private String			hostName;
	private int				portNumber;
	private boolean			connected	= false;
	private DCProtocol		dcp			= new DCProtocol();

	public DCClient(String hostName, int portNumber) {
		this.hostName = hostName;
		this.portNumber = portNumber;

		try {
			kkSocket = new Socket(hostName, portNumber);
			out = new PrintWriter(kkSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));
			connected = true;
			System.out.println("** Connected to " + hostName + "/" + portNumber + " **");
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + hostName + " --  ignoring.");
		} catch (IOException e) {
			if (!"Connection refused".equals(e.getMessage()) && !"Connection timed out".equals(e.getMessage()))
				System.err.println("Couldn't get I/O for the connection to " + hostName + ":" + portNumber + "\t(" + e.getMessage()
						+ ").");
		}
	}

	public void addListener(Display L) {
		// This fixes one bug (getting the ChannelSelector the most current info on what the Display is showing)
		// But it creates another: The Display gets stuck in a tight look listening to a closed socket,
		// at gov.fnal.ppd.signage.comm.DCMulitServerThread.run(DCMulitServerThread.java:130)

		// dcp.addListener(L);
	}

	public boolean waitForServer() throws IOException {
		if (in != null) {
			char[] cbuf = new char[DEFAULT_BUFFER_SIZE];
			int numRead = in.read(cbuf, 0, DEFAULT_BUFFER_SIZE);
			System.out.println(getClass().getSimpleName() + " --DEBUG-- Got these lines: " + numRead);
			String line = new String(cbuf);

			DDMessage fromServer = new DDMessage(line);
			connected = dcp.processInput(fromServer);
		}
		return connected;
	}

	public boolean isConnected() {
		return connected;
	}

	public Object getTheReply() {
		return dcp.getTheReply();
	}

	public void send(String s) {
		if (out != null) {
			this.out.println(s);
		} else {
			System.out.println("Not connected to the server at " + hostName + ":" + portNumber);
		}
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: java " + DCClient.class.getCanonicalName() + " <host name> <port number>");
			System.exit(1);
		}

		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		DCClient dcc = new DCClient(hostName, portNumber);

		System.out.println("Connected. Now it's your turn:");
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String fromUser;

		// while (dcc.waitForServer()) {
		while (true) {
			fromUser = stdIn.readLine();
			if (fromUser != null) {
				System.out.println("Client (me): " + fromUser);
				dcc.send(fromUser);
			}
		}
	}
}