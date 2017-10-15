/*
 * jEdit - Programmer's Text Editor
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright © 2014 jEdit contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.log.parse;

import org.gjt.sp.jedit.gui.KeyEventTranslator;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.util.Log;
import org.log.LogEventTypes;
import org.log.LogItem;
import org.log.LogKey;
import org.log.LogSelection;
import org.log.LogServiceKey;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ViewMain extends JFrame {
	
	public static final int WINDOW_WIDTH = 250;
	public static final int WINDOW_HEIGHT = 400;

	private final ArrayList<ArrayList<LogItem>> items = new ArrayList<ArrayList<LogItem>>();
	private final JEditTextArea textArea;

	static {
		try {
			System.setOut(new PrintStream(Paths.get("jedit-main.log").toFile()));
		} catch (IOException e) {
			Log.log(Log.ERROR, null, "Cannot change system out", e);
		}
	}

	public ViewMain(ArrayList<ArrayList<LogItem>> items, JEditTextArea textArea) throws HeadlessException {
		//super();
		try {
			System.setOut(new PrintStream(Paths.get("jedit-main.log").toFile()));
		} catch (IOException e) {
			Log.log(Log.ERROR, null, "Cannot change system out", e);
		}
		this.items.addAll(items);
		this.textArea = textArea;
		setSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("List actions");
		setVisible(true);
		//pack();
		
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		
		String[] data = getActions();
		final JList<String> list = new JList<String>(data);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		panel.add(new JScrollPane(list), BorderLayout.CENTER);
		
		list.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				final int index = list.getSelectedIndex();
				final int iterator = ParseUtil.getIterator();
				
				if(list.getSelectedValue().equals("MOVING_CURSOR")) 
					return;
				
				if(index > iterator - 1) {
					for(int i = iterator; i < index; i++)
						ParseUtil.nextAction(true);
					
					if(isShowMessage(list.getSelectedValue()))
						ParseUtil.nextAction(false);
					else
						ParseUtil.nextAction(true);
				} else if(index < iterator - 1) {
					for(int i = iterator; i > index + 1; i--)
						ParseUtil.previousAction(true);
					
					if(isShowMessage(list.getSelectedValue()))
						ParseUtil.previousAction(false);
					else
						ParseUtil.previousAction(true);
				}

				list.setSelectedIndex(index);		
			}
		});
		
		getContentPane().add(panel);
	}

	private boolean isShowMessage(String actionName) {
		return actionName.equals("CUT_ACTION")
				|| actionName.equals("COPY_ACTION")
				|| actionName.equals("COMPILE_ACTION")
				|| actionName.equals("RUN_ACTION")
				|| actionName.equals("SAVE_ACTION")
				|| actionName.equals("PASTE_ACTION");
	}
	
	private String[] getActions() {
		String[] res = new String[items.size()];
		int i = 0;
		for(ArrayList<LogItem> sameItems : items) {
			LogItem item = sameItems.get(0);
			
			String actionName = "";			
			if(item.getType() == LogEventTypes.SERVICE_KEY) {
				if(ParseUtil.isDeletedKey(((LogServiceKey)item).getKeyCode()))
					actionName = "DELETE_CHAR";
				else
					actionName = "MOVING_CURSOR";
			} else
				actionName = item.getType().toString();
					
			res[i] = actionName;
			i++;
		}
		
		return res;
	}
}
