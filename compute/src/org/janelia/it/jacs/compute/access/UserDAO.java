
package org.janelia.it.jacs.compute.access;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: sreenath
 * Date: Sep 3, 2009
 * Time: 11:59:51 AM
 */
public class UserDAO extends ComputeBaseDAO {

    public UserDAO(Logger logger) {
        super(logger);
    }

    /**
     * Method used to add new users to the system
     * @param newUserName - login of the user in the system
     * @return a formatted user object, or null if there was a problem
     * @throws DaoException thrown if there was a problem adding the user to the database
     */
    public User createUser(String newUserName) throws DaoException {
        User tmpUser = getUserByNameOrKey(newUserName);
        if (null!=tmpUser) {
            _logger.warn("Cannot create user "+newUserName+" as they already exist!");
            return tmpUser;
        }
        else {
            tmpUser = new User();
            tmpUser.setName(newUserName);
            saveOrUpdate(tmpUser);
            return tmpUser;
        }
    }

    public Group getGroupByNameOrKey(String name) {
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Group.class);
        c.add(Expression.eq("name", name));
        List l = c.list();
        if (l.size() == 0) return null;
        return (Group) l.get(0);
    }

    public Group createGroup(String groupOwner, String groupName) throws DaoException {
        User tmpUser = getUserByNameOrKey(groupOwner);
        if (tmpUser==null) {
        	throw new DaoException("Group owner does not exist: "+groupOwner);
        }
        Group tmpGroup = getGroupByNameOrKey(groupName);
        if (null!=tmpGroup) {
            _logger.warn("Cannot create group as it already exists: "+groupName);
            return tmpGroup;
        }
        else {
        	tmpGroup = new Group();
        	tmpGroup.setName(groupName);

        	SubjectRelationship relation = new SubjectRelationship();
        	relation.setGroup(tmpGroup);
        	relation.setUser(tmpUser);
        	relation.setRelationshipType(SubjectRelationship.TYPE_GROUP_OWNER);
        	tmpGroup.getUserRelationships().add(relation);
        	
            saveOrUpdate(tmpGroup);
            return tmpGroup;
        }
    }
        
    public void removeGroup(String groupName) throws DaoException {
    	Group tmpGroup = getGroupByNameOrKey(groupName);
        if (null==tmpGroup) {
            throw new DaoException("Cannot delete group which does not exist: "+groupName);
        }
        
        genericDelete(tmpGroup);
    }

    /**
     * Defines the relationship between a user and group. If the user has any existing relationship with the group, it is
     * deleted first. 
     * @param userNameOrKey
     * @param groupNameOrKey
     * @param relationshipType
     * @return
     */
    public SubjectRelationship setRelationship(String userNameOrKey, String groupNameOrKey, String relationshipType) throws DaoException {

        if (!SubjectRelationship.TYPE_GROUP_ADMIN.equals(relationshipType) 
                && !SubjectRelationship.TYPE_GROUP_OWNER.equals(relationshipType) 
                && !SubjectRelationship.TYPE_GROUP_MEMBER.equals(relationshipType)) {
            throw new DaoException("Illegal relationship type: "+relationshipType);
        }
                
        Group tmpGroup = getGroupByNameOrKey(groupNameOrKey);
        if (tmpGroup==null) {
            throw new DaoException("Cannot add user to non-existent group: "+groupNameOrKey);
        }
        User user = getUserByNameOrKey(userNameOrKey);
        if (user==null) {
            throw new DaoException("Cannot add non-existent user to group: "+userNameOrKey);
        }
        
        SubjectRelationship relation = null;
        for(SubjectRelationship sr : tmpGroup.getUserRelationships()) {
            if (sr.getUser().getKey().equals(userNameOrKey)) {
                // Update existing relationship
                relation = sr;
                relation.setRelationshipType(relationshipType);
                saveOrUpdate(relation);
            }
        }
        
        if (relation==null) {
            // Create new relationship
            relation = new SubjectRelationship();
            relation.setGroup(tmpGroup);
            relation.setUser(user);
            relation.setRelationshipType(relationshipType);
            tmpGroup.getUserRelationships().add(relation);
            saveOrUpdate(tmpGroup);
        }
        return relation;
    }
    
    public void addUserToGroup(String userNameOrKey, String groupNameOrKey) throws DaoException {
        setRelationship(userNameOrKey, groupNameOrKey, SubjectRelationship.TYPE_GROUP_MEMBER);
    }
    
    public void removeUserFromGroup(String groupUser, String groupName) throws DaoException {
        Group tmpGroup = getGroupByNameOrKey(groupName);
        if (tmpGroup==null) {
            throw new DaoException("Cannot remove user from non-existent group: "+groupName);
        }
        
    	SubjectRelationship toDelete = null;
    	for(SubjectRelationship relation : tmpGroup.getUserRelationships()) {
    		if (relation.getUser().getKey().equals(groupUser) || relation.getUser().getName().equals(groupUser)) {
    			toDelete = relation;
    		}
    	}
    	if (toDelete==null) {
    		throw new DaoException("User "+groupUser+" does not belong to group "+groupName);
    	}
    	
    	tmpGroup.getUserRelationships().remove(toDelete);
    	saveOrUpdate(tmpGroup);
    }
}
