package gov.fnal.ppd.dd.xml;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static gov.fnal.ppd.dd.MakeChannelSelector.selectorSetup;
import gov.fnal.ppd.dd.changer.ChannelCatalogFactory;
import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.channel.ChannelImpl;
import gov.fnal.ppd.dd.channel.ChannelPlayList;
import gov.fnal.ppd.dd.chat.MessageType;
import gov.fnal.ppd.dd.emergency.Severity;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.util.ObjectSigning;
import gov.fnal.ppd.dd.util.attic.xml.Ping;
import gov.fnal.ppd.dd.util.attic.xml.Pong;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

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
			Object stuff = null;
			try {
				switch (which) {
				case 0:
					Ping ping = new Ping();
					ping.setIPAddress("131.225.100.100");
					ping.setDisplayNum(15);
					stuff = ping;
					break;

				case 1:
					Pong pong = new Pong();
					pong.setIPAddress("131.225.100.100");
					pong.setDisplayNum(15);
					// pong.setChannelSpec((ChannelSpec) ChannelCatalogFactory.getInstance().getDefaultChannel());

					stuff = pong;
					break;

				case 2:
					// Not useful at this time because the Base64 stuff is broken for me
					// PlainImageChange imageContent = new PlainImageChange();
					// imageContent.setEncodedId(id++);
					// stuff = imageContent;

					// Use it to do Emergency message

					EmergencyMessXML emx = new EmergencyMessXML();
					emx.setHeadline("This is the headline");
					emx.setMessage("This is the message");
					emx.setSeverity(Severity.EMERGENCY);
					emx.setFootnote("A footnote");
					emx.setDwellTime(45 * 60 * 1000);

					stuff = emx;
					break;

				case 3:
					ChannelSpec cs = new ChannelSpec(new ChannelImpl("Test Channel", new ChannelCategory("PUBLIC_DETAILS",
							"Details"), "This is the desription", new URI("http://www.fnal.gov"), 23, System.currentTimeMillis()
							% (24L * 3600L * 1000L)));
					stuff = cs;
					break;

				case 4:
					// ChangeChannel cc = new ChangeChannel();
					// cc.setIPAddress("131.225.101.1");
					// cc.setContent(ChannelCatalogFactory.getInstance().getDefaultChannel());
					// cc.setDisplayNumber(14);
					//
					// stuff = cc;
					break;

				case 5:
					ChangeChannelByNumber csn = new ChangeChannelByNumber();
					csn.setChannelNumber(20);
					csn.setIPAddress("131.225.101.1");
					csn.setChecksum(123455678908L);

					stuff = csn;
					// Not relevant until/unless we implement the DB fetch on the message server
					// ChannelListRequest clr = new ChannelListRequest();
					// clr.setEncodedId(id++);
					// clr.setChannelCategory(ChannelCategory.Public);
					// stuff = clr;
					break;

				case 6:

					// This creates an entirely ASCII XML object, including the signature.
					// EnhancedChangeChannel ecc = new EnhancedChangeChannel();
					// ecc.setIPAddress("131.225.101.1");
					// ecc.setContent(ChannelCatalogFactory.getInstance().getDefaultChannel());
					// ecc.setDisplayNumber(14);
					// ecc.setFrom("Me");
					// ecc.setTo("You");
					// ecc.setType(MessageType.MESSAGE);
					//
					// // Signing
					// try {
					// ecc.setSignature(ObjectSigning.getInstance().getSignedObject(ecc).getSignature());
					// } catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | IOException e) {
					// e.printStackTrace();
					// }
					//
					// stuff = ecc;
					break;

				case 7:
					// Not sure how/if this is actually useful here
					// StatusRequestToDisplay sr = new StatusRequestToDisplay();
					// sr.setEncodedId(id++);
					// sr.setDisplayNum(15);
					// stuff = sr;
					break;

				case 8:
					// Not Ready For Prime Time
					// ScreenDump sd = new ScreenDump();
					// sd.setDisplayNum(18);
					// sd.setEncodedId(id++);
					// sd.setScreenSpec(new Object() {
					// public String toString() {
					// return "This is a specification for a screen dump.  Use my screen number 2, "
					// + "which has a URL of http://www.xoc.org/screens/number2.html";
					// }
					// });
					// stuff = sd;
					break;

				case 9:
					Set<SignageContent> map = ChannelCatalogFactory.getInstance().getChannelCatalog(new ChannelCategory("PUBLIC"));
					List<SignageContent> lst = new ArrayList<SignageContent>();
					for (SignageContent C : map) {
						lst.add(C);
					}
					ChannelPlayList cpl = new ChannelPlayList(lst, 60); // This is the XML class
					ChangeChannelList ccl = new ChangeChannelList(); // This is the DigitalSignage class
					ccl.setIPAddress("131.225.1.2");
					ccl.setContent(cpl);
					stuff = ccl;
					break;
				}

				if (stuff != null) {
					String message = MyXMLMarshaller.getXML(stuff);
					System.out.println(which + ": " + message);
				}
			} catch (JAXBException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}
}