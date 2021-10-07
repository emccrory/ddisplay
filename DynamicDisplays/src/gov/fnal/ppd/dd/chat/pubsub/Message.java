package gov.fnal.ppd.dd.chat.pubsub;

public interface Message {

	/**
	 * 
	 * @return String - the contents of the message
	 */
	public String getContents();

	/**
	 * 
	 * @param contents String - the new contents of the message
	 */
	public void setContents(String contents);

}