package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.launcher.archive.ArchiveAccessHelper;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.entity.EntityVisitor;
import org.janelia.it.jacs.shared.utils.entity.EntityVistationBuilder;

/**
 * Move large Sample files to archive to save space.
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleArchiveService extends AbstractEntityService {

	public transient static final String CENTRAL_DIR_PROP = "FileStore.CentralDir";
	private static final String centralDir = SystemConfigurationProperties.getString(CENTRAL_DIR_PROP);
	
    public static final String MODE_CREATE_ARCHIVE_LIST = "CREATE_ARCHIVE_LIST";
    public static final String MODE_SEND_ARCHIVAL_MESSAGE = "SEND_ARCHIVAL_MESSAGE";
    
    protected int numChanges;
    private String mode;
    
    private Long sampleEntityId;
    private Set<String> originalPaths = new HashSet<String>();
    
    public void execute() throws Exception {
        
        mode = processData.getString("MODE");
        
    	sampleEntityId = processData.getLong("SAMPLE_ENTITY_ID");
        if (sampleEntityId==null) {
            throw new IllegalArgumentException("Input parameter SAMPLE_ENTITY_ID may not be null");
        }
    	
        if (mode.equals(MODE_CREATE_ARCHIVE_LIST)) {
            doCreateArchiveList();
        }
        else if (mode.equals(MODE_SEND_ARCHIVAL_MESSAGE)) {
            sendArchivalMessage();
        }
        else {
            logger.error("Do not recognize mode type="+mode);
        }
    }
    
    private void doCreateArchiveList() throws Exception {
        logger.info("Finding files to archive...");
        addSampleFiles(sampleEntityId);
        logger.info("Putting "+originalPaths.size()+" paths in ORIGINAL_FILE_PATHS");
        processData.putItem("ORIGINAL_FILE_PATHS", new ArrayList<String>(originalPaths));
    }

    private void sendArchivalMessage() throws Exception {
        logger.info("Finding files to archive...");
        addSampleFiles(sampleEntityId);
        logger.info("Sending messages to archive "+originalPaths.size()+" paths");
        ArchiveAccessHelper.sendMoveToArchiveMessage(originalPaths, null);
    }
    
	private void addSampleFiles(Long sampleEntityId) throws Exception {
	    logger.info("Finding file nodes under sample id="+sampleEntityId);
        
        Entity sampleEntity = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
        if (sampleEntity == null) {
            throw new IllegalArgumentException("Sample not found with id="+sampleEntityId);
        }
        
        entityLoader.populateChildren(sampleEntity);
        for(Entity child : EntityUtils.getChildrenOfType(sampleEntity, EntityConstants.TYPE_SAMPLE)) {
            addSampleFiles(child.getId());
        }
        
        EntityVistationBuilder.create(entityLoader).startAt(sampleEntity)
                .childrenOfType(EntityConstants.TYPE_PIPELINE_RUN)
                .childrenOfAttr(EntityConstants.ATTRIBUTE_RESULT)
                .run(new EntityVisitor() {
            public void visit(Entity result) throws Exception {
                entityLoader.populateChildren(result);
                processResult(result);
                for(Entity resultResult : EntityUtils.getChildrenForAttribute(result, EntityConstants.ATTRIBUTE_RESULT)) {
                    processResult(resultResult);
                }
            }
        });
	}
    
    private void processResult(Entity result) throws ComputeException {

        logger.debug("  Process result "+result.getName()+" (id="+result.getId()+")");
        
    	if (!result.getOwnerKey().equals(ownerKey)) return;
    	
    	String filepath = result.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);

        if (filepath==null) {
            logger.warn("    File path for "+result.getId()+" is null");
            return;
        }

        if (!filepath.startsWith(centralDir)) {
            return;
        }
    	
        logger.info("  Will archive "+filepath);
    	originalPaths.add(filepath);
    }
    
}
