/*
 * Copyright (c) 2010, SQL Power Group Inc.
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

package ca.sqlpower.architect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ca.sqlpower.architect.etl.kettle.KettleSettings;
import ca.sqlpower.architect.olap.OLAPRootObject;
import ca.sqlpower.architect.profile.ProfileManager;
import ca.sqlpower.object.AbstractSPObject;
import ca.sqlpower.object.ObjectDependentException;
import ca.sqlpower.object.SPObject;
import ca.sqlpower.object.annotation.Accessor;
import ca.sqlpower.object.annotation.Constructor;
import ca.sqlpower.object.annotation.ConstructorParameter;
import ca.sqlpower.object.annotation.Mutator;
import ca.sqlpower.object.annotation.NonBound;
import ca.sqlpower.object.annotation.NonProperty;
import ca.sqlpower.object.annotation.Transient;
import ca.sqlpower.object.annotation.ConstructorParameter.ParameterType;
import ca.sqlpower.sql.JDBCDataSource;
import ca.sqlpower.sqlobject.SQLDatabase;
import ca.sqlpower.sqlobject.SQLObject;
import ca.sqlpower.sqlobject.SQLObjectException;
import ca.sqlpower.sqlobject.SQLObjectRoot;
import ca.sqlpower.util.SessionNotFoundException;

/**
 * 
 * This class is the root object of an ArchitectSession. There is an ArchitectProject
 * for every ArchitectSession. The ArchitectProject, and all its children, will be
 * listened to and persisted to the JCR. This includes the SQL object tree,
 * the profile manager.
 *
 */

public class ArchitectProject extends AbstractSPObject {
    
    /**
     * Defines an absolute ordering of the child types of this class.
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends SPObject>> allowedChildTypes = 
        Collections.unmodifiableList(new ArrayList<Class<? extends SPObject>>(
                Arrays.asList(SQLObjectRoot.class, ProfileManager.class, OLAPRootObject.class, KettleSettings.class)));
    
    /**
     * There is a 1:1 ratio between the session and the project.
     */
    private ArchitectSession session;
    private final SQLObjectRoot rootObject;
    private ProfileManager profileManager; 
    
    /**
     * This OLAP object contains the OLAP session.
     */
    private final OLAPRootObject olapRootObject;
    
    /**
     * The current integrity watcher on the project.
     */
    private SourceObjectIntegrityWatcher currentWatcher;
    
    private final KettleSettings kettleSettings;
    
    /**
     * Constructs an architect project. The init method must be called immediately
     * after creating a project.
     * @throws SQLObjectException
     */
    public ArchitectProject() throws SQLObjectException {
        this(new SQLObjectRoot(), new OLAPRootObject(), new KettleSettings());
        SQLDatabase targetDatabase = new SQLDatabase();
        targetDatabase.setPlayPenDatabase(true);
        rootObject.addChild(targetDatabase, 0);
    }

    /**
     * The init method for this project must be called immediately after this
     * object is constructed.
     * 
     * @param rootObject
     *            The root object that holds all of the source databases for the
     *            current project.
     */
    @Constructor
    public ArchitectProject(
            @ConstructorParameter(isProperty=ParameterType.CHILD, propertyName="rootObject") SQLObjectRoot rootObject,
            @ConstructorParameter(isProperty=ParameterType.CHILD, propertyName="olapRootObject") OLAPRootObject olapRootObject,
            @ConstructorParameter(isProperty=ParameterType.CHILD, propertyName="kettleSettings") KettleSettings kettleSettings) 
            throws SQLObjectException {
        this.rootObject = rootObject;
        rootObject.setParent(this);
        this.olapRootObject = olapRootObject;
        olapRootObject.setParent(this);
        this.kettleSettings = kettleSettings;
        kettleSettings.setParent(this);
        setName("Architect Project");
    }

    /**
     * Call this to initialize the session and the children of the project.
     */
    @Transient @Mutator
    public void setSession(ArchitectSession session) {
        if (this.session != null) {
            rootObject.removeSQLObjectPreEventListener(currentWatcher);
            currentWatcher = null;
        }
        this.session = session;
        if (this.session != null) {
            currentWatcher = new SourceObjectIntegrityWatcher(session);
            rootObject.addSQLObjectPreEventListener(currentWatcher);
        }
    }
    
