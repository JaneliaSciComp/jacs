package org.janelia.it.jacs.compute.service.fly;

import java.io.File;
import java.util.*;

import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.compute.service.fileDiscovery.FileDiscoveryHelper;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.entity.EntityData;
import org.janelia.it.jacs.model.ontology.OntologyAnnotation;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.jacs.shared.utils.StringUtils;

/**
 * This readds specimens which were removed previously by ScreenScoresLoadingService3. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ScreenScoresLoadingService4 extends ScreenScoresLoadingService {
	
    private Set<String> adds = new HashSet<String>();
    private int numAdded = 0;
    
    public void execute(IProcessData processData) throws ServiceException {
    	
        try {
            logger = ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            entityBean = EJBFactory.getLocalEntityBean();
            computeBean = EJBFactory.getLocalComputeBean();
            annotationBean = EJBFactory.getLocalAnnotationBean();
            user = computeBean.getUserByName(ProcessDataHelper.getTask(processData).getOwner());
            createDate = new Date();
            helper = new FileDiscoveryHelper(entityBean, computeBean, user);
            
            // Process arguments
            
            String addsFile = (String)processData.getItem("ADDS_FILE_PATH");
        	if (addsFile == null) {
        		throw new IllegalArgumentException("ADDS_FILE_PATH may not be null");
        	}
        	
        	readAdds(new File(addsFile));

        	// Precache distribution folder ids
        	Map<String,Entity> distFolders = new HashMap<String,Entity>();
        	
        	Entity topLevelFolder = helper.getRootEntity(ScreenScoresLoadingService.TOP_LEVEL_EVALUATION_FOLDER, false);
        	populateChildren(topLevelFolder);

        	for(Entity compartment : topLevelFolder.getOrderedChildren()) {
        		populateChildren(compartment);
        		
            	for(Entity intFolder : compartment.getOrderedChildren()) {
            		populateChildren(intFolder);
            		int i = getValueFromFolderName(intFolder);
            		
                	for(Entity distFolder : intFolder.getOrderedChildren()) {
                		populateChildren(distFolder);
                		int d = getValueFromFolderName(distFolder);
                		
                		String key = compartment.getName()+"/"+i+"/"+d;
                		distFolders.put(key, distFolder);
                	}
            	}
        	}
        	
        	for(Entity sample : entityBean.getEntitiesByTypeName(EntityConstants.TYPE_SCREEN_SAMPLE)) {

        		Specimen specimen = Specimen.createSpecimenFromFullName(sample.getName());
        		if (!adds.contains(specimen.getSpecimenName())) continue;
        			
        		populateChildren(sample);
        		Entity patternAnnotation = EntityUtils.findChildWithName(sample, "Pattern Annotation");
        		if (patternAnnotation==null) continue;
        		
        		numAdded++;
        		logger.info("Readding "+sample.getName());
        		
        		Map<Long,List<OntologyAnnotation>> annotMap = new HashMap<Long,List<OntologyAnnotation>>();
        		for(Entity annotEntity : annotationBean.getAnnotationsForChildren(MAA_USERNAME, patternAnnotation.getId())) {
        			OntologyAnnotation annototation = new OntologyAnnotation();
        			annototation.init(annotEntity);
        			List<OntologyAnnotation> entityAnnots = annotMap.get(annototation.getTargetEntityId());
        			if (entityAnnots==null) {
        				entityAnnots = new ArrayList<OntologyAnnotation>();
        				annotMap.put(annototation.getTargetEntityId(), entityAnnots);
        			}
        			entityAnnots.add(annototation);
        		}
        		
        		populateChildren(patternAnnotation);
        		for(Entity mask : patternAnnotation.getChildren()) {
					String maaIntensity = null;
					String maaDistribution = null;
					String caIntensity = null;
					String caDistribution = null;
					for(OntologyAnnotation annotation : annotMap.get(mask.getId())) {
						if (ScreenScoresLoadingService.MAA_INTENSITY_NAME.equals(annotation.getKeyString())) {
							maaIntensity = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.CA_INTENSITY_NAME.equals(annotation.getKeyString())) {
							caIntensity = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.MAA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
							maaDistribution = annotation.getValueString();
						}
						else if (ScreenScoresLoadingService.CA_DISTRIBUTION_NAME.equals(annotation.getKeyString())) {
							caDistribution = annotation.getValueString();
						}
					}	
        			
					if (maaIntensity!=null && maaDistribution!=null) {
						// The current evaluation
						int mi = getValueFromAnnotation(maaIntensity);
						int md = getValueFromAnnotation(maaDistribution);
						int fi = mi;
						int fd = md;
						
						if (!StringUtils.isEmpty(caIntensity)) {
							int ci = getValueFromAnnotation(caIntensity);
							fi = ci;
						}
						
						if (!StringUtils.isEmpty(caDistribution)) {
							int cd = getValueFromAnnotation(caDistribution);
							fd = cd;
						}
						
						String key = mask.getName()+"/"+fi+"/"+fd;
						Entity targetDistFolder = (Entity)distFolders.get(key);
						populateChildren(targetDistFolder);
						
						boolean exists = false;
						for(Entity child : targetDistFolder.getChildren()) {
							if (child.getId().equals(mask.getId())) {
								exists = true;
							}
						}
						
						if (exists) {
							logger.info("  "+mask.getName()+" already exists in "+targetDistFolder.getId()+" ("+key+")");
						}
						else  {
							logger.info("  Adding "+mask.getName()+" to "+targetDistFolder.getId()+" ("+key+")");
							EntityData ed = targetDistFolder.addChildEntity(mask);
							entityBean.saveOrUpdateEntityData(ed);
						}
					}
        		}
        	}
        	
        	logger.info("Done readding "+numAdded+" specimens out of requested "+adds.size());
        } 
        catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private void readAdds(File addsFile) throws Exception {
		Scanner scanner = new Scanner(addsFile);
        try {
            while (scanner.hasNextLine()){
                String specimen = scanner.nextLine();
                adds.add(specimen);
            }
        }
        finally {
        	scanner.close();
        }
    }

	private int getValueFromAnnotation(String annotationValue) {
		return Integer.parseInt(""+annotationValue.charAt(1));
	}
	
	private int getValueFromFolderName(Entity entity) {
		return getValueFromFolderName(entity.getName());
	}
	
	private int getValueFromFolderName(String folderName) {
		return Integer.parseInt(""+folderName.charAt(folderName.length()-1));
	}
	
}
