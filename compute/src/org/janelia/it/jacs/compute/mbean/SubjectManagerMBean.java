package org.janelia.it.jacs.compute.mbean;

/**
 * MBean for subject (user and group) management. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SubjectManagerMBean {

    public void createUser(String username);
    public void createGroup(String ownerNameOrKey, String groupName);
    public void removeGroup(String groupNameOrKey);
    public void addUserToGroup(String userNameOrKey, String groupNameOrKey);
    public void removeUserFromGroup(String userNameOrKey, String groupNameOrKey);
    public void recreateWorkspace(String subjectNameOrKey);
    
}