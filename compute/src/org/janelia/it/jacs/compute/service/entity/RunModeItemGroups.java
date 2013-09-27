package org.janelia.it.jacs.compute.service.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Grouped items for a particular run mode.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunModeItemGroups {

	private static final Logger logger = Logger.getLogger(RunModeItemGroups.class);
	
    public static final int GROUP_SIZE = 200;
    
	private String runMode;
	private List<List> groupList = new ArrayList<List>();
	
	public RunModeItemGroups(String runMode, Collection fullList) {
		this.runMode = runMode;
		createGroups(fullList, GROUP_SIZE);
		logger.info("Processed "+fullList.size()+" "+runMode+" entities into "+groupList.size()+" groups.");
	}
	
    private List<List> createGroups(Collection fullList, int groupSize) {
        List currentGroup = null;
        for (Object s : fullList) {
            if (currentGroup==null) {
                currentGroup = new ArrayList();
            } 
            else if (currentGroup.size()==groupSize) {
                groupList.add(currentGroup);
                currentGroup = new ArrayList();
            }
            currentGroup.add(s);
        }
        if (currentGroup!=null && currentGroup.size() > 0) {
            groupList.add(currentGroup);
        }
        return groupList;
    }

	public String getRunMode() {
		return runMode;
	}

	public List<List> getGroupList() {
		return groupList;
	}    
}
