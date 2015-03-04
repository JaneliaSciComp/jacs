
package org.janelia.it.jacs.compute.access;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Methods for dealing with subjects such as users and groups.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubjectDAO extends ComputeBaseDAO {

    public SubjectDAO(Logger logger) {
        super(logger);
    }

    public List<Subject> getSubjects() {
        if (log.isTraceEnabled()) {
            log.trace("getSubjects()");    
        }
        
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Subject.class);
        return c.list();
    }

    public List<User> getUsers() {
        if (log.isTraceEnabled()) {
            log.trace("getUsers()");    
        }
        
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(User.class);
        return c.list();
    }

    public List<Group> getGroups() {
        if (log.isTraceEnabled()) {
            log.trace("getGroups()");    
        }
        
        Session session = getCurrentSession();
        Criteria c = session.createCriteria(Group.class);
        return c.list();
    }
    
    public List<User> getAllUsers() throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("getAllUsers()");    
        }
        try {
            Session session = getCurrentSession();
            StringBuffer hql = new StringBuffer("select u from User u order by u.id");
            Query query = session.createQuery(hql.toString());
            return query.list();
        } 
        catch (Exception e) {
            throw new DaoException(e);
        }
    }
    
    /**
     * Method used to add new users to the system
     * @param newUserName - login of the user in the system
     * @return a formatted user object, or null if there was a problem
     * @throws DaoException thrown if there was a problem adding the user to the database
     */
    public User createUser(String newUserName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createUser(newUserName="+newUserName+")");    
        }
        User tmpUser = getUserByNameOrKey(newUserName);
        if (null!=tmpUser) {
            log.warn("Cannot create user "+newUserName+" as they already exist!");
            return tmpUser;
        }
        else {
            tmpUser = new User();
            tmpUser.setName(newUserName);
            saveOrUpdate(tmpUser);
            return tmpUser;
        }
    }

    public Group getGroupByNameOrKey(String nameOrKey) {
        if (log.isTraceEnabled()) {
            log.trace("getGroupByNameOrKey(nameOrKey="+nameOrKey+")");    
        }
        
        Session session = getCurrentSession();
        StringBuffer hql = new StringBuffer("select g from Group g ");
        hql.append("left outer join fetch g.userRelationships ur ");
        hql.append("left outer join fetch ur.user ");
        hql.append("where g.name = :name or g.key = :name ");
        Query query = session.createQuery(hql.toString());
        query.setString("name", nameOrKey);
        return (Group)query.uniqueResult();
    }
    
    public Group createGroup(String groupOwner, String groupName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("createGroup(groupOwner="+groupOwner+", groupName="+groupName+")");    
        }
        User tmpUser = getUserByNameOrKey(groupOwner);
        if (tmpUser==null) {
        	throw new DaoException("Group owner does not exist: "+groupOwner);
        }
        Group tmpGroup = getGroupByNameOrKey(groupName);
        if (null!=tmpGroup) {
            log.warn("Cannot create group as it already exists: "+groupName);
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
        if (log.isTraceEnabled()) {
            log.trace("createGroup(groupName="+groupName+")");    
        }
    	Group tmpGroup = getGroupByNameOrKey(groupName);
        if (null==tmpGroup) {
            throw new DaoException("Cannot delete group which does not exist: "+groupName);
        }
        
        genericDelete(tmpGroup);
    }

    /**
     * Defines the relationship between a user and group. If the user has any existing relationship with the group, it is
     * updated instead.
     * @param userNameOrKey
     * @param groupNameOrKey
     * @param relationshipType
     * @return
     */
    public SubjectRelationship setRelationship(String userNameOrKey, String groupNameOrKey, String relationshipType) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("setRelationship(userNameOrKey="+userNameOrKey+", groupNameOrKey="+groupNameOrKey+", relationshipType="+relationshipType+")");    
        }

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
        if (log.isTraceEnabled()) {
            log.trace("addUserToGroup(userNameOrKey="+userNameOrKey+", groupNameOrKey="+groupNameOrKey+")");    
        }
        setRelationship(userNameOrKey, groupNameOrKey, SubjectRelationship.TYPE_GROUP_MEMBER);
    }
    
    public void removeUserFromGroup(String groupUser, String groupName) throws DaoException {
        if (log.isTraceEnabled()) {
            log.trace("removeUserFromGroup(groupUser="+groupUser+", groupName="+groupName+")");    
        }
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
