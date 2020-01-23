package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.changer.ChannelClassificationDictionary.getCategories;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Test;

import test.gov.fnal.ppd.dd.NeedsCredentials;

public class ChannelClassificationDictionaryTest extends NeedsCredentials {

	@Test
	public void testGetCategories() {
		assumeTrue(credentialsOK);
		assertTrue(getCategories().length > 1);
	}

}
