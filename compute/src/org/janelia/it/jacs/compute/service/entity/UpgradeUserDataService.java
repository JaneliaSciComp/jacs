package org.janelia.it.jacs.compute.service.entity;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(UpgradeUserDataService.class);

	private static final String[] admins = { User.SYSTEM_USER_LOGIN, "saffordt", "rokickik" };
	
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model to latest version: "+serverVersion);

        // Create System user
        computeBean.createUser(User.SYSTEM_USER_LOGIN);
        
        // Create FlyLight group
        Group flylightGroup = computeBean.getGroupByNameOrKey("group:flylight");
        flylightGroup.getUserRelationships().clear();
        computeBean.saveOrUpdateGroup(flylightGroup);

        for(User user : computeBean.getUsers()) {	
        	SubjectRelationship sr = new SubjectRelationship(user, flylightGroup, 
        			user.getName().equals(User.SYSTEM_USER_LOGIN) ? 
        					SubjectRelationship.TYPE_GROUP_OWNER : 
        					SubjectRelationship.TYPE_GROUP_MEMBER);
            logger.info("Adding "+user.getName()+" to group "+flylightGroup.getName()+" as "+sr.getRelationshipType());
        	flylightGroup.getUserRelationships().add(sr);
        }
        computeBean.saveOrUpdateGroup(flylightGroup);

        //  Create Administrator Group
        Group adminGroup = computeBean.getGroupByNameOrKey(Group.ADMIN_GROUP_KEY);
        
        if (adminGroup!=null) {
            adminGroup.getUserRelationships().clear();
            computeBean.saveOrUpdateGroup(adminGroup);
        }
        else {
        	adminGroup = computeBean.createGroup(User.SYSTEM_USER_KEY, Group.ADMIN_GROUP_NAME);
        	adminGroup.setFullName("Administrators");
        }
        
        for(String username : admins) {
            User admin = computeBean.getUserByNameOrKey(username);
            if (admin==null) {
            	logger.error("Could not find user: "+username);
            }
            else {
	        	SubjectRelationship sr = new SubjectRelationship(admin, adminGroup, SubjectRelationship.TYPE_GROUP_MEMBER);
	            logger.info("Adding "+username+" to group "+adminGroup.getName()+" as "+sr.getRelationshipType());
	            adminGroup.getUserRelationships().add(sr);
            }
        }

        computeBean.saveOrUpdateGroup(adminGroup);
    }
}
