package test.gov.fnal.ppd.dd.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import gov.fnal.ppd.dd.display.client.ListOfValidChannels;
import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ListOfValidChannelsTest extends NeedsCredentials {

	@Test
	public void testContainsSignageContent() {
		assumeTrue(NeedsCredentials.credentialsOK);
		try {
			ListOfValidChannels valid = new ListOfValidChannels();

			// A better check that this would be to do a separate SQL query, which would be
			//
			// SELECT COUNT(DISTINCT URL) FROM Channel WHERE Approval=1
			//
			// And compare these numbers
			assertTrue(valid.size() > 0);
			//

			int count = 0;
			for (Object Obj : valid.toArray()) {
				try {
					// A non test: Assures that HashSet works (it would be an unmitigated catastrophe if it didn't)
					String key = (String) Obj;
					assertTrue(valid.contains(key));
					if (valid.contains(key))
						count++;
				} catch (Exception e) {
					fail("Exception caught testContainsSignageContent_1: " + e.getClass().getSimpleName() + " -- "
							+ e.getLocalizedMessage());
				}
			}
			assertEquals(valid.size(), count);
		} catch (Exception e) {
			fail("Exception caught testContainsSignageContent_2: " + e.getClass().getSimpleName() + " -- "
					+ e.getLocalizedMessage());
		}
	}
}
