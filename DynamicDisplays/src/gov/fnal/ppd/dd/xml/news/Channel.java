/*
 * Channel
 *
 * Copyright (c) 2015 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.xml.news;

import gov.fnal.ppd.dd.xml.MyXMLMarshaller;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This is what an RSS news feed looks like. It has to be called "Channel" because this is what is in the RSS. Unfortunate
 * overloading of this popular word.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@XmlRootElement(name = "channel")
@SuppressWarnings("javadoc")
public class Channel {

	private String	title;
	private String	link;
	private String	language;
	private String	copyright;
	private String	feedburner;
	private String	creator;

	private String	description;
	private String	pubDate;
	private int		ttl;
	private Item[]	item;

	public String getLink() {
		return link;
	}

	@XmlElement
	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	@XmlElement
	public void setDescription(String description) {
		this.description = description;
	}

	public String getPubDate() {
		return pubDate;
	}

	@XmlElement
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}

	public int getTtl() {
		return ttl;
	}

	@XmlElement
	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

	public Item[] getItem() {
		return item;
	}

	@XmlElement
	public void setItem(Item[] item) {
		this.item = item;
	}

	public String getTitle() {
		return title;
	}

	@XmlElement
	public void setTitle(String title) {
		this.title = title;
	}

	@XmlElement
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	@XmlElement
	public String getCopyright() {
		return copyright;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	@XmlElement
	public String getFeedburner() {
		return feedburner;
	}

	public void setFeedburner(String feedburner) {
		this.feedburner = feedburner;
	}

	@XmlElement
	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public static void main(String[] args) {
		Path path = FileSystems.getDefault().getPath(args[0]);
		String xml = "";
		int limit = 9999;
		if (args.length > 1)
			limit = Integer.parseInt(args[1]);
		try {
			List<String> lines = Files.readAllLines(path, Charset.defaultCharset());
			boolean start = false;
			for (String L : lines) {
				if (L.contains("<channel>"))
					start = true;
				if (start && L.length() > 0 && !L.contains("</rss>") && !L.contains("<atom:link") && !L.contains("dc:creator")) {
					if (L.contains("<feedburner") && L.contains("</item>"))
						xml += "</item>\n";
					else if (L.contains("<feedburner") && L.contains("</channel>"))
						xml += "</channel>\n";
					else if (L.contains("<media:") && L.contains("<item>"))
						xml += "<item>\n";
					else if (L.contains("<atom") && L.contains("<item>"))
						xml += "<item>\n";
					else if (!L.contains("<media:"))
						xml += L + "\n";
				} // else
					// System.out.println("-- skipping " + L);
			}

		} catch (IOException e) {
			System.err.println(path.toAbsolutePath());
			e.printStackTrace();
		}

		// System.out.println(xml);
		try {
			Channel c = (Channel) MyXMLMarshaller.unmarshall(Channel.class, xml);

			System.out.println("<em>" + mychop(c.getTitle()) + ", " + mychop(c.getLink()) + " (Retrieved " + (new Date())
					+ ")</em>");
			if (c.item != null) {
				for (Item I : c.item) {
					if (I == null || I.getTitle() == null)
						continue;
					if (I.getTitle().length() < 10)
						continue;
					if (I.getDescription() == null || I.getDescription().length() < 10)
						System.out.println("<b>" + mychop(I.getTitle()) + "</b>");
					else
						System.out.println("<b>" + mychop(I.getTitle()) + "</b>: " + mychop(I.getDescription()));
					if (limit-- <= 0)
						break;
				}
			}
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static String mychop(String s) {
		if (s == null)
			return "";
		if (s.length() - 2 <= 0)
			return "";

		if (s.startsWith("\n"))
			s = s.substring(1);
		if (s.endsWith("\n"))
			return s.substring(0, s.length() - 1);

		return s;
	}
}
