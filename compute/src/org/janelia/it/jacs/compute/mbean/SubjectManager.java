package org.janelia.it.jacs.compute.mbean;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.User;

public class SubjectManager implements SubjectManagerMBean {

    private static final Logger log = Logger.getLogger(SubjectManager.class);
    
    public void createUser(String username) {
        try {
            ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
            User user = computeBean.getUserByNameOrKey(username);
            // If we don't know them, and they authenticated, add to the database and create a location in the filestore
            if (null == user) {
                boolean successful = computeBean.createUser(username);
                if (!successful) {
                    // will not be able to execute any computes, so throw an exception
                	log.error("Unable to create directory and/or account for user " + username);
                }
                else {
                	log.debug("Created directory and/or account for user " + username);
                    user = computeBean.getUserByNameOrKey(username);
                	EJBFactory.getLocalAnnotationBean().createWorkspace(user.getKey());
                	log.debug("Created workspace for user " + username);
                }
            }
            else {
            	log.debug("User " + username+" already exists.");
            }
        }
        catch (Exception e) {
    		// Already printed by the EntityBeanLocal and AnnotationBeanLocal
        }
    }
    
    public void createGroup(String ownerNameOrKey, String groupNameOrKey) {
    	String ownerKey = getKeyForSubjectNameOrKey(ownerNameOrKey);
    	String groupKey = getKeyForSubjectNameOrKey(groupNameOrKey);
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
    		computeBean.createGroup(ownerKey, groupKey);
    	}
    	catch (DaoException e) {
    		// Already printed by the ComputeBean
    	}
    }
    
    public void removeGroup(String groupNameOrKey) {
    	String groupKey = getKeyForSubjectNameOrKey(groupNameOrKey);
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
    		computeBean.removeGroup(groupKey);
    	}
    	catch (DaoException e) {
    		// Already printed by the ComputeBean
    	}
    }
    
    public void addUserToGroup(String userNameOrKey, String groupNameOrKey) {
    	String userKey = getKeyForSubjectNameOrKey(userNameOrKey);
    	String groupKey = getKeyForSubjectNameOrKey(groupNameOrKey);
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	AnnotationBeanLocal annotateBean = EJBFactory.getLocalAnnotationBean();
    	try {
    		computeBean.addUserToGroup(userKey, groupKey);
    		annotateBean.addGroupWorkspaceToUserWorkspace(userKey, groupKey);
    	}
    	catch (ComputeException e) {
    		// Already printed by the ComputeBean
    	}
    }

    public void removeUserFromGroup(String userNameOrKey, String groupNameOrKey) {
    	String userKey = getKeyForSubjectNameOrKey(userNameOrKey);
    	String groupKey = getKeyForSubjectNameOrKey(groupNameOrKey);
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	AnnotationBeanLocal annotateBean = EJBFactory.getLocalAnnotationBean();
    	try {
    		computeBean.removeUserFromGroup(userKey, groupKey);
    		annotateBean.removeGroupWorkspaceFromUserWorkspace(userKey, groupKey);
    	}
    	catch (ComputeException e) {
    		// Already printed by the ComputeBean
    	}
    }

    public void recreateWorkspace(String subjectNameOrKey) {
    	String subjectKey = getKeyForSubjectNameOrKey(subjectNameOrKey);
    	EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
    	AnnotationBeanLocal annotateBean = EJBFactory.getLocalAnnotationBean();
    	try {
    		Entity workspace = entityBean.getDefaultWorkspace(subjectKey);
    		entityBean.deleteEntityTreeById(subjectKey, workspace.getId());
    		annotateBean.createWorkspace(subjectKey);
    	}
    	catch (Exception e) {
    		// Already printed by the EntityBeanLocal and AnnotationBeanLocal
    	}
    }

    private String getKeyForSubjectNameOrKey(String nameOrKey) {
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
            Subject subject = computeBean.getSubjectByNameOrKey(nameOrKey);
            if (subject!=null) {
            	return subject.getKey();
            }
    	}
    	catch (Exception e) {
    		// Already printed by the EntityBeanLocal and AnnotationBeanLocal
    	}
    	return null;
    }
}