package org.janelia.it.jacs.compute.service.entity.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.compute.service.domain.SampleHelperNG;
import org.janelia.it.jacs.compute.service.entity.AbstractDomainService;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.sample.DataSet;
import org.janelia.it.jacs.model.domain.sample.LineRelease;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.workspace.ObjectSet;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.joda.time.DateTime;
import org.joda.time.Period;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Synchronize the folder hierarchy for a Fly Line Release. 
 *   
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SyncReleaseFoldersService extends AbstractDomainService {
	
    private SampleHelperNG sampleHelper;
    private LineRelease release;
    private TreeNode topLevelFolder;
    private TreeNode releaseFolder;
    private Multimap<String,Sample> samplesByLine = ArrayListMultimap.<String,Sample>create();
    
    public void execute() throws Exception {
        
        this.sampleHelper = new SampleHelperNG(computeBean, ownerKey, logger);
        
    	Long releaseId = data.getRequiredItemAsLong("RELEASE_ENTITY_ID");
    	this.release = domainDao.getDomainObject(ownerKey, LineRelease.class, releaseId);
        if (release == null) {
            throw new IllegalArgumentException("Release not found with id="+releaseId);
        }
        logger.info("Release: "+release);
        
        DateTime releaseDate = new DateTime(release.getReleaseDate());
        logger.info("Release date: "+releaseDate);
        
        DateTime cutoffDate = null;
        Integer lagTime = release.getLagTimeMonths();
        if (lagTime != null) {
            cutoffDate = releaseDate.minus(Period.months(lagTime));
            logger.info("Cutoff date: "+cutoffDate);
        }
        
    	this.topLevelFolder = sampleHelper.createOrVerifyRootEntity(ownerKey, DomainConstants.NAME_FLY_LINE_RELEASES, true);
    	logger.info("Top level folder: "+topLevelFolder);
    	
    	this.releaseFolder = sampleHelper.createOrVerifyChildFolder(topLevelFolder, release.getName(), true);
    	logger.info("Release folder: "+releaseFolder);
    	
    	Set<String> includedDataSets = new HashSet<String>(release.getDataSets());
    	
    	List<DataSet> dataSets = new ArrayList<>();
    	for(DataSet dataSet : domainDao.getDataSets(ownerKey)) {
    	    if (includedDataSets.isEmpty() || includedDataSets.contains(dataSet.getIdentifier())) {
    	        dataSets.add(dataSet);
    	    }
    	}
    	
    	int samplesAdded = 0;
    	for(DataSet dataSetEntity : dataSets) {
    	    String identifier = dataSetEntity.getIdentifier();
    	    logger.info("Processing data set "+identifier);
    	    for(Sample sample : domainDao.getSamplesForDataSet(ownerKey, identifier)) {
                logger.info("  Processing sample "+sample.getName());
	            Date completionDate = sample.getCompletionDate();
	            if (completionDate!=null) {
	                String status = sample.getStatus();
	                if (DomainConstants.VALUE_BLOCKED.equals(status)) {
	                    logger.info("    Sample is blocked");
	                }
	                else if (DomainConstants.VALUE_RETIRED.equals(status)) {
	                    logger.info("    Sample is retired");
	                }
	                else {
	                    String line = sample.getLine();
	                    if (line==null) {
	                        logger.warn("    Cannot process sample without line: "+sample.getId());
	                        continue;
	                    }
    	                if (cutoffDate==null || cutoffDate.isAfter(new DateTime(completionDate))) {
    	                    samplesByLine.put(line, sample);
    	                    logger.info("    Adding sample to line: "+line);
    	                    samplesAdded++;
    	                }
    	                else {
    	                    logger.info("    Sample completed after cutoff date: "+completionDate);
    	                }
	                }
	            }
	            else {
	                logger.info("    Sample has no completion date");
	            }
    	    }
    	}

        logger.info("Considering "+samplesByLine.keySet().size()+" fly lines, with "+samplesAdded+" samples");
    	
        int numAdded = 0;
    	List<String> lines = new ArrayList<>(samplesByLine.keySet());
    	Collections.sort(lines);
    	for(String line : lines) {

            List<Sample> samples = new ArrayList<>(samplesByLine.get(line));
            logger.info("Processing line "+line+" with "+samples.size()+" samples");
            
            // Ensure there is at least one 63x polarity sample

            boolean has63xPolaritySample = false;
            for(Sample sample : samples) {
                if (has63xPolaritySample(sample)) {
                    has63xPolaritySample = true;
                    break;
                }
            }
    	    
            if (!has63xPolaritySample) {
                logger.info("  Ignoring line which has no 63x polarity samples");
                continue;
            }
            
            ObjectSet lineFolder = verifyOrCreateChildFolder(releaseFolder, line);
    	    
    	    // Sort samples
    	    Collections.sort(samples, new Comparator<Sample>() {
                @Override
                public int compare(Sample o1, Sample o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });

            logger.info("  Adding samples to line folder");
            
    	    // Add missing samples
    	    for(Sample sample : samples) {
    	        logger.debug("    Processing sample "+sample.getName());
    	        if (!lineFolder.getMembers().contains(sample.getId())) {
                    logger.debug("      Adding to line folder: "+lineFolder.getName()+" (id="+lineFolder.getId()+")");
                    domainDao.addMembers(ownerKey, lineFolder, Arrays.asList(Reference.createFor(sample)));
                    numAdded++;
    	        }
    	    }
    	    
    	    // Re-sort line folder
    	    sampleHelper.sortMembersByName(lineFolder);
    	}

        // Re-sort release folder
        sampleHelper.sortChildrenByName(releaseFolder);

        logger.info("Added "+numAdded+" samples to line folders");
        
        processData.putItem("RELEASE_FOLDER_ID", releaseFolder.getId().toString());
        contextLogger.info("Putting '"+releaseFolder.getId()+"' in RELEASE_FOLDER_ID");
    }
    
    private boolean has63xPolaritySample(Sample sample) throws Exception {

        String identifier = sample.getDataSet();
        if (identifier==null || !identifier.toLowerCase().contains("polarity")) {
            // If the parent sample is not a polarity sample then we don't need to check anything else
            return false;
        }
        
        return sample.getObjectives().keySet().contains("63x");
    }

    public ObjectSet verifyOrCreateChildFolder(TreeNode parentFolder, String childName) throws Exception {

        for(ObjectSet flyLineFolder : domainDao.getDomainObjectsAs(releaseFolder.getChildren(), ObjectSet.class)) {
            if (flyLineFolder.getName().equals(childName)) {
                return flyLineFolder;
            }
        }
        
        ObjectSet childSet = new ObjectSet();
        childSet.setName(childName);
        childSet.setClassName(Sample.class.getName());
        childSet = domainDao.save(ownerKey, childSet);
        
        domainDao.addChildren(ownerKey, parentFolder, Arrays.asList(Reference.createFor(childSet)));
        
        return childSet;
    }
}
