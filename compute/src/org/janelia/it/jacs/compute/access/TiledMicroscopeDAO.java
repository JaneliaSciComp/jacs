package org.janelia.it.jacs.compute.access;

import Jama.Matrix;
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
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
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
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 4/30/13
 * Time: 12:57 PM
 */
public class TiledMicroscopeDAO extends ComputeBaseDAO {

    private AnnotationDAO annotationDAO;
    private ComputeDAO computeDAO;

    private TmProtobufExchanger protobufExchanger = new TmProtobufExchanger();
    private TmFromEntityPopulator tmFactory = new TmFromEntityPopulator();

    public static final String VERSION_ATTRIBUTE = "Version";
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
            TmWorkspace tmWorkspace = tmFactory.loadWorkspace(workspaceEntity, sampleEntity, null);
            tmWorkspace.setPreferences(preferences);
            return tmWorkspace;

        } catch (Exception e) {
            e.printStackTrace();
            throw new DaoException(e);
        }
    }

    public TmSample createTiledMicroscopeSample(String subject, String sampleName, String pathToRenderFolder) throws DaoException {
        try {            
            String subjectKey = subject;
            if (! subject.contains(":")) {
                subjectKey = "user:" + subject;
            }
            log.debug("Subject key is " + subjectKey);
            String folderName = "3D Tile Microscope Samples";
            Collection<Entity> folders = annotationDAO.getEntitiesByName(subjectKey, folderName);
            Entity folder = null;
            if (folders!=null && folders.size()>0) {
                for (Entity nextFolder: folders) {
                    if (nextFolder.getOwnerKey().equals(subjectKey)) {
                        folder = nextFolder;
                        break;
                    }
                }
            }
            if (folder == null) {
                // Either nothing found in getter, or nothing passed ownership
                // test.
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
     * @param sampleId required, to find the base path, optionally can get from workspace.
     * @param workspaceNameParam optional, may be left blank or null.
     * @throws ComputeException thrown as wrapper for any exceptions.
     */
    public void importSWCFolder(String swcFolderLoc, String ownerKey, Long sampleId, String workspaceNameParam) throws ComputeException {
        try {
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
//                changeWorkspaceVersion(workspaceEntity, TmWorkspace.Version.PB_1, false);
                // Sometimes, the workspace entity will have been written back, bestowing an ID upon it.
                if (workspaceEntity.getId() == null  ||  workspaceEntity.getId() == -1) {
                    workspaceEntity.setId(idSource.next());
                }
                final TmFromEntityPopulator populator = new TmFromEntityPopulator();
                Entity sampleEntity = annotationDAO.getEntityById(sampleId);
                try {
                    tmWorkspace = populator.loadWorkspace(workspaceEntity, sampleEntity, null);
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

            // Set the latest workspace version.
            EntityData wsVersionEd = new EntityData();
            wsVersionEd.setOwnerKey(workspaceEntity.getOwnerKey());
            wsVersionEd.setCreationDate(new Date());
            wsVersionEd.setEntityAttrName(EntityConstants.ATTRIBUTE_PROPERTY);
            wsVersionEd.setValue(TmWorkspace.WS_VERSION_PROP + "=" + TmWorkspace.Version.PB_1);
            wsVersionEd.setParentEntity(workspaceEntity);
            workspaceEntity.getEntityData().add(wsVersionEd);

            workspaceEntity = annotationDAO.saveBulkEntityTree(workspaceEntity);
            log.info("Completed: saving SWC folder " + swcFolderLoc + " to database.");

            // Cleanup: attach the workspace to its proper parent folder.
            Entity parentEntity = folder;
            EntityData ed = parentEntity.addChildEntity(workspaceEntity);
            annotationDAO.saveOrUpdate(ed);
            annotationDAO.saveOrUpdate(parentEntity);

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ComputeException(ex);
        }

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

            //long startGeoLink = System.nanoTime();

            TmModelManipulator neuronManager = new TmModelManipulator(null);
            neuronManager.addLinkedGeometricAnnotationsInMemory(nodeParentLinkage, annotations, neuron);
            //combinedGeoLinkTime += (System.nanoTime() - startGeoLink) / 1000;
            tmWorkspace.getNeuronList().add(neuron);
        } catch (Exception ex) {
            throw new ComputeException(ex);
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
        for (EntityData ed: neuronEntity.getEntityData()) {
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
        for (EntityData ed: rootList) {
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

    public TmWorkspace loadWorkspace(final Long workspaceId) throws DaoException {
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
            List<Long> propSetEntityIds = annotationDAO.getChildEntityIdsByType(workspaceEntity.getId(), EntityConstants.TYPE_PROPERTY_SET);
            Entity prefsEntity = null;
            if (propSetEntityIds.size() > 1) {
                log.warn("More than one property set on workspace " + workspace.getId() + " keeping only first.");
            }
            for (Long propSetEntityId: propSetEntityIds) {
                prefsEntity = annotationDAO.getEntityById(propSetEntityId);
                break;
            }
            try {
                workspace = tmFactory.loadWorkspace(workspaceEntity, sampleEntity, prefsEntity, wsVersion);
                connectivityException = false;
            } catch (TmConnectivityException e) {
                e.printStackTrace();
                connectivityException = true;
            }
            if (connectivityException) {
                fixConnectivityWorkspace(workspaceId);
                workspaceEntity = annotationDAO.getEntityById(workspaceId);
                workspace = tmFactory.loadWorkspace(workspaceEntity, sampleEntity, prefsEntity, wsVersion);
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
                if (prefs == null) {
                    log.info("No preferences object found in workspace " + workspaceEntity.getId() + " " + workspaceEntity.getName() + " adding empty prefs object.");
                    prefs = new TmPreferences();
                    workspace.setPreferences(prefs);
                }
                String abandonedColorMapPref = prefs.getProperty(NEURON_STYLES_PREF);
                String oldColorMapPref = prefs.getProperty(OLD_NEURON_STYLES_PREF);
                if (oldColorMapPref == null  &&  abandonedColorMapPref != null) {
                    oldColorMapPref = abandonedColorMapPref;
                    this.createOrUpdateWorkspacePreference(workspaceId, OLD_NEURON_STYLES_PREF, oldColorMapPref);
                    refreshPrefsInDomainObject(workspaceEntity, workspace);
                }

                // Loop through, saving neurons as PROTOBUF, and mapping of old/new ids.
                final Map<Long, Long> oldToNew = Collections.synchronizedMap(new HashMap<Long,Long>());
                final EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();

                for (TmNeuron neuron : workspace.getNeuronList()) {
                    // Must pre-sanitize the neuron IDs.
                    Long oldNeuronID = neuron.getId();
                    neuron.setId(null);
                    for (TmGeoAnnotation anno : neuron.getGeoAnnotationMap().values()) {
                        anno.setNeuronId(-1L);
                        if (anno.getParentId().equals(oldNeuronID)  ||  !neuron.getGeoAnnotationMap().keySet().contains(anno.getParentId())) {
                            anno.setParentId(-1L);
                        }
                    }
                    neuron.setWorkspaceId(workspaceId);
                    neuron.setOwnerKey(workspaceEntity.getOwnerKey());
                    neuron.setCreationDate(new Date());
                    neuron = pushGuaranteedNewNeuron(workspaceEntity, neuron, entityBean);
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
                    refreshPrefsInDomainObject(workspaceEntity, workspace);
                }

                // In event of some error, it is possible only part of the
                // neurons handled above will have been converted. Hence they
                // are being marked with the version, post-convert.
                setWorkspaceLatestVersion(workspaceEntity);
				workspace.setWorkspaceVersion(TmWorkspace.Version.PB_1);
                annotationDAO.saveOrUpdateEntity(workspaceEntity);
                log.info("Conversion completed for workspace " + workspaceId + " '" + workspace.getName() + "'.");

            } // end if we need to update workspace from pre-protobuf version

            return workspace;
        } catch (Exception e) {
            log.error(e.getMessage() + " on workspace " + workspaceId);
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

    private TmNeuron pushGuaranteedNewNeuron(Entity collectionEntity, TmNeuron neuron, EntityBeanLocal entityBean) throws Exception {
        // Must now create a new entity data
        EntityData entityData = new EntityData();
        entityData.setOwnerKey(neuron.getOwnerKey());
        entityData.setCreationDate(neuron.getCreationDate());
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
        entityData.setParentEntity(collectionEntity);

        // save back.
        log.debug("Saving back the neuron " + entityData.getId());
        EntityData savedEntityData = entityBean.saveOrUpdateEntityData(entityData);
        log.debug("Adding neuron to its collection.");
        collectionEntity.getEntityData().add(savedEntityData);
        log.debug("Saved back the neuron " + entityData.getId());

        log.debug("Re-saving neuron with neuron id embedded in geo annotations.");
        // Now that the neuron has been db-dipped, we can get its ID, and 
        // push that into all points.
        Long id = savedEntityData.getId();
        for (TmGeoAnnotation anno : neuron.getGeoAnnotationMap().values()) {
            anno.setNeuronId(id);
            if (anno.getParentId().equals(-1L)) {
                anno.setParentId(id);
            }
        }
        // Need to make serializable version of the data.
        neuron.setId(id);
        createEntityData(entityData, neuron);
        EntityData savedEd = entityBean.saveOrUpdateEntityData(entityData);
        neuron.setId(id);

        return neuron;
    }

    private void createEntityData(EntityData entityData, TmNeuron neuron) throws Exception {
        byte[] serializableBytes = protobufExchanger.serializeNeuron(neuron);
        BASE64Encoder encoder = new BASE64Encoder();
        entityData.setValue(encoder.encode(serializableBytes));
        entityData.setEntityAttrName(EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
    }

    private EntityData changeWorkspaceVersion(Entity workspaceEntity, TmWorkspace.Version version, boolean saveWsEntity) throws DaoException {
        // Eliminate any excessive previous version value(s).
        // This is to cleanup any tagalong leftovers.
        List<EntityData> existingVersionEds = new ArrayList<>();
        for (EntityData ed : workspaceEntity.getEntityData()) {
            if (ed.getEntityAttrName().equals(EntityConstants.ATTRIBUTE_PROPERTY)) {
                String propValue = ed.getValue();
                if (propValue.startsWith(TmWorkspace.WS_VERSION_PROP)) {
                    existingVersionEds.add(ed);
                }
            }
        }
        if (existingVersionEds.size() > 1) {
            for (EntityData ed : existingVersionEds) {
                annotationDAO.deleteEntityData(ed);
                workspaceEntity.getEntityData().remove(ed);
            }
        }
        EntityData wsVersionEd;
        if (existingVersionEds.size() == 1) {
            wsVersionEd = existingVersionEds.get(0);
            wsVersionEd.setValue(TmWorkspace.WS_VERSION_PROP + "=" + version);
        }
        else {
            wsVersionEd = new EntityData();
            wsVersionEd.setOwnerKey(workspaceEntity.getOwnerKey());
            wsVersionEd.setCreationDate(new Date());
            wsVersionEd.setEntityAttrName(EntityConstants.ATTRIBUTE_PROPERTY);
            wsVersionEd.setValue(TmWorkspace.WS_VERSION_PROP + "=" + version);
            wsVersionEd.setParentEntity(workspaceEntity);
            workspaceEntity.getEntityData().add(wsVersionEd);
        }

        annotationDAO.saveOrUpdateEntityData(wsVersionEd);
        if (saveWsEntity) {
            annotationDAO.saveOrUpdateEntity(workspaceEntity);

        }
        return wsVersionEd;
    }

    private EntityData changeWorkspaceVersion(Entity workspaceEntity, TmWorkspace.Version version) throws DaoException {
        return changeWorkspaceVersion(workspaceEntity, version, true);
    }

    private void refreshPrefsInDomainObject(Entity workspaceEntity, TmWorkspace workspace) throws Exception {
        TmPreferences prefs;
        // Refresh preferences in the domain object.
        for (Entity child : workspaceEntity.getChildren()) {
            if (child.getEntityTypeName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                prefs = tmFactory.createTmPreferences(child);
                workspace.setPreferences(prefs);
                break;
            }
        }
    }

    // NOTE: This does NOT return a valid entity tree - this hijacks the legacy TmWorkspace, etc., objects to return
    // protobuf data
    public Set<TmNeuron> getNeuronsFromProtobufDataByWorkspaceId(Long workspaceId) throws Exception {
        Set<TmNeuron> neuronSet=new HashSet<>();
        AnnotationDAO annotationDAO = new AnnotationDAO(log);
        Entity workspaceEntity = annotationDAO.getEntityById(workspaceId);
        Set<EntityData> entityDatas=workspaceEntity.getEntityData();
        for (EntityData ed : entityDatas) {
            byte[] protobufBytes=annotationDAO.getB64DecodedEntityDataValue(workspaceId, ed.getId(), EntityConstants.ATTRIBUTE_PROTOBUF_NEURON);
            if (protobufBytes!=null) {
                TmProtobufExchanger exchanger=new TmProtobufExchanger();
                neuronSet.add(exchanger.deserializeNeuron(protobufBytes));
            }
        }
        return neuronSet;
    }

}
