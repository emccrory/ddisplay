package gov.fnal.ppd.dd.xml;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Encapsulate an error message into an XML class
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class ErrorMessage extends MessagingDataXML {
	private String theErrorMessage;

	public ErrorMessage() {
		this("no topic");
	}

	public ErrorMessage(String name) {
		this.theErrorMessage = name;
	}

	@XmlElement
	public String getErrorMessageText() {
		return theErrorMessage;
	}

	public void setErrorMessageText(String text) {
		this.theErrorMessage = text;
	}
	@Override
	public boolean willNotChangeAnything() {
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 43;
		int result = 1;
		result = prime * result + ((theErrorMessage == null) ? 0 : theErrorMessage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ErrorMessage other = (ErrorMessage) obj;
		if (theErrorMessage == null) {
			if (other.theErrorMessage != null)
				return false;
		} else if (!theErrorMessage.equals(other.theErrorMessage))
			return false;
		return true;
	}

}