    /**
     * Returns the top level object in the SQLObject hierarchy.
     * It has no parent and its children are SQLDatabase's.
     */
    @NonProperty
    public SQLObjectRoot getRootObject() {
        return rootObject;
    }
    
    @NonProperty
    public ProfileManager getProfileManager() {
        return profileManager;
    }
    
    @NonProperty
    public SQLDatabase getDatabase(JDBCDataSource ds) {
        try {
            for (SQLDatabase obj : getRootObject().getChildren(SQLDatabase.class)) {
                if (obj.getDataSource().equals(ds)) {
                    return (SQLDatabase) obj;
                }
            }
            SQLDatabase db = new SQLDatabase(ds);
            getRootObject().addChild(db);
            return db;
        } catch (SQLObjectException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Transient @Accessor
    public SQLDatabase getTargetDatabase() {
        for (SQLDatabase db : rootObject.getChildren(SQLDatabase.class)) {
            if (db.isPlayPenDatabase()) {
                return db;
            }
        }
        throw new IllegalStateException("No target database!");
    }    
    
    @NonProperty
    public void setSourceDatabaseList(List<SQLDatabase> databases) throws SQLObjectException {
        SQLObject root = getRootObject();
        SQLDatabase targetDB = getTargetDatabase();
        try {
            root.begin("Setting source database list");
            for (int i = root.getChildCount()-1; i >= 0; i--) {
                root.removeChild(root.getChild(i));
            }
            if (targetDB != null) {
                root.addChild(targetDB);
            }
            for (SQLDatabase db : databases) {
                root.addChild(db);
            }
            root.commit();
        } catch (IllegalArgumentException e) {
            root.rollback("Could not remove child: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (ObjectDependentException e) {
            root.rollback("Could not remove child: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    @NonProperty
    public void setProfileManager(ProfileManager manager) {
        ProfileManager oldManager = profileManager;
        profileManager = manager;
        if (oldManager != null) {
            fireChildRemoved(ProfileManager.class, oldManager, 0);
        }
        fireChildAdded(ProfileManager.class, manager, 0);
        profileManager.setParent(this);
    }

    @Override
    protected boolean removeChildImpl(SPObject child) {
        return false;
    }        
    
    @Transient @Accessor
    public ArchitectSession getSession() throws SessionNotFoundException {
        if (session != null) {
            return session;
        } else {
            throw new SessionNotFoundException("The project has not been given a session yet. " +
            		"This should only happen during the construction of the project.");
        }
    }

    public boolean allowsChildren() {
        return true;
    }
    
    public int childPositionOffset(Class<? extends SPObject> childType) {
        
        if (SQLObjectRoot.class.isAssignableFrom(childType)) {
            return 0;
        } else if (ProfileManager.class.isAssignableFrom(childType)) {
            return 1;
        } else if (OLAPRootObject.class.isAssignableFrom(childType)) {
            return 2;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Transient @Accessor
    public List<Class<? extends SPObject>> getAllowedChildTypes() {
        return allowedChildTypes;
    }

    @NonProperty
    public List<SPObject> getChildren() {
        List<SPObject> allChildren = new ArrayList<SPObject>();
        allChildren.add(rootObject);
        if (profileManager != null) {
            allChildren.add(profileManager);
        }
        allChildren.add(olapRootObject);
        allChildren.add(kettleSettings);
        return allChildren;
    }
    
    @NonBound
    public List<? extends SPObject> getDependencies() {
        return Collections.emptyList();
    }

    public void removeDependency(SPObject dependency) {
        rootObject.removeDependency(dependency);
        profileManager.removeDependency(dependency);
        olapRootObject.removeDependency(dependency);
        kettleSettings.removeDependency(dependency);
    }
    
    protected void addChildImpl(SPObject child, int index) {
        if (child instanceof ProfileManager) {
            setProfileManager((ProfileManager) child);
        } else {
            throw new IllegalArgumentException("Cannot add child of type " + 
                    child.getClass() + " to the project once it has been created.");
        }
    }

    @NonProperty
    public OLAPRootObject getOlapRootObject() {
        return olapRootObject;
    }
    
    @NonProperty
    public KettleSettings getKettleSettings() {
        return kettleSettings;
    }
}