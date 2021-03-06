/*
 * ImageContent
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.ZZattic;

import java.awt.Image;
import java.net.URI;
import java.net.URL;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.imageio.ImageIO;

import gov.fnal.ppd.dd.changer.ChannelClassification;
import gov.fnal.ppd.dd.signage.SignageContent;

/**
 * Proof-of-principle that Signage Content can be something other than a web page. It is not _fully_ tested.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ImageContent implements SignageContent {

	private static final long	serialVersionUID	= 8092282215310981603L;
	private String				name, description;
	private Image				image;
	private URI					uri;
	private long				time				= 0L;
	private int					code;
	private long				expiration			= 0L;

	/**
	 * @param name
	 *            The name assigned to this Content
	 * @param des
	 *            A description of this Content
	 * @param image
	 *            The image that is this content
	 */
	public ImageContent(final String name, final String des, final Image image) {
		this.name = name;
		this.description = des;
		this.image = image;
	}

	/**
	 * @param name
	 *            The name assigned to this Content
	 * @param des
	 *            A description of this Content
	 * @param uri
	 *            The URI that points to this Content (assumed to be an image)
	 */
	public ImageContent(final String name, final String des, final URI uri) {
		this.name = this.description = name;
		this.description = des;
		this.uri = uri;

		try {
			URL url = uri.toURL();
			image = ImageIO.read(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param content
	 */
	public ImageContent(final SignageContent content) {
		this.name = content.getName();
		this.description = content.getDescription();
		this.uri = content.getURI();
		this.time = content.getTime();

		if (content instanceof ImageContent)
			this.image = ((ImageContent) content).image;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public ChannelClassification getChannelClassification() {
		return ChannelClassification.IMAGE;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setDescription(String d) {
		this.description = d;
	}

	@Override
	public void setChannelClassification(ChannelClassification c) {
		// This really should be an IMAGE
		assert (c.equals(ChannelClassification.IMAGE));
	}

	@Override
	public void setURI(URI i) {
		this.uri = i;
	}

	@Override
	public long getTime() {
		return time;
	}

	@Override
	public void setTime(long time) {
		this.time = time;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public void setCode(int n) {
		code = n;
	}

	@Override
	public long getExpiration() {
		return expiration;
	}

	@Override
	public void setExpiration(long expire) {
		this.expiration = expire;
	}

	public long getChecksum() {
		Checksum checksum = new CRC32();

		// Hmmm. Not sure if this is sufficient!
		byte bytes[] = (image.hashCode() + image.toString()).getBytes();
		// update the current checksum with the specified array of bytes
		checksum.update(bytes, 0, bytes.length);

		// get the current checksum value
		return checksum.getValue();
	}
}
