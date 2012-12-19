
package org.janelia.it.jacs.compute.access;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;

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

    public String getEmailByUserName(String userName) {
        String sql = "select email from user_accounts where user_login = '" + userName + "'";
        Query query = getCurrentSession().createSQLQuery(sql);
        List<String> returnList = query.list();

        if (null == returnList || returnList.size() <= 0) {
            _logger.debug("No email found for user " + userName + " - SQL: '" + sql + "'");
            return null; // empty list
        }

        return returnList.get(0);
    }

    /**
     * Method used to add new users to the system
     * @param userLogin - login of the user in the system
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

    public Group addUserToGroup(String groupUser, String groupName) throws DaoException {
        Group tmpGroup = getGroupByNameOrKey(groupName);
        if (tmpGroup==null) {
            throw new DaoException("Cannot add user to non-existent group: "+groupName);
        }
    	User user = getUserByNameOrKey(groupUser);
    	if (user==null) {
            throw new DaoException("Cannot add non-existent user to group: "+groupUser);
    	}
    	
    	SubjectRelationship relation = new SubjectRelationship();
    	relation.setGroup(tmpGroup);
    	relation.setUser(user);
    	relation.setRelationshipType(SubjectRelationship.TYPE_GROUP_MEMBER);
    	tmpGroup.getUserRelationships().add(relation);
    	
        saveOrUpdate(tmpGroup);
        return tmpGroup;
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
