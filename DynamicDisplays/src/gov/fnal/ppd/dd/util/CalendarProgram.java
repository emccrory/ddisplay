package gov.fnal.ppd.dd.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.GregorianCalendar;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Stolen from the web.
 * 
 * This may, someday, provide the basis for a scheduling system in the Dynamic Display system. But as it stands now, it is just
 * another visual calendar.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation
 *
 */
public class CalendarProgram {
	static JLabel				lblMonth, lblYear;
	static JButton				btnPrev, btnNext;
	static JTable				tblCalendar;
	@SuppressWarnings("rawtypes")
	static JComboBox			cmbYear;
	static JFrame				frmMain;
	static Container			pane;
	static DefaultTableModel	mtblCalendar;						// Table model
	static JScrollPane			stblCalendar;						// The scrollpane
	static JPanel				pnlCalendar;
	static int					realYear, realMonth;
	static int					realDay, currentYear, currentMonth;

	/**
	 * @param args
	 */
	public static void main(final String args[]) {
		// Look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
		} catch (InstantiationException e) {
		} catch (IllegalAccessException e) {
		} catch (UnsupportedLookAndFeelException e) {
		}

		// Prepare frame
		frmMain = new JFrame("Calendar"); // Create frame
		frmMain.setSize(330, 380);
		pane = frmMain.getContentPane(); // Get content pane
		pane.setLayout(null); // Apply null layout
		frmMain.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close when X is clicked

		// Create controls
		lblMonth = new JLabel("January");
		lblYear = new JLabel("Jump to ...");
		cmbYear = new JComboBox();
		btnPrev = new JButton("< <");
		btnNext = new JButton("> >");
		mtblCalendar = new DefaultTableModel() {
			private static final long serialVersionUID = -5373587509402398227L;

			public boolean isCellEditable(int rowIndex, int mColIndex) {
				return false;
			}
		};
		tblCalendar = new JTable(mtblCalendar);
		stblCalendar = new JScrollPane(tblCalendar);
		pnlCalendar = new JPanel(null);

		// Register action listeners
		btnPrev.addActionListener(new btnPrev_Action());
		btnNext.addActionListener(new btnNext_Action());
		cmbYear.addActionListener(new cmbYear_Action());

		// Add controls to pane
		pane.add(pnlCalendar);
		pnlCalendar.add(lblMonth);
		pnlCalendar.add(lblYear);
		pnlCalendar.add(cmbYear);
		pnlCalendar.add(btnPrev);
		pnlCalendar.add(btnNext);
		pnlCalendar.add(stblCalendar);

		// Set bounds [This seems to be placing the components exactly where they need to be]
		pnlCalendar.setBounds(0, 0, 320, 350);
		lblMonth.setBounds(160 - lblMonth.getPreferredSize().width / 2, 25, 100, 25);
		lblYear.setBounds(10, 305, 80, 20);
		cmbYear.setBounds(230, 305, 80, 20);
		btnPrev.setBounds(10, 25, 50, 25);
		btnNext.setBounds(260, 25, 50, 25);
		stblCalendar.setBounds(10, 50, 300, 255);

		// Since we have everything exactly where we want it, don't let the user resize it.
		frmMain.setResizable(false);

		// Make frame visible
		frmMain.setVisible(true);

		// Get real month/year
		GregorianCalendar cal = new GregorianCalendar(); // Create calendar
		realDay = cal.get(GregorianCalendar.DAY_OF_MONTH); // Get day
		realMonth = cal.get(GregorianCalendar.MONTH); // Get month
		realYear = cal.get(GregorianCalendar.YEAR); // Get year
		currentMonth = realMonth; // Match month and year
		currentYear = realYear;

		// Set border
		pnlCalendar.setBorder(BorderFactory.createTitledBorder("" + currentYear));

		// Add headers
		String[] headers = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" }; // All headers
		for (int i = 0; i < 7; i++) {
			mtblCalendar.addColumn(headers[i]);
		}

