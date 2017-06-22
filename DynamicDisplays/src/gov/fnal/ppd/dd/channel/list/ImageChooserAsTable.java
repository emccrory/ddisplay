package gov.fnal.ppd.dd.channel.list;

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
import gov.fnal.ppd.dd.channel.ChannelImage;
import gov.fnal.ppd.dd.signage.Channel;

import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

/**
 * @author Elliott McCrory, Fermilab AD/Instrumentation, 2017
 * 
 */
public class ImageChooserAsTable extends JTable {
	private static final long		serialVersionUID	= 8283499370010280137L;
	private ImageChooserTableModel	model;

	/**
	 * 
	 */
	public ImageChooserAsTable() {
		super();
		model = new ImageChooserTableModel();
		setModel(model);
		setAutoCreateRowSorter(true);
		// setFillsViewportHeight(true);

		TableColumnModel tcm = getColumnModel();
		for (int i = 0; i < (tcm.getColumnCount()); i++) {
			tcm.getColumn(i).setPreferredWidth(model.relativeWidths[i]);
		}

		// Columns: (0) Image Icon, (1) Experiment, (2) URL
		
		setRowHeight(160);
		setFont(new Font("Monospace", Font.PLAIN, SHOW_IN_WINDOW ? 12 : 18));
		setDefaultRenderer(ChannelImage.class, new ChannelImageCellRenderer());
	}

	/**
	 * @param viewRow
	 * @return the channel at this row
	 */
	public ChannelImage getRow(final int viewRow) {
		return (ChannelImage) model.getRow(viewRow);
	}
}
