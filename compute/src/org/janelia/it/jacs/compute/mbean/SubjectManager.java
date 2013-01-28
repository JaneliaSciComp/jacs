package org.janelia.it.jacs.compute.mbean;

import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;

public class SubjectManager implements SubjectManagerMBean {
    
    public void createGroup(String groupOwner, String groupName) {
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
    		computeBean.createGroup(groupOwner, groupName);
    	}
    	catch (DaoException e) {
    		// Already printed by the ComputeBean
    	}
    }
    
    public void removeGroup(String groupName) {
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
    		computeBean.removeGroup(groupName);
    	}
    	catch (DaoException e) {
    		// Already printed by the ComputeBean
    	}
    }
    
    public void addUserToGroup(String groupUser, String groupName) {
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
    		computeBean.addUserToGroup(groupUser, groupName);
    	}
    	catch (DaoException e) {
    		// Already printed by the ComputeBean
    	}
    }

    public void removeUserFromGroup(String groupUser, String groupName) {
    	ComputeBeanLocal computeBean = EJBFactory.getLocalComputeBean();
    	try {
    		computeBean.removeUserFromGroup(groupUser, groupName);
    	}
    	catch (DaoException e) {
    		// Already printed by the ComputeBean
    	}
    }
    
}