package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.*;
import org.janelia.it.jacs.compute.access.DaoException;
import org.janelia.it.jacs.compute.access.SageDAO;
import org.janelia.it.jacs.compute.access.domain.DomainDAL;
import org.janelia.it.jacs.compute.service.entity.AbstractEntityService;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LSMImage;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.utility.SageLoaderTask;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

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

        SageDAO sageDao = new SageDAO(logger);

        DomainDAL domainDAL = DomainDAL.getInstance();
        DataSet dataSet = domainDAL.getDataSetByIdentifier(ownerKey, datasetName);
        Multimap<String, LSMImage> imagesGroupedBySlideCode = LinkedListMultimap.create();
        for (String lsmName : lsmNames) {
            try {
                Object sageVersion = sageDao.getSlideImageByDatasetAndLSMName(datasetName, lsmName);
                if (sageVersion == null) {
                    List<LSMImage> images = domainDAL.getDomainObjectsByName(ownerKey, LSMImage.class, lsmName);
                    if (images.size() > 1) {
                        logger.warn("Multiple LSMs named " + lsmName + " under owner " + ownerKey);
                    }
                    else if (images.size() == 0) {
                        logger.warn("No LSM named " + lsmName + " under owner " + ownerKey);
                    }
                    else {
                        LSMImage lsmImage = images.iterator().next();
                        imagesGroupedBySlideCode.put(lsmImage.getSlideCode(), lsmImage);
                    }
                }
            } catch (DaoException e) {
                logger.warn("Error while retrieving image for " + lsmName, e);
            }
        }

        List<Task> sageLoadingTasks = getSageLoaderTasksForCurrentDataset(imagesGroupedBySlideCode, dataSet);

        processData.putItem("SAGE_TASK", sageLoadingTasks);
    }

    // formerly: prepareSlideImageGroupsForCurrentDataset
    private List<Task> getSageLoaderTasksForCurrentDataset(Multimap<String, LSMImage> imagesGroupedBySlideCode,
                                                     DataSet dataset) {
        List<Task> sageLoadingTasks = new ArrayList<>();
        String owner = extractOwnerId(dataset.getOwnerKey());
        processData.putItem("DATASET_OWNER", owner);
        String configPath = dataset.getSageConfigPath();
        String grammarPath = dataset.getSageGrammarPath();

        for (final String slideCode: imagesGroupedBySlideCode.keySet()) {
            try {
                Collection<LSMImage> slideImages = imagesGroupedBySlideCode.get(slideCode);
                String[] labAndLine = getLabAndLine(slideImages);
                List<String> slideImageNames = getSlideImageNames(slideCode, slideImages);
                SageLoaderTask sageLoaderTask = createSageLoaderTask(owner, configPath, grammarPath, labAndLine, slideImageNames);
                sageLoadingTasks.add(sageLoaderTask);

            } catch (Exception e) {
                logger.error("Error while preparing image groups for  " + datasetName + ": " + slideCode, e);
            }
        }
        return sageLoadingTasks;
    }

    private List<String> getSlideImageNames(final String slideCode, Collection<LSMImage> slideImages) {
        return FluentIterable
                            .from(slideImages)
                            .filter(new Predicate<LSMImage>() {
                                @Override
                                public boolean apply(@Nullable LSMImage image) {
                                    if (image.getName() != null && image.getName().length() > 0) {
                                        return true;
                                    } else {
                                        logger.warn("Invalid image name encountered for " + slideCode + " " + image.getLine());
                                        return false;
                                    }
                                }
                            })
                            .transform(new Function<LSMImage, String>() {
                                @Nullable
                                @Override
                                public String apply(LSMImage image) {
                                    return image.getName();
                                }
                            })
                            .toImmutableList();
    }

    private SageLoaderTask createSageLoaderTask(String owner, String configPath, String grammarPath, String[] labAndLine, List<String> slideImageNames) throws DaoException {
        SageLoaderTask sageLoaderTask = new SageLoaderTask(owner,
                new ArrayList<Event>(),
                slideImageNames,
                labAndLine[1],
                configPath,
                grammarPath,
                labAndLine[0],
                "true",
                null);
        sageLoaderTask.setParentTaskId(task.getObjectId());
        computeBean.saveOrUpdateTask(sageLoaderTask);
        logger.info("Created SageLoaderTask " + sageLoaderTask.getObjectId());
        return sageLoaderTask;
    }

    private String[] getLabAndLine(Collection<LSMImage> slideImages) {
        String line = null;
        String lab = null;
        int tileNum = 0;
        for (LSMImage lsmImage : slideImages) {
            String thisImageLab = lsmImage.getFlycoreLabId();
            String thisImageLine = lsmImage.getLine();
            if (lab == null) {
                line = thisImageLine;
                lab = thisImageLab;
            } else if (!lab.equals(thisImageLab)) {
                logger.warn("Lab value for " + lsmImage.getName() + " - " + thisImageLab
                        + "  does not match " + lab);
            }
            if (line == null) {
                line = thisImageLine;
            } else if (!line.equals(thisImageLine)) {
                logger.warn("Line value for " + lsmImage.getName() + " - " + thisImageLine
                        + "  does not match " + line);
            }
            tileNum++;
        }
        logger.info("Processed tile count of :" + tileNum);
        return new String[] {lab, line};
    }
}