		tblCalendar.getParent().setBackground(tblCalendar.getBackground()); // Set background

		// No resize/reorder
		tblCalendar.getTableHeader().setResizingAllowed(false);
		tblCalendar.getTableHeader().setReorderingAllowed(false);

		// Single cell selection
		tblCalendar.setColumnSelectionAllowed(true);
		tblCalendar.setRowSelectionAllowed(true);
		tblCalendar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// Set row/column count
		tblCalendar.setRowHeight(38);
		mtblCalendar.setColumnCount(7);
		mtblCalendar.setRowCount(6);

		// Populate table
		for (int i = realYear - 100; i <= realYear + 100; i++) {
			cmbYear.addItem(String.valueOf(i));
		}

		// Refresh calendar
		refreshCalendar(realMonth, realYear); // Refresh calendar
	}

	/**
	 * @param month
	 * @param year
	 */
	public static void refreshCalendar(final int month, final int year) {
		// Variables
		String[] months = { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October",
				"November", "December" };
		int nod, som; // Number Of Days, Start Of Month

		// Allow/disallow buttons
		btnPrev.setEnabled(true);
		btnNext.setEnabled(true);
		if (month == 0 && year <= realYear - 10) {
			btnPrev.setEnabled(false);
		} // Too early
		if (month == 11 && year >= realYear + 100) {
			btnNext.setEnabled(false);
		} // Too late
		lblMonth.setText(months[month] + " " + year); // Refresh the month label (at the top)
		lblMonth.setBounds(160 - lblMonth.getPreferredSize().width / 2, 25, 180, 25); // Re-align label with calendar
		cmbYear.setSelectedItem(String.valueOf(year)); // Select the correct year in the combo box
		pnlCalendar.setBorder(BorderFactory.createTitledBorder("" + year));

		// Clear table
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 7; j++) {
				mtblCalendar.setValueAt(null, i, j);
			}
		}

		// Get first day of month and number of days
		GregorianCalendar cal = new GregorianCalendar(year, month, 1);
		nod = cal.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
		som = cal.get(GregorianCalendar.DAY_OF_WEEK);

		// Draw calendar
		for (int i = 1; i <= nod; i++) {
			int row = new Integer((i + som - 2) / 7);
			int column = (i + som - 2) % 7;
			mtblCalendar.setValueAt(i, row, column);
		}

		// Apply renderers
		tblCalendar.setDefaultRenderer(tblCalendar.getColumnClass(0), new tblCalendarRenderer());
	}

	static class tblCalendarRenderer extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 6536367067594571963L;

		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row,
				int column) {
			super.getTableCellRendererComponent(table, value, selected, focused, row, column);
			if (column == 0 || column == 6) { // Week-end
				setBackground(new Color(255, 220, 220));
			} else { // Week
				setBackground(new Color(255, 255, 255));
			}
			if (value != null) {
				if (Integer.parseInt(value.toString()) == realDay && currentMonth == realMonth && currentYear == realYear) { // Today
					setBackground(new Color(220, 220, 255));
				}
			}
			setBorder(null);
			setForeground(Color.black);
			return this;
		}
	}

	static class btnPrev_Action implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (currentMonth == 0) { // Back one year
				currentMonth = 11;
				currentYear -= 1;
			} else { // Back one month
				currentMonth -= 1;
			}
			refreshCalendar(currentMonth, currentYear);
		}
	}

	static class btnNext_Action implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (currentMonth == 11) { // Foward one year
				currentMonth = 0;
				currentYear += 1;
			} else { // Foward one month
				currentMonth += 1;
			}
			refreshCalendar(currentMonth, currentYear);
		}
	}

	static class cmbYear_Action implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (cmbYear.getSelectedItem() != null) {
				String b = cmbYear.getSelectedItem().toString();
				currentYear = Integer.parseInt(b);
				refreshCalendar(currentMonth, currentYear);
			}
		}
	}
}
