package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.awt.Image;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

public class ImageContent implements SignageContent {

	private static final long	serialVersionUID	= 8092282215310981603L;

	private String				name, description;

	private Image				image;

	private URI					uri;

	private SignageType			type;

	public ImageContent(String name, Image image) {
		this.name = this.description = name;
		this.image = image;
	}

	public ImageContent(String name, URI uri) {
		this.name = this.description = name;
		this.uri = uri;

		image = getImage();
	}

	public ImageContent(String name, String des, Image image) {
		this.name = name;
		this.description = des;
		this.image = image;
	}

	public ImageContent(String name, String des, URI uri) {
		this.name = this.description = name;
		this.description = des;
		this.uri = uri;
		image = getImage();
	}

	private Image getImage() {
		Image retval = null;
		try {
			URL url = uri.toURL();
			retval = ImageIO.read(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retval;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Object getContent() {
		return image;
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
	public void setContent(Serializable content) {

	}

	@Override
	public void setDescription(String d) {
		this.description = d;
	}

	@Override
	public void setCategory(ChannelCategory c) {
		// This really should be an IMAGE
	}

	@Override
	public void setType(SignageType t) {
		this.type = t;
	}

	@Override
	public void setURI(URI i) {
		this.uri = i;
	}

}
