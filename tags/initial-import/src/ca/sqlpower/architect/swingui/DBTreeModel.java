package ca.sqlpower.architect.swingui;

import javax.swing.tree.*;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import java.util.LinkedList;
import java.util.Iterator;

import ca.sqlpower.architect.*;

public class DBTreeModel implements TreeModel, SQLObjectListener {

	protected SQLObject root;
	protected boolean debugMode;

	public DBTreeModel(SQLObject root) throws ArchitectException {
		this.root = root;
		this.treeModelListeners = new LinkedList();
		listenToSQLObjectAndChildren(root);
		debugMode = false;
	}

	public Object getRoot() {
		if (debugMode) System.out.println("DBTreeModel.getRoot: returning "+root);
		return root;
	}

	public Object getChild(Object parent, int index) {
		try {
			if (debugMode) System.out.println("DBTreeModel.getChild("+parent+","+index+"): returning "+((SQLObject) parent).getChild(index));
			return ((SQLObject) parent).getChild(index);
		} catch (ArchitectException e) {
			e.printStackTrace();
			return null;
		}
	}

	public int getChildCount(Object parent) {
		try {
			if (debugMode) System.out.println("DBTreeModel.getChildCount("+parent+"): returning "+((SQLObject) parent).getChildCount());
			return ((SQLObject) parent).getChildCount();
		} catch (ArchitectException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public boolean isLeaf(Object parent) {
		if (debugMode) System.out.println("DBTreeModel.isLeaf("+parent+"): returning "+!((SQLObject) parent).allowsChildren());
		return !((SQLObject) parent).allowsChildren();
	}

	public void valueForPathChanged(TreePath path, Object newValue) {
		throw new UnsupportedOperationException("model doesn't support editting yet");
	}

	public int getIndexOfChild(Object parent, Object child) {
		try {
			if (debugMode) System.out.println("DBTreeModel.getIndexOfChild("+parent+","+child+"): returning "+((SQLObject) parent).getChildren().indexOf(child));
			return ((SQLObject) parent).getChildren().indexOf(child);
		} catch (ArchitectException e) {
			e.printStackTrace();
			return -1;
		}
	}

	// -------------- treeModel event source support -----------------
	protected LinkedList treeModelListeners;

	public void addTreeModelListener(TreeModelListener l) {
		treeModelListeners.add(l);
	}

	public void removeTreeModelListener(TreeModelListener l) {
		treeModelListeners.remove(l);
	}

	protected void fireTreeNodesInserted(TreeModelEvent e) {
		System.out.println("Firing treeNodesInserted event: "+e);
		Iterator it = treeModelListeners.iterator();
		while (it.hasNext()) {
			((TreeModelListener) it.next()).treeNodesInserted(e);
		}
	}
	
	protected void fireTreeNodesRemoved(TreeModelEvent e) {
		Iterator it = treeModelListeners.iterator();
		while (it.hasNext()) {
			((TreeModelListener) it.next()).treeNodesRemoved(e);
		}
	}

	public static SQLObject[] getPathToNode(SQLObject node) {
		LinkedList path = new LinkedList();
		while (node != null) {
			path.add(0, node);
			node = node.getParent();
		}
		return (SQLObject[]) path.toArray(new SQLObject[path.size()]);
	}

	// --------------------- SQLObject listener support -----------------------
	public void dbChildrenInserted(SQLObjectEvent e) {
		try {
			SQLObject[] newEventSources = e.getChildren();
			for (int i = 0; i < newEventSources.length; i++) {
				listenToSQLObjectAndChildren(newEventSources[i]);
			}
		} catch (ArchitectException ex) {
			ex.printStackTrace();
		}
		TreeModelEvent tme = new TreeModelEvent(this,
												getPathToNode(e.getSQLSource()),
												e.getChangedIndices(),
												e.getChildren());
		fireTreeNodesInserted(tme);
	}

	public void dbChildrenRemoved(SQLObjectEvent e) {
		try {
			SQLObject[] oldEventSources = e.getChildren();
			for (int i = 0; i < oldEventSources.length; i++) {
				unlistenToSQLObjectAndChildren(oldEventSources[i]);
			}
		} catch (ArchitectException ex) {
			ex.printStackTrace();
		}
		TreeModelEvent tme = new TreeModelEvent(this,
												getPathToNode(e.getSQLSource()),
												e.getChangedIndices(),
												e.getChildren());
		fireTreeNodesRemoved(tme);
	}
	
	public void dbObjectChanged(SQLObjectEvent e) { 
		throw new UnsupportedOperationException("not yet");
	}

	public void dbStructureChanged(SQLObjectEvent e) {
		throw new UnsupportedOperationException("not yet");
	}

	protected void listenToSQLObjectAndChildren(SQLObject o) throws ArchitectException {
		o.addSQLObjectListener(this);
		if (o.isPopulated()) {
			Iterator it = o.getChildren().iterator();
			while (it.hasNext()) {
				listenToSQLObjectAndChildren((SQLObject) it.next());
			}
		}
	}

	protected void unlistenToSQLObjectAndChildren(SQLObject o) throws ArchitectException {
		o.removeSQLObjectListener(this);
		if (o.isPopulated()) {
			Iterator it = o.getChildren().iterator();
			while (it.hasNext()) {
				SQLObject ob = (SQLObject) it.next();
				unlistenToSQLObjectAndChildren(ob);
			}
		}
	}
}