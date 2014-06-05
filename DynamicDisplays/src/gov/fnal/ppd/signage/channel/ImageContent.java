package gov.fnal.ppd.signage.channel;

import gov.fnal.ppd.signage.SignageContent;
import gov.fnal.ppd.signage.SignageType;
import gov.fnal.ppd.signage.changer.ChannelCategory;

import java.awt.Image;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * Proof-of-principle that Signage Content can be something other than a web page. It is not _fully_ tested.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * @copy 2014
 * 
 */
public class ImageContent implements SignageContent {

	private static final long	serialVersionUID	= 8092282215310981603L;
	private String				name, description;
	private Image				image;
	private URI					uri;
	private SignageType			type;

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
		assert(c == ChannelCategory.IMAGE);
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
