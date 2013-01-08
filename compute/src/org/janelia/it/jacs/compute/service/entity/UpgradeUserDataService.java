package org.janelia.it.jacs.compute.service.entity;

import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.fly.ScreenScoresLoadingService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(UpgradeUserDataService.class);

	private static final String[] admins = { "saffordt", "rokickik", "brunsc", "fosterl", "murphys", "trautmane", "yuy" };
	
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model to latest version: "+serverVersion);

        // ------------------------------------------------------------------------------------------------------------
        // Create System user
        // ------------------------------------------------------------------------------------------------------------
        
        computeBean.createUser(User.SYSTEM_USER_LOGIN, "System");
        
        // ------------------------------------------------------------------------------------------------------------
        // Create FlyLight group
        // ------------------------------------------------------------------------------------------------------------
        
        Group flylightGroup = computeBean.getGroupByNameOrKey("group:flylight");
        flylightGroup.getUserRelationships().clear();
        computeBean.saveOrUpdateGroup(flylightGroup);

        List<User> allUsers = computeBean.getUsers();
        for(User user : allUsers) {	
        	SubjectRelationship sr = new SubjectRelationship(user, flylightGroup, 
        			user.getName().equals(User.SYSTEM_USER_LOGIN) ? 
        					SubjectRelationship.TYPE_GROUP_OWNER : 
        					SubjectRelationship.TYPE_GROUP_MEMBER);
            logger.info("Adding "+user.getName()+" to group "+flylightGroup.getName()+" as "+sr.getRelationshipType());
        	flylightGroup.getUserRelationships().add(sr);
        }
        computeBean.saveOrUpdateGroup(flylightGroup);

        // ------------------------------------------------------------------------------------------------------------
        //  Create Administrator Group
        // ------------------------------------------------------------------------------------------------------------
        
        Group adminGroup = computeBean.getGroupByNameOrKey(Group.ADMIN_GROUP_KEY);
        
        if (adminGroup!=null) {
            adminGroup.getUserRelationships().clear();
            adminGroup.setFullName("Administrators");
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
        
        // ------------------------------------------------------------------------------------------------------------
        // Add Protected tags
        // ------------------------------------------------------------------------------------------------------------
        
        for(User user : allUsers) {	
        	for(Entity commonRoot : annotationBean.getCommonRootEntities(user.getKey())) {
        		if (EntityConstants.NAME_MY_DATA_SETS.endsWith(commonRoot.getName()) 
        				|| EntityConstants.NAME_PUBLIC_DATA_SETS.endsWith(commonRoot.getName()) 
        				|| EntityConstants.NAME_SPLIT_PICKING.endsWith(commonRoot.getName())
        				|| ScreenScoresLoadingService.TOP_LEVEL_EVALUATION_FOLDER.endsWith(commonRoot.getName())
        				|| "FlyLight Screen Split Lines".endsWith(commonRoot.getName())
        				|| "Arnim Data Combinations".endsWith(commonRoot.getName())) {
        			EntityUtils.addAttributeAsTag(commonRoot, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            		entityBean.saveOrUpdateEntity(commonRoot);
        		}
        	}
        }
        
        
    }
}
