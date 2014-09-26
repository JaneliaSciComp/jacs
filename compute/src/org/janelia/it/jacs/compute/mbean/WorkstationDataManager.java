package org.janelia.it.jacs.compute.mbean;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.AnnotationBeanLocal;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.api.EntityBeanRemote;
import org.janelia.it.jacs.compute.service.entity.OrphanAnnotationCheckerService;
import org.janelia.it.jacs.compute.service.entity.OrphanEntityCheckerService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FlyScreenDiscoveryService;
import org.janelia.it.jacs.compute.service.fileDiscovery.SampleRun;
import org.janelia.it.jacs.compute.service.fly.MaskGuideService;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.compute.service.mongodb.MongoDbLoadService;
import org.janelia.it.jacs.compute.service.solr.SolrIndexingService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.TaskParameter;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileDiscoveryTask;
import org.janelia.it.jacs.model.tasks.fileDiscovery.FileTreeLoaderPipelineTask;
import org.janelia.it.jacs.model.tasks.fly.FlyScreenPatternAnnotationTask;
import org.janelia.it.jacs.model.tasks.fly.MaskSampleAnnotationTask;
import org.janelia.it.jacs.model.tasks.neuron.NeuronMergeTask;
import org.janelia.it.jacs.model.tasks.tic.SingleTicTask;
import org.janelia.it.jacs.model.tasks.utility.GenericTask;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.shared.annotation.MaskAnnotationDataManager;
import org.janelia.it.jacs.shared.utils.EntityUtils;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 5/24/11
 * Time: 10:37 AM
 */
public class WorkstationDataManager implements WorkstationDataManagerMBean {

    private static final Logger logger = Logger.getLogger(WorkstationDataManager.class);


    public WorkstationDataManager() {
    }

    public void runAnnotationImport(String user, String annotationsPath, String ontologyName) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("annotations filepath", annotationsPath, null));
            taskParameters.add(new TaskParameter("ontology name", ontologyName, null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "annotationImport", "Annotation Import Pipeline");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("AnnotationImportPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runScreenScoresLoading(String user, String acceptsPath, String loadedPath) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("accepts file path", acceptsPath, null));
            taskParameters.add(new TaskParameter("loaded file path", loadedPath, null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "screenScoresLoading", "Screen Scores Loading");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresLoadingPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void runCompartmentLoading(String user, String alignmentSpaceName, String maskChanPath, String topLevelFolderName, String opticalResolution, String pixelResolution) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("mask chan file path", maskChanPath, null));
            taskParameters.add(new TaskParameter("alignment space name", alignmentSpaceName, null ));
            taskParameters.add(new TaskParameter("top level folder name", topLevelFolderName, null));
            taskParameters.add(new TaskParameter("optical resolution", opticalResolution, null));
            taskParameters.add(new TaskParameter("pixel resolution", pixelResolution, null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "compartmentLoading", "Mask Chan Encoded Compartment Loading");
            task.setJobName("Mask Chan Compartment Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MaskChanCompartmentLoadPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runScreenScoresExport(String user, String outputFilepath) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("output filepath", outputFilepath, null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "screenScoresExport", "Screen Scores Export");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresExportPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSplitLinesLoading(String user, String topLevelFolderName, String representativesPath, String splitConstructsPath) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("top level folder name", topLevelFolderName, null));
            taskParameters.add(new TaskParameter("representatives filepath", representativesPath, null));
            taskParameters.add(new TaskParameter("split constructs filepath", splitConstructsPath, null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "splitLinesLoading", "Split Lines Loading");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SplitLinesLoadingPipeline", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runNeo4jSync(Boolean clearDb) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(MongoDbLoadService.PARAM_clearDb, Boolean.toString(clearDb), null));
            Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(),
                    taskParameters, "neo4jSync", "Neo4j Sync");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("Neo4jSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMongoDbSync(Boolean clearDb) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(MongoDbLoadService.PARAM_clearDb, Boolean.toString(clearDb), null));
            Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(),
                    taskParameters, "mongoDbSync", "MongoDb Sync");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MongoDbSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSolrIndexSync(Boolean clearIndex) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(SolrIndexingService.PARAM_clearIndex, Boolean.toString(clearIndex), null));
            Task task = new GenericTask(new HashSet<Node>(), "system", new ArrayList<Event>(),
                    taskParameters, "solrIndexSync", "Solr Index Sync");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SolrIndexSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runNeuronMergeTest(String taskOwner, String separationEntityId, String commaSeparatedNeuronFragmentList) {
        try {
            NeuronMergeTask task = new NeuronMergeTask(new HashSet<Node>(), taskOwner, new ArrayList<Event>(), new HashSet<TaskParameter>());
            task.setJobName("Neuron Merge Task");
            task.setParameter(NeuronMergeTask.PARAM_separationEntityId, separationEntityId);
            task.setParameter(NeuronMergeTask.PARAM_commaSeparatedNeuronFragmentList, commaSeparatedNeuronFragmentList);
            task = (NeuronMergeTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("NeuronMerge", task.getObjectId());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSolrTreeIndexing(Long rootId) {
        try {
            EJBFactory.getLocalSolrBean().indexAllEntitiesInTree(rootId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runOrphanCheckerServices(Boolean deleteOrphans) {
        try {
            logger.info("Building list of users with samples...");
            Set<String> subjectKeys = new TreeSet<String>();
            for(Entity sample : EJBFactory.getLocalEntityBean().getEntitiesByTypeName(EntityConstants.TYPE_SAMPLE)) {
                subjectKeys.add(sample.getOwnerKey());
            }
            logger.info("Found users with samples: "+subjectKeys);
            for(String subjectKey : subjectKeys) {
                runOrphanEntityCheckerService(subjectKey, deleteOrphans);
                runOrphanAnnotationCheckerService(subjectKey, deleteOrphans, deleteOrphans);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runOrphanEntityCheckerService(String user, Boolean deleteOrphanTrees) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(OrphanEntityCheckerService.PARAM_deleteOrphanEntityTrees, Boolean.toString(deleteOrphanTrees), null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "orphanEntityChecker", "Orphan Entity Checker");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("OrphanEntityChecker", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runOrphanAnnotationCheckerService(String user, Boolean deleteAnnotationsMissingTargets, Boolean deleteAnnotationsMissingTerms) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(OrphanAnnotationCheckerService.PARAM_deleteAnnotationsMissingTargets, Boolean.toString(deleteAnnotationsMissingTargets), null));
            taskParameters.add(new TaskParameter(OrphanAnnotationCheckerService.PARAM_deleteAnnotationsMissingTerms, Boolean.toString(deleteAnnotationsMissingTerms), null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "orphanAnnotationChecker", "Orphan Annotation Checker");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("OrphanAnnotationChecker", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runUpgradeData() {
        try {
            for(Subject subject : EJBFactory.getLocalComputeBean().getSubjects()) {
            	String subjectKey = subject.getKey();
                logger.info("Queuing upgrade pipelines for "+subjectKey);
                runUpgradeUserData(subjectKey);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runUpgradeUserData(String user) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "upgradeUserData", "Upgrade User Data");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("UpgradeUserData", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runUpgradeSingleSample(String sampleEntityId) {
        try {
            Entity sample = EJBFactory.getLocalEntityBean().getEntityById(sampleEntityId);
            if (sample==null) throw new IllegalArgumentException("Entity with id "+sampleEntityId+" does not exist");
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter("sample entity id", sampleEntityId, null));
            Task task = new GenericTask(new HashSet<Node>(), sample.getOwnerKey(), new ArrayList<Event>(),
                    taskParameters, "upgradeSingleSample", "Upgrade Single Sample");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("UpgradeSingleSample", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void annexEntityTree(String subjectKey, String entityId) {
        EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
        try {
            entityBean.annexEntityTree(subjectKey, new Long(entityId));
        }
        catch (ComputeException e) {
            // Already printed by the ComputeBean
        }
    }

    public void runFlyScreenPipeline(String user, Boolean refresh) {
        try {
            String topLevelFolderName = FlyScreenDiscoveryService.SCREEN_SAMPLE_TOP_LEVEL_FOLDER_NAME;
            String inputDirList = "/nobackup/jacs/jacsData/filestore/system/ScreenStaging";
            Task task = new FileDiscoveryTask(new HashSet<Node>(),
                    user, new ArrayList<Event>(), new HashSet<TaskParameter>(),
                    inputDirList, topLevelFolderName, refresh);
            task.setJobName("FlyLight Screen File Discovery Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FlyLightScreen", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runFlyScreenPatternAnnotationPipeline(String user, Boolean refresh) {
        try {
            String topLevelFolderName = ScreenSampleLineCoordinationService.SCREEN_PATTERN_TOP_LEVEL_FOLDER_NAME;
            Task task = new FlyScreenPatternAnnotationTask(new HashSet<Node>(),
                    user, new ArrayList<Event>(), new HashSet<TaskParameter>(),
                    topLevelFolderName, refresh);
            task.setJobName("FlyLight Screen Pattern Annotation Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FlyScreenPatternAnnotation", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @Deprecated this method creates ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH attributes, which are deprecated.
     */
    @Deprecated
    public void performScreenPipelineSurgery(String username) {
        try {
            final EntityBeanLocal entityBean = EJBFactory.getLocalEntityBean();
            Set<Entity> entities=entityBean.getEntitiesByName("FlyLight Screen Data");
            Set<Entity> userEntities=new HashSet<Entity>();
            for (Entity e : entities) {
                if (e.getOwnerKey().equals(username)) {
                    userEntities.add(e);
                }
            }

            if (userEntities.size()==1) {
                Entity topEntity=entities.iterator().next();
                logger.info("Found top-level entity name="+topEntity.getName()+" , now changing attributes");
                Entity screenTree=entityBean.getEntityTree(topEntity.getId());

                EntityUtils.replaceAllAttributeTypesInEntityTree(screenTree, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE, EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH, new EntityUtils.SaveUnit() {
                    public void saveUnit(Object o) throws Exception {
                        EntityData ed=(EntityData)o;
                        logger.info("Saving attribute change for ed="+ed.getId());
                        entityBean.saveOrUpdateEntityData(ed);
                    }
                }
                );
                logger.info("Done replacement, now saving");
                entityBean.saveOrUpdateEntity(topEntity);
                logger.info("Done with save");
            } else {
                logger.error("Found more than one top-level entity - this is unexpected - exiting");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runFileTreeLoaderPipeline(String user, String rootDirectoryPath, String topLevelFolderName) {
        try {
            Task task = new FileTreeLoaderPipelineTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    new HashSet<TaskParameter>(), rootDirectoryPath, topLevelFolderName);
            task.setJobName("File Tree Loader Pipeline Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("FileTreeLoader", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runTicPipeline(String user, String dirOfInputFile, String inputFileName, String transformationMatrixFile,
                               String borderValue, String correctionFile, String microscopeSettingsFile){
        try {
            SingleTicTask task = new SingleTicTask(new HashSet<Node>(), user, new ArrayList<Event>(), new HashSet<TaskParameter>());
            task.setParameter(SingleTicTask.PARAM_inputFilePrefix, "/groups/jacs/jacsShare/saffordTest/tic/StackSeries20Copy.tif");
            task.setParameter(SingleTicTask.PARAM_transformationFile, "/groups/jacs/jacsShare/saffordTest/tic/PSF3stack.mat");
            task.setParameter(SingleTicTask.PARAM_intensityCorrectionFactorFile, "/groups/jacs/jacsShare/saffordTest/tic/kpos-int-cor.mat");
            task.setJobName("Transcription Imaging Consortium Pipeline Task");
            task = (SingleTicTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("TranscriptionImagingConsortium", task.getObjectId());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createPatternAnnotationQuantifierSummaryFile() {
        try {
            Map<Entity, Map<String, Double>> entityQuantifierMap=EJBFactory.getLocalAnnotationBean().getPatternAnnotationQuantifiers();
            //PatternAnnotationDataManager.createPatternAnnotationQuantifierSummaryFile(entityQuantifierMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createMaskSummaryFile(String maskFolderName) {
        logger.info("createMaskSummaryFile : maskFolderName="+maskFolderName);
        try {
            Map<Entity, Map<String, Double>> entityQuantifierMap=EJBFactory.getLocalAnnotationBean().getMaskQuantifiers(maskFolderName);
            MaskAnnotationDataManager maskManager=new MaskAnnotationDataManager();
            String resourceDirString= SystemConfigurationProperties.getString("FileStore.CentralDir")+
                    SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
            String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
            File summaryFile=new File(resourceDirString + File.separator+maskFolderName, quantifierSummaryFilename);
            File nameIndexFile=new File(resourceDirString + File.separator+maskFolderName, "maskNameIndex.txt");
            maskManager.loadMaskCompartmentList(nameIndexFile.toURI().toURL());
            maskManager.createMaskAnnotationQuantifierSummaryFile(summaryFile, entityQuantifierMap);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMaskAnnotationPipeline(String user, String maskFolderName, Boolean refresh) {
        logger.info("runMaskAnnotationPipeline maskFolderName="+maskFolderName);
        try {
            Task task = new MaskSampleAnnotationTask(new HashSet<Node>(),
                    user, new ArrayList<Event>(), new HashSet<TaskParameter>(),
                    maskFolderName, refresh);
            task.setJobName("Mask Annotation Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            logger.info("runMaskAnnotationPipeline: submitting job MaskSampleAnnotation maskFolderName="+maskFolderName);
            EJBFactory.getLocalComputeBean().submitJob("MaskSampleAnnotation", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runMaskGuideService(String user, String maskFolderName, Boolean refresh) {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            taskParameters.add(new TaskParameter(MaskGuideService.PARAM_maskFolderName, maskFolderName, null));
            taskParameters.add(new TaskParameter(MaskGuideService.PARAM_refresh, refresh.toString(), null));
            Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(),
                    taskParameters, "runMaskGuideService", "Mask Guide Service");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("MaskGuide", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runSlowImportTask(String parentDirPath, String topLevelFolderName, String owner) {
        try {
            String process = "FileTreeLoader";
            File parentDir = new File(parentDirPath);
            if (parentDir.isDirectory() && null!=parentDir.listFiles()) {
                File[] childDirs = parentDir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                for (File file : childDirs) {
                    FileTreeLoaderPipelineTask task = new FileTreeLoaderPipelineTask(new HashSet<Node>(),
                            owner, new ArrayList<Event>(), new HashSet<TaskParameter>(), file.getAbsolutePath(), topLevelFolderName);
                    task.setJobName("Import Files Task");
                    task = (FileTreeLoaderPipelineTask) EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
                    EJBFactory.getLocalComputeBean().submitJob(process, task.getObjectId());
                    Thread.sleep(120000);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void runDataDeDuplication() {
        FileWriter writer;
        try {
            writer = new FileWriter(new File("/groups/jacs/jacsShare/saffordTest/OutputTanyaDuplicates.txt"));
            Scanner scanner = new Scanner(new File("/groups/jacs/jacsShare/saffordTest/TanyaDuplicates.csv"));
            TreeMap<Integer,ArrayList<SampleRun>> systemMap = new TreeMap<Integer, ArrayList<SampleRun>>();
            TreeMap<Integer,ArrayList<SampleRun>> wolfftMap = new TreeMap<Integer, ArrayList<SampleRun>>();
            while (scanner.hasNextLine()) {
                final String[] pieces = scanner.nextLine().split("\t");
                Integer tmpKey = Integer.valueOf(pieces[2]);
                ArrayList<SampleRun> tmpList;
                SampleRun newSample = new SampleRun(Long.valueOf(pieces[1]),pieces[0]);
                TreeMap<Integer, ArrayList<SampleRun>> targetMap;
                if (pieces[0].equals("wolfft")) {
                    targetMap = wolfftMap;
                }
                else if (pieces[0].equals("system")) {
                    targetMap = systemMap;
                }
                else {
                    System.out.printf("Unknown owner found for sample!!!!!!!!!!! - "+pieces[0]);
                    writer.write("Unknown owner found for sample!!!!!!!!!!! - "+pieces[0]+"\n");
                    continue;
                }
                if (targetMap.containsKey(tmpKey)) {
                    tmpList = targetMap.get(tmpKey);
                    tmpList.add(newSample);
                }
                else {
                    tmpList = new ArrayList<SampleRun>();
                    tmpList.add(newSample);
                    targetMap.put(tmpKey, tmpList);
                }
            }
            for (Integer index : wolfftMap.keySet()) {
                System.out.println(index);
                for (SampleRun sampleRun : wolfftMap.get(index)) {
                    System.out.println("\t- "+sampleRun.getOwner()+" - "+sampleRun.getFragmentCollection());
                    writer.write("\t- "+sampleRun.getOwner()+" - "+sampleRun.getFragmentCollection()+"\n");
                    printAnnotations(writer, sampleRun.getOwner(), sampleRun.getFragmentCollection());
                    Set<Entity> tmpChildren = EJBFactory.getLocalEntityBean().getChildEntities(sampleRun.getFragmentCollection());
                    for (Entity child: tmpChildren) {
                        printAnnotations(writer, sampleRun.getOwner(), child.getId());
                    }
                }
                if (null!=systemMap.get(index)) {
                    for (SampleRun sampleRun : systemMap.get(index)) {
                        System.out.println("\t- "+sampleRun.getOwner()+" - "+sampleRun.getFragmentCollection());
                        writer.write("\t- "+sampleRun.getOwner()+" - "+sampleRun.getFragmentCollection()+"\n");
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (ComputeException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printAnnotations(FileWriter tmpWriter, String tmpOwner, long entityId) throws ComputeException, IOException {
        List<Entity> tmpAnnotationList = EJBFactory.getRemoteAnnotationBean().getAnnotationsForChildren(tmpOwner, entityId);
        for (Entity entity : tmpAnnotationList) {
            System.out.println("\t\t- "+entity.getName()+"\t"+entity.getEntityTypeName());
            tmpWriter.write("\t\t- "+entity.getName()+"\t"+entity.getEntityTypeName()+"\n");
        }
    }

    @Override
    /**
     * Marked for death
     * @deprecated
     */
    public void create3DTileMicroscopeSamples(String user, String destinationFolderName) {
        try {
            // Two main areas for data
            String[] rootPaths = new String[]{"/groups/mousebrainmicro/mousebrainmicro/render"};
            EntityBeanRemote e = EJBFactory.getRemoteEntityBean();
            // Parameters
            String subjectKey = "user:"+user;

            // Main script
            Set<Entity> folders = e.getEntitiesByName(subjectKey, destinationFolderName);
            Entity folder;
            if (folders!=null && folders.size()>0) {
                folder = folders.iterator().next();
            }
            else {
                folder = newEntity(destinationFolderName, EntityConstants.TYPE_FOLDER, subjectKey, true);
                folder = e.saveOrUpdateEntity(subjectKey, folder);
            }

            // Loop through the main areas and pull out the data directories.  Create entities for them if necessary
            for (String rootPath : rootPaths) {
                File[] rootPathDataDirs = (new File(rootPath)).listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                });
                for (File tmpData : rootPathDataDirs) {
                    // If they exist do nothing
                    Set<Entity> testFolders = e.getEntitiesByName(subjectKey, tmpData.getName());
                    if (null!=testFolders && testFolders.size()>0) continue;
                    // else add in the new data
                    Entity sample = newEntity(tmpData.getName(), EntityConstants.TYPE_3D_TILE_MICROSCOPE_SAMPLE, subjectKey, false);
                    sample.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, tmpData.getAbsolutePath());
                    sample = e.saveOrUpdateEntity(subjectKey, sample);
                    System.out.println("Saved sample as "+sample.getId());
                    e.addEntityToParent(subjectKey, folder.getId(), sample.getId(), folder.getMaxOrderIndex() + 1, EntityConstants.ATTRIBUTE_ENTITY);
                }
            }
        }
        catch (Exception ex) {
            logger.error("Error running pipeline", ex);
        }
    }

    public Entity newEntity(String name, String entityTypeName, String ownerKey, boolean isCommonRoot) throws ComputeException {
        Date createDate = new Date();
        Entity entity = new Entity();
        entity.setName(name);
        entity.setOwnerKey(ownerKey);
        entity.setCreationDate(createDate);
        entity.setUpdatedDate(createDate);
        entity.setEntityTypeName(entityTypeName);
        if (isCommonRoot) {
            entity.setValueByAttributeName(EntityConstants.ATTRIBUTE_COMMON_ROOT, "Common Root");
            Entity workspace = EJBFactory.getLocalEntityBean().getDefaultWorkspace(ownerKey);
            EJBFactory.getLocalEntityBean().addRootToWorkspace(ownerKey, workspace.getId(), entity.getId());
        }
        return entity;
    }
    
    public void addChildFolder(String parentId, String folderName) {
        
        try {
            EntityBeanLocal e = EJBFactory.getLocalEntityBean();
            Entity parent = e.getEntityById(null, Long.parseLong(parentId));
            
            Entity folder = newEntity(folderName, EntityConstants.TYPE_FOLDER, parent.getOwnerKey(), false);
            folder = e.saveOrUpdateEntity(parent.getOwnerKey(), folder);

            e.addEntityToParent(folder.getOwnerKey(), parent.getId(), folder.getId(), null, EntityConstants.ATTRIBUTE_ENTITY);
        }
        catch (Exception ex) {
            logger.error("Error running addChildFolder", ex);
        }
    }
    
    public void runBenchmarks() {
        try {
            EntityBeanLocal e = EJBFactory.getLocalEntityBean();
            AnnotationBeanLocal a = EJBFactory.getLocalAnnotationBean();
//
//            long start = System.currentTimeMillis();
//            int commonRootCount = a.getCommonRootEntities("group:leetlab").size();
//            logger.info("getCommonRootEntities('group:leetlab') took "+(System.currentTimeMillis()-start)+" ms and returned "+commonRootCount);
//            
//            start = System.currentTimeMillis();
//            e.grantPermissions("group:leetlab", 1759767174932594786L, "user:rokickik", "r", true);
//            logger.info("grantPermissions('TZL_stg14-Hey01328_Y1') took "+(System.currentTimeMillis()-start)+" ms");
//            
//            start = System.currentTimeMillis();
//            e.revokePermissions("group:leetlab", 1759767174932594786L, "user:rokickik", true);
//            logger.info("revokePermissions('TZL_stg14-Hey01328_Y1') took "+(System.currentTimeMillis()-start)+" ms");
//
//            start = System.currentTimeMillis();
//            int num = countTree(1803555221738094690L); // Count the number of items in the "Pan Lineage 40x" tree
//            logger.info("countTree('Pan Lineage 40x') took "+(System.currentTimeMillis()-start)+" ms and returned "+num);
//            
//            logger.info("getProjectedResults(Sample->LSM Stack) ...");
//            
//            Long retiredDataId = 1870629090470396002L;
//            String subjectKey = "group:heberleinlab";
//            
//            start = System.currentTimeMillis();
//            Map<Long,Entity> entityMap = new HashMap<Long,Entity>();
//            List<Long> entityIds = new ArrayList<Long>();
//            for(Entity child : e.getChildEntities("group:heberleinlab", retiredDataId)) {
//                entityIds.add(child.getId());
//                entityMap.put(child.getId(), child);
//            }
//            logger.info("1) getting original entity set ("+entityIds.size()+" ids) took "+(System.currentTimeMillis()-start)+" ms");
//            
//            start = System.currentTimeMillis();
//            List<String> upMapping = new ArrayList<String>();
//            List<String> downMapping = new ArrayList<String>();
//            downMapping.add("Supporting Data");
//            downMapping.add("Image Tile");
//            downMapping.add("LSM Stack");
//            List<MappedId> mappings = e.getProjectedResults(subjectKey, entityIds, upMapping, downMapping);
//            logger.info("2) mapping "+entityIds.size()+" ids took "+(System.currentTimeMillis()-start)+" ms");
//
//            start = System.currentTimeMillis();
//            int i = 0;
//            for(MappedId mappedId : mappings) {
//                Entity original = entityMap.get(mappedId.getOriginalId());
//                Entity mapped = e.getEntityById(subjectKey, mappedId.getMappedId());
//                logger.info(original.getName()+" -> "+mapped.getName());
//                i++;
//            }
//            logger.info("3) retrieval "+i+" ids took "+(System.currentTimeMillis()-start)+" ms");
//            
//            start = System.currentTimeMillis();
//            int count = countTree(retiredDataId);
//            logger.info("4) count entity tree returned "+count+" and took "+(System.currentTimeMillis()-start)+" ms");
//            
//            start = System.currentTimeMillis();
//            e.deleteEntityTreeById(subjectKey, retiredDataId);
//            logger.info("5) deletion of entity tree took "+(System.currentTimeMillis()-start)+" ms");
            
        	// 

//
//            //logger.info("!!!!!!!!! get folder: getEntityById("+1988022805484011618L+")"); // 3 queries 
//            Entity rootFolder = e.getEntityById(1889491952735354978L);
//          
//            //logger.info("!!!!!!!!! load folder chidlren: loadLazyEntity("+1988022805484011618L+")"); // 7 queries
//            e.loadLazyEntity(rootFolder, false);
//                        
//            long start = System.currentTimeMillis();
//            
//            for(Entity sample : rootFolder.getOrderedChildren()) {
//                //logger.info("!!!!!!!!! load sample: loadLazyEntity("+sample.getId()+")"); // 2 queries
//                e.loadLazyEntity(sample, false);
//            	
//            	String qiScore = null;
//            	
//            	for(Entity subsample : sample.getChildren()) {
//            		if (!subsample.getEntityTypeName().equals("Sample")) {
//            			continue;
//            		}
//            		String objective = subsample.getValueByAttributeName(EntityConstants.ATTRIBUTE_OBJECTIVE);
//            		if (objective!=null && objective.equals("20x")) {
//            			qiScore = getQiScore(e, subsample);
//            		}
//            	}
//            	
//            	if (qiScore == null) {
//            		qiScore = getQiScore(e, sample);
//            	}
//            	
//            	logger.info("Sample "+sample.getName()+" = "+qiScore);
//            }
//            
//            logger.info("Getting nascar icons took "+(System.currentTimeMillis()-start)+" ms");
            

			
            Long root = 1957959676293283929L;

			long start = System.currentTimeMillis();
			e.grantPermissions("user:nerna", root, "user:rokickik", "r", true);
			logger.info("grantPermissions('LC21_samples') took "
					+ (System.currentTimeMillis() - start) + " ms");

			start = System.currentTimeMillis();
			e.revokePermissions("user:nerna", root, "user:rokickik", true);
			logger.info("revokePermissions('LC21_samples') took "
					+ (System.currentTimeMillis() - start) + " ms");

		} catch (Exception ex) {
            logger.error("Error running runBenchmarks", ex);
        }
    }

    /*
     @Deprecated going to the validation engine code instead. LLF
     */
    private String getQiScore(EntityBeanLocal e, Entity sample) throws Exception {

    	String qiScore = null;
        //logger.info("!!!!!!!!! load sub sample: loadLazyEntity("+subsample.getId()+")"); // 2 queries
        e.loadLazyEntity(sample, false);
    
		Entity run = EntityUtils.getLatestChildOfType(sample, EntityConstants.TYPE_PIPELINE_RUN);                       
		if (run!=null) {
            //logger.info("!!!!!!!!! load run: loadLazyEntity("+run.getId()+")"); // 2 queries
            e.loadLazyEntity(run, false);
			
            Entity alignment = EntityUtils.findChildWithName(run, "JBA Alignment");
			if (alignment!=null) {
				//logger.info("!!!!!!!!! load alignment: loadLazyEntity("+alignment.getId()+")"); // 2 queries
                e.loadLazyEntity(alignment, false);
                
				Entity d3i = alignment.getChildByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_3D_IMAGE);
				if (d3i!=null) {
					qiScore = d3i.getValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE);
				}
			}
			
		}
		
		return qiScore;
    }
    
    private int countTree(Long entityId) throws Exception {
        EntityBeanLocal e = EJBFactory.getLocalEntityBean();
        Entity entity = e.getEntityById(entityId);
        int i = 1;
        for(Entity child : entity.getChildren()) {
            i += countTree(child.getId());
        }
        return i;
    }
    
    public void runNernaRetiredDataCleanup() {
        try {
            HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
            Task task = new GenericTask(new HashSet<Node>(), "nerna", new ArrayList<Event>(),
                    taskParameters, "cleanupRetiredData", "Cleanup Retired Data");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("CleanupRetiredData", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
