/*
 * Copyright (c) 2008, SQL Power Group Inc.
 *
 * This file is part of Power*Architect.
 *
 * Power*Architect is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Power*Architect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package ca.sqlpower.architect.swingui.olap;

import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.swingui.ArchitectSwingSession;
import ca.sqlpower.architect.swingui.PlayPen;

public class OLAPPlayPenFactory {

    private static final Logger logger = Logger.getLogger(OLAPPlayPenFactory.class);

    public static PlayPen createPlayPen(ArchitectSwingSession session, OLAPEditSession oSession) {
        if (session == null) {
            throw new NullPointerException("Null session");
        }
        if (oSession == null) {
            throw new NullPointerException("Null oSession");
        }
        
        PlayPen pp = new PlayPen(session);
        pp.setPopupFactory(new ContextMenuFactory(session, oSession));
        
        return pp;
    }

    /**
     * Sets up OLAP-specific keyboard actions on the playpen. This is done
     * separately because the OLAP session has to be finished creating the
     * actions before this will work, but it needs a playpen before the actions
     * can be created.
     * 
     * @param pp
     *            The playpen to register the keyboard actions on.
     * @param oSession
     *            The session pp belongs to, also the session that owns the
     *            actions to register.
     */
    static void setupOLAPKeyboardActions(PlayPen pp, OLAPEditSession oSession) {
        pp.setupKeyboardActions();
        
        InputMap im = pp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = pp.getActionMap();
        
        if (im == null) {
            throw new NullPointerException("Null input map");
        }
        if (am == null) {
            throw new NullPointerException("Null action map");
        }
        
        
        String KEY_DELETE_SELECTED = "ca.sqlpower.architect.swingui.PlayPen.KEY_DELETE_SELECTED"; //$NON-NLS-1$

        InputMap inputMap = pp.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), KEY_DELETE_SELECTED);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), KEY_DELETE_SELECTED);
        pp.getActionMap().put(KEY_DELETE_SELECTED, oSession.getDeleteSelectedAction());
        if (oSession.getDeleteSelectedAction() == null) logger.warn("oSession.deleteSelectedAction is null!"); //$NON-NLS-1$

        pp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) oSession.getZoomToFitAction().getValue(Action.ACCELERATOR_KEY), "ZOOM TO FIT"); //$NON-NLS-1$
        pp.getActionMap().put("ZOOM TO FIT", oSession.getZoomToFitAction()); //$NON-NLS-1$

        pp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) oSession.getZoomInAction().getValue(Action.ACCELERATOR_KEY), "ZOOM IN"); //$NON-NLS-1$
        pp.getActionMap().put("ZOOM IN", oSession.getZoomInAction()); //$NON-NLS-1$

        pp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) oSession.getZoomOutAction().getValue(Action.ACCELERATOR_KEY), "ZOOM OUT"); //$NON-NLS-1$
        pp.getActionMap().put("ZOOM OUT", oSession.getZoomOutAction()); //$NON-NLS-1$

        pp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put((KeyStroke) oSession.getZoomNormalAction().getValue(Action.ACCELERATOR_KEY), "ZOOM RESET"); //$NON-NLS-1$
        pp.getActionMap().put("ZOOM RESET", oSession.getZoomNormalAction()); //$NON-NLS-1$
        
        
        
        
        
        im.put((KeyStroke) oSession.getCreateCubeAction().getValue(Action.ACCELERATOR_KEY), "NEW CUBE"); //$NON-NLS-1$
        am.put("NEW CUBE", oSession.getCreateCubeAction()); //$NON-NLS-1$

        im.put((KeyStroke) oSession.getCreateMeasureAction().getValue(Action.ACCELERATOR_KEY), "NEW MEASURE"); //$NON-NLS-1$
        am.put("NEW MEASURE", oSession.getCreateMeasureAction()); //$NON-NLS-1$
        
        im.put((KeyStroke) oSession.getCreateDimensionAction().getValue(Action.ACCELERATOR_KEY), "NEW DIMENSION"); //$NON-NLS-1$
        am.put("NEW DIMENSION", oSession.getCreateDimensionAction()); //$NON-NLS-1$
        
        im.put((KeyStroke) oSession.getCreateHierarchyAction().getValue(Action.ACCELERATOR_KEY), "NEW HIERARCHY"); //$NON-NLS-1$
        am.put("NEW HIERARCHY", oSession.getCreateHierarchyAction()); //$NON-NLS-1$
    }
}