package gov.fnal.ppd.dd.chat.pubsub.simple;

import gov.fnal.ppd.dd.chat.pubsub.Message;

/**
 * https://riptutorial.com/design-patterns/example/6498/publish-subscribe-in-java (Retrieved Sept 2021)
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class MessageImpl implements Message {

	private String contents;

	public MessageImpl(String contents){
		this.setContents(contents);
	}

	/* (non-Javadoc)
	 * @see gov.fnal.ppd.dd.chat.pubsub.Message#getContents()
	 */
	@Override
	public String getContents() {
		return contents;
	}

	/* (non-Javadoc)
	 * @see gov.fnal.ppd.dd.chat.pubsub.Message#setContents(java.lang.String)
	 */
	@Override
	public void setContents(String contents) {
		this.contents = contents;
	}	
}
