package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.getNextEntropy;
import static gov.fnal.ppd.dd.util.Util.println;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

import gov.fnal.ppd.dd.chat.xml.attic.ChangeChannel;
import gov.fnal.ppd.dd.chat.xml.attic.ChangeChannelByNumber;
import gov.fnal.ppd.dd.xml.ChangeChannelList;
import gov.fnal.ppd.dd.xml.EmergencyMessXML;
import gov.fnal.ppd.dd.xml.ErrorMessage;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

public class MessageConveyor {

	private static boolean			verbose		= true;	// PropertiesFile.getBooleanProperty("IncomingMessVerbose", false);

	private MessageConveyor() {
	}

	/**
	 * Read the input socket for the next, complete XML document. This ASSUMES that we only get complete XML documents!
	 * 
	 * @param sInput
	 * 
	 * @return The next message
	 */

	public static MessageCarrierXML getNextDocument(Class<?> clazz, BufferedReader receiveRead) throws UnrecognizedCommunicationException {
		new Exception("-----> Entering getNextDocument() for " + clazz.getCanonicalName() + ", sInput=" + receiveRead.hashCode())
				.printStackTrace();
		// int cnt = 0;
		String receiveMessage;
		try {
			String theXMLDocument = "";
			// while ( receiveRead.readLine() == null && cnt < 100){
			// System.out.println("Null again " + cnt++);
			// }
			if ((receiveMessage = receiveRead.readLine()) != null) {
				// The first line SHOULD BE "BEGIN 1234567890192882\n" where that number is some random long value
				if (!receiveMessage.startsWith("BEGIN ")) {
					printlnErr(clazz, "Expecting BEGIN but got [" + receiveMessage + "]");
					throw new UnrecognizedCommunicationException(
							"Expecting BEGIN [000 marker 000] but got [" + receiveMessage + "], sInput=" + receiveRead.hashCode());
				}
			} else {
				printlnErr(clazz, "Read a null line in getNextMessage()");
				throw new RuntimeException("Unexpected NULL message received.");
			}
			if (verbose)
				System.out.println("*0* " + receiveRead.hashCode() + " " + receiveMessage);
			String marker = "END  " + receiveMessage.replace("BEGIN", "");
			do {
				if ((receiveMessage = receiveRead.readLine()) != null && !receiveMessage.equals(marker)) {
					theXMLDocument += receiveMessage;
					if (verbose)
						System.out.println("*** " + receiveRead.hashCode() + " " + receiveMessage);
				}
			} while (!receiveMessage.equals(marker));
			System.out.println("*1* " + receiveRead.hashCode() + " " + receiveMessage);
			System.out
					.println("-----> Leaving getNextDocument() for " + clazz.getCanonicalName() + ", sInput=" + receiveRead.hashCode());

			return (MessageCarrierXML) MyXMLMarshaller.unmarshall(MessageCarrierXML.class, theXMLDocument);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Send a message
	 * 
	 * @param clazz
	 *            Who is sending (for print statements)
	 * @param msg
	 *            The message
	 * @param sOutput
	 *            the output stream
	 * @return Did it work?
	 */
	public static boolean sendMessage(Class<?> clazz, MessageCarrierXML msg, OutputStream sOutput) {
		try {
			String theMessageString = MyXMLMarshaller.getXML(msg);
			Class<? extends MessagingDataXML> data = msg.getMessageValue().getClass();
			if (MessageConveyor.writeToSocket(theMessageString, sOutput)) {
				if (data.equals(ChangeChannel.class) || data.equals(ChangeChannelList.class)
						|| data.equals(ChangeChannelByNumber.class) || data.equals(EmergencyMessXML.class)
						|| data.equals(ErrorMessage.class)) {
					println(clazz, ".sendMessage()\n" + theMessageString);
				}
			} else {
				printlnErr(clazz, "Failed to send message of type " + data.getCanonicalName());
			}
			// The following line is controversial: Is it a bug or is it a feature? Sun claims it is a feature -- it allows the
			// sender to re-send any message easily. But others (myself included) see this as a bug -- if you remember every message
			// every time, you'll run out of memory eventually. The call to "reset()" causes the output object to ignore this,
			// and all previous, messages it has sent.
			// sOutput.reset();
			return true;
		} catch (Exception e) {
			catchSleep(1);
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 
	 * @param theAsciiDocument
	 *            The message to send through the socket
	 * @param os
	 *            The output stream
	 * @return Did it work?
	 */
	public static boolean writeToSocket(String theAsciiDocument, OutputStream os) {
		long marker = getNextEntropy();
		try {
			System.out.println("Writing BEGIN " + marker);
			os.write(("BEGIN " + marker + "\n").getBytes());
			System.out.println("Writing the message:\n" + theAsciiDocument);
			os.write(theAsciiDocument.getBytes());
			System.out.println("Writing END " + marker);
			os.write(("END   " + marker + "\n").getBytes());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
