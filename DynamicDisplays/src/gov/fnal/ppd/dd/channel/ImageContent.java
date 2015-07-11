/*
 * ImageContent
 *
 * Copyright (c) 2014-15 by Fermilab Research Alliance (FRA), Batavia, Illinois, USA.
 */
package gov.fnal.ppd.dd.channel;

import gov.fnal.ppd.dd.changer.ChannelCategory;
import gov.fnal.ppd.dd.signage.SignageContent;
import gov.fnal.ppd.dd.signage.SignageType;

import java.awt.Image;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

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
	private SignageType			type;
	private long				time				= 0L;
	private int					frameNumber;
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
		this.frameNumber = content.getFrameNumber();

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
	public ChannelCategory getCategory() {
		return ChannelCategory.IMAGE;
	}

	@Override
	public URI getURI() {
		return uri;
	}

	@Override
	public SignageType getType() {
		return type;
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
	public void setCategory(ChannelCategory c) {
		// This really should be an IMAGE
		assert (c.equals(ChannelCategory.IMAGE));
	}

	@Override
	public void setType(SignageType t) {
		this.type = t;
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
	public int getFrameNumber() {
		return frameNumber;
	}

	@Override
	public void setFrameNumber(int f) {
		frameNumber = f;
	}

	@Override
	public long getExpiration() {
		return expiration;
	}

	@Override
	public void setExpiration(long expire) {
		this.expiration = expire;
	}

}
