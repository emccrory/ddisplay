package test.gov.fnal.ppd.dd.db;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.fnal.ppd.dd.db.ChannelsFromDatabase.*;

import org.junit.Test;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.signage.Channel;
import gov.fnal.ppd.dd.signage.SignageContent;

public class ChannelsFromDatabaseTest {

	@Test
	public void testGetChannels() {
		try {
			Map<String, SignageContent> c = new HashMap<String, SignageContent>();
			getChannels(c);
			assertTrue(c.size() > 0);
		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testGetImages() {
		try {
			Map<String, SignageContent> c = new HashMap<String, SignageContent>();
			getImages(c);
			assertTrue(c.size() > 0);
		} catch (Exception e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

	@Test
	public void testGetCategoriesDatabase() {
		ChannelClassification[] c = getCategoriesDatabaseForLocation();
		assertTrue(c.length > 0);
	}

	@Test
	public void testGetDocentContent() {
		List<SignageContent> L = getDocentContent("Default");
		assertTrue(L.size() > 0);
	}

	@Test
	public void testReadValidChannels() {
		Map<Integer, Channel> m = new HashMap<Integer, Channel>();
		readValidChannels(m);
		assertTrue(m.size() > 0);
	}

	@Test
	public void testGetSpecialChannelsForDisplay() {
		try {
			Set<SignageContent> s = getSpecialChannelsForDisplay(1);
			assertTrue(s.size() > 0);
		} catch (DatabaseNotVisibleException e) {
			fail("Exception caught: " + e.getClass().getSimpleName() + " -- " + e.getLocalizedMessage());
		}
	}

}
