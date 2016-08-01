package org.janelia.it.jacs.compute.service.domain.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.api.ComputeBeanRemote;
import org.janelia.it.jacs.compute.service.common.ContextLogger;
import org.janelia.it.jacs.compute.service.common.ProcessDataAccessor;
import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.Image;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;

/**
 * A helper class for dealing with common entities such as default images. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainHelper {

    public static final String TREENODE_CLASSNAME = TreeNode.class.getSimpleName();
    public static final String IMAGE_CLASSNAME = Image.class.getSimpleName();

    protected Logger logger;
    protected ContextLogger contextLogger;
    protected DomainDAL domainDAL;
    protected String ownerKey;
   
    public DomainHelper(String ownerKey, Logger logger) {
        this(ownerKey, logger, null);
    }

    public DomainHelper(String ownerKey,
                        Logger logger,
                        ContextLogger contextLogger) {
        this.ownerKey = ownerKey;
        this.domainDAL = DomainDAL.getInstance();
        this.logger = logger;
        if (contextLogger == null) {
            this.contextLogger = new ContextLogger(logger);
        }
        else {
            this.contextLogger = contextLogger;
        }
    }

    /**
     * Create/return a child TreeNode.
     * @param parentFolder
     * @param childName
     * @return
     * @throws Exception
     */
    public TreeNode createChildFolder(TreeNode parentFolder, String subjectKey, String childName, Integer index) throws Exception {
        if (parentFolder.getChildren()!=null) {
            for (Reference childReference : parentFolder.getChildren()) {
                if (!childReference.getTargetClassName().equals(TREENODE_CLASSNAME)) {
                    continue;
                }
                DomainObject child = domainDAL.getDomainObject(subjectKey, childReference);
                if (child.getName().equals(childName)) {
                    return (TreeNode) child;
                }
            }
        }

        // We need to create a new folder
        TreeNode node = new TreeNode();
        node.setName(childName);
        node =  domainDAL.save(subjectKey, node);
        List<Reference> childRef = new ArrayList<>();
        childRef.add(Reference.createFor(TreeNode.class, node.getId()));
        domainDAL.addChildren(subjectKey, parentFolder, childRef, index);
        return node;
    }

    /**
     * Create a child folder or verify it exists and return it.
     * @param parentFolder
     * @param childName
     * @return
     * @throws Exception
     */
    public TreeNode createOrVerifyChildFolder(TreeNode parentFolder, String childName, boolean createIfNecessary) throws Exception {
        
        TreeNode folder = null;
        for(DomainObject domainObject : domainDAL.getDomainObjects(ownerKey, parentFolder.getChildren())) {
            if (domainObject instanceof TreeNode && domainObject.getName().equals(childName)) {
                TreeNode child = (TreeNode)domainObject;
                if (child.getName().equals(childName)) {
                    if (folder != null) {
                        logger.warn("Unexpectedly found multiple child folders with name=" + childName+" for parent folder id="+parentFolder.getId());
                    }
                    else {
                        folder = child;
                    }
                }
            }
        }
        
        if (folder == null) {
            folder = new TreeNode();
            folder.setName(childName);
            domainDAL.save(ownerKey, folder);
            domainDAL.addChildren(ownerKey, parentFolder, Arrays.asList(Reference.createFor(folder)));
        }

        logger.debug("Using childFolder with id=" + folder.getId());
        return folder;
    }
    
    /**
     * Create the given top level object set, or verify it exists and return it. 
     * @param topLevelFolderName
     * @param createIfNecessary
     * @return
     * @throws Exception
     */
    public TreeNode createOrVerifyRootEntity(String ownerKey, String topLevelFolderName, boolean createIfNecessary) throws Exception {
        TreeNode topLevelFolder = null;
        Workspace workspace = domainDAL.getDefaultWorkspace(ownerKey);
        
        for(DomainObject domainObject : domainDAL.getDomainObjects(ownerKey, workspace.getChildren())) {
            if (domainObject instanceof TreeNode && domainObject.getName().equals(topLevelFolderName)) {
                topLevelFolder = (TreeNode)domainObject;
                logger.debug("Found existing topLevelFolder common root, name=" + topLevelFolder.getName());
                break;
            }
        }

        if (topLevelFolder == null) {
            if (createIfNecessary) {
                logger.debug("Creating new topLevelFolder with name=" + topLevelFolderName);
                topLevelFolder = new TreeNode();
                topLevelFolder.setName(topLevelFolderName);
                domainDAL.save(ownerKey, topLevelFolder);
                domainDAL.addChildren(ownerKey, workspace, Arrays.asList(Reference.createFor(topLevelFolder)));
                logger.debug("Saved top level folder as " + topLevelFolder.getId());
            } 
            else {
                throw new Exception("Could not find top-level folder by name=" + topLevelFolderName);
            }
        }

        logger.debug("Using topLevelFolder with id=" + topLevelFolder.getId());
        return topLevelFolder;
    }


    private Sample getRequiredSample(String key, Long sampleId) throws Exception {
        Sample sample = domainDAL.getDomainObject(ownerKey, Sample.class, new Long(sampleId));
        if (sample == null) {
            throw new IllegalArgumentException("Sample not found: " + sampleId);
        }
        if (contextLogger.isInfoEnabled()) {
            contextLogger.info("Retrieved sample " + sample.getName() + " for " + key + " " + sampleId);
        }
        contextLogger.appendToLogContext("sample " + sample.getName());

        return sample;
    }

    public Sample getRequiredSample(ProcessDataAccessor data) throws Exception {
        final String defaultKey = "SAMPLE_ENTITY_ID";
        final Object sampleId = data.getRequiredItem(defaultKey);
        if (sampleId instanceof Long) {
            return getRequiredSample(defaultKey, (Long)sampleId);
        }
        else if (sampleId instanceof String) {
            return getRequiredSample(defaultKey, new Long((String)sampleId));
        }
        else {
            throw new IllegalArgumentException("Illegal type for SAMPLE_ENTITY_ID (must be Long or String)");
        }
    }
    
    public ObjectiveSample getRequiredObjectiveSample(Sample sample, ProcessDataAccessor data) throws Exception {
        final String objective = data.getRequiredItemAsString("OBJECTIVE");
        ObjectiveSample objectiveSample = sample.getObjectiveSample(objective);
        if (objectiveSample==null) {
            throw new IllegalArgumentException("Objective '"+objective+"' not found for sample " + sample.getId());
        }
        return objectiveSample;
    }
    
    public SamplePipelineRun getRequiredPipelineRun(Sample sample, ObjectiveSample objectiveSample, ProcessDataAccessor data) throws Exception {
        Long pipelineRunId = data.getRequiredItemAsLong("PIPELINE_RUN_ENTITY_ID");
        SamplePipelineRun run = objectiveSample.getRunById(pipelineRunId);
        if (run==null) {
            throw new IllegalArgumentException("Pipeline run "+pipelineRunId+" not found in sample "+sample.getId());
        }
        return run;
    }
}
