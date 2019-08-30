package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import static gov.fnal.ppd.dd.util.Util.getChannelFromNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;

import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.xml.MessageTypeXML;
import gov.fnal.ppd.dd.emergency.EmergencyMessage;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.xml.signature.SignXMLUsingDSAKeys;
import gov.fnal.ppd.dd.xml.signature.SignedXMLDocument;

/**
 * <p>
 * Test program to show how to create XML files from the Channel/Display architecture developed here.
 * </p>
 * 
 * <p>
 * The relevant XML classes (that work) are
 * <ul>
 * <li><strong>Ping</strong> - A message that requests a status (Pong) reply</li>
 * <li><strong>Pong</strong> - A message that is a reply to a Ping message</li>
 * <li><strong>ChannelSpec</strong> - Fully specifies a Channel</li>
 * <li><strong>ChangeChannel</strong> - A message that asks a Display to show a specific Channel</li>
 * <li><strong>ChangeChannelList</strong> - A message that asks a Display to show a list of Channels</li>
 * </ul>
 * </p>
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2012-2014
 * 
 */
public class WriteXMLFile {

	/**
	 * @param argv
	 */
	public static void main(final String argv[]) {
		credentialsSetup();

		selectorSetup();

		for (int which = 0; which < 10; which++) {
			MessagingDataXML stuff = null;
			try {
				switch (which) {
				case 0:
				case 1:
					break;

				case 2:
					// The Emergency message
					EmergencyMessage em = new EmergencyMessage();
					em.setHeadline("This is the headline");
					em.setMessage("This is the message");
					em.setSeverity(Severity.EMERGENCY);
					em.setFootnote("A footnote");
					em.setDwellTime(45 * 60 * 1000);

					stuff = new EmergencyMessXML(em);
					break;

				case 3:
					// A plain channel spec
					// ChannelSpec cs = new ChannelSpec(new ChannelImpl("Test Channel",
					// new ChannelClassification("PUBLIC_DETAILS", "Details"), "This is the desription",
					// new URI("http://www.fnal.gov"), 23, System.currentTimeMillis() % (24L * 3600L * 1000L)));
					// stuff = cs;
					// theType = MessageTypeXML.CHANGECHANNEL;
					// break;

				case 4:
					ChangeChannel cc = new ChangeChannel();
					cc.setContent(getChannelFromNumber(22));
					cc.setDisplayNumber(14);

					stuff = cc;
					break;

				case 5:
					ChangeChannelByNumber csn = new ChangeChannelByNumber();
					csn.setChannelNumber(20);
					csn.setChecksum(123455678908L);

					stuff = csn;
					break;

				case 6:
					// This creates a login message
					LoginMessage lm = new LoginMessage("Some Client");
					stuff = lm;
					break;

				case 7:
					// WhoIsInreply
					WhoIsInReply wiir = new WhoIsInReply();
					ClientInformation[] cis = new ClientInformation[3];
					cis[0] = new ClientInformation("Client1");
					cis[1] = new ClientInformation("Client2");
					cis[2] = new ClientInformation("Client3");
					wiir.setClient(cis);
					stuff = wiir;
					break;

				case 8:
					AmAliveMessage aam = new AmAliveMessage();
					aam.setClient(new ClientInformation("Some Client, baby!", System.currentTimeMillis() - 123456789L));
					stuff = aam;
					break;

				case 9:
					Set<SignageContent> map = ChannelCatalogFactory.getInstance()
							.getChannelCatalog(new ChannelClassification("PUBLIC"));
					List<SignageContent> lst = new ArrayList<SignageContent>();
					for (SignageContent C : map) {
						if (lst.size() > 3)
							continue;
						lst.add(C);
					}
					ChannelPlayList cpl = new ChannelPlayList("A list", lst, 60); // This is the SignageContent class
					ChangeChannelList ccl = new ChangeChannelList(); // This is the XML class
					// ChannelSpec cspec = new ChannelSpec(cpl); // A different XML class? This does not work properly.

					ccl.setContent(cpl);
					stuff = ccl;
					break;
				}

				if (stuff != null) {
					// String message = MyXMLMarshaller.getXML(stuff);
					try {
						for (int i = 0; i < 40; i++)
							System.out.print(which + " ");
						System.out.println("\n");
						// System.out.println(message);

						// Enclose this in a Message Carrier object
						MessageCarrierXML mc = new MessageCarrierXML("From me", "To you", stuff);
						String theMessageString = MyXMLMarshaller.getXML(mc);
						Document theMessageDocument = SignedXMLDocument.convertToDocument(theMessageString);
						SignXMLUsingDSAKeys.signDocument(theMessageDocument, System.out);
						System.out.println("\n------------------------------------------------\n");
						// SignedXMLDocument signed = new SignedXMLDocument(theMessageString);
						// System.out.println(signed.getSignedDocumentString());
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.exit(0);
	}
}