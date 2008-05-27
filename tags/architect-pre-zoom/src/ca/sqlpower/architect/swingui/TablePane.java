package ca.sqlpower.architect.swingui;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import javax.swing.*;
import javax.swing.tree.TreePath;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
import java.util.ListIterator;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.IOException;

import org.apache.log4j.Logger;

import ca.sqlpower.architect.*;

public class TablePane 
	extends JComponent 
	implements SQLObjectListener, java.io.Serializable, Selectable, DragSourceListener {

	private static final Logger logger = Logger.getLogger(TablePane.class);

	protected DragGestureListener dgl;
	protected DragGestureRecognizer dgr;
	protected DragSource ds;

	/**
	 * A constant indicating the title label on a TablePane.
	 */
	public static final int COLUMN_INDEX_TITLE = -1;

	/**
	 * A constant indicating no column or title.
	 */
	public static final int COLUMN_INDEX_NONE = -2;

	/**
	 * This is the column index at which to the insertion point is
	 * currently rendered. Columns will be added after this column.
	 * If it is COLUMN_INDEX_NONE, no insertion point will be
	 * rendered and columns will be added at the bottom.
	 */
	protected int insertionPoint;

	/**
	 * How many pixels should be left between the surrounding box and
	 * the column name labels.
	 */
	protected Insets margin = new Insets(1,1,1,1);

	/**
	 * A selected TablePane is one that the user has clicked on.  It
	 * will appear more prominently than non-selected TablePanes.
	 */
	protected boolean selected;

	protected DropTarget dt;

	protected ArrayList columnSelection;

	/**
	 * During a drag operation where a column is being dragged from
	 * this TablePane, this variable points to the column being
	 * dragged.  At all other times, it should be null.
	 */
	protected SQLColumn draggingColumn;

	static {
		UIManager.put(TablePaneUI.UI_CLASS_ID, "ca.sqlpower.architect.swingui.BasicTablePaneUI");
	}

	private SQLTable model;

	public TablePane() {
		setOpaque(true);
		setMinimumSize(new Dimension(100,200));
		setPreferredSize(new Dimension(100,200));
		dt = new DropTarget(this, new TablePaneDropListener());

		dgl = new TablePaneDragGestureListener();
		ds = new DragSource();
		dgr = getToolkit().createDragGestureRecognizer(MouseDragGestureRecognizer.class, ds, this, DnDConstants.ACTION_MOVE, dgl);
		setInsertionPoint(COLUMN_INDEX_NONE);
		addMouseListener(new PopupListener());
		updateUI();
	}

	public TablePane(SQLTable m) {
		this();
		setModel(m);
	}

	public void setUI(TablePaneUI ui) {super.setUI(ui);}

    public void updateUI() {
		setUI((TablePaneUI)UIManager.getUI(this));
		invalidate();
    }

    public String getUIClassID() {
        return TablePaneUI.UI_CLASS_ID;
    }

	/**
	 * You must call this method when you are done with a TablePane
	 * component.  It unregisters this instance (and its UI delegate)
	 * on all event listener lists on which it was previously
	 * registered.
	 */
	public void destroy() {
		try {
			ArchitectUtils.unlistenToHierarchy(this, model);
		} catch (ArchitectException e) {
			logger.error("Caught exception while unlistening to all children", e);
		}
	}

	// -------------------- sqlobject event support ---------------------

	/**
	 * Listens for property changes in the model (columns
	 * added).  If this change affects the appearance of
	 * this widget, we will notify all change listeners (the UI
	 * delegate) with a ChangeEvent.
	 */
	public void dbChildrenInserted(SQLObjectEvent e) {
		int ci[] = e.getChangedIndices();
		for (int i = 0; i < ci.length; i++) {
			columnSelection.add(ci[i], Boolean.FALSE);
		}
		try {
			ArchitectUtils.listenToHierarchy(this, e.getChildren());
		} catch (ArchitectException ex) {
			logger.error("Caught exception while listening to added children", ex);
		}
		firePropertyChange("model.children", null, null);
		revalidate();
	}

	/**
	 * Listens for property changes in the model (columns
	 * removed).  If this change affects the appearance of
	 * this widget, we will notify all change listeners (the UI
	 * delegate) with a ChangeEvent.
	 */
	public void dbChildrenRemoved(SQLObjectEvent e) {
		if (e.getSource() == this.model.getColumnsFolder()) {
			int ci[] = e.getChangedIndices();
			for (int i = 0; i < ci.length; i++) {
				columnSelection.remove(ci[i]);
			}
			if (columnSelection.size() > 0) {
				selectNone();
				columnSelection.set(Math.min(ci[0], columnSelection.size()-1), Boolean.TRUE);
			}
		}
		try {
			ArchitectUtils.unlistenToHierarchy(this, e.getChildren());
			if (columnSelection.size() != this.model.getColumns().size()) {
				logger.error("Selection list and children are out of sync: selection="+columnSelection+"; children="+model.getChildren());
			}
		} catch (ArchitectException ex) {
			logger.error("Couldn't remove children", ex);
			JOptionPane.showMessageDialog(this, "Couldn't delete column: "+ex.getMessage());
		}
		firePropertyChange("model.children", null, null);
		revalidate();
	}

	/**
	 * Listens for property changes in the model (columns
	 * properties modified).  If this change affects the appearance of
	 * this widget, we will notify all change listeners (the UI
	 * delegate) with a ChangeEvent.
	 */
	public void dbObjectChanged(SQLObjectEvent e) {
		firePropertyChange("model."+e.getPropertyName(), null, null);
		repaint();
	}

	/**
	 * Listens for property changes in the model (significant
	 * structure change).  If this change affects the appearance of
	 * this widget, we will notify all change listeners (the UI
	 * delegate) with a ChangeEvent.
	 */
	public void dbStructureChanged(SQLObjectEvent e) {
		if (e.getSource() == model.getColumnsFolder()) {
			int numCols = e.getChildren().length;
			columnSelection = new ArrayList(numCols);
			for (int i = 0; i < numCols; i++) {
				columnSelection.add(Boolean.FALSE);
			}
			firePropertyChange("model.children", null, null);
			revalidate();
		}
	}

	// ----------------------- accessors and mutators --------------------------
	
	/**
	 * Gets the value of model
	 *
	 * @return the value of model
	 */
	public SQLTable getModel()  {
		return this.model;
	}

	/**
	 * Sets the value of model, removing this TablePane as a listener
	 * on the old model and installing it as a listener to the new
	 * model.
	 *
	 * @param argModel Value to assign to this.model
	 */
	public void setModel(SQLTable m) {
		SQLTable old = model;
        if (old != null) {
			try {
				ArchitectUtils.listenToHierarchy(this, old);
			} catch (ArchitectException e) {
				logger.error("Caught exception while unlistening to old model", e);
			}
		}

        if (m == null) {
			throw new IllegalArgumentException("model may not be null");
		} else {
            model = m;
		}

		try {
			columnSelection = new ArrayList(m.getColumns().size());
			for (int i = 0; i < m.getColumns().size(); i++) {
				columnSelection.add(Boolean.FALSE);
			}
		} catch (ArchitectException e) {
			logger.error("Error getting children on new model", e);
		}

		try {
			ArchitectUtils.listenToHierarchy(this, model);
		} catch (ArchitectException e) {
			logger.error("Caught exception while listening to new model", e);
		}
		setName("TablePanel: "+model.getShortDisplayName());

        firePropertyChange("model", old, model);
	}

	/**
	 * Gets the value of margin
	 *
	 * @return the value of margin
	 */
	public Insets getMargin()  {
		return this.margin;
	}

	/**
	 * Sets the value of margin
	 *
	 * @param argMargin Value to assign to this.margin
	 */
	public void setMargin(Insets argMargin) {
		Insets old = margin;
		this.margin = (Insets) argMargin.clone();
		firePropertyChange("margin", old, margin);
		revalidate();
	}

	/**
	 * See {@link #insertionPoint}.
	 */
	public int getInsertionPoint() {
		return insertionPoint;
	}

	/**
	 * See {@link #insertionPoint}.
	 */
	public void setInsertionPoint(int ip) {
		int old = insertionPoint;
		this.insertionPoint = ip;
		if (ip != old) {
			firePropertyChange("insertionPoint", old, insertionPoint);
			repaint();
		}
	}
	
	/**
	 * See {@link #selected}.
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * See {@link #selected}.
	 */
	public void setSelected(boolean v) {
		if (v == false) {
			selectNone();
		}
		boolean old = selected;
		selected = v;
		if (v != old) {
			fireSelectionEvent(this);
			repaint();
		}
	}

	// --------------------- column selection support --------------------

	public void selectNone() {
		PlayPen pp = (PlayPen) getParent();
		for (int i = 0; i < columnSelection.size(); i++) {
			columnSelection.set(i, Boolean.FALSE);
		}
	}
	
	/**
	 * @param i The column to select.  If less than 0, {@link
	 * #selectNone()} is called rather than selecting a column.
	 */
	public void selectColumn(int i) {
		if (i < 0) {
			selectNone();
			return;
		}
		columnSelection.set(i, Boolean.TRUE);
		PlayPen pp = (PlayPen) getParent();
	}

	public boolean isColumnSelected(int i) {
		try {
			return ((Boolean) columnSelection.get(i)).booleanValue();
		} catch (IndexOutOfBoundsException ex) {
			logger.error("Couldn't determine selected status of col "+i+" on table "+model.getName());
			return false;
		}
	}

	/**
	 * Returns the index of the first selected column, or
	 * COLUMN_INDEX_NONE if there are no selected columns.
	 */
	public int getSelectedColumnIndex() {
		ListIterator it = columnSelection.listIterator();
		while (it.hasNext()) {
			if (((Boolean) it.next()).booleanValue() == true) {
				return it.previousIndex();
			}
		}
		return COLUMN_INDEX_NONE;
	}

	// --------------------- SELECTION EVENT SUPPORT ---------------------

	protected LinkedList selectionListeners = new LinkedList();

	public void addSelectionListener(SelectionListener l) {
		selectionListeners.add(l);
	}

	public void removeSelectionListener(SelectionListener l) {
		selectionListeners.remove(l);
	}
	
	protected void fireSelectionEvent(Selectable source) {
		SelectionEvent e = new SelectionEvent(source);
		logger.debug("Notifying "+selectionListeners.size()+" listeners of selection change");
		Iterator it = selectionListeners.iterator();
		while (it.hasNext()) {
			((SelectionListener) it.next()).itemSelected(e);
		}
	}

	// ------------------ utility methods ---------------------

	/**
	 * Returns the index of the column that point p is on top of.  If
	 * p is on top of the table name, returns COLUMN_INDEX_TITLE.
	 * Otherwise, p is not over a column or title and the returned
	 * index is COLUMN_INDEX_NONE.
	 */
	public int pointToColumnIndex(Point p) throws ArchitectException {
		return ((TablePaneUI) ui).pointToColumnIndex(p);
	}


	// ------------------------ DROP TARGET LISTENER ------------------------

	/**
	 * Tracks incoming objects and adds successfully dropped objects
	 * at the current mouse position.
	 */
	public static class TablePaneDropListener implements DropTargetListener {

		/**
		 * Called while a drag operation is ongoing, when the mouse
		 * pointer enters the operable part of the drop site for the
		 * DropTarget registered with this listener.
		 */
		public void dragEnter(DropTargetDragEvent dtde) {
			if (logger.isDebugEnabled()) {
				TablePane tp = (TablePane) dtde.getDropTargetContext().getComponent();
				logger.debug("DragEnter event on "+tp.getName());
			}
			dragOver(dtde);
		}
		
		/**
		 * Called while a drag operation is ongoing, when the mouse
		 * pointer has exited the operable part of the drop site for the
		 * DropTarget registered with this listener.
		 */
		public void dragExit(DropTargetEvent dte) {
			TablePane tp = (TablePane) dte.getDropTargetContext().getComponent();
			if (logger.isDebugEnabled()) {
				logger.debug("DragExit event on "+tp.getName());
			}
			tp.setInsertionPoint(COLUMN_INDEX_NONE);
		}
		
		/**
		 * Called when a drag operation is ongoing, while the mouse
		 * pointer is still over the operable part of the drop site for
		 * the DropTarget registered with this listener.
		 */
		public void dragOver(DropTargetDragEvent dtde) {
			TablePane tp = (TablePane) dtde.getDropTargetContext().getComponent();
			if (logger.isDebugEnabled()) {
				logger.debug("DragOver event on "+tp.getName()+": "+dtde);
				logger.debug("Drop Action = "+dtde.getDropAction());
				logger.debug("Source Actions = "+dtde.getSourceActions());
			}
			dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE & dtde.getDropAction());
			try {
				int idx = tp.pointToColumnIndex(dtde.getLocation());
				if (idx < 0) idx = 0;
				tp.setInsertionPoint(idx);
			} catch (ArchitectException e) {
				logger.error("Got exception translating drag location", e);
			}
		}
		
		/**
		 * Called when the drag operation has terminated with a drop on
		 * the operable part of the drop site for the DropTarget
		 * registered with this listener.
		 */
		public void drop(DropTargetDropEvent dtde) {
			TablePane tp = (TablePane) dtde.getDropTargetContext().getComponent();
			logger.debug("Drop target drop event on "+tp.getName()+": "+dtde);
			Transferable t = dtde.getTransferable();
			DataFlavor importFlavor = bestImportFlavor(tp, t.getTransferDataFlavors());
			if (importFlavor == null) {
				dtde.rejectDrop();
				tp.setInsertionPoint(COLUMN_INDEX_NONE);
			} else {
				try {
					DBTree dbtree = ArchitectFrame.getMainInstance().dbTree;  // XXX: bad
					int insertionPoint = tp.pointToColumnIndex(dtde.getLocation());
					if (insertionPoint < 0) insertionPoint = 0;
					ArrayList paths = (ArrayList) t.getTransferData(importFlavor);
					logger.debug("Importing items from tree: "+paths);
					Iterator pathIt = paths.iterator();
					while (pathIt.hasNext()) {
						Object someData = dbtree.getNodeForDnDPath((int[]) pathIt.next());
						logger.debug("drop: got object of type "+someData.getClass().getName());
						if (someData instanceof SQLTable) {
							SQLTable table = (SQLTable) someData;
							if (table.getParentDatabase() == tp.getModel().getParentDatabase()) {
								// can't import table from target into target!!
								dtde.rejectDrop();
							} else {
								dtde.acceptDrop(DnDConstants.ACTION_COPY);
								tp.getModel().inherit(insertionPoint, table);
								dtde.dropComplete(true);
							}
						} else if (someData instanceof SQLColumn) {
							SQLColumn col = (SQLColumn) someData;
							if (col.getParentTable().getParentDatabase()
								== tp.getModel().getParentDatabase()) {
								dtde.acceptDrop(DnDConstants.ACTION_MOVE);
								int removedIndex = col.getParent().getChildren().indexOf(col);
								if (tp.getModel() == col.getParentTable()) {
									// moving column inside the same table
									if (insertionPoint > removedIndex) {
										insertionPoint--;
									}
								}
								col.getParentTable().removeColumn(col);
								logger.debug("Adding column '"+col.getName()
											 +"' to table '"+tp.getModel().getName()
											 +"' at position "+insertionPoint);
								tp.getModel().addColumn(insertionPoint, col);
								dtde.dropComplete(true);
							} else {
								dtde.acceptDrop(DnDConstants.ACTION_COPY);
								tp.getModel().inherit(insertionPoint, col);
								logger.debug("Inherited "+col.getColumnName()+" to table");
								dtde.dropComplete(true);
							}
						} else {
							dtde.rejectDrop();
						}
					}
				} catch(Exception ex) {
					JOptionPane.showMessageDialog(tp, "Drop failed: "+ex.getMessage());
					logger.error("Error processing drop operation", ex);
					dtde.rejectDrop();
				} finally {
					tp.setInsertionPoint(COLUMN_INDEX_NONE);
				}
			}
		}
		
		/**
		 * Called if the user has modified the current drop gesture.
		 */
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

		/**
		 * Chooses the best import flavour from the flavors array for
		 * importing into c.  The current implementation actually just
		 * chooses the first acceptable flavour.
		 *
		 * @return The first acceptable DataFlavor in the flavors
		 * list, or null if no acceptable flavours are present.
		 */
		public DataFlavor bestImportFlavor(JComponent c, DataFlavor[] flavors) {
			logger.debug("can I import "+Arrays.asList(flavors));
 			for (int i = 0; i < flavors.length; i++) {
				String cls = flavors[i].getDefaultRepresentationClassAsString();
				logger.debug("representation class = "+cls);
				logger.debug("mime type = "+flavors[i].getMimeType());
				logger.debug("type = "+flavors[i].getPrimaryType());
				logger.debug("subtype = "+flavors[i].getSubType());
				logger.debug("class = "+flavors[i].getParameter("class"));
				logger.debug("isSerializedObject = "+flavors[i].isFlavorSerializedObjectType());
				logger.debug("isInputStream = "+flavors[i].isRepresentationClassInputStream());
				logger.debug("isRemoteObject = "+flavors[i].isFlavorRemoteObjectType());
				logger.debug("isLocalObject = "+flavors[i].getMimeType().equals(DataFlavor.javaJVMLocalObjectMimeType));


 				if (flavors[i].equals(DnDTreePathTransferable.flavor)) {
					logger.debug("YES");
 					return flavors[i];
				}
 			}
			logger.debug("NO!");
 			return null;
		}

		/**
		 * This is set up this way because this DropTargetListener was
		 * derived from a TransferHandler.  It works, so no sense in
		 * changing it.
		 */
		public boolean canImport(JComponent c, DataFlavor[] flavors) {
			return bestImportFlavor(c, flavors) != null;
		} 
	}

	public static class TablePaneDragGestureListener implements DragGestureListener {
		public void dragGestureRecognized(DragGestureEvent dge) {
			TablePane tp = (TablePane) dge.getComponent();
			int colIndex = COLUMN_INDEX_NONE;

			// ignore drag events that aren't from the left mouse button
			if (dge.getTriggerEvent() instanceof MouseEvent
			   && (dge.getTriggerEvent().getModifiers() & InputEvent.BUTTON1_MASK) == 0)
				return;
			
			try {
				colIndex = tp.pointToColumnIndex(dge.getDragOrigin());
			} catch (ArchitectException e) {
				logger.error("Got exception while translating drag point", e);
			}
			logger.debug("Recognized drag gesture! col="+colIndex);
			if (colIndex == COLUMN_INDEX_TITLE) {
				new PlayPen.FloatingTableListener((PlayPen) tp.getParent(), tp, dge.getDragOrigin());
			} else if (colIndex >= 0) {
				// export column as DnD event
				logger.debug("Exporting column with DnD");
				try {
					tp.draggingColumn = tp.model.getColumn(colIndex);
					DBTree tree = ArchitectFrame.getMainInstance().dbTree;
					int[] path = tree.getDnDPathToNode(tp.draggingColumn);
					if (logger.isDebugEnabled()) {
						StringBuffer array = new StringBuffer();
						for (int i = 0; i < path.length; i++) {
							array.append(path[i]);
							array.append(",");
						}
						logger.debug("Path to dragged node: "+array);
					}
					// export list of DnD-type tree paths
					ArrayList paths = new ArrayList(1);
					paths.add(path);
					logger.info("DBTree: exporting 1-item list of DnD-type tree path");
					dge.getDragSource().startDrag
						(dge, 
						 null, //DragSource.DefaultCopyNoDrop, 
						 new DnDTreePathTransferable(paths),
						 tp);
				} catch (ArchitectException ex) {
					logger.error("Couldn't drag column", ex);
					JOptionPane.showMessageDialog(tp, "Can't drag column: "+ex.getMessage());
				}
			}
		}
	}

	public static class PopupListener extends MouseAdapter {

		/**
		 * Double-click support.
		 */
		public void mouseClicked(MouseEvent evt) {
			if (evt.getClickCount() == 2) {
				TablePane tp = (TablePane) evt.getSource();
				if (tp.isSelected()) {
					ArchitectFrame af = ArchitectFrame.getMainInstance();
					int selectedColIndex = tp.getSelectedColumnIndex();
					if (selectedColIndex == COLUMN_INDEX_NONE) {
						af.editTableAction.actionPerformed
							(new ActionEvent(tp, ActionEvent.ACTION_PERFORMED, "DoubleClick"));
					} else if (selectedColIndex >= 0) {
						af.editColumnAction.actionPerformed
							(new ActionEvent(tp, ActionEvent.ACTION_PERFORMED, "DoubleClick"));
					}
				}
			}
		}

		public void mousePressed(MouseEvent evt) {
			evt.getComponent().requestFocus();

			// table/column selection
			if ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
				TablePane tp = (TablePane) evt.getComponent();
				PlayPen pp = (PlayPen) tp.getParent();
				try {
					pp.selectNone();
					tp.setSelected(true);
					tp.selectNone();
					tp.selectColumn(tp.pointToColumnIndex(evt.getPoint()));
				} catch (ArchitectException e) {
					logger.error("Exception converting point to column", e);
				}
			}

			maybeShowPopup(evt);
		}

		public void mouseReleased(MouseEvent evt) {
			maybeShowPopup(evt);
		}

		public void maybeShowPopup(MouseEvent evt) {
			if (evt.isPopupTrigger() && !evt.isConsumed()) {
				TablePane tp = (TablePane) evt.getComponent();
				PlayPen pp = (PlayPen) tp.getParent();
				pp.selectNone();
				tp.setSelected(true);
				try {
					tp.selectNone();
					int idx = tp.pointToColumnIndex(evt.getPoint());
					if (idx >= 0) {
						tp.selectColumn(idx);
					}
				} catch (ArchitectException e) {
					logger.error("Exception converting point to column", e);
					return;
				}
				pp.tablePanePopup.show(tp, evt.getX(), evt.getY());
			}
		}
	}
	
	// --------------------- Drag Source Listener ------------------------
	public void dragEnter(DragSourceDragEvent dsde) {
	}

	public void dragOver(DragSourceDragEvent dsde) {
	}

	public void dropActionChanged(DragSourceDragEvent dsde) {
	}
		
	public void dragExit(DragSourceEvent dse) {
	}

	public void dragDropEnd(DragSourceDropEvent dsde) {
		if (dsde.getDropSuccess()) {
			logger.debug("Succesful drop");
		} else {
			logger.debug("Unsuccesful drop");
		}
		draggingColumn = null;
	}
}