package gov.fnal.ppd.dd.util.nonguiUtils;

/**
 * A class that needs to know when the underlying version of Java has changed will need to implement this interface.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public interface JavaChangeListener {

	public void javaHasChanged();
}
