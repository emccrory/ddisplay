package gov.fnal.ppd.dd.chat.original;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

// - Comments like this were not part of the original code

public class GossipServer {
	public static void main(String[] args) throws Exception {
		// - This try-block added for completeness, Aug 2019
		try (ServerSocket sersock = new ServerSocket(3000)) {
			System.out.println("Server  ready for chatting");
			// - Accept a connection on this socket. Every client will have its own connection, so this should be threaded in the
			// - real world.
			Socket sock = sersock.accept();
			
			// reading from keyboard (keyRead object)
			BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));
			// sending to client (pwrite object)
			OutputStream ostream = sock.getOutputStream();
			PrintWriter pwrite = new PrintWriter(ostream, true);

			// receiving from server (receiveRead object)
			InputStream istream = sock.getInputStream();
			BufferedReader receiveRead = new BufferedReader(new InputStreamReader(istream));

			String receiveMessage, sendMessage;
			int count = 0;
			while (true) {
				System.out.println("Reading line " + count++);
				if ((receiveMessage = receiveRead.readLine()) != null) {
					System.out.println(receiveMessage);
				}
				// - Removed reading from the terminal, Aug 2019
				// sendMessage = keyRead.readLine();
				// pwrite.println(sendMessage);
				// pwrite.flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}