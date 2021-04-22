package gov.fnal.ppd.dd.channel.list.table;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * SimpleTableDemo.java requires no other files.
 */

import static gov.fnal.ppd.dd.GlobalVariables.SHOW_IN_WINDOW;
import gov.fnal.ppd.dd.signage.Channel;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 * This class contains the complexities for creating a Java table (JTable). This is used in one place - The ChannelListGUI that is
 * used in the ChannelSelector for letting the user create a list of channels to send to a display.
 * 
 * In my opinion, JTables are quite complicated. And this class tries to contain all that so the calling class is not overly
 * complex. But the upshot is that the GUI presented to the user is also rather complicated. In a perfect world with a lot more time
 * and expertise, this would be made MUCH simpler for the user.
 * 
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017-21
 * 
 */
public class ChannelCooserAsTable extends JTable {
	private static final long			serialVersionUID	= 8283499370010280137L;
	private ChannelChooserTableModel	model;

	/**
	 * 
	 */
	public ChannelCooserAsTable() {
		super();
		model = new ChannelChooserTableModel();
		setModel(model);
		setAutoCreateRowSorter(true);
		setFillsViewportHeight(true);

		TableColumnModel tcm = getColumnModel();
		for (int i = 0; i < (tcm.getColumnCount()); i++) {
			tcm.getColumn(i).setPreferredWidth(model.getRelativeWidths()[i]);
		}

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);
		setDefaultRenderer(Number.class, centerRenderer);

		setRowHeight((SHOW_IN_WINDOW ? 40 : 60));

		setFont(new Font("Monospace", Font.PLAIN, SHOW_IN_WINDOW ? 11 : 16));

		setDefaultRenderer(Channel.class, new ChannelCellRenderer());
	}

	/**
	 * @param viewRow
	 * @return the channel at this row
	 */
	public Channel getRow(final int viewRow) {
		return model.getRow(viewRow);
	}
}
