package org.janelia.it.jacs.compute.access;

import Jama.Matrix;
import com.google.common.base.Stopwatch;
import org.janelia.it.jacs.shared.utils.StringUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;
import org.apache.log4j.Logger;

import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.largevolume.RawFileFetcher;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.jacs.model.entity.*;
import org.janelia.it.jacs.model.user_data.User;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.*;
import org.janelia.it.jacs.shared.img_3d_loader.TifVolumeFileLoader;

import org.janelia.it.jacs.shared.swc.SWCData;
import org.janelia.it.jacs.compute.access.util.FileByTypeCollector;
import org.janelia.it.jacs.compute.annotation.api.AnnotationCollector;
import org.janelia.it.jacs.model.IdSource;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmFromEntityPopulator;
import org.janelia.it.jacs.model.user_data.tiled_microscope_builder.TmModelManipulator;
import org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf.TmProtobufExchanger;
import org.janelia.it.jacs.model.util.MatrixUtilities;
import org.janelia.it.jacs.shared.swc.ImportExportSWCExchanger;
import org.janelia.it.jacs.shared.swc.MatrixDrivenSWCExchanger;
import org.janelia.it.jacs.shared.swc.SWCDataConverter;
import org.janelia.it.jacs.shared.swc.SWCNode;
import sun.misc.BASE64Encoder;

/**
 * Created with IntelliJ IDEA. User: murphys Date: 4/30/13 Time: 12:57 PM
 */
public class TiledMicroscopeDAO extends ComputeBaseDAO {

    private AnnotationDAO annotationDAO;
    private ComputeDAO computeDAO;

    private TmFromEntityPopulator tmFactory = new TmFromEntityPopulator();

    public static final String VERSION_ATTRIBUTE = "Version";
    private final static String TMP_GEO_VALUE = "@@@ new geo value string @@@";
    private final static String WORKSPACES_FOLDER_NAME = "Workspaces";
    private final static String BASE_PATH_PROP = "SWC.Import.BaseDir";
    public static final String OLD_NEURON_STYLES_PREF = "old-annotation-neuron-styles";
    public static final String NEURON_STYLES_PREF = "annotation-neuron-styles";

    public TiledMicroscopeDAO(Logger logger) {
        super(logger);
        annotationDAO = new AnnotationDAO(logger);
        computeDAO = new ComputeDAO(logger);

    }

    public void createTiledMicroscopeEntityTypes() throws DaoException {
        log.debug("createTiledMicroscopeEntityTypes() - TiledMicroscopeDAO layer");

        try {
            log.debug("Creating Workspace entity");
            annotationDAO.createNewEntityType(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE, EntityConstants.ATTRIBUTE_ENTITY);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE, EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);

            log.debug("Creating Neuron entity");
            annotationDAO.createNewEntityType(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, EntityConstants.ATTRIBUTE_ANCHORED_PATH);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON, EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);

