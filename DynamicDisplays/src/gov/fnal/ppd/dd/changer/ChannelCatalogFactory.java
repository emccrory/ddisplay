/*
 * ChannelCatalogFactory
 *
 * Copyright (c) 2013-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.changer;

import static gov.fnal.ppd.dd.GlobalVariables.credentialsSetup;

import gov.fnal.ppd.dd.interfaces.ChannelCatalog;
import gov.fnal.ppd.dd.interfaces.DatabaseNotVisibleException;
import gov.fnal.ppd.dd.util.nonguiUtils.CredentialsNotFoundException;

/**
 * Singleton class to deal with getting the Channel Catalog to the client.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ChannelCatalogFactory {

	private static ChannelCatalog	me;

	private ChannelCatalogFactory() {
		try {
			ChannelMap cfd = new ChannelMap();
			cfd.load();
			me = cfd;
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * @return The singleton
	 */
	public static ChannelCatalog getInstance() {
		if (me == null)
			refresh();
		return me;
	}

	/**
	 * Re-read the channels from the database
	 * 
	 * @return The instance of ChannelCatalog that is relevant here
	 */
	public static ChannelCatalog refresh() {
		me = null;
		System.gc();
		try {
			ChannelMap cfd = new ChannelMap();
			cfd.load();
			//System.out.println("Got catalogs " + cfd);
			me = cfd;
		} catch (DatabaseNotVisibleException e) {
			e.printStackTrace();
		}
		return me;
	}
	
	public static void main(String[] args) {
		try {
			credentialsSetup();
		} catch (CredentialsNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		System.out.println("ChannelCatalogFactory: " + getInstance().toString().length() + " characters.  First 1024:\n" + getInstance().toString().substring(0, 1024).replace(',', '\n') + " ... ");
	}
}
