package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.changer.ChannelClassificationDictionary.getCategoriesForLocation;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import test.gov.fnal.ppd.dd.NeedsCredentials;

/**
 * Tests of the class ChannelClassificationDictionary
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class ChannelClassificationDictionaryTest extends NeedsCredentials {

	@Test
	public void testGetCategories() {
		assumeTrue(credentialsOK);
		assertTrue(getCategoriesForLocation().length > 1);
	}
}
