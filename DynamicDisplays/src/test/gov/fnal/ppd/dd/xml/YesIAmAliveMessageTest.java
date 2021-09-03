package test.gov.fnal.ppd.dd.xml;

import javax.xml.bind.JAXBException;

import org.junit.Test;

import gov.fnal.ppd.dd.xml.ClientInformation;
import gov.fnal.ppd.dd.xml.MessageCarrierXML;
import gov.fnal.ppd.dd.xml.MessagingDataXML;
import gov.fnal.ppd.dd.xml.MyXMLMarshaller;
import gov.fnal.ppd.dd.xml.XMLDocumentAndString;
import gov.fnal.ppd.dd.xml.messages.YesIAmAliveMessage;

public class YesIAmAliveMessageTest {
	@Test
	public void testYesIAmAlive() {
		String expect = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
				+ "<messageCarrierXML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://null signage.xsd\">\n"
				+ "    <messageOriginator>From me</messageOriginator>\n    <messageRecipient>To you</messageRecipient>\n"
				+ "    <messageValue xsi:type=\"yesIAmAliveMessage\">\n        <client>\n"
				+ "            <beginTime></beginTime>\n            <name>Some Client, baby!</name>\n        </client>\n"
				+ "    </messageValue>\n    <timeStamp></timeStamp>\n" + "</messageCarrierXML>\n";

		MessagingDataXML stuff = null;
		YesIAmAliveMessage aam = new YesIAmAliveMessage();
		aam.setClient(new ClientInformation("Some Client, baby!", System.currentTimeMillis()));
		stuff = aam;

		MessageCarrierXML mc = new MessageCarrierXML("From me", "To you", stuff);
		try {
			String theMessageString = MyXMLMarshaller.getXML(mc);

			XMLDocumentAndString theMessageDocument = new XMLDocumentAndString(theMessageString);
			String xml = theMessageDocument.getTheXML();
			// Remove the time stamps
			xml = xml.replaceAll("<beginTime>[0-9]+</beginTime>", "<beginTime></beginTime>");
			xml = xml.replaceAll("<timeStamp>[0-9]+</timeStamp>", "<timeStamp></timeStamp>");
			assert(expect.equals(xml));
			
		} catch (JAXBException e) {
			e.printStackTrace();
			assert(false);
		}
	}
}
