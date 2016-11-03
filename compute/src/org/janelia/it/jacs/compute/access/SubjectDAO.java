
package org.janelia.it.jacs.compute.access;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.user_data.SubjectRelationship;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Methods for dealing with subjects such as users and groups.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubjectDAO extends AbstractBaseDAO {

    private static Logger LOG = LoggerFactory.getLogger(SubjectDAO.class);

    public SubjectDAO(EntityManager entityManager) {
        super(entityManager);
    }

    public List<Subject> getSubjects() {
        LOG.trace("getSubjects");
        return findAll(0, -1, Subject.class);
    }

    public List<User> getUsers() {
        LOG.trace("getUsers");
        return findAll(0, -1, User.class);
    }

    public List<Group> getGroups() {
        LOG.trace("getGroups");
        return findAll(0, -1, Group.class);
    }

    public User getUserByNameOrKey(String nameOrKey) {
        LOG.trace("getUserByNameOrKey  {}", nameOrKey);
        String hql =
                "select u from User u left outer join fetch u.groupRelationships gr " +
                "left outer join fetch gr.group " +
                "where u.name = :nameOrKey or u.key = :nameOrKey ";
        return getAtMostOneResult(prepareQuery(hql, ImmutableMap.<String, Object>of("nameOrKey", nameOrKey), User.class));
    }

    public Group getGroupByNameOrKey(String nameOrKey) {
        LOG.trace("getGroupByNameOrKey  {}", nameOrKey);
        String hql =
                "select g from Group g left outer join fetch g.userRelationships ur " +
                "left outer join fetch ur.user " +
                "where g.name = :nameOrKey or g.key = :nameOrKey ";
        return getAtMostOneResult(prepareQuery(hql, ImmutableMap.<String, Object>of("nameOrKey", nameOrKey), Group.class));
    }

    public Subject getSubjectByNameOrKey(String nameOrKey) {
        LOG.trace("getSubjectByNameOrKey {}", nameOrKey);
        String hql = "select s from Subject s where s.name = :nameOrKey or s.key = :nameOrKey";
        return getAtMostOneResult(prepareQuery(hql, ImmutableMap.<String, Object>of("nameOrKey", nameOrKey), Subject.class));

    }

    public List<String> getGroupKeysForUsernameOrSubjectKey(String userKey) {
        LOG.trace("getGroupKeysForUsernameOrSubjectKey {}", userKey);
        String hql = "select g.key from Group g " +
                "join g.userRelationships ur " +
                "join ur.user u " +
                "where u.name = :userKey or u.key = :userKey ";
        return findByQueryParams(hql, ImmutableMap.<String, Object>of("userKey", userKey), String.class);
    }

    public List<String> getSubjectKeys(String subjectKey) {
        List<String> subjectKeyList = new ArrayList<>();
        if (subjectKey == null || "".equals(subjectKey.trim())) return subjectKeyList;
        subjectKeyList.add(subjectKey);
        subjectKeyList.addAll(getGroupKeysForUsernameOrSubjectKey(subjectKey));
        return subjectKeyList;
    }

    /**
     * Method used to add new users to the system
     * @param newUserName - login of the user in the system
     * @return a formatted user object, or null if there was a problem
     * @throws DaoException thrown if there was a problem adding the user to the database
     */
    public User createUser(String newUserName) throws DaoException {
        LOG.trace("createUser {}", newUserName);
        User tmpUser = getUserByNameOrKey(newUserName);
        if (null != tmpUser) {
            LOG.warn("Cannot create user {} as it already exist!", newUserName);
            return tmpUser;
        } else {
            tmpUser = new User();
            tmpUser.setName(newUserName);
            save(tmpUser);
            return tmpUser;
        }
    }

    public Group createGroup(String groupOwner, String groupName) throws DaoException {
        LOG.trace("createGroup(groupOwner={}, groupName={})", groupOwner, groupName);    
        User tmpUser = getUserByNameOrKey(groupOwner);
        if (tmpUser==null) {
            throw new DaoException("Group owner does not exist: "+groupOwner);
        }
        Group tmpGroup = getGroupByNameOrKey(groupName);
        if (null!=tmpGroup) {
            LOG.warn("Cannot create group as it already exists: {}", groupName);
            return tmpGroup;
        } else {
            tmpGroup = new Group();
            tmpGroup.setName(groupName);

            SubjectRelationship relation = new SubjectRelationship();
            relation.setGroup(tmpGroup);
            relation.setUser(tmpUser);
            relation.setRelationshipType(SubjectRelationship.TYPE_GROUP_OWNER);
            tmpGroup.getUserRelationships().add(relation);

            save(tmpGroup);
            return tmpGroup;
        }
    }
        
    public void removeGroup(String groupName) throws DaoException {
        LOG.trace("createGroup {}", groupName);
        Group tmpGroup = getGroupByNameOrKey(groupName);
        if (null==tmpGroup) {
            throw new DaoException("Cannot delete group which does not exist: "+groupName);
        }
        
        delete(tmpGroup);
    }

    /**
     * Defines the relationship between a user and group. If the user has any existing relationship with the group, it is
     * updated instead.
     * @param userNameOrKey user identifier
     * @param groupNameOrKey group identifier
     * @param relationshipType relationship type
     * @return the created user group relationship
     */
    public SubjectRelationship setRelationship(String userNameOrKey, String groupNameOrKey, String relationshipType) throws DaoException {
        LOG.trace("setRelationship userNameOrKey={}, groupNameOrKey={}, relationshipType={}", userNameOrKey, groupNameOrKey, relationshipType);

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
                save(relation);
            }
        }
        
        if (relation==null) {
            // Create new relationship
            relation = new SubjectRelationship();
            relation.setGroup(tmpGroup);
            relation.setUser(user);
            relation.setRelationshipType(relationshipType);
            tmpGroup.getUserRelationships().add(relation);
            save(tmpGroup);
        }
        return relation;
    }
    
    public void addUserToGroup(String userNameOrKey, String groupNameOrKey) throws DaoException {
        LOG.trace("addUserToGroup(userNameOrKey="+userNameOrKey+", groupNameOrKey="+groupNameOrKey+")");
        setRelationship(userNameOrKey, groupNameOrKey, SubjectRelationship.TYPE_GROUP_MEMBER);
    }
    
    public void removeUserFromGroup(String groupUser, String groupName) throws DaoException {
        LOG.trace("removeUserFromGroup(groupUser="+groupUser+", groupName="+groupName+")");
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
        save(tmpGroup);
    }
}
