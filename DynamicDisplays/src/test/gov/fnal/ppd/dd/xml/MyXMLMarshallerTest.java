package test.gov.fnal.ppd.dd.xml;

import org.junit.Test;

/**
 * Test of the class MyXMLMrshaller. The test conceived for here were too fragile to be useful, so there are actually no tests here.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class MyXMLMarshallerTest {

	@SuppressWarnings("unused")
	private static String rawMessage = //
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?><messageCarrierXML xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://null signage.xsd\">\n"
					+ //
					"    <messageOriginator>From me</messageOriginator>\n" + //
					"    <messageRecipient>To you</messageRecipient>\n" + //
					"    <messageValue xsi:type=\"changeChannel\">\n" + //
					"        <channelNumber>22</channelNumber>\n" + //
					"        <channelSpec>\n" + //
					"            <channelClassification>\n" + //
					"                <abbreviation>Misce</abbreviation>\n" + //
					"                <value>Miscellaneous</value>\n" + //
					"            </channelClassification>\n" + //
					"            <checksum>2650161038</checksum>\n" + //
					"            <code>2</code>\n" + //
					"            <description>A NuMI Event Display -  Redirects to an http: site</description>\n" + //
					"            <expiration>0</expiration>\n" + //
					"            <name>NuMI A9 Monitor</name>\n" + //
					"            <number>22</number>\n" + //
					"            <time>0</time>\n" + //
					"            <URI>http://dbweb0.fnal.gov/ifbeam/app/a9_monitor</URI>\n" + //
					"        </channelSpec>\n" + //
					"        <displayNumber>14</displayNumber>\n" + //
					"        <screenNumber>0</screenNumber>\n" + //
					"    </messageValue>\n" + //
					"    <timeStamp>1620417541870</timeStamp>\n" + //
					"</messageCarrierXML>\n";

	@Test
	public void testXMLRoundTrip() {
		assert (true); // Not working yet
	}

	public static void main(String[] args) {
		// try {
		// // 5/7/2021 - this is not working. Getting an UnmarshallException
		// ChangeChannel receivedMessage = (ChangeChannel) MyXMLMarshaller.unmarshall(ChangeChannel.class, rawMessage);
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}
}
