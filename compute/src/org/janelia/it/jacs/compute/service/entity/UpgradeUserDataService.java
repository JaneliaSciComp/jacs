package org.janelia.it.jacs.compute.service.entity;

import java.util.Set;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.types.Category;
import org.janelia.it.jacs.model.ontology.types.Tag;
import org.janelia.it.jacs.model.ontology.types.Text;
import org.janelia.it.jacs.model.user_data.Group;
import org.janelia.it.jacs.model.user_data.User;

/**
 * Upgrade the model to use the most current entity structure.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class UpgradeUserDataService extends AbstractEntityService {
    
    private static final Logger log = Logger.getLogger(UpgradeUserDataService.class);
    
    public void execute() throws Exception {
        
        final String serverVersion = computeBean.getAppVersion();
        logger.info("Updating data model for "+ownerKey+" to latest version: "+serverVersion);

        createGroupEveryone();
        createCommonOntology();
    }

	private void createGroupEveryone() throws Exception {

		log.info("Creating "+Group.ALL_USERS_GROUP_KEY);
		Group allUsers = computeBean.createGroup("user:system", Group.ALL_USERS_GROUP_NAME);
		
		allUsers.setFullName("Workstation Users");
		computeBean.saveOrUpdateSubject(allUsers);
		
        annotationBean.createWorkspace(allUsers.getKey());

		log.info("Adding all users to "+allUsers.getKey());
		for(User user : computeBean.getUsers()) {
			computeBean.addUserToGroup(user.getKey(), Group.ALL_USERS_GROUP_KEY);
			annotationBean.addGroupWorkspaceToUserWorkspace(user.getKey(), Group.ALL_USERS_GROUP_KEY);
		}
	}
	
	private void createCommonOntology() throws Exception {

		log.info("Creating common ontology");
		
		Entity ontology = annotationBean.createOntologyRoot(Group.ALL_USERS_GROUP_KEY, "Image Evaluation");
		
		// Convert "Error Ontology" into a subtree of the common ontology
		Set<Entity> errorOntologySet = entityBean.getEntitiesByName("group:flylight", "Error Ontology");
		if (!errorOntologySet.isEmpty()) {
			Entity errorOntology = errorOntologySet.iterator().next();
			errorOntology.setEntityTypeName(EntityConstants.TYPE_ONTOLOGY_ELEMENT);
			errorOntology.setValueByAttributeName(EntityConstants.ATTRIBUTE_ONTOLOGY_TERM_TYPE, "Category");
			errorOntology.setName("Report");
			entityBean.saveOrUpdateEntity(errorOntology);
			entityBean.annexEntityTree(Group.ALL_USERS_GROUP_KEY, errorOntology.getId());
			errorOntology = entityBean.getEntityById(errorOntology.getId()); // Get annexed entity with correct ownership
			entityBean.addEntityToParent(ontology, errorOntology, 1, EntityConstants.ATTRIBUTE_ONTOLOGY_ELEMENT);
		}
		
		// Create "Disposition" subtree
		EntityData dispositionEd = annotationBean.createOntologyTerm(Group.ALL_USERS_GROUP_KEY, ontology.getId(), "Disposition", new Category(), 2);
		Entity disposition = dispositionEd.getChildEntity();
		int i = 1;
		annotationBean.createOntologyTerm(Group.ALL_USERS_GROUP_KEY, disposition.getId(), "Accepted", new Tag(), i++);
		annotationBean.createOntologyTerm(Group.ALL_USERS_GROUP_KEY, disposition.getId(), "Discard", new Tag(), i++);
		annotationBean.createOntologyTerm(Group.ALL_USERS_GROUP_KEY, disposition.getId(), "Re-image", new Tag(), i++);
		annotationBean.createOntologyTerm(Group.ALL_USERS_GROUP_KEY, disposition.getId(), "Image at 63x", new Tag(), i++);
		annotationBean.createOntologyTerm(Group.ALL_USERS_GROUP_KEY, disposition.getId(), "Image ROI at 63x", new Text(), i++);
		
	}
}
