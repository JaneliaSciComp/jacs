package org.janelia.it.jacs.compute.mbean;

/**
 * MBean for subject (user and group) management. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface SubjectManagerMBean {
       
    public void createGroup(String groupOwner, String groupName);
    public void removeGroup(String groupName);
    public void addUserToGroup(String groupUser, String groupName);
    public void removeUserFromGroup(String groupUser, String groupName);
    
}