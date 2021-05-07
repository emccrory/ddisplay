package gov.fnal.ppd.dd.util.guiUtils;

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * helper class for CreateListOfChannels, to make the panel in the dialog box presented to the user. Shamelessly stolen from the
 * Oracle/Java "How To" examples.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 * 
 */
public class ListSelectionForSaveList extends JPanel {

	/**
	 * @author Elliott McCrory, Fermilab AD/Instrumentation
	 * 
	 */
	public interface SpecialLocalListChangeListener {

		/**
		 * @param listValue
		 *            The value of the list index
		 */
		void setListValue(int listValue);
	}

	private static final long				serialVersionUID	= -8516344045768695215L;
	private ListSelectionModel				listSelectionModel;
	private SpecialLocalListChangeListener	myListener;
	private JList<String>					list;

	/**
	 * @param list
	 *            The list of names for the selection
	 * @param displayArea
	 *            The area in which to display the results (mostly filled in by the calling class, but we need it here to attache to
	 *            the panel)
	 */
	public ListSelectionForSaveList(final JList<String> list, final JTextArea displayArea) {
		super(new BorderLayout());
		this.list = list;

		listSelectionModel = list.getSelectionModel();
		listSelectionModel.addListSelectionListener(new SharedListSelectionHandler());
		listSelectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listPane = new JScrollPane(list);

		JPanel controlPane = new JPanel();

		JScrollPane outputPane = new JScrollPane(displayArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		// Do the layout.
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		add(splitPane, BorderLayout.CENTER);

		JPanel topHalf = new JPanel();
		topHalf.setLayout(new BoxLayout(topHalf, BoxLayout.LINE_AXIS));
		JPanel listContainer = new JPanel(new GridLayout(1, 1));
		listContainer.setBorder(BorderFactory.createTitledBorder(" Channel Lists "));
		listContainer.add(listPane);

		topHalf.add(listContainer);
		// topHalf.add(tableContainer);

		splitPane.add(topHalf);

		JPanel bottomHalf = new JPanel(new BorderLayout());
		bottomHalf.add(controlPane, BorderLayout.PAGE_START);
		bottomHalf.add(outputPane, BorderLayout.CENTER);
		// XXX: next line needed if bottomHalf is a scroll pane:
		// bottomHalf.setMinimumSize(new Dimension(400, 50));
		splitPane.add(bottomHalf);

		if (!SHOW_IN_WINDOW) {
			list.setFont(list.getFont().deriveFont(18.0f));
			listPane.getVerticalScrollBar().setPreferredSize(new Dimension(30, 0));
			listPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 30));
			outputPane.getVerticalScrollBar().setPreferredSize(new Dimension(30, 0));
			outputPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 30));

			topHalf.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
			topHalf.setMinimumSize(new Dimension(350, 350));
			topHalf.setPreferredSize(new Dimension(350, 400));
		} else {
			topHalf.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
			topHalf.setMinimumSize(new Dimension(200, 200));
			topHalf.setPreferredSize(new Dimension(200, 300));
		}
		bottomHalf.setPreferredSize(new Dimension(600, 300));

	}

	class SharedListSelectionHandler implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			ListSelectionModel lsm = (ListSelectionModel) e.getSource();

			boolean isAdjusting = e.getValueIsAdjusting();
			if (!isAdjusting) {

				if (lsm.isSelectionEmpty()) {
				} else {
					// Find out which index is selected.
					int listNumber = lsm.getMinSelectionIndex();
					String listValue = list.getModel().getElementAt(listNumber);
					myListener.setListValue(Integer.parseInt(listValue.substring(0, listValue.indexOf(':'))));
				}
			}
		}
	}

	/**
	 * A special-purpose listener. This is used by CreateListOfChannels.SaveRestoreListOfChannels
	 * 
	 * @param listener
	 */
	public void addListener(final SpecialLocalListChangeListener listener) {
		this.myListener = listener;
	}
}