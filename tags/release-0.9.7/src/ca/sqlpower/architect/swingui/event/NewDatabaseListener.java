/*
 * Copyright (c) 2007, SQL Power Group Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of SQL Power Group Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ca.sqlpower.architect.swingui.event;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JDialog;

import ca.sqlpower.architect.swingui.ArchitectFrame;
import ca.sqlpower.architect.swingui.DBCSPanel;
import ca.sqlpower.architect.swingui.action.DBCSOkAction;
import ca.sqlpower.sql.SPDataSource;
import ca.sqlpower.swingui.DataEntryPanelBuilder;

/**
 * When a new database connection has been established, this listener
 * kicks in to add it to the dropdown list.
 * <p>
 * XXX it would be simpler just to re-create the dropdown list every
 *     time it gets displayed
 */
public class NewDatabaseListener implements ActionListener {

	private ArchitectFrame frame;
	private String title;
	
	public NewDatabaseListener(ArchitectFrame frame, String title, JComboBox comboBox) {
		super();
		this.frame = frame;
		this.title = title;
	}

	public void actionPerformed(ActionEvent e) {
		
		final DBCSPanel dbcsPanel = new DBCSPanel(
                frame.getArchitectSession().getUserSettings().getPlDotIni());
		
		dbcsPanel.setDbcs(new SPDataSource());

		DBCSOkAction okButton = new DBCSOkAction(dbcsPanel, frame.getArchitectSession(), true);
		
		Action cancelAction = new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				dbcsPanel.discardChanges();
			}
		};
		
		JDialog d = DataEntryPanelBuilder.createDataEntryPanelDialog(
				dbcsPanel,frame,
				title, DataEntryPanelBuilder.OK_BUTTON_LABEL,
				okButton, cancelAction);
		
		okButton.setConnectionDialog(d);
		
		d.pack();
		d.setLocationRelativeTo(frame);
		d.setVisible(true);
	}
	
}