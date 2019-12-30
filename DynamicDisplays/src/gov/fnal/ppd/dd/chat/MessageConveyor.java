package gov.fnal.ppd.dd.chat;

import static gov.fnal.ppd.dd.util.Util.catchSleep;
import static gov.fnal.ppd.dd.util.Util.getNextEntropy;
import static gov.fnal.ppd.dd.util.Util.printlnErr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import gov.fnal.ppd.dd.util.PropertiesFile;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.XMLDocumentAndString;
import gov.fnal.ppd.dd.xml.signature.SignXMLUsingDSAKeys;
import gov.fnal.ppd.dd.xml.signature.SignatureNotFoundException;
import gov.fnal.ppd.dd.xml.signature.Validate;

public class MessageConveyor {

	private static boolean verbose = PropertiesFile.getBooleanProperty("IncomingMessVerbose", false);
	private static int badConnectionAttempts = 0;

	public static int getBadConnectionAttempts() {
		return badConnectionAttempts;
	}

	private MessageConveyor() {
	}

	/**
	 * Read the input socket for the next, complete XML document. This ASSUMES that we only get complete XML documents!
	 * 
	 * @param sInput
	 * 
	 * @return The next message
	 */

	public static MessageCarrierXML getNextDocument(Class<?> clazz, BufferedReader receiveRead, String username)
			throws UnrecognizedCommunicationException {
		if (verbose)
			System.out.println(
					"-----> Entering getNextDocument() for " + clazz.getCanonicalName() + ", sInput=" + receiveRead.hashCode());
		String receiveMessage;
		try {
			String theXMLDocument = "";

			if ((receiveMessage = receiveRead.readLine()) != null) {
				// The first line SHOULD BE "BEGIN 1234567890192882\n" where that number is some random long value
				if (!receiveMessage.startsWith("BEGIN ")) {
					// Often we land here when the Powers That Be are trying to break into our system, so the message is sometimes simply garbage.
					// We expect it to be all ASCII, but maybe not.
					badConnectionAttempts++;
					String interpretedMessage = escapeThisMessage(receiveMessage);
					printlnErr(clazz, "Expecting BEGIN but got [" + interpretedMessage + "] (X = unprintable char)");
					throw new UnrecognizedCommunicationException(
							"Expecting BEGIN [000 marker 000] but got [" + interpretedMessage + "], sInput=" + receiveRead.hashCode());
				}
			} else {
				printlnErr(clazz, "Read a null line in getNextDocument() from " + username);
				throw new NullPointerException(
						"NULL message received.  This probably means that the client has disconnected, which would not be a big deal.");
			}
			if (verbose)
				System.out.println("*0* " + receiveRead.hashCode() + " " + receiveMessage);
			String marker = "END  " + receiveMessage.replace("BEGIN", "");
			do {
				if ((receiveMessage = receiveRead.readLine()) != null && !receiveMessage.contains(marker)) {
					theXMLDocument += receiveMessage + "\n";
					if (verbose)
						System.out.println("*** " + receiveRead.hashCode() + " " + receiveMessage);
				}
			} while (!receiveMessage.contains(marker));
			if (!receiveMessage.equals(marker)) {
				String s = receiveMessage.replace(marker, "");
				if (verbose) {
					System.out.println("*2* " + receiveRead.hashCode() + " " + s);
				}
				theXMLDocument += s;
			} else if (verbose) {
				System.out.println("*1* " + receiveRead.hashCode() + " " + receiveMessage);
			}
			if (verbose)
				System.out.println("-----> Leaving getNextDocument() for " + clazz.getCanonicalName() + ", sInput="
						+ receiveRead.hashCode() + ", Document length=" + theXMLDocument.length());

			XMLDocumentAndString theDocument = new XMLDocumentAndString(theXMLDocument);

			MessageCarrierXML retval = (MessageCarrierXML) MyXMLMarshaller.unmarshall(MessageCarrierXML.class,
					theDocument.getTheXML());
			boolean isValid = false;
			try {
				isValid = Validate.isSignatureValid(theDocument);
			} catch (SignatureNotFoundException e) {
				; // This exception is usually OK, so we'll ignore it.
			} catch (Exception e) {
				printlnErr(clazz, "Exception when trying to check the signature");
				e.printStackTrace();
			}
			retval.setSignatureIsValid(isValid);
			return retval;
		} catch (Exception e) {
			printlnErr(clazz, e.getClass().getCanonicalName() + " caught while receiving an incoming document; returning null.  Details:");
			e.printStackTrace();
		}
		return null;
	}

	private static String escapeThisMessage(String receiveMessage) {
		String retval = "";
		for (int i = 0; i < receiveMessage.length() && i < 50; i++)
			retval += isPrintable(receiveMessage.charAt(i)) ? receiveMessage.charAt(i) : 'X';
		if (receiveMessage.length() > 49) {
			retval += " ... plus " + (receiveMessage.length() - 49) + " more";
		}
		return retval;
	}

	private static final boolean isPrintable(char c) {
		return c >= 32 && c <= 127;
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
			Class<? extends MessagingDataXML> data = msg.getMessageValue().getClass();
			String theMessageString = MyXMLMarshaller.getXML(msg);

			if (!msg.isReadOnly()) {
				XMLDocumentAndString theMessageDocument = new XMLDocumentAndString(theMessageString);
				ByteOutputStream localOutputStream = new ByteOutputStream();
				SignXMLUsingDSAKeys.signDocument(theMessageDocument.getTheDocument(), localOutputStream);
				theMessageString = localOutputStream.toString();
				XMLDocumentAndString theSignedDoc = new XMLDocumentAndString(theMessageString);

				if (!Validate.isSignatureValid(theSignedDoc)) {
					// This should never happen!!
					System.err.println("\n\n >>>>>>>>>> The signature on the Document that is about to be sent IS NOT VALID!\n\n");
				}
			}
			if (!MessageConveyor.writeToSocket(theMessageString, sOutput)) {
				printlnErr(clazz, "Failed to send message of type " + data.getCanonicalName());
				return false;
			}
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
		// if (debug)
		// new Exception("MessageConveyor.writeToSocket, length of the document is " + theAsciiDocument.getBytes().length)
		// .printStackTrace();
		if (os == null) {
			// We have not connected to the server yet, so we cannot ask who is there
			printlnErr(MessageConveyor.class, "Have not connected to the messaging server yet, so cannot send the requested document to it.");
			return false;
		}

		long marker = getNextEntropy();
		try {
			if (verbose)
				System.out.println("Writing BEGIN " + marker + " to OutputStream " + os);
			os.write(("BEGIN " + marker + "\n").getBytes());
			if (verbose)
				System.out.println("Writing the message:\n" + theAsciiDocument);
			os.write(theAsciiDocument.getBytes());
			if (verbose)
				System.out.println("Writing END " + marker);
			os.write(("END   " + marker + "\n").getBytes());
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
