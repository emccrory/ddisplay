package test.gov.fnal.ppd.dd.util;

import static gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion.getInstance;
import static gov.fnal.ppd.dd.util.nonguiUtils.JavaVersion.setWatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import gov.fnal.ppd.dd.util.nonguiUtils.JavaChangeListener;

public class JavaVersionTest {

	public JavaVersionTest() {
		setWatch(false);
	}
	@Test
	public void testGetInstance() {
		assertNotNull(getInstance());
	}

	@Test
	public void testGet() {
		assertTrue(getInstance().get().length() > 4);
	}

	@Test
	public void testGetCurrentVersion() {
		assertEquals(getInstance().getCurrentVersion(), getInstance().get());
	}

	@Test
	public void testHasVersionChanged() {
		assertFalse(getInstance().hasVersionChanged());
	}

	@Test
	public void testAddJavaChangeListener() {
		getInstance().addJavaChangeListener(new JavaChangeListener() {
			
			@Override
			public void javaHasChanged() {
				// Nothing will change during the time it takes for this test to run.				
			}
		});
	}

}
