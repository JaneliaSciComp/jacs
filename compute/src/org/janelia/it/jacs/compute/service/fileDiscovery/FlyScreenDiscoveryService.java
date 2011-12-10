package org.janelia.it.jacs.compute.service.fileDiscovery;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.entity.EntityType;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 11/21/11
 * Time: 11:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlyScreenDiscoveryService extends FileDiscoveryService {

    // In this discovery service, we do not want to create an entity tree which looks like the
    // filesystem. Instead, we want to traverse a filesystem tree and create a flat set of
    // entities which we discover at any position in the tree.

    // We start by assuming that only the initial Entity corresponding to the top-level directory
    // has been created and persisted.

    Entity topFolder;

    protected static class FlyScreenSample {
        public String StackPath;
        public String QualityCsvPath;

        public FlyScreenSample() {}

        static String getKeyFromStackName(String stackName) {
            String[] cArr = stackName.split("\\.reg\\.local");
            return cArr[0];
        }

        static String getKeyFromQualityCsvName(String csvName) {
            String[] cArr = csvName.split("\\.quality\\.csv");
            return cArr[0];
        }
    }

    @Override
    protected void processFolderForData(Entity folder) throws Exception {

        if (topFolder==null) {
            topFolder=folder;
        } else {
            throw new Exception("Expected only a single call to processFolderForData() - topFolder should be null at this point");
        }

        File dir = new File(folder.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH));
        logger.info("Processing folder="+dir.getAbsolutePath()+" id="+folder.getId());

        if (!dir.canRead()) {
        	logger.info("Cannot read from folder "+dir.getAbsolutePath());
        	return;
        }

        // We need to know the pre-existing set of Screen Samples, so we can detect new ones
        Set<String> currentSceenSamples=new HashSet<String>();
        for (EntityData ed : topFolder.getEntityData()) {
            Entity child = ed.getChildEntity();
            if (child != null && child.getEntityType().getName().equals(EntityConstants.TYPE_SCREEN_SAMPLE)) {
                currentSceenSamples.add(child.getName());
            }
        }

        processFlyLightScreenDirectory(dir, currentSceenSamples);
    }

    protected void processFlyLightScreenDirectory(File dir, Set<String> currentScreenSamples) throws Exception {

        // First, find the new sample stack and quality files in the directory
        List<File> fileList=getOrderedFilesInDir(dir);
        Map<String, FlyScreenSample> sampleMap=new HashMap<String, FlyScreenSample>();

        for (File file : fileList) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".reg.local.v3dpbd")) {
                    String key=FlyScreenSample.getKeyFromStackName(file.getName());
                    if (!currentScreenSamples.contains(key)) {
                        FlyScreenSample sample=sampleMap.get(key);
                        if (sample==null) {
                            sample=new FlyScreenSample();
                            sampleMap.put(key, sample);
                        }
                        sample.StackPath=file.getAbsolutePath();
                    }
                } else if (file.getName().endsWith(".quality.csv")) {
                    String key=FlyScreenSample.getKeyFromQualityCsvName(file.getName());
                    if (!currentScreenSamples.contains(key)) {
                        FlyScreenSample sample=sampleMap.get(key);
                        if (sample==null) {
                            sample=new FlyScreenSample();
                            sampleMap.put(key, sample);
                        }
                        sample.QualityCsvPath=file.getAbsolutePath();
                    }
                }
            } else {
                processFlyLightScreenDirectory(file, currentScreenSamples);
            }
        }

        // Next, create the new samples
        EntityType screenSampleType=annotationBean.getEntityTypeByName(EntityConstants.TYPE_SCREEN_SAMPLE);
        for (String key : sampleMap.keySet()) {
            FlyScreenSample screenSample = sampleMap.get(key);
            Entity screenSampleEntity = new Entity();
            screenSampleEntity.setCreationDate(createDate);
            screenSampleEntity.setUpdatedDate(createDate);
            screenSampleEntity.setUser(user);
            screenSampleEntity.setName(key);
            screenSampleEntity.setEntityType(screenSampleType);
            screenSampleEntity = annotationBean.saveOrUpdateEntity(screenSampleEntity);
            logger.info("Created new Screen Sample " + key + " id=" + screenSampleEntity.getId());
            addToParent(topFolder, screenSampleEntity, null, EntityConstants.ATTRIBUTE_ENTITY);
            String[] alignmentScores = getAlignmentScoresFromQualityFile(screenSample.QualityCsvPath);
            addImageEntitiesToScreenSample(screenSampleEntity, screenSample, alignmentScores);
        }

    }

    String[] getAlignmentScoresFromQualityFile(String filepath) {
        String[] scoreArray=new String[2];
        try {
            BufferedReader reader=new BufferedReader(new FileReader(filepath));
            String descriptionLine=reader.readLine();
            String scoreLine=reader.readLine();
            String[] scoreArr2=scoreLine.split(",");
            scoreArray[0]=scoreArr2[0].trim();
            scoreArray[1]=scoreArr2[1].trim();
            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.getMessage());
            scoreArray[0]="0.0";
            scoreArray[1]="0.0";
        }
        return scoreArray;
    }

    protected void addImageEntitiesToScreenSample(Entity screenSampleEntity, FlyScreenSample screenSample,
                                                  String[] alignmentScores) throws Exception {
        Entity alignedStack = new Entity();
        alignedStack.setUser(user);
        alignedStack.setEntityType(annotationBean.getEntityTypeByName(EntityConstants.TYPE_ALIGNED_BRAIN_STACK));
        alignedStack.setCreationDate(createDate);
        alignedStack.setUpdatedDate(createDate);
        alignedStack.setName(screenSampleEntity.getName()+" aligned stack");
        alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH, screenSample.StackPath);
        alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QI_SCORE, alignmentScores[0]);
        alignedStack.setValueByAttributeName(EntityConstants.ATTRIBUTE_ALIGNMENT_QM_SCORE, alignmentScores[1]);
        alignedStack = annotationBean.saveOrUpdateEntity(alignedStack);
        addToParent(screenSampleEntity, alignedStack, null, EntityConstants.ATTRIBUTE_ENTITY);
        logger.info("Saved stack " + alignedStack.getName() + " as "+alignedStack.getId());
    }


}
