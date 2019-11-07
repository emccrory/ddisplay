package gov.fnal.ppd.dd.xml;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;

/**
 * Encapsulate information about a client node in the Dynamic Displays system
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
@SuppressWarnings("javadoc")
public class ClientInformation {
	private String	name;
	private long	beginningTimeStamp;

	public ClientInformation() {
		this("unspecified", System.currentTimeMillis());
	}

	public ClientInformation(String name) {
		this(name, System.currentTimeMillis());
	}

	public ClientInformation(String name, long started) {
		this.name = name;
		this.beginningTimeStamp = started;
	}

	@XmlElement
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement
	public long getBeginTime() {
		return beginningTimeStamp;
	}

	public void setBeginTime(final long n) {
		beginningTimeStamp = n;
	}

	public String toString() {
		return name + " [" + new Date(beginningTimeStamp) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 97;
		int result = 1;
		result = prime * result + (int) (beginningTimeStamp ^ (beginningTimeStamp >>> 32));
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ClientInformation other = (ClientInformation) obj;
		if (beginningTimeStamp != other.beginningTimeStamp)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
