package test.gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;
import static org.junit.Assert.*;

import org.junit.Test;

import gov.fnal.ppd.dd.changer.ListOfExistingContent;

public class ListOfExistingContentTest {

	static {
		credentialsSetup();
	}

	@Test
	public void testListOfExistingContent() {
		ListOfExistingContent content = new ListOfExistingContent();

		// This is not right since this Map is filled by watching the display status change in the database.
		// See gov.fnal.ppd.dd.util.CheckDisplayStatus.

		// This is a place where refactoring should happen. CheckDisplayStatus has all KINDS of Model, View and Controller stuff
		// mixed into itself.

		assertTrue(content.size() == 0);
	}

}