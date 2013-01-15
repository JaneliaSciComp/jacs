package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.fly.ScreenScoresLoadingService;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {

	private static final Logger logger = Logger.getLogger(UpgradeUserDataService.class);

	private static final String[] adminMembers = { "saffordt", "rokickik", "brunsc", "fosterl", "murphys", "trautmane", "yuy" };
	
	private static final String[] leetlabMembers = { "saffordt", "rokickik", "yuy", "fosterl", "chenh12", "huangy11", "jingx", "leetz", "leey10", "liuz10", "luanh", "renq", "schroederm", "wangy14" };
	
    private static final String[] cardlabMembers = { "saffordt", "wum10" };
    
    private static final String[] simpsonlabMembers = { "saffordt", "midgleyf", "weaverc10" };
    
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model to latest version: "+serverVersion);

        List<String> allUsers = new ArrayList<String>();
        for(User user : computeBean.getUsers()) {   
            if (!user.getName().equals(User.SYSTEM_USER_LOGIN)) {
                allUsers.add(user.getName());
            }
        }
 
        // ------------------------------------------------------------------------------------------------------------
        // Create users and groups
        // ------------------------------------------------------------------------------------------------------------

        computeBean.createUser(User.SYSTEM_USER_LOGIN, "System");
        computeBean.createUser("chenh12", "Hui-Min Chen");
        computeBean.createUser("huangy11", "Yu-Fen Huang");
        computeBean.createUser("jingx", "Xiaotang Jing");
        computeBean.createUser("liuz10", "Zhiyong Liu");
        computeBean.createUser("luanh", "Haojiang Luan");
        
        Group flylightGroup = getOrCreateGroup("flylight", "FlyLight", User.SYSTEM_USER_KEY);
        Group adminGroup = getOrCreateGroup(Group.ADMIN_GROUP_NAME, "Administrators", User.SYSTEM_USER_KEY);
        
        
        // ------------------------------------------------------------------------------------------------------------
        //  Add Group Members
        // ------------------------------------------------------------------------------------------------------------
        
        addGroupMembers(flylightGroup, allUsers.toArray(new String[0]));
        addGroupMembers(adminGroup, adminMembers);
        addGroupMembers("leetlab", leetlabMembers);
        addGroupMembers("cardlab", cardlabMembers);
        addGroupMembers("simpsonlab", simpsonlabMembers);
        
        // ------------------------------------------------------------------------------------------------------------
        // Add Protected tags
        // ------------------------------------------------------------------------------------------------------------
        
        for(String username : allUsers) {	
            String subjectKey = "user:"+username;
            logger.info("Processing common roots for "+username); 
        	for(Entity commonRoot : annotationBean.getCommonRootEntities(subjectKey)) {
        	    if (!EntityUtils.isOwner(commonRoot, subjectKey)) continue;
        		if (EntityConstants.NAME_MY_DATA_SETS.endsWith(commonRoot.getName()) 
        				|| EntityConstants.NAME_PUBLIC_DATA_SETS.endsWith(commonRoot.getName()) 
        				|| EntityConstants.NAME_SPLIT_PICKING.endsWith(commonRoot.getName())
        				|| ScreenScoresLoadingService.TOP_LEVEL_EVALUATION_FOLDER.endsWith(commonRoot.getName())
        				|| "FlyLight Screen Split Lines".endsWith(commonRoot.getName())
        				|| "Arnim Data Combinations".endsWith(commonRoot.getName())) {
        		    logger.info("  Ensuring protection for "+commonRoot.getName()+" (id="+commonRoot.getId()+")"); 
        		    EntityUtils.addAttributeAsTag(commonRoot, EntityConstants.ATTRIBUTE_IS_PROTECTED);
            		entityBean.saveOrUpdateEntity(commonRoot);
        		}
        	}
        }
    }
    
    private Group getOrCreateGroup(String groupName, String groupFullName, String ownerKey) throws Exception {
        Group group = computeBean.getGroupByNameOrKey(groupName);
        if (group==null) {
            group = computeBean.createGroup(ownerKey, groupName);
        }
        group.getUserRelationships().clear();
        group.setFullName(groupFullName);
        computeBean.saveOrUpdateGroup(group);
        return group;
    }
    
    private void addGroupMembers(String groupName, String[] members) throws Exception {
        Group group = computeBean.getGroupByNameOrKey(groupName);
        if (group==null) {
            throw new IllegalStateException("Group does not exist: "+groupName);
        }
        group.getUserRelationships().clear();
        computeBean.saveOrUpdateGroup(group);
        addGroupMembers(group, members);
    }
    
    private void addGroupMembers(Group group, String[] members) throws Exception {
        for(String username : members) {
            computeBean.addUserToGroup(username, group.getName());
        }
    }
}
