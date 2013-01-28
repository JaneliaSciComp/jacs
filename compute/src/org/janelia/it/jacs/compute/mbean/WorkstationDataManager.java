package org.janelia.it.jacs.compute.mbean;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeException;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.api.EntityBeanLocal;
import org.janelia.it.jacs.compute.service.entity.OrphanAnnotationCheckerService;
import org.janelia.it.jacs.compute.service.fileDiscovery.FlyScreenDiscoveryService;
import org.janelia.it.jacs.compute.service.fileDiscovery.SampleRun;
import org.janelia.it.jacs.compute.service.fly.MaskGuideService;
import org.janelia.it.jacs.compute.service.fly.ScreenSampleLineCoordinationService;
import org.janelia.it.jacs.compute.service.mongodb.MongoDbLoadService;
import org.janelia.it.jacs.compute.service.solr.SolrIndexingService;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityAttribute;
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
        			taskParameters, "annotationImport", "annotationImport");
            task.setJobName("Annotation Import Pipeline Task");
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
            task.setJobName("Screen Scores Loading Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("ScreenScoresLoadingPipeline", task.getObjectId());
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
            task.setJobName("Screen Scores Export Task");
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
            task.setJobName("Split Lines Loading Task");
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
            task.setJobName("Neo4j Sync Task");
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
            task.setJobName("MongoDB Sync Task");
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
            task.setJobName("Solr Index Sync Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("SolrIndexSync", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void runNeuronMergeTest(String taskOwner, String separationEntityId, String commaSeparatedNeuronFragmentList) {
        try {
            NeuronMergeTask task = new NeuronMergeTask(new HashSet<Node>(), taskOwner, new ArrayList<org.janelia.it.jacs.model.tasks.Event>(), new HashSet<TaskParameter>());
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
    
    public void runOrphanAnnotationCheckerService(String user, Boolean deleteAnnotationsMissingTargets, Boolean deleteAnnotationsMissingTerms) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	taskParameters.add(new TaskParameter(OrphanAnnotationCheckerService.PARAM_removeAnnotationsMissingTargets, Boolean.toString(deleteAnnotationsMissingTargets), null)); 
        	taskParameters.add(new TaskParameter(OrphanAnnotationCheckerService.PARAM_removeAnnotationsMissingTerms, Boolean.toString(deleteAnnotationsMissingTerms), null)); 
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "orphanAnnotationChecker", "Orphan Annotation Checker");
            task.setJobName("Orphan Annotation Checker Task");
            task = EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);
            EJBFactory.getLocalComputeBean().submitJob("OrphanAnnotationChecker", task.getObjectId());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public void runUpgradeUserData(String user) {
        try {
        	HashSet<TaskParameter> taskParameters = new HashSet<TaskParameter>();
        	Task task = new GenericTask(new HashSet<Node>(), user, new ArrayList<Event>(), 
        			taskParameters, "upgradeUserData", "Upgrade User Data");
            task.setJobName("Update User Data Task");
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
            task.setJobName("Upgrade Single Sample Task");
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
            String inputDirList = "/groups/scicomp/jacsData/ScreenStaging";
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
                EntityAttribute pEa=entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                EntityAttribute nEa=entityBean.getEntityAttributeByName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);

                EntityUtils.replaceAllAttributeTypesInEntityTree(screenTree, pEa, nEa, new EntityUtils.SaveUnit() {
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
            task.setParameter(SingleTicTask.PARAM_inputFilePrefix, "/groups/scicomp/jacsData/saffordTest/tic/StackSeries20Copy.tif");
            task.setParameter(SingleTicTask.PARAM_transformationFile, "/groups/scicomp/jacsData/saffordTest/tic/PSF3stack.mat");
            task.setParameter(SingleTicTask.PARAM_intensityCorrectionFactorFile, "/groups/scicomp/jacsData/saffordTest/tic/kpos-int-cor.mat");
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
            String resourceDirString= SystemConfigurationProperties.getString("MaskSampleAnnotation.ResourceDir");
            String quantifierSummaryFilename= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
            File summaryFile=new File(resourceDirString + File.separator+maskFolderName, quantifierSummaryFilename);
            File nameIndexFile=new File(resourceDirString + File.separator+maskFolderName, "maskNameIndex.txt");
            maskManager.loadMaskCompartmentList(nameIndexFile);
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
            task.setJobName("Mask Guide Task");
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
            writer = new FileWriter(new File("/groups/scicomp/jacsData/saffordTest/OutputTanyaDuplicates.txt"));
            Scanner scanner = new Scanner(new File("/groups/scicomp/jacsData/saffordTest/TanyaDuplicates.csv"));
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
            System.out.println("\t\t- "+entity.getName()+"\t"+entity.getEntityType().getName());
            tmpWriter.write("\t\t- "+entity.getName()+"\t"+entity.getEntityType().getName()+"\n");
        }
    }
    
}