package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.base.Splitter;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;

import com.google.common.collect.ImmutableList;

/**
 * Discovers the SAGE samples associated with the given LSM and makes sage loader tasks to
 * process the corresponding samples.
 */
public class LSMSampleInitService extends AbstractEntityService {

    private String datasetName;

    public void execute() throws Exception {
        datasetName = processData.getString("DATASET_NAME");
        List<String> lsmNames = ImmutableList.copyOf(
                Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split((String) processData.getMandatoryItem("LSM_NAMES")));
        logger.info("Sage loader processing for " + ownerKey + "'s " + datasetName);

//        SageDAO sageDao = new SageDAO(logger);

        DataSet dataSet = getDataSet();

//        Multimap<String, SlideImage> imagesGroupedBySlideCode = LinkedListMultimap.create();
        // Sage only-ever should see its own version of the image name.
//        Map<String,String> jacsImageNameToSageImageName = new HashMap<>();
//        Map<String,SageBean> slideCodeToSageBean = new HashMap<>();
//        for (String lsmName : lsmNames) {
//            try {
//                SlideImage sageImageInfo = sageDao.getSlideImageByDatasetAndLSMName(datasetName, lsmName);
//                if (sageImageInfo == null) {
//                    String message = "LSM: " + lsmName + " unknown to sage. Not processing.";
//                    fail(message);
//                }
//                SageBean sageBean = new SageBean(sageImageInfo.getLab(), sageImageInfo.getLine());
//
//                boolean populatedInSage = sageImageInfo.getImagePath() != null;
//                /* *** TEMP *** */
//                if (populatedInSage) {
//                    logger.info("Successfully detected completion in sage " + lsmName);
//                }
//                /* *** End Temp *** */
//
//                if (1 == 1/* *** TEMP *** */  ||  (!populatedInSage)) {
//                    String jacsLsmName = convertToJacsFormat(lsmName);
//                    jacsImageNameToSageImageName.put(jacsLsmName, lsmName);
//                    String slideCode = sageImageInfo.getSlideCode();
//                    SageBean assocWithSlideCode = slideCodeToSageBean.get(slideCode);
//                    if (assocWithSlideCode != null && !assocWithSlideCode.equals(sageBean)) {
//                        throw new DaoException("Multiple sage values for slide code " + slideCode + ", seeing " + sageBean + ", and " + assocWithSlideCode);
//                    }
//                    slideCodeToSageBean.put(slideCode, sageBean);
//                    imagesGroupedBySlideCode.put(slideCode, sageImageInfo);
//                }
//            } catch (DaoException e) {
//                logger.warn("Error while retrieving image for " + lsmName, e);
//            }
//        }

        if (lsmNames.isEmpty()) {
            String message = "No LSMs were found. Not spawning any Sage loading tasks. Aborting pipeline.";
            fail(message);
        }
        else {
//            List<Task> sageLoadingTasks = getSageLoaderTasksForCurrentDataset(imagesGroupedBySlideCode, dataSet, jacsImageNameToSageImageName, slideCodeToSageBean);
            // All LSMs are supposed belong to the same Data Set so Index them in one shot.
            List<Task> sageLoadingTasks = new ArrayList<>();
            // Forcing lab to flylight for now.
            sageLoadingTasks.add(createSageLoaderTask(task.getOwner(), dataSet.getSageConfigPath(), dataSet.getSageGrammarPath(), "flylight", null, lsmNames));
            processData.putItem("SAGE_TASK", sageLoadingTasks);
        }
    }

    private void fail(String message) throws Exception {
        logger.error(message);
        task.addMessage(message);
        computeBean.addEventToTask(task.getObjectId(), new Event(message, new Date(), Event.ERROR_EVENT));
        throw new Exception(message);
    }

    private DataSet getDataSet() throws Exception {
        DomainDAL domainDAL = DomainDAL.getInstance();
        DataSet dataSet = domainDAL.getDataSetByIdentifier(ownerKey, datasetName);
        if (dataSet == null) {
            List<DataSet> dataSets = domainDAL.getUserDomainObjectsByName(ownerKey, DataSet.class, datasetName);
            if (dataSets == null) {
                throw new Exception("Failed to find dataset by name: " + datasetName);
            }
            else if (!(dataSets.size() == 1)) {
                for (DataSet ds: dataSets) {
                    logger.info("Name=" + ds.getName()+", Owner=" + ds.getOwnerName() +  ", ID=" + ds.getId() + ", Identifier=" + ds.getIdentifier());
                }
                throw new Exception(dataSets.size() + " datasets found for name " + datasetName);
            }
            else {
                dataSet = dataSets.get(0);
            }
        }
        return dataSet;
    }

//    private List<Task> getSageLoaderTasksForCurrentDataset(Multimap<String, SlideImage> imagesGroupedBySlideCode,
//                                                           DataSet dataset,
//                                                           Map<String,String> jacsImageNameToSageImageName,
//                                                           Map<String,SageBean> slideCodeToSageBean) {
//        List<Task> sageLoadingTasks = new ArrayList<>();
//        String owner = extractOwnerId(dataset.getOwnerKey());
//        processData.putItem("DATASET_OWNER", owner);
//        String configPath = dataset.getSageConfigPath();
//        String grammarPath = dataset.getSageGrammarPath();
//
//        for (final String slideCode: imagesGroupedBySlideCode.keySet()) {
//            try {
//                Collection<SlideImage> slideImages = imagesGroupedBySlideCode.get(slideCode);
//                SageBean sageBean = slideCodeToSageBean.get(slideCode);
//                String line = sageBean.getLine();
//                String lab = sageBean.getLab();
//                List<String> sageImageNames = getSageImageNames(slideCode, slideImages, jacsImageNameToSageImageName);
//                SageLoaderTask sageLoaderTask = createSageLoaderTask(owner, configPath, grammarPath, lab, line, sageImageNames);
//                sageLoadingTasks.add(sageLoaderTask);
//
//            } catch (Exception e) {
//                logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
//            }
//        }
//        return sageLoadingTasks;
//    }

//    private List<String> getSageImageNames(
//            final String slideCode,
//            Collection<SlideImage> slideImages,
//            final Map<String,String> jacsImageNameToSageImageName) {
//
//        return FluentIterable
//                            .from(slideImages)
//                            .filter(new Predicate<SlideImage>() {
//                                @Override
//                                public boolean apply(@Nullable SlideImage image) {
//                                    if (image.getName() != null && image.getName().length() > 0) {
//                                        return true;
//                                    } else {
//                                        logger.warn("Invalid image name encountered for " + slideCode + " " + image.getLine());
//                                        return false;
//                                    }
//                                }
//                            })
//                            .transform(new Function<SlideImage, String>() {
//                                @Nullable
//                                @Override
//                                public String apply(SlideImage image) {
//                                    String jacsImageName = image.getName();
//                                    String sageName = jacsImageNameToSageImageName.get(jacsImageName);
//                                    if (sageName == null) {
//                                        // As long as the mapping ALWAYS has the translation, this should never happen.
//                                        throw new RuntimeException("No Sage back-translation exists for " + jacsImageName);
//                                    }
//                                    else {
//                                        return sageName;
//                                    }
//                                }
//                            })
//                            .toImmutableList();
//    }

    private SageLoaderTask createSageLoaderTask(String owner, String configPath, String grammarPath, String lab, String line, List<String> slideImageNames) throws DaoException {
        SageLoaderTask sageLoaderTask = new SageLoaderTask(owner,
                new ArrayList<Event>(),
                slideImageNames,
                line,
                configPath,
                grammarPath,
                lab,
                "true",
                null);
        sageLoaderTask.setParentTaskId(task.getObjectId());
        computeBean.saveOrUpdateTask(sageLoaderTask);
        logger.info("Created SageLoaderTask " + sageLoaderTask.getObjectId());
        return sageLoaderTask;
    }

//    private String getLineCheckLab(Collection<LSMImage> slideImages) {
//        String line = null;
//        String lab = null;
//        int tileNum = 0;
//        for (LSMImage lsmImage : slideImages) {
//            String thisImageLab = lsmImage.getFlycoreLabId();
//            String thisImageLine = lsmImage.getLine();
//            if (lab == null) {
//                line = thisImageLine;
//                lab = thisImageLab;
//            } else if (!lab.equals(thisImageLab)) {
//                logger.warn("Lab value for " + lsmImage.getName() + " - " + thisImageLab
//                        + "  does not match " + lab);
//            }
//            if (line == null) {
//                line = thisImageLine;
//            } else if (!line.equals(thisImageLine)) {
//                logger.warn("Line value for " + lsmImage.getName() + " - " + thisImageLine
//                        + "  does not match " + line);
//            }
//            tileNum++;
//        }
//        logger.info("Processed tile count of :" + tileNum);
//        return line;
//    }

//    private String convertToJacsFormat(String sageFormat) {
//        String rtnVal = sageFormat;
//        int slashPos = sageFormat.indexOf('/');
//        if (slashPos > -1) {
//            rtnVal = sageFormat.substring(slashPos + 1);
//        }
//        return rtnVal;
//    }
//
//    /** Convenience container for values coming fom sage database. */
//    private static class SageBean {
//        private String lab;
//        private String line;
//        public SageBean(String lab, String line) {
//            this.lab = lab;
//            this.line = line;
//            if (lab == null  ||  line == null) {
//                throw new IllegalArgumentException("Construct with non-null values.");
//            }
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (o == null  ||  !(o instanceof SageBean)) {
//                return false;
//            }
//            else {
//                SageBean otherBean = (SageBean) o;
//                return otherBean.toString().equals(toString());
//            }
//        }
//
//        @Override
//        public int hashCode() {
//            return toString().hashCode();
//        }
//
//        /** toString : used as a pivotal equality-check value. */
//        @Override
//        public String toString() {
//            return getLab() + " " + getLine();
//        }
//
//        public String getLab() {
//            return lab;
//        }
//
//        public String getLine() {
//            return line;
//        }
//    }
}
