package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Create mask/load artifacts for existing neuron separations.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class MaskChanArtifactService extends AbstractEntityService {

	private static final String centralDir = SystemConfigurationProperties.getString("FileStore.CentralDir");
	
	public static final String PARAM_separationId = "separation id";
	
    public static final String MODE_UNDEFINED = "UNDEFINED";
    public static final String MODE_CREATE_INPUT_LIST = "CREATE_INPUT_LIST";
    public static final int GROUP_SIZE = 200;
	
    private String mode = MODE_UNDEFINED;

    public void execute() throws Exception {
        mode = processData.getString("MODE");
        if (mode.equals(MODE_CREATE_INPUT_LIST)) {
            doCreateInputList();
        }
        else {
            logger.error("Unrecognized mode: "+mode);
        }
    }
    
    private void doCreateInputList() throws Exception {

        logger.info("Finding neuron separations...");
        
        List<Entity> entities = new ArrayList<Entity>();
        for(Entity result : entityBean.getUserEntitiesByTypeName(ownerKey, EntityConstants.TYPE_NEURON_SEPARATOR_PIPELINE_RESULT)) {
        	logger.info("Processing neuron separation, id="+result.getId());
        	
        	populateChildren(result);
        	Entity neuronFragments = EntityUtils.findChildWithType(result, EntityConstants.TYPE_NEURON_FRAGMENT_COLLECTION);
        	populateChildren(neuronFragments);
        	
        	// Check for missing mask or chan files
        	boolean allMaskChans = true;
        	if (neuronFragments==null) {
        	    allMaskChans = false;
        	}
        	else {
                for(Entity fragment : EntityUtils.getChildrenOfType(neuronFragments, EntityConstants.TYPE_NEURON_FRAGMENT)) {
                    populateChildren(fragment);    
                    if (fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_MASK_IMAGE) == null || fragment.getValueByAttributeName(EntityConstants.ATTRIBUTE_CHAN_IMAGE) == null) {
                        allMaskChans = false;
                        break;
                    }
                }
        	}
        	
        	if (!allMaskChans) {
                String dir = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
                if (!dir.contains(centralDir)) {
                    logger.info("  Ignoring entity with path which does not contain the FileStore.CentralDir: "+result.getId());
                }
                else {
                    logger.info("  Adding separation to list");
                    entities.add(result);       
                }
        	}
        }
        
        List<List> inputGroups = createGroups(entities, GROUP_SIZE);
        processData.putItem("ENTITY_LIST", inputGroups);
		logger.info("Processed "+entities.size()+" entities into "+inputGroups.size()+" groups.");
    }

    private List<List> createGroups(Collection fullList, int groupSize) {
        List<List> groupList = new ArrayList<List>();
        List currentGroup = null;
        for (Object s : fullList) {
            if (currentGroup==null) {
                currentGroup = new ArrayList();
            } 
            else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup = new ArrayList();
            }
            currentGroup.add(s);
        }
        if (currentGroup!=null && currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }
}