            log.debug("Creating PropertySet entity");
            annotationDAO.createNewEntityType(EntityConstants.TYPE_PROPERTY_SET);
            annotationDAO.createNewEntityAttr(EntityConstants.TYPE_PROPERTY_SET, EntityConstants.ATTRIBUTE_PROPERTY);
        } catch (Exception e) {
            throw new DaoException(e);
        }

        log.debug("createTiledMicroscopeEntityTypes() - done");
    }

    public TmWorkspace createTiledMicroscopeWorkspace(Long parentId, Long brainSampleId, String name, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Tiled Microscope Workspace must be created with valid 3D Tile Microscope Sample Id");
            }
            Entity workspaceEntity = new Entity();
            workspaceEntity.setCreationDate(new Date());
            workspaceEntity.setUpdatedDate(new Date());
            workspaceEntity.setName(name);
            User user = computeDAO.getUserByNameOrKey(ownerKey);
            if (user == null) {
                throw new Exception("Owner Key=" + ownerKey + " is not valid");
            }
            workspaceEntity.setOwnerKey(ownerKey);
            workspaceEntity.setEntityTypeName(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
            annotationDAO.saveOrUpdate(workspaceEntity);
            // create preferences
            TmPreferences preferences = createTiledMicroscopePreferences(workspaceEntity.getId());
            if (parentId != null) {
                Entity parentEntity = annotationDAO.getEntityById(parentId);
                EntityData ed = parentEntity.addChildEntity(workspaceEntity);
                annotationDAO.saveOrUpdate(ed);
                annotationDAO.saveOrUpdate(parentEntity);
            }
            // associate brain sample
            EntityData sampleEd = new EntityData();
            sampleEd.setOwnerKey(workspaceEntity.getOwnerKey());
            sampleEd.setCreationDate(new Date());
            sampleEd.setUpdatedDate(new Date());
            sampleEd.setEntityAttrName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
            // need this?
            // sampleEd.setOrderIndex(0);
            sampleEd.setParentEntity(workspaceEntity);
            sampleEd.setValue(brainSampleId.toString());
            annotationDAO.saveOrUpdate(sampleEd);
            workspaceEntity.getEntityData().add(sampleEd);

            Entity sampleEntity = annotationDAO.getEntityById(brainSampleId);
            setWorkspaceLatestVersion(workspaceEntity);

            annotationDAO.saveOrUpdate(workspaceEntity);
            // back to user
            TmWorkspace tmWorkspace = tmFactory.loadWorkspace(workspaceEntity, sampleEntity);
            tmWorkspace.setPreferences(preferences);
            return tmWorkspace;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * @deprecated
     */
    public TmNeuron createTiledMicroscopeNeuron(Long workspaceId, String name) throws DaoException {
        try {
            Entity workspace = annotationDAO.getEntityById(workspaceId);
            final String ownerKey = workspace.getOwnerKey();

            return createTiledMicroscopeNeuron(workspace, name, ownerKey);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmSample createTiledMicroscopeSample(String user, String sampleName, String pathToRenderFolder) throws DaoException {
        try {
            String subjectKey = "user:" + user;
            String folderName = "3D Tile Microscope Samples";
            Collection<Entity> folders = annotationDAO.getEntitiesByName(subjectKey, folderName);
            Entity folder;
            if (folders != null && folders.size() > 0) {
                folder = folders.iterator().next();
            } else {
                folder = annotationDAO.createFolderInDefaultWorkspace(subjectKey, folderName).getChildEntity();
            }

            Entity sample = newEntity(sampleName, EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE, subjectKey, false);
            sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, pathToRenderFolder);
            annotationDAO.saveOrUpdateEntity(sample);
            log.debug("Saved sample as " + sample.getId());
            annotationDAO.addEntityToParent(folder, sample, folder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
            return new TmSample(
                    sample.getId(),
                    sample.getName(),
                    sample.getCreationDate(),
                    sample.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH).getValue()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * Make a set of neurons, and possibly a workspace, representing the folder
     * given here. Set the neurons' (workspace') ownership to the owner key
     * given.
     *
     * @param swcFolderLoc where is the server-accessible folder?
     * @param ownerKey winds up owning it all.
     * @param sampleId required, to find the base path, optionally can get from
     * workspace.
     * @param workspaceNameParam optional, may be left blank or null.
     * @throws ComputeException thrown as wrapper for any exceptions.
     */
    public void importSWCFolder(String swcFolderLoc, String ownerKey, Long sampleId, String workspaceNameParam) throws ComputeException {
        //this.combinedCreateNeuronTime = 0L;
        //this.combinedGeoLinkTime = 0L;
        //this.combinedNodeIterTime = 0L;
        //this.combinedReadTime = 0L;

        File swcFolder = new File(swcFolderLoc);
        if (!swcFolder.isAbsolute()) {
            String basePathstring = SystemConfigurationProperties.getString(BASE_PATH_PROP);
            File basePath = new File(basePathstring);
            swcFolder = new File(basePath, swcFolderLoc);
        }

        if (!swcFolder.exists() || !swcFolder.canRead() || !swcFolder.isDirectory()) {
            throw new ComputeException("Folder " + swcFolder + " either does not exist, is not a directory, or cannot be read.");
        }

        Iterator<Long> idSource = new IdSource();
        Entity workspaceEntity = null;
        TmWorkspace tmWorkspace = null;
        Entity folder = null;
        if (sampleId == null) {
            throw new ComputeException("Cannot apply SWC neurons without either valid workspace or sample ID.");
        } else {
            // Ensure there is a workspaces folder, and then
            // create a new workspace within that folder.
            String folderName = WORKSPACES_FOLDER_NAME;
            Collection<Entity> folders = annotationDAO.getEntitiesByName(ownerKey, folderName);
            if (folders != null && folders.size() > 0) {
                for (Entity nextFolder : folders) {
                    // Some users can have multiple different workspaces
                    // folders, owing to sharing, etc.
                    if (nextFolder.getOwnerKey().equals(ownerKey)) {
                        folder = nextFolder;
                        break;
                    }
                }
            } else {
                folder = annotationDAO.createFolderInDefaultWorkspace(ownerKey, folderName).getChildEntity();
            }
            String workspaceName = null;
            if (workspaceNameParam == null || workspaceNameParam.length() == 0) {
                workspaceName = swcFolder.getName();
            } else {
                workspaceName = workspaceNameParam.trim();
            }
            log.info("Creating new workspace called " + workspaceName + ", belonging to " + ownerKey + ".");
            workspaceEntity = createTiledMicroscopeWorkspaceInMemory(sampleId, workspaceName, ownerKey);
            workspaceEntity.setId(idSource.next());
            final TmFromEntityPopulator populator = new TmFromEntityPopulator();
            Entity sampleEntity = annotationDAO.getEntityById(sampleId);
            try {
                tmWorkspace = populator.loadWorkspace(workspaceEntity, sampleEntity);
            } catch (Exception ex) {
                throw new ComputeException(ex);
            }
        }

        SWCDataConverter swcDataConverter = new SWCDataConverter();
        Entity sampleEntity = annotationDAO.getEntityById(sampleId);
        if (!sampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
            throw new ComputeException("Sample ID given is not sample type.  Instead, " + sampleId + " is a " + sampleEntity.getEntityTypeName());
        }
        String sampleBasePath = sampleEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
        if (sampleBasePath == null) {
            throw new ComputeException("Failed to find a base file path for " + sampleId);
        }

        CoordinateToRawTransform coordToRawTransform = this.getTransform(sampleBasePath);
        double[] storedScale = coordToRawTransform.getScale();
        int[] storedOrigin = coordToRawTransform.getOrigin();
        double[] scale = new double[storedScale.length];
        int[] origin = new int[storedOrigin.length];

        for (int i = 0; i < scale.length; i++) {
            origin[i] = (int) (storedOrigin[i] / storedScale[i]);
            scale[i] = storedScale[i] / 1000.0;
        }

        Matrix micronToVox = MatrixUtilities.buildMicronToVox(scale, origin);
        log.info("Computed micronToVox of ");
        micronToVox.print(4, 4);
        Matrix voxToMicron = MatrixUtilities.buildVoxToMicron(scale, origin);
        log.info("Computed voxToMicron of ");
        voxToMicron.print(4, 4);
        ImportExportSWCExchanger exchanger = new MatrixDrivenSWCExchanger(micronToVox, voxToMicron);
        swcDataConverter.setSWCExchanger(exchanger);

        // Collect all files for processing.
        FileByTypeCollector fileCollector = new FileByTypeCollector(swcFolder.getAbsolutePath(), ".swc", 3);
        try {
            fileCollector.exec();
        } catch (IOException ioe) {
            log.error("IO Exception " + ioe + " during directory walk.");
            throw new ComputeException(ioe);
        }
        Set<File> swcFiles = fileCollector.getFileSet();

        int swcCounter = 0;
        log.info("Importing total of " + swcFiles.size() + " SWC files into new workspace.");
        for (File swcFile : swcFiles) {
            if (swcCounter % 1000 == 0) {
                log.info("Importing SWC file number: " + swcCounter + " into memory.");
            }
            long precomputedNeuronId = idSource.next();
            importSWCFile(swcFile, tmWorkspace, swcDataConverter, ownerKey, precomputedNeuronId, idSource);
            swcCounter++;
        }
        log.info("Final SWC file imported into workspace.");

        // Now need to serialize our in-memory model, to the database.
        log.info("Begin: saving SWC folder " + swcFolderLoc + " to database.");
        // Need to bulk up the tree, before saving its bulk.
        try {
            addProtobufNeuronEntityDatas(workspaceEntity, tmWorkspace);
        } catch (Exception ex) {
            throw new ComputeException(ex);
        }

        annotationDAO.saveBulkEntityTree(workspaceEntity);
        log.info("Completed: saving SWC folder " + swcFolderLoc + " to database.");

        // Cleanup: attach the workspace to its proper parent folder.
        Entity parentEntity = folder;
        EntityData ed = parentEntity.addChildEntity(workspaceEntity);
        annotationDAO.saveOrUpdate(ed);
        annotationDAO.saveOrUpdate(parentEntity);

    }

    private void addProtobufNeuronEntityDatas(Entity workspaceEntity, TmWorkspace tmWorkspace) throws Exception {
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        BASE64Encoder encoder = new BASE64Encoder();
        Set<EntityData> entityData = new HashSet<>();
        entityData.addAll(workspaceEntity.getEntityData());
        for (TmNeuron neuron : tmWorkspace.getNeuronList()) {
            EntityData neuronEntityData = new EntityData();
            neuronEntityData.setOwnerKey(workspaceEntity.getOwnerKey());
            neuronEntityData.setCreationDate(neuron.getCreationDate());
            neuronEntityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
            neuronEntityData.setId(neuron.getId());
            neuronEntityData.setParentEntity(workspaceEntity);
            neuronEntityData.setOrderIndex(0);
            neuronEntityData.setValue(encoder.encode(exchanger.serializeNeuron(neuron)));

            entityData.add(neuronEntityData);
        }
        workspaceEntity.setEntityData(entityData);
    }

    private Entity createTiledMicroscopeWorkspaceInMemory(Long brainSampleId, String name, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Tiled Microscope Workspace must be created with valid 3D Tile Microscope Sample Id");
            }
            Entity workspace = new Entity();
            workspace.setCreationDate(new Date());
            workspace.setUpdatedDate(new Date());
            workspace.setName(name);
            workspace.setOwnerKey(ownerKey);
            workspace.setEntityTypeName(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);

            // associate brain sample
            EntityData sampleEd = new EntityData();
            sampleEd.setOwnerKey(workspace.getOwnerKey());
            sampleEd.setCreationDate(new Date());
            sampleEd.setUpdatedDate(new Date());
            sampleEd.setEntityAttrName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);

            // Additional 'characteristics' of the workspace.
            sampleEd.setParentEntity(workspace);
            sampleEd.setValue(brainSampleId.toString());
            workspace.getEntityData().add(sampleEd);
            setWorkspaceLatestVersion(workspace);
            createTiledMicroscopePreferencesInMemory(workspace);

            return workspace;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    private void importSWCFile(File swcFile, TmWorkspace tmWorkspace, SWCDataConverter swcDataConverter, String ownerKey, long precomputedNeuronId, Iterator<Long> idSource) throws ComputeException {
        // the constructor also triggers the parsing, but not the validation
        try {
            //long startRead = System.nanoTime();
            SWCData swcData = SWCData.read(swcFile);
            if (!swcData.isValid()) {
                throw new ComputeException(String.format("invalid SWC file %s; reason: %s",
                        swcFile.getName(), swcData.getInvalidReason()));
            }
            //combinedReadTime += (System.nanoTime() - startRead) / 1000;

            // note from CB, July 2013: Vaa3d can't handle large coordinates in swc files,
            //  so he added an OFFSET header and recentered on zero when exporting
            // therefore, if that header is present, respect it
            double[] externalOffset = swcData.parseOffset();

            // create one neuron for the file; take name from the filename (strip extension)
            String neuronName = swcData.parseName();
            if (neuronName == null) {
                neuronName = swcFile.getName();
            }
            if (neuronName.endsWith(SWCData.STD_SWC_EXTENSION)) {
                neuronName = neuronName.substring(0, neuronName.length() - SWCData.STD_SWC_EXTENSION.length());
            }

            //long startCreateN = System.nanoTime();
            final TmNeuron neuron = this.createTmNeuronInMemory(tmWorkspace, neuronName, precomputedNeuronId);
            //combinedCreateNeuronTime += (System.nanoTime() - startCreateN) / 1000;

            Map<Integer, Integer> nodeParentLinkage = new HashMap<>();
            Map<Integer, TmGeoAnnotation> annotations = new HashMap<>();
            //long startNodeIter = System.nanoTime();
            for (SWCNode node : swcData.getNodeList()) {
                // Internal points, as seen in annotations, are same as external
                // points in SWC: represented as voxels. --LLF
                double[] internalPoint = swcDataConverter.internalFromExternal(
                        new double[]{
                            node.getX() + externalOffset[0],
                            node.getY() + externalOffset[1],
                            node.getZ() + externalOffset[2],}
                );

                // Build an external, unblessed annotation.  Set the id to the index.
                TmGeoAnnotation unserializedAnnotation = new TmGeoAnnotation(
                        new Long(node.getIndex()), "",
                        internalPoint[0], internalPoint[1], internalPoint[2],
                        null, new Date()
                );
                unserializedAnnotation.setRadius(node.getRadius());
                unserializedAnnotation.setNeuronId(neuron.getId());
                annotations.put(node.getIndex(), unserializedAnnotation);

                nodeParentLinkage.put(node.getIndex(), node.getParentIndex());
            }
            //combinedNodeIterTime += (System.nanoTime() - startNodeIter) /1000;

            // Fire off the bulk update.  The "un-serialized" or
            // db-unknown annotations could be swapped for "blessed" versions.
            //long startGeoLink = System.nanoTime();
            addLinkedGeometricAnnotationsInMemory(nodeParentLinkage, annotations, neuron, idSource);
            //combinedGeoLinkTime += (System.nanoTime() - startGeoLink) / 1000;
            tmWorkspace.getNeuronList().add(neuron);
        } catch (Exception ex) {
            throw new ComputeException(ex);
        }
    }

    /**
     * @deprecated
     */
    private TmNeuron createTiledMicroscopeNeuron(Entity workspace, String name, String ownerKey) throws DaoException {
        return createTiledMicroscopeNeuron(workspace, name, ownerKey, true);
    }

    /**
     * @deprecated
     */
    private TmNeuron createTiledMicroscopeNeuron(Entity workspace, String name, String ownerKey, boolean workspaceChecks) throws DaoException {
        try {
            if (workspace == null || !workspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled Neuron must be created with valid Workspace Id");
            }
            Entity neuron = new Entity();
            neuron.setCreationDate(new Date());
            neuron.setUpdatedDate(new Date());
            neuron.setName(name);
            neuron.setOwnerKey(ownerKey);
            neuron.setEntityTypeName(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
            annotationDAO.saveOrUpdate(neuron);
            // old
            // EntityData ed = workspace.addChildEntity(neuron, EntityConstants.ATTRIBUTE_ENTITY);
            // annotationDAO.saveOrUpdate(ed);
            // Konrad said use annDAO instead so permissions are properly carried over:            
            annotationDAO.addEntityToParent(workspace, neuron, 1, EntityConstants.ATTRIBUTE_ENTITY);
            if (workspaceChecks) {
                annotationDAO.saveOrUpdate(workspace);
            }
            TmNeuron tmNeuron = tmFactory.loadNeuron(neuron);
            return tmNeuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * This creates a neuron in a way suitable for server-side-only use:
     * bypassing some of the ownership safeguards.
     *
     * @param workspace neuron shall exist here.
     * @param name final name of neuron.
     * @return the neuron model.
     * @throws DaoException
     */
    private TmNeuron createTmNeuronInMemory(TmWorkspace workspace, String name, Long precomputedId) throws DaoException {
        try {
            TmModelManipulator neuronManager = new TmModelManipulator(null);
            return neuronManager.createTiledMicroscopeNeuron(workspace, name, precomputedId);

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    private Entity newEntity(String name, String entityTypeName, String ownerKey, boolean isCommonRoot) throws ComputeException {
        Date createDate = new Date();
        Entity entity = new Entity();
        entity.setName(name);
        entity.setOwnerKey(ownerKey);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityTypeName(entityTypeName);
        if (isCommonRoot) {
            entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT, "Common Root");
        }
        return entity;
    }

    protected TmPreferences createTiledMicroscopePreferences(Long workspaceId) throws DaoException {
        try {
            Entity workspace = annotationDAO.getEntityById(workspaceId);
            if (!workspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled microscope preferences must be created with valid Workspace Id");
            }
            Entity preferences = new Entity();
            preferences.setName("preferences");
            preferences.setCreationDate(new Date());
            preferences.setUpdatedDate(new Date());
            preferences.setOwnerKey(workspace.getOwnerKey());
            preferences.setEntityTypeName(EntityConstants.TYPE_PROPERTY_SET);
            annotationDAO.saveOrUpdate(preferences);
            EntityData ed = workspace.addChildEntity(preferences, EntityConstants.ATTRIBUTE_ENTITY);
            annotationDAO.saveOrUpdate(ed);
            annotationDAO.saveOrUpdate(workspace);
            TmPreferences tmPreferences = tmFactory.createTmPreferences(preferences);
            return tmPreferences;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected TmPreferences createTiledMicroscopePreferencesInMemory(Entity workspace) throws DaoException {
        try {
            if (!workspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Tiled microscope preferences must be created with valid Workspace Id");
            }
            Entity preferences = new Entity();
            preferences.setName("preferences");
            preferences.setCreationDate(new Date());
            preferences.setUpdatedDate(new Date());
            preferences.setOwnerKey(workspace.getOwnerKey());
            preferences.setEntityTypeName(EntityConstants.TYPE_PROPERTY_SET);
            workspace.addChildEntity(preferences, EntityConstants.ATTRIBUTE_ENTITY);
            TmPreferences tmPreferences = tmFactory.createTmPreferences(preferences);
            return tmPreferences;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmAnchoredPath addAnchoredPath(Long neuronID, Long annotationID1, Long annotationID2,
            List<List<Integer>> pointlist) throws Exception {

        try {
            for (List<Integer> point : pointlist) {
                if (point.size() != 3) {
                    throw new Exception("all points must be 3-vectors");
                }
            }

            // retrieve neuron; object is easier to check that annotations in neuron
            TmNeuron neuron = loadNeuron(neuronID);
            if (!neuron.getGeoAnnotationMap().containsKey(annotationID1)
                    || !neuron.getGeoAnnotationMap().containsKey(annotationID2)) {
                throw new Exception("both annotations must be in neuron");
            }

            // to do real work, though, we need the entity:
            Entity neuronEntity = annotationDAO.getEntityById(neuronID);
            if (!neuronEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type=" + neuronID);
            }

            EntityData pathData = new EntityData();
            pathData.setOwnerKey(neuronEntity.getOwnerKey());
            pathData.setCreationDate(new Date());
            pathData.setUpdatedDate(new Date());
            pathData.setEntityAttrName(EntityConstants.ATTRIBUTE_ANCHORED_PATH);
            pathData.setOrderIndex(0);
            pathData.setParentEntity(neuronEntity);
            // perhaps not entirely kosher to use this temp value, but it works
            pathData.setValue(threadSafeTempGeoValue());
            annotationDAO.saveOrUpdate(pathData);
            neuronEntity.getEntityData().add(pathData);
            annotationDAO.saveOrUpdate(neuronEntity);

            // Find and update value string
            boolean valueStringUpdated = false;
            String valueString = null;
            for (EntityData ed : neuronEntity.getEntityData()) {
                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_ANCHORED_PATH)) {
                    if (ed.getValue().equals(threadSafeTempGeoValue())) {
                        valueString
                                = tmFactory.toAnchoredPathStringFromArguments(ed.getId(), annotationID1, annotationID2, pointlist);
                        ed.setValue(valueString);
                        annotationDAO.saveOrUpdate(ed);
                        valueStringUpdated = true;
                    }
                }
            }
            if (!valueStringUpdated) {
                throw new Exception("Could not find anchor path entity data to update for value string");
            }
            TmAnchoredPath anchoredPath
                    = tmFactory.createTmAnchoredPath(valueString);
            return anchoredPath;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmGeoAnnotation addGeometricAnnotation(Long neuronId, Long parentAnnotationId, int index,
            double x, double y, double z, String comment) throws DaoException {
        try {
            Stopwatch stopwatch = new Stopwatch();
            stopwatch.start();
            // Retrieve neuron
            Entity neuron = annotationDAO.getEntityById(neuronId);
            if (!neuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type=" + neuronId);
            }
            // Check if root; if not, find its parent
            boolean isRoot = false;
            if (parentAnnotationId == null) {
                isRoot = true;
            } else {
                // Validate
                boolean foundParent = false;
                for (EntityData ed : neuron.getEntityData()) {
                    if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)
                            || ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                        String value = ed.getValue();
                        // note: really ought to unify this parsing with the parsing of EntityData in TmNeuron
                        String[] vArr = value.split(":");
                        Long pId = new Long(vArr[0]);
                        if (pId.equals(parentAnnotationId)) {
                            foundParent = true;
                            break;
                        }
                    }
                }
                if (!foundParent) {
                    throw new Exception("Could not find parent matching parentId=" + parentAnnotationId);
                }
            }
            // Todd wants to see signs of LVV activity in the server logs; add annotation is the
            //  only common operation that makes sense
            log.info("LVV: adding annotation to neuron " + neuronId + " at " + x + ", " + y + ", " + z);
            return createGeometricAnnotation(neuron, isRoot, parentAnnotationId, index, x, y, z, comment, neuronId);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * Given a collection of annotations, under a common neuron, make
     * annotations for each in the database, preserving the linkages implied in
     * the "value" target of the map provided.
     *
     * @param annotations map of node offset id vs "unserialized" annotation.
     * @param nodeParentLinkage map of node offset id vs parent node offset id.
     * @throws DaoException
     */
    public void addLinkedGeometricAnnotations(
            Map<Integer, Integer> nodeParentLinkage,
            Map<Integer, TmGeoAnnotation> annotations
    ) throws DaoException {
        Entity neuron = null;
        Long neuronId = null;
        Map<Long, Entity> idToNeuron = new HashMap<>();
        try {
            int putativeRootCount = 0;
            // Cache to avoid re-fetch.
            Map<Integer, Long> nodeIdToAnnotationId = new HashMap<>();
            // Ensure the order of progression through nodes matches node IDs.
            Set<Integer> sortedKeys = new TreeSet<>(annotations.keySet());
            for (Integer nodeId : sortedKeys) {
                boolean isRoot = false;
                TmGeoAnnotation unserializedAnnotation = annotations.get(nodeId);
                // Deal with the neuron-parent.
                Long nextNeuronId = unserializedAnnotation.getNeuronId();
                if (neuron == null) {
                    log.trace("MARK3: starting 1x neuron fetch");
                    neuronId = nextNeuronId;
                    neuron = idToNeuron.get(neuronId);
                    if (neuron == null) {
                        neuron = annotationDAO.getEntityById(neuronId);
                        idToNeuron.put(neuronId, neuron);
                    }
                    if (neuron == null) {
                        throw new Exception("Failed to find neuron for id " + neuronId);
                    }
                    if (!neuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                        throw new Exception("Id is not valid TmNeuron. Type=" + neuronId);
                    }
                    log.trace("MARK3: ending 1x neuron fetch");
                }
                if (neuronId != nextNeuronId) {
                    throw new Exception("Do not mix neuron-parents in a batch of annotations.  Found these two mixed: " + neuronId + "," + nextNeuronId);
                }

                // Establish node linkage.
                Integer parentIndex = nodeParentLinkage.get(nodeId);
                Long parentAnnotationId = null;
                if (parentIndex != null && parentIndex != -1) {
                    // NOTE: unless the annotation has been processed as
                    // below, prior to now, the parent ID will be null.                    
                    parentAnnotationId = nodeIdToAnnotationId.get(parentIndex);
                    if (parentAnnotationId == null) {
                        parentAnnotationId = neuronId;
                    }
                } else {
                    putativeRootCount++;
                    parentAnnotationId = neuronId;
                    isRoot = true;
                }

                // Make the actual, DB-based annotation, and save its linkage
                // through its original node id.
                TmGeoAnnotation serializedAnnotation = createGeometricAnnotation(neuron, isRoot, parentAnnotationId, unserializedAnnotation);
                nodeIdToAnnotationId.put(nodeId, serializedAnnotation.getId());

                log.trace("Node " + nodeId + " at " + serializedAnnotation.toString() + ", has id " + serializedAnnotation.getId()
                        + ", has parent " + serializedAnnotation.getParentId() + ", under neuron " + serializedAnnotation.getNeuronId());
            }

            if (putativeRootCount > 1) {
                log.warn("Number of nodes with neuron as parent is " + putativeRootCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    /**
     * Given a collection of annotations, under a common neuron, make
     * annotations for each in the database, preserving the linkages implied in
     * the "value" target of the map provided.
     *
     * @param annotations map of node offset id vs "unserialized" annotation.
     * @param nodeParentLinkage map of node offset id vs parent node offset id.
     * @throws DaoException
     */
    public void addLinkedGeometricAnnotationsInMemory(
            Map<Integer, Integer> nodeParentLinkage,
            Map<Integer, TmGeoAnnotation> annotations,
            TmNeuron tmNeuron,
            Iterator<Long> idSource
    ) throws DaoException {
        Long neuronId = tmNeuron.getId();
        try {
            int putativeRootCount = 0;
            // Cache to avoid re-fetch.
            Map<Integer, Long> nodeIdToAnnotationId = new HashMap<>();
            // Ensure the order of progression through nodes matches node IDs.
            Set<Integer> sortedKeys = new TreeSet<>(annotations.keySet());
            for (Integer nodeId : sortedKeys) {
                boolean isRoot = false;
                TmGeoAnnotation unlinkedAnnotation = annotations.get(nodeId);

                // Establish node linkage.
                Integer parentIndex = nodeParentLinkage.get(nodeId);
                Long parentAnnotationId = null;
                if (parentIndex != null && parentIndex != -1) {
                    // NOTE: unless the annotation has been processed as
                    // below, prior to now, the parent ID will be null.                    
                    parentAnnotationId = nodeIdToAnnotationId.get(parentIndex);
                    if (parentAnnotationId == null) {
                        parentAnnotationId = neuronId;
                    }
                } else {
                    putativeRootCount++;
                    parentAnnotationId = neuronId;
                    isRoot = true;
                }

                // Make the actual annotation, and save its linkage
                // through its original node id.
                TmGeoAnnotation linkedAnnotation = createGeometricAnnotationInMemory(tmNeuron, isRoot, parentAnnotationId, unlinkedAnnotation, idSource);
                TmGeoAnnotation parentAnnotation = tmNeuron.getParentOf(linkedAnnotation);
                if (parentAnnotation != null) {
                    parentAnnotation.addChild(linkedAnnotation);
                }
                nodeIdToAnnotationId.put(nodeId, linkedAnnotation.getId());

                log.trace("Node " + nodeId + " at " + linkedAnnotation.toString() + ", has id " + linkedAnnotation.getId()
                        + ", has parent " + linkedAnnotation.getParentId() + ", under neuron " + linkedAnnotation.getNeuronId());
            }

            if (putativeRootCount > 1) {
                log.warn("Number of nodes with neuron as parent is " + putativeRootCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    /**
     * add a structured text annotation to a thing that doesn't have one
     */
    public TmStructuredTextAnnotation addStructuredTextAnnotation(Long neuronID, Long parentID, int parentType, int formatVersion,
            String data) throws DaoException {

        try {
            // get the neuron entity
            Entity neuron = annotationDAO.getEntityById(neuronID);
            if (!neuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                throw new Exception("Id is not valid TmNeuron type =" + neuronID);
            }

            // parent must be neuron or geoann:
            if (parentType != TmStructuredTextAnnotation.GEOMETRIC_ANNOTATION
                    && parentType != TmStructuredTextAnnotation.NEURON) {
                throw new Exception("parent must be a geometric annotation or a neuron");
            }

            // parent must not already have a structured text annotation
            if (loadNeuron(neuronID).getStructuredTextAnnotationMap().containsKey(parentID)) {
                throw new Exception("parent ID already has a structured text annotation; use update, not add");
            }

            EntityData entityData = new EntityData();
            entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT);
            entityData.setOwnerKey(neuron.getOwnerKey());
            entityData.setCreationDate(new Date());
            entityData.setUpdatedDate(new Date());
            entityData.setOrderIndex(0);
            entityData.setParentEntity(neuron);
            // this is kind of bogus, but it works:
            entityData.setValue(threadSafeTempGeoValue());
            annotationDAO.saveOrUpdate(entityData);
            neuron.getEntityData().add(entityData);
            annotationDAO.saveOrUpdate(neuron);

            // Find and update value string
            boolean valueStringUpdated = false;
            String valueString = null;
            for (EntityData ed : neuron.getEntityData()) {
                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_STRUCTURED_TEXT)) {
                    if (ed.getValue().equals(threadSafeTempGeoValue())) {
                        valueString = tmFactory.toStructuredTextStringFromArguments(
                                ed.getId(), parentID, parentType, formatVersion, data
                        );
                        ed.setValue(valueString);
                        annotationDAO.saveOrUpdate(ed);
                        valueStringUpdated = true;
                    }
                }
            }
            if (!valueStringUpdated) {
                throw new Exception("Could not find temp geo entry to update for value string");
            }
            TmStructuredTextAnnotation structuredAnnotation = tmFactory.createTmStructuredTextAnnotation(valueString);
            return structuredAnnotation;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    public void updateAnchoredPath(TmAnchoredPath anchoredPath, Long annotationID1, Long annotationID2,
            List<List<Integer>> pointList) throws DaoException {
        try {
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, anchoredPath.getId());
            String valueString = tmFactory.toAnchoredPathStringFromArguments(anchoredPath.getId(),
                    annotationID1, annotationID2, pointList);
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * @deprecated
     */
    public void updateGeometricAnnotation(TmGeoAnnotation geoAnnotation,
            int index, double x, double y, double z, String comment) throws DaoException {
        try {
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, geoAnnotation.getId());
            String valueString = TmGeoAnnotation.toStringFromArguments(geoAnnotation.getId(), geoAnnotation.getParentId(),
                    index, x, y, z, comment);
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void updateStructuredTextAnnotation(TmStructuredTextAnnotation textAnnotation, String data)
            throws DaoException {
        try {
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, textAnnotation.getId());
            String valueString = tmFactory.toStructuredTextStringFromArguments(textAnnotation.getId(),
                    textAnnotation.getParentId(), textAnnotation.getParentType(), textAnnotation.getFormatVersion(), data);
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * reparent a geometric annotation to another one (taking its whole subtree
     * with it); both annotations must be in the input neuron
     *
     * @param annotation
     * @param newParentAnnotationID
     * @param neuron
     * @throws DaoException
     */
    public void reparentGeometricAnnotation(TmGeoAnnotation annotation, Long newParentAnnotationID,
            TmNeuron neuron) throws DaoException {

        // verify that both annotations are in the input neuron
        if (!neuron.getGeoAnnotationMap().containsKey(annotation.getId())) {
            throw new DaoException("input neuron doesn't contain child annotation " + annotation.getId());
        }
        if (!neuron.getGeoAnnotationMap().containsKey(newParentAnnotationID)) {
            throw new DaoException("input neuron doesn't contain new parent annotation " + newParentAnnotationID);
        }

        // is it already the parent?
        if (annotation.getParentId().equals(newParentAnnotationID)) {
            return;
        }

        // do NOT create cycles! new parent cannot be in original annotation's subtree:
        for (TmGeoAnnotation testAnnotation : neuron.getSubTreeList(annotation)) {
            if (newParentAnnotationID.equals(testAnnotation.getId())) {
                return;
            }
        }

        // if annotation is a root annotation, change its attribute and save
        EntityData ed;
        try {
            ed = (EntityData) computeDAO.genericLoad(EntityData.class, annotation.getId());
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

        // if the annotation is a root annotation, change its attribute:
        if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
        }

        // change the parent ID and save
        String valueString = TmGeoAnnotation.toStringFromArguments(annotation.getId(), newParentAnnotationID,
                annotation.getIndex(), annotation.getX(), annotation.getY(), annotation.getZ(),
                annotation.getComment());
        ed.setValue(valueString);
        annotationDAO.saveOrUpdate(ed);

    }

    /**
     * @param neuron = neuron containing annotation
     * @param newRoot = annotation to make root in neurite within neuron
     */
    public void rerootNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws DaoException {
        if (newRoot == null || neuron == null) {
            return;
        }

        if (!neuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new DaoException(String.format("input neuron %d doesn't contain new root annotation %d",
                    neuron.getId(), newRoot.getId()));
        }

        // is it already a root?
        if (newRoot.isRoot()) {
            return;
        }

        // from input, follow parents up to current root, keeping them all
        List<TmGeoAnnotation> parentList = new ArrayList<TmGeoAnnotation>();
        TmGeoAnnotation testAnnotation = newRoot;
        while (!testAnnotation.isRoot()) {
            parentList.add(testAnnotation);
            testAnnotation = neuron.getParentOf(testAnnotation);
        }
        TmGeoAnnotation oldRoot = testAnnotation;
        parentList.add(testAnnotation);

        try {
            // change new root to GEO_ROOT entity data type; reset its parent
            //  to the neuron (as one does for a root)
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, newRoot.getId());
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            String valueString = TmGeoAnnotation.toStringFromArguments(newRoot.getId(), neuron.getId(),
                    newRoot.getIndex(), newRoot.getX(), newRoot.getY(), newRoot.getZ(), newRoot.getComment());
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);

            // reparent intervening annotations; skip the first item, which is the
            //  new root (which we've already dealt with)
            for (int i = 1; i < parentList.size(); i++) {
                // change the parent ID and save
                TmGeoAnnotation ann = parentList.get(i);
                Long newParentAnnotationID = parentList.get(i - 1).getId();
                ed = (EntityData) computeDAO.genericLoad(EntityData.class, ann.getId());
                valueString = TmGeoAnnotation.toStringFromArguments(ann.getId(), newParentAnnotationID,
                        ann.getIndex(), ann.getX(), ann.getY(), ann.getZ(), ann.getComment());
                ed.setValue(valueString);
                annotationDAO.saveOrUpdate(ed);
            }

            // change old root to GEO_TREE entity data type
            ed = (EntityData) computeDAO.genericLoad(EntityData.class, oldRoot.getId());
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
            annotationDAO.saveOrUpdate(ed);

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    /**
     * split a neurite into two
     *
     * @param neuron = neuron containing the neurite
     * @param newRoot = annotation within neurite that will become root of new
     * neurite, taking all its descendants with it
     * @throws DaoException
     */
    public void splitNeurite(TmNeuron neuron, TmGeoAnnotation newRoot) throws DaoException {
        if (newRoot == null || neuron == null) {
            return;
        }

        if (!neuron.getGeoAnnotationMap().containsKey(newRoot.getId())) {
            throw new DaoException(String.format("input neuron %d doesn't contain new root annotation %d",
                    neuron.getId(), newRoot.getId()));
        }

        // is it already a root?  then you can't split it (should have been 
        //  checked before it gets here)
        if (newRoot.isRoot()) {
            return;
        }

        // turns out this is an easy, one step operation; all we need to do is
        //  change the annotation's parent to the neuron entity, and change
        //  its type to GEO_ROOT from GEO_TREE
        try {
            // change new root to GEO_ROOT entity data type; reset its parent
            //  to the neuron (as one does for a root)
            EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, newRoot.getId());
            ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
            String valueString = TmGeoAnnotation.toStringFromArguments(newRoot.getId(), neuron.getId(),
                    newRoot.getIndex(), newRoot.getX(), newRoot.getY(), newRoot.getZ(), newRoot.getComment());
            ed.setValue(valueString);
            annotationDAO.saveOrUpdate(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    /**
     * move the neurite containing the input annotation to the specified neuron;
     * also moves TmStructuredTextAnnotations and TmAnchoredPaths
     *
     * @throws DaoException
     */
    public void moveNeurite(TmGeoAnnotation annotation, TmNeuron newNeuron) throws DaoException {

        // already in the neuron?  we're done
        EntityData annotationED = (EntityData) computeDAO.genericLoad(EntityData.class, annotation.getId());
        Entity oldNeuronEntity = annotationED.getParentEntity();
        if (oldNeuronEntity.getId().equals(newNeuron.getId())) {
            return;
        }

        // find root annotation of neurite; we need the neuron to help us with connectivity
        TmNeuron oldNeuron;
        try {
            oldNeuron = tmFactory.loadNeuron(oldNeuronEntity);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
        TmGeoAnnotation rootAnnotation = annotation;
        while (!rootAnnotation.isRoot()) {
            rootAnnotation = oldNeuron.getParentOf(rootAnnotation);
        }

        try {
            // move each annotation's entity data to a new entity (the new neuron)
            Set<Long> movedAnnotationIDs = new HashSet<>();
            Entity newNeuronEntity = annotationDAO.getEntityById(newNeuron.getId());
            for (TmGeoAnnotation ann : oldNeuron.getSubTreeList(rootAnnotation)) {
                movedAnnotationIDs.add(ann.getId());
                EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, ann.getId());
                ed.setParentEntity(newNeuronEntity);
                annotationDAO.saveOrUpdate(ed);
                // move any TmStructuredTextAnnotations as well:
                if (oldNeuron.getStructuredTextAnnotationMap().containsKey(ann.getId())) {
                    TmStructuredTextAnnotation note = oldNeuron.getStructuredTextAnnotationMap().get(ann.getId());
                    ed = (EntityData) computeDAO.genericLoad(EntityData.class, note.getId());
                    ed.setParentEntity(newNeuronEntity);
                    annotationDAO.saveOrUpdate(ed);
                }
            }

            // loop over anchored paths; if endpoints are in set of moved annotations,
            //  move the path as well
            for (TmAnchoredPathEndpoints endpoints : oldNeuron.getAnchoredPathMap().keySet()) {
                // both endpoints are necessarily in the same neurite, so only need
                //  to test one:
                if (movedAnnotationIDs.contains(endpoints.getAnnotationID1())) {
                    EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class,
                            oldNeuron.getAnchoredPathMap().get(endpoints).getId());
                    ed.setParentEntity(newNeuronEntity);
                    annotationDAO.saveOrUpdate(ed);
                }
            }

            // if it's the root, also change its parent annotation to the new neuron
            if (rootAnnotation.getId().equals(rootAnnotation.getId())) {
                EntityData ed = (EntityData) computeDAO.genericLoad(EntityData.class, rootAnnotation.getId());
                String valueString = TmGeoAnnotation.toStringFromArguments(rootAnnotation.getId(), newNeuron.getId(),
                        rootAnnotation.getIndex(), rootAnnotation.getX(), rootAnnotation.getY(), rootAnnotation.getZ(), rootAnnotation.getComment());
                ed.setValue(valueString);
                annotationDAO.saveOrUpdate(ed);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }

    }

    public List<TmWorkspaceDescriptor> getWorkspacesForBrainSample(Long brainSampleId, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity brainSampleEntity = annotationDAO.getEntityById(brainSampleId);
            if (!brainSampleEntity.getEntityTypeName().equals(EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE)) {
                throw new Exception("Workspaces must be parented with valid 3D Tile Microscope Sample Id");
            }
            List<TmWorkspaceDescriptor> descriptorList = new ArrayList<TmWorkspaceDescriptor>();
            for (Entity possibleWorkspace : brainSampleEntity.getChildren()) {
                if (possibleWorkspace.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                    if (possibleWorkspace.getOwnerKey().equals(ownerKey)) {
                        Long wId = possibleWorkspace.getId();
                        String wName = possibleWorkspace.getName();
                        int neuronCount = 0;
                        for (EntityData ed : possibleWorkspace.getEntityData()) {
                            if (ed.getEntityAttrName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                                neuronCount++;
                            }
                        }
                        TmWorkspaceDescriptor descriptor = new TmWorkspaceDescriptor(wId, wName, neuronCount);
                        descriptorList.add(descriptor);
                    }
                }
            }
            Collections.sort(descriptorList, new Comparator<TmWorkspaceDescriptor>() {
                @Override
                public int compare(TmWorkspaceDescriptor a,
                                   TmWorkspaceDescriptor b) {
                    if (a.getId() < b.getId()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            return descriptorList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public List<TmNeuronDescriptor> getNeuronsForWorkspace(Long workspaceId, String ownerKey) throws DaoException {
        try {
            // Validate sample
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
            if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            List<TmNeuronDescriptor> descriptorList = new ArrayList<TmNeuronDescriptor>();
            for (Entity possibleNeuron : workspaceEntity.getChildren()) {
                if (possibleNeuron.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                    if (possibleNeuron.getOwnerKey().equals(ownerKey)) {
                        Long nId = possibleNeuron.getId();
                        String nName = possibleNeuron.getName();
                        int annoCount = 0;
                        for (EntityData ed : possibleNeuron.getEntityData()) {
                            String edName = ed.getEntityAttrName();
                            if (edName.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE) || edName.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                                annoCount++;
                            }
                        }
                        TmNeuronDescriptor descriptor = new TmNeuronDescriptor(nId, nName, annoCount);
                        descriptorList.add(descriptor);
                    }
                }
            }
            Collections.sort(descriptorList, new Comparator<TmNeuronDescriptor>() {
                @Override
                public int compare(TmNeuronDescriptor a,
                        TmNeuronDescriptor b) {
                    if (a.getId() < b.getId()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            });
            return descriptorList;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void removeWorkspacePreference(Long workspaceId, String key) throws DaoException {
        try {
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
            if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            for (Entity e : workspaceEntity.getChildren()) {
                if (e.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                    Set<EntityData> edToRemove = new HashSet<EntityData>();
                    for (EntityData ed : e.getEntityData()) {
                        String pString = ed.getValue();
                        String[] pArr = pString.split("=");
                        String pKey = pArr[0];
                        if (pKey.equals(key)) {
                            edToRemove.add(ed);
                        }
                    }
                    for (EntityData ed : edToRemove) {
                        computeDAO.genericDelete(ed);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void createOrUpdateWorkspacePreference(Long workspaceId, String key, String value) throws DaoException {
        try {
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
            if (!workspaceEntity.getEntityTypeName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            String propertyAttrName = EntityConstants.ATTRIBUTE_PROPERTY;
            for (Entity e : workspaceEntity.getChildren()) {
                if (e.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                    EntityData edToUpdate = null;
                    for (EntityData ed : e.getEntityData()) {
                        String pString = ed.getValue();
                        String[] pArr = pString.split("=");
                        String pKey = pArr[0];
                        if (pKey.equals(key)) {
                            edToUpdate = ed;
                        }
                    }
                    if (edToUpdate == null) {
                        EntityData ed = new EntityData(null, propertyAttrName, e, null, e.getOwnerKey(), key + "=" + value, new Date(), null, 0);
                        annotationDAO.genericSave(ed);
                        e.getEntityData().add(ed);
                    } else {
                        edToUpdate.setValue(key + "=" + value);
                        annotationDAO.genericSave(edToUpdate);
                    }
                    annotationDAO.genericSave(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    protected void generalTreeDelete(String ownerKey, Long entityId, String type) throws DaoException {
        try {
            Entity entity = annotationDAO.getEntityById(entityId);
            if (!entity.getEntityTypeName().equals(type)) {
                throw new Exception("Neurons must be parented with valid Workspace Id");
            }
            if (entity.getOwnerKey().equals(ownerKey)) {
                annotationDAO.deleteEntityTree(ownerKey, entity);
            } else {
                throw new Exception("Owners do not match");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void deleteAnchoredPath(Long pathID) throws DaoException {
        try {
            EntityData ed = (EntityData) annotationDAO.genericLoad(EntityData.class, pathID);
            annotationDAO.genericDelete(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void deleteNeuron(String ownerKey, Long neuronId) throws DaoException {
        generalTreeDelete(ownerKey, neuronId, EntityConstants.TYPE_TILE_MICROSCOPE_NEURON);
    }

    public void deleteWorkspace(String ownerKey, Long workspaceId) throws DaoException {
        generalTreeDelete(ownerKey, workspaceId, EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
    }

    public void deleteGeometricAnnotation(Long geoId) throws DaoException {
        try {
            EntityData ed = (EntityData) annotationDAO.genericLoad(EntityData.class, geoId);
            annotationDAO.genericDelete(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public void deleteStructuredText(Long annID) throws DaoException {
        try {
            EntityData ed = (EntityData) annotationDAO.genericLoad(EntityData.class, annID);
            annotationDAO.genericDelete(ed);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public Map<Integer, byte[]> getTextureBytes(String basePath, int[] viewerCoord, int[] dimensions) throws DaoException {
        Map<Integer, byte[]> rtnVal = new HashMap<>();
        try {
            // Get the bean of data around the point of interest.
            if (log.isDebugEnabled()) {
                log.debug("Getting nearest raw info to coord " + viewerCoord[0] + "," + viewerCoord[1] + "," + viewerCoord[2] + " from base path " + basePath);
            }
            RawFileInfo rawFileInfo
                    = getNearestFileInfo(basePath, viewerCoord);
            if (rawFileInfo == null) {
                throw new Exception("Failed to find any tiff files in " + basePath + ".");
            }
            if (log.isDebugEnabled()) {
                log.info("Got nearest raw info to coord " + viewerCoord[0] + "," + viewerCoord[1] + "," + viewerCoord[2] + " from base path " + basePath);
            }

            // Grab the channels.
            TifVolumeFileLoader loader = new TifVolumeFileLoader();
            if (dimensions != null) {
                loader.setOutputDimensions(dimensions);
            }
            loader.setConversionCharacteristics(
                    rawFileInfo.getTransformMatrix(),
                    rawFileInfo.getInvertedTransform(),
                    rawFileInfo.getMinCorner(),
                    rawFileInfo.getExtent(),
                    rawFileInfo.getQueryMicroscopeCoords()
            );

            loader.loadVolumeFile(rawFileInfo.getChannel0().getAbsolutePath());
            rtnVal.put(0, loader.getTextureByteArray());

            loader.loadVolumeFile(rawFileInfo.getChannel1().getAbsolutePath());
            rtnVal.put(1, loader.getTextureByteArray());

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DaoException(ex);
        }
        return rtnVal;
    }

    public CoordinateToRawTransform getTransform(String basePath) throws DaoException {
        try {
            RawFileFetcher fetcher = RawFileFetcher.getRawFileFetcher(basePath);
            return fetcher.getTransform();
        } catch (Exception ex) {
            throw new DaoException(ex);
        }
    }

    public RawFileInfo getNearestFileInfo(String basePath, int[] viewerCoord) throws DaoException {
        RawFileInfo rtnVal = null;
        try {
            RawFileFetcher fetcher = RawFileFetcher.getRawFileFetcher(basePath);
            rtnVal = fetcher.getNearestFileInfo(viewerCoord);
        } catch (Exception ex) {
            throw new DaoException(ex);
        }
        return rtnVal;
    }

    /**
     * fix connectivity issues for all neurons in a workspace
     */
    private void fixConnectivityWorkspace(Long workspaceID) throws DaoException {
        // remember, can't load workspace object, because that's what we're fixing!
        Entity entity = annotationDAO.getEntityById(workspaceID);
        for (TmNeuronDescriptor neuronDescriptor : getNeuronsForWorkspace(workspaceID, entity.getOwnerKey())) {
            fixConnectivityNeuron(neuronDescriptor.getId());
        }
    }

    /**
     * fix connectity issues for a neuron (bad parents, since children aren't
     * stored in the entity data); fix in this case means breaking links
     */
    private void fixConnectivityNeuron(Long neuronID) throws DaoException {
        // remember, can't load neuron or workspace objects, because those are what we're fixing!

        log.info("attempting to fix connectivity for neuron " + neuronID);

        // first, load all entity data and find the root and link (non-root)
        //  annotations; they can all be parents, but each has different
        //  possible parents
        HashSet<Long> parentSet = new HashSet<>();
        ArrayList<EntityData> linkList = new ArrayList<>();
        ArrayList<EntityData> rootList = new ArrayList<>();
        Entity neuronEntity = annotationDAO.getEntityById(neuronID);
        for (EntityData ed : neuronEntity.getEntityData()) {
            String attributeName = ed.getEntityAttrName();
            if (attributeName.equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                parentSet.add(ed.getId());
                linkList.add(ed);
            } else if (attributeName.equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                parentSet.add(ed.getId());
                rootList.add(ed);
            }
        }

        // check roots: are they children of the neuron?
        for (EntityData ed : rootList) {
            TmGeoAnnotation annotation;
            try {
                annotation = tmFactory.createTmGeoAnnotation(ed);
            } catch (Exception e) {
                e.printStackTrace();
                throw new DaoException(e);
            }
            if (annotation != null && !annotation.getParentId().equals(neuronID)) {
                log.info("root " + annotation.getId() + " had wrong parent; reassigned to current neuron");
                String valueString = TmGeoAnnotation.toStringFromArguments(annotation.getId(),
                        neuronID, annotation.getIndex(), annotation.getX(), annotation.getY(),
                        annotation.getZ(), annotation.getComment());
                ed.setValue(valueString);
                annotationDAO.saveOrUpdate(ed);
            }
        }

        // check non-roots: do we have their parents?
        for (EntityData ed : linkList) {
            // I really don't get why we launder everything to DaoException, but
            //  that seems to be the pattern:
            TmGeoAnnotation annotation;
            try {
                annotation = tmFactory.createTmGeoAnnotation(ed);
            } catch (Exception e) {
                e.printStackTrace();
                throw new DaoException(e);
            }
            if (annotation != null && !parentSet.contains(annotation.getParentId())) {
                log.info("link " + annotation.getId() + " had missing parent; promoted to root");

                // when a missing parent is found:
                //     edit value: set parent ID to neuron ID
                //     edit attribute name: set to root not tree
                String valueString = TmGeoAnnotation.toStringFromArguments(annotation.getId(),
                        neuronID, annotation.getIndex(), annotation.getX(), annotation.getY(),
                        annotation.getZ(), annotation.getComment());
                ed.setValue(valueString);
                ed.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
                annotationDAO.saveOrUpdate(ed);
            }
        }
    }

    public TmWorkspace loadWorkspace(Long workspaceId) throws DaoException {
        try {
            Long sampleID = null;
            Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
            Entity sampleEntity = null;
            // The default workspace version will be the latest pre-proto-buf
            // version.  A version found in the database overrides that.
            TmWorkspace.Version wsVersion = TmWorkspace.Version.ENTITY_4;
            if (workspaceEntity != null) {
                EntityData sampleEd = workspaceEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_WORKSPACE_SAMPLE_IDS);
                if (sampleEd == null) {
                    throw new Exception("workspace " + workspaceEntity.getName() + " has no associated brand sample!");
                } else {
                    sampleID = Long.valueOf(sampleEd.getValue());
                    sampleEntity = annotationDAO.getEntityById(sampleID);
                }
                // This will probably never be needed.
                EntityData wsVersionEd = workspaceEntity.getEntityDataByAttributeName(EntityConstants.ATTRIBUTE_PROPERTY);
                if (wsVersionEd != null) {
                    String wsVersionValue = wsVersionEd.getValue();
                    String[] nvp = wsVersionValue.split("=");
                    wsVersion = TmWorkspace.Version.valueOf(nvp[1].trim());
                }
            }

            // see notes in TmNeuron() on the connectivity retry scheme
            TmWorkspace workspace = null;
            boolean connectivityException;
            try {
                workspace = tmFactory.loadWorkspace(workspaceEntity, sampleEntity, wsVersion);
                connectivityException = false;
            } catch (TmConnectivityException e) {
                e.printStackTrace();
                connectivityException = true;
            }
            if (connectivityException) {
                fixConnectivityWorkspace(workspaceId);
                workspaceEntity = annotationDAO.getEntityById(workspaceId);
                workspace = tmFactory.loadWorkspace(workspaceEntity, sampleEntity, wsVersion);
            }

            // Move workspace to modern version.
            if (TmWorkspace.Version.ENTITY_4 == wsVersion || TmWorkspace.Version.ENTITY_PB_TRANSITION == wsVersion) {
                // Algorithm:
                // 1. Ensure the WS version is at intermediate.
                // 2. Delete any old entity-data/PROTOBUF neurons
                // 3. Try and load in saved-as-old preferences.
                // 4. If no saved-as-old preferences existed, save preferences as-old.
                // 5. Loop through, saving all Neurons as entity-data/PROTOBUF, saving mapping of new-to-old ID.
                // 6. Regenerate the prefs (style/color) map.
                // 7. Save prefs as current prefs string.
                // 8. Advance WS version to PB_1.

                // Advance version to the intermediate.
                setWorkspaceIntermediateVersion(workspaceEntity);
                annotationDAO.saveOrUpdateEntity(workspaceEntity);

                // If any new-style neurons are on the workspace, delete them.
                annotationDAO.loadLazyEntity(workspaceEntity.getOwnerKey(), workspaceEntity, true);
                List<EntityData> toDelete = new ArrayList<>();
                for (EntityData childED : workspaceEntity.getEntityData()) {
                    if (childED.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON)) {
                        toDelete.add(childED);
                    }
                }
                for (EntityData childED : toDelete) {
                    annotationDAO.deleteEntityData(childED);
                }

                // Get the old color map prefs, if they exist.  Write back if needed.
                TmPreferences prefs = workspace.getPreferences();
                String abandonedColorMapPref = prefs.getProperty(NEURON_STYLES_PREF);
                String oldColorMapPref = prefs.getProperty(OLD_NEURON_STYLES_PREF);
                if (oldColorMapPref == null) {
                    oldColorMapPref = abandonedColorMapPref;
                    this.createOrUpdateWorkspacePreference(workspaceId, OLD_NEURON_STYLES_PREF, oldColorMapPref);
                }

                // Refresh.
                prefs = workspace.getPreferences();

                // Loop through, saving neurons as PROTOBUF, and mapping of old/new ids.
                Map<Long, Long> oldToNew = new HashMap<>();
                AnnotationCollector collector = new AnnotationCollector();
                for (TmNeuron neuron : workspace.getNeuronList()) {
                    // Must pre-sanitize the neuron IDs.
                    Long oldNeuronID = neuron.getId();
                    neuron.setId(null);
                    for (TmGeoAnnotation anno : neuron.getGeoAnnotationMap().values()) {
                        anno.setNeuronId(-1L);
                    }
                    neuron.setWorkspaceId(workspaceId);
                    neuron.setOwnerKey(workspaceEntity.getOwnerKey());
                    neuron.setCreationDate(new Date());
                    neuron = collector.pushNeuron(neuron);
                    oldToNew.put(oldNeuronID, neuron.getId());
                }

                // Regenerate the prefs.  Will do so by string-manipulation,
                // rather than at the high level of a deserialized map.
                // Note: color map is entirely optional, based on user's past
                //       actions.
                String newColorMapPref = oldColorMapPref; // Save old value.
                if (newColorMapPref != null) {
                    for (Long oldNeuronId : oldToNew.keySet()) {
                        Long newNeuronId = oldToNew.get(oldNeuronId);
                        String oldNeuronIdStr = Long.toString(oldNeuronId);
                        String newNeuronIdStr = Long.toString(newNeuronId);

                        newColorMapPref = StringUtils.digitSafeReplace(newColorMapPref, oldNeuronIdStr, newNeuronIdStr);
                        if (newColorMapPref == null) {
                            throw new Exception("Failed to replace all values of the color map " + oldColorMapPref + " for workspace " + workspaceId);
                        }
                        log.info("New preferences string [truncated to 400 bytes]: {" + newColorMapPref.substring(0, Math.min(newColorMapPref.length(), 400)) + "...}");
                    }

                    // Save back the prefs map.
                    this.createOrUpdateWorkspacePreference(workspaceId, NEURON_STYLES_PREF, newColorMapPref);
                }

                // In event of some error, it is possible only part of the 
                // neurons handled above will have been converted. Hence they
                // are being marked with the version, post-convert.
                setWorkspaceLatestVersion(workspaceEntity);
                annotationDAO.saveOrUpdateEntity(workspaceEntity);

            }

            return workspace;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmNeuron loadNeuron(Long neuronId) throws DaoException {
        try {
            Entity neuronEntity = annotationDAO.getEntityById(neuronId);
            TmNeuron neuron = null;
            // fixing potential connectivity errors is inelegant; we try
            //  once, then let the exception bubble up the second time;
            //  I did the if-statement because I was worried about
            //  putting the retry in the catch, for fear of infinite recursion
            boolean connectivityException;
            try {
                neuron = tmFactory.loadNeuron(neuronEntity);
                connectivityException = false;
            } catch (TmConnectivityException e) {
                connectivityException = true;
            }
            if (connectivityException) {
                fixConnectivityNeuron(neuronId);
                neuronEntity = annotationDAO.getEntityById(neuronId);
                neuron = tmFactory.loadNeuron(neuronEntity);
            }
            return neuron;
        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public EntityData setWorkspaceLatestVersion(Entity workspaceEntity) throws DaoException {
        return changeWorkspaceVersion(workspaceEntity, TmWorkspace.Version.PB_1);
    }

    public EntityData setWorkspaceIntermediateVersion(Entity workspaceEntity) throws DaoException {
        return changeWorkspaceVersion(workspaceEntity, TmWorkspace.Version.ENTITY_PB_TRANSITION);
    }

    /**
     * Create an annotation. Uses an 'unserialized' or 'unmanaged' object.
     *
     * @param neuron to which annotation belongs.
     * @param isRoot T -> no parent but the neuron
     * @param parentAnnotationId whatever parent. Neuron or otherwise.
     * @param unserializedAnno to pull all info for managed object.
     * @return the managed object, complete with its DB-based ID.
     * @throws Exception from any called methods.
     */
    private TmGeoAnnotation createGeometricAnnotation(Entity neuron, boolean isRoot, Long parentAnnotationId, TmGeoAnnotation unserializedAnno) throws Exception {
        return createGeometricAnnotation(neuron, isRoot, parentAnnotationId, 0, unserializedAnno.getX(), unserializedAnno.getY(), unserializedAnno.getZ(), unserializedAnno.getComment(), neuron.getId());
    }

    private TmGeoAnnotation createGeometricAnnotation(Entity neuron, boolean isRoot, Long parentAnnotationId, int index, double x, double y, double z, String comment, Long neuronId) throws DaoException, Exception {
        EntityData geoEd = new EntityData();
        geoEd.setOwnerKey(neuron.getOwnerKey());
        geoEd.setCreationDate(new Date());
        geoEd.setUpdatedDate(new Date());
        Long parentId = 0L;
        if (isRoot) {
            parentId = neuron.getId();
            geoEd.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE);
        } else {
            parentId = parentAnnotationId;
            geoEd.setEntityAttrName(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE);
        }
        geoEd.setOrderIndex(0);
        geoEd.setParentEntity(neuron);
        geoEd.setValue(threadSafeTempGeoValue());
        annotationDAO.saveOrUpdate(geoEd);
        neuron.getEntityData().add(geoEd);
        annotationDAO.saveOrUpdate(neuron);
        // Find and update value string
        boolean valueStringUpdated = false;
        String valueString = null;
        for (EntityData ed : neuron.getEntityData()) {
            if (isRoot) {
                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_ROOT_COORDINATE)) {
                    if (ed.getValue().equals(threadSafeTempGeoValue())) {
                        valueString = TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                        ed.setValue(valueString);
                        annotationDAO.saveOrUpdate(ed);
                        valueStringUpdated = true;
                    }
                }
            } else {
                if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_GEO_TREE_COORDINATE)) {
                    if (ed.getValue().equals(threadSafeTempGeoValue())) {
                        valueString = TmGeoAnnotation.toStringFromArguments(ed.getId(), parentId, index, x, y, z, comment);
                        ed.setValue(valueString);
                        annotationDAO.saveOrUpdate(ed);
                        valueStringUpdated = true;
                    }
                }
            }
        }
        if (!valueStringUpdated) {
            throw new Exception("Could not find geo entry to update for value string");
        }
        TmGeoAnnotation geoAnnotation = tmFactory.createTmGeoAnnotation(geoEd);
        // normally this is filled in automatically when the annotation is part of
        //  a neuron, but here it's not (explicitly); however, we know the value
        //  to put in:
        geoAnnotation.setNeuronId(neuronId);
        return geoAnnotation;
    }

    /**
     * More 'rapid' version of the geo-annotation creation. Expects all data to
     * reside in memory during construction. More like an in-memory builder, to
     * make something serializable at the end.
     *
     * @param neuron under this neuron.
     * @param isRoot based
     * @param parentAnnotationId parent for thing under construction.
     * @param unserializedAnno in-memory
     * @return new annotation
     * @throws Exception
     */
    private TmGeoAnnotation createGeometricAnnotationInMemory(TmNeuron neuron, boolean isRoot, Long parentAnnotationId, TmGeoAnnotation unserializedAnno, Iterator<Long> idSource) throws Exception {
        return createGeometricAnnotationInMemory(neuron, isRoot, parentAnnotationId, 0, unserializedAnno.getX(), unserializedAnno.getY(), unserializedAnno.getZ(), unserializedAnno.getComment(), neuron.getId(), idSource);
    }

    private TmGeoAnnotation createGeometricAnnotationInMemory(
            TmNeuron tmNeuron, boolean isRoot, Long parentAnnotationId, int index, double x, double y, double z, String comment, Long neuronId, Iterator<Long> idSource) throws DaoException, Exception {

        long generatedId = idSource.next();
        TmGeoAnnotation geoAnnotation = new TmGeoAnnotation(generatedId, comment, x, y, z, parentAnnotationId, new Date());
        geoAnnotation.setNeuronId(neuronId);
        geoAnnotation.setIndex(index);
        tmNeuron.getGeoAnnotationMap().put(geoAnnotation.getId(), geoAnnotation);
        if (isRoot) {
            tmNeuron.addRootAnnotation(geoAnnotation);
        }
        return geoAnnotation;
    }

    private EntityData changeWorkspaceVersion(Entity workspaceEntity, TmWorkspace.Version version) throws DaoException {
        // Eliminate any previous version value(s).
        List<EntityData> toDelete = new ArrayList<>();
        for (EntityData ed : workspaceEntity.getEntityData()) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                String propValue = ed.getValue();
                if (propValue.startsWith(TmWorkspace.WS_VERSION_PROP)) {
                    toDelete.add(ed);
                }
            }
        }
        for (EntityData ed : toDelete) {
            annotationDAO.deleteEntityData(ed);
            workspaceEntity.getEntityData().remove(ed);
        }

        EntityData wsVersionEd = new EntityData();
        wsVersionEd.setOwnerKey(workspaceEntity.getOwnerKey());
        wsVersionEd.setCreationDate(new Date());
        wsVersionEd.setEntityAttrName(EntityConstants.ATTRIBUTE_PROPERTY);
        wsVersionEd.setValue(TmWorkspace.WS_VERSION_PROP + "=" + version);
        wsVersionEd.setParentEntity(workspaceEntity);
        workspaceEntity.getEntityData().add(wsVersionEd);
        annotationDAO.saveOrUpdateEntityData(wsVersionEd);
        annotationDAO.saveOrUpdateEntity(workspaceEntity);
        return wsVersionEd;
    }

    private String threadSafeTempGeoValue() {
        return TMP_GEO_VALUE + Thread.currentThread().getName();
    }

}
