package org.janelia.it.jacs.compute.service.activeData.scanner;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.*;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.EntityConstants;
import org.janelia.it.jacs.model.user_data.FileNode;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * Created by murphys on 10/2/14.
 */
public class AlignmentSampleScanner extends EntityScanner {

    static final Logger logger = Logger.getLogger(AlignmentSampleScanner.class);

    public static final String SAMPLE_INFO = "SAMPLE_INFO";
    public static final String SAMPLE_ENTITY = "SAMPLE_ENTITY";

    public static final String ALIGNMENT_RESOURCE_DIR = SystemConfigurationProperties.getString("AlignmentResource.Dir");
    public final String VAA3D_PATH = SystemConfigurationProperties.getString("AlignmentResource.Vaa3dPath");
    public final String VAA3D_LIBRARY_PATH = SystemConfigurationProperties.getString("AlignmentResource.Vaa3dLibraryPath");

    public AlignmentSampleScanner() {
        super();
    }

    public AlignmentSampleScanner(List<VisitorFactory> visitorFactoryList) {
        super(visitorFactoryList);
    }

    @Override
    public long[] generateIdList(Object dataResource) throws Exception {
        long[] idArr=generateIdListByEntityType(dataResource, EntityConstants.TYPE_SAMPLE);

        int DEBUG_SIZE=10000;

        // FOR DEBUGGING, KEEP LIST SHORT
        if (idArr.length<DEBUG_SIZE) {
            return idArr;
        } else {
            long[] shortArr=new long[DEBUG_SIZE];
            List<Long> picked=new ArrayList<>();
            Random rand = new Random();
            while(picked.size()<DEBUG_SIZE) {
                int guess = rand.nextInt(idArr.length);
                Long value=idArr[guess];
                if (!picked.contains(value)) {
                    picked.add(value);
                }
            }
            for (int i=0;i<DEBUG_SIZE;i++) {
                shortArr[i]=picked.get(i);
            }
            return shortArr;
        }

    }


    public static class SampleInfo {

        public static final String SAMPLE_INFO_FILENAME = "sampleInfo.txt";

        public Long id;
        public String owner;
        public String name;
        public String lineDescriptor;
        public String lsm20xPath;
        public String preparationType;
        public List<Lsm63xInfo> lsm63xInfoList;
        public List<AlignmentResult> alignmentResultList;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("SAMPLE id="+id+"\n");
            sb.append("  OWNER="+owner+"\n");
            sb.append("  NAME="+name+"\n");
            sb.append("  LINE="+lineDescriptor+"\n");
            sb.append("  PREPARATION="+preparationType+"\n");
            sb.append("  LSM_20x_PATH="+lsm20xPath+"\n");
            if (lsm63xInfoList!=null) {
                for (Lsm63xInfo lsm63xInfo : lsm63xInfoList) {
                    sb.append(lsm63xInfo.toString());
                }
            }
            if (alignmentResultList!=null) {
                for (AlignmentResult alignmentResult : alignmentResultList) {
                    sb.append(alignmentResult.toString());
                }
            }
            return sb.toString();
        }

        public String getResourcePath(String resourceRootPath) {
            String sampleOffsetPath= FileNode.getTreePathForId(id);
            File sampleDir=new File(resourceRootPath+"/"+"samples"+"/"+sampleOffsetPath);
            File sampleInfoFile=new File(sampleDir, SAMPLE_INFO_FILENAME);
            return sampleInfoFile.getAbsolutePath();
        }
    }

    public static class Lsm63xInfo {
        public String locationDescription;
        public String path;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("  LSM_63x_STACK description="+locationDescription+"\n");
            sb.append("    PATH="+path+"\n");
            return sb.toString();
        }
    }

    public static class AlignmentResult {
        public Long id;
        public String spaceDescriptor;
        public Float qiByCsv;
        public Float qualByPropNcc;
        public AlignedStackInfo alignedStackInfo;
        public NeuronSeparationInfo neuronSeparationInfo;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("  ALIGNMENT_RESULT id="+id+"\n");
            sb.append("    SPACE="+spaceDescriptor+"\n");
            sb.append("    QI_BY_CSV="+qiByCsv+"\n");
            sb.append("    QUAL_BY_PROP_NCC="+qualByPropNcc+"\n");
            if (alignedStackInfo!=null) {
                sb.append(alignedStackInfo.toString());
            }
            if (neuronSeparationInfo!=null) {
                sb.append(neuronSeparationInfo.toString());
            }
            return sb.toString();
        }
    }

    public static class AlignedStackInfo {
        public Long id;
        public String path;
        public String opticalResolution;
        public String pixelResolution;
        public Float alignmentInconsistencyScore;
        public Float alignmentInconsistencyScore_0;
        public Float alignmentInconsistencyScore_1;
        public Float alignmentIncosistencyScore_2;
        public Float alignmentModelViolationScore;
        public Float alignmentNccScore;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("    ALIGNED_STACK id="+id+"\n");
            sb.append("      PATH="+path+"\n");
            sb.append("      OPTICAL_RESOLUTION="+opticalResolution+"\n");
            sb.append("      PIXEL_RESOLUTION="+pixelResolution+"\n");
            sb.append("      INCONSISTENCY_SCORE="+alignmentInconsistencyScore+"\n");
            sb.append("      INCONSISTENCY_SCORE_0="+alignmentInconsistencyScore_0+"\n");
            sb.append("      INCONSISTENCY_SCORE_1="+alignmentInconsistencyScore_1+"\n");
            sb.append("      INCONSISTENCY_SCORE_2="+alignmentIncosistencyScore_2+"\n");
            sb.append("      MODEL_VIOLATION_SCORE="+alignmentModelViolationScore+"\n");
            sb.append("      NCC_SCORE="+alignmentNccScore+"\n");
            return sb.toString();
        }
    }

    public static class NeuronFragmentInfo {
        public Long id;
        public int index;
        public String maskPath;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("      FRAGMENT id="+id+"\n");
            sb.append("        INDEX="+index+"\n");
            sb.append("        MASK_PATH="+maskPath+"\n");
            return sb.toString();
        }
    }

    public static class NeuronSeparationInfo {
        public Long id;
        public Integer neuronCount;
        public String consolidatedLabelPath;
        public String consolidatedSignalPath;
        public List<NeuronFragmentInfo> fragmentInfoList;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("    NEURON_SEPARATION id="+id+"\n");
            sb.append("      NEURON_COUNT="+neuronCount+"\n");
            sb.append("      CONSOLIDATED_LABEL="+consolidatedLabelPath+"\n");
            sb.append("      CONSOLIDATED_SIGNAL="+consolidatedSignalPath+"\n");
            if (fragmentInfoList!=null && fragmentInfoList.size()>0) {
                for (NeuronFragmentInfo fragmentInfo : fragmentInfoList) {
                    sb.append(fragmentInfo.toString());
                }
            }
            return sb.toString();
        }
    }

    @Override
    public void preEpoch(ActiveDataScan scan) throws Exception {
        logger.info("preEpoch() called");
        if (ALIGNMENT_RESOURCE_DIR!=null && ALIGNMENT_RESOURCE_DIR.length()>0) {

            // Alignment Resource Dir
            logger.info("Checking "+ALIGNMENT_RESOURCE_DIR);
            FileUtil.ensureDirExists(ALIGNMENT_RESOURCE_DIR);

            // Sample Dir
            File sampleDir = new File(ALIGNMENT_RESOURCE_DIR+"/"+"samples");
            logger.info("Checking "+sampleDir.getAbsolutePath());
            FileUtil.ensureDirExists(sampleDir.getAbsolutePath());

            // Neuron Index Dir
            File neuronIndexDir = new File(ALIGNMENT_RESOURCE_DIR+"/"+"neuron-index");
            logger.info("Checking "+neuronIndexDir.getAbsolutePath());
            FileUtil.ensureDirExists(neuronIndexDir.getAbsolutePath());

            // Screen Index Dir
            File screenIndexDir = new File(ALIGNMENT_RESOURCE_DIR+"/"+"screen-index");
            logger.info("Checking "+screenIndexDir.getAbsolutePath());
            FileUtil.ensureDirExists(screenIndexDir.getAbsolutePath());

            // Compartment Index Dir
            File compartmentIndexDir = new File(ALIGNMENT_RESOURCE_DIR+"/"+"compartment-index");
            logger.info("Checking "+compartmentIndexDir.getAbsolutePath());
            FileUtil.ensureDirExists(compartmentIndexDir.getAbsolutePath());

        }
    }

    @Override
    public void postEpoch(ActiveDataScan scan) throws Exception {
        File postFile = new File(scan.getScanDirectory() + "/" + "alignedSampleSummary.txt");
        logger.info("postEpoch() - output path="+postFile.getAbsolutePath());
        BufferedWriter bw=new BufferedWriter(new FileWriter(postFile));

        ActiveDataClient activeData = (ActiveDataClient) ActiveDataServerSimpleLocal.getInstance();
        Map<Long, List<ActiveDataEntityEvent>> eventMap = activeData.getEventMap(getSignature());
        Set<Long> keySet=eventMap.keySet();
        List<Long> orderedEntityIdList = new ArrayList<>();
        orderedEntityIdList.addAll(keySet);
        Collections.sort(orderedEntityIdList);

        int sampleCount=0;
        int mainIndexCount=0;
        for (Long entityId : orderedEntityIdList) {
            logger.info("Validating and adding to geometric search index entry "+sampleCount+" of "+orderedEntityIdList.size());
            List<ActiveDataEntityEvent> eventList = eventMap.get(entityId);
            for (ActiveDataEntityEvent event : eventList) {
                Object eventData = event.getData();
                if (eventData!=null) {
                    if (eventData instanceof SampleInfo) {
                        bw.append(eventData.toString());
                        SampleInfo sampleInfo = (SampleInfo)eventData;
                        if (addSampleToMainIndex(sampleInfo)) {
                            logger.info("Added "+mainIndexCount+" samples to main index");
                            mainIndexCount++;
                        } else {
                            int skipped=sampleCount-mainIndexCount;
                            logger.info("Skipped "+skipped+" samples for main index, due to missing file or error");
                        }
                    }
                }
            }
            sampleCount++;
        }

        bw.close();
    }

    private boolean addSampleToMainIndex(SampleInfo sampleInfo) {
        // First, check if it has a an index entry
        String sampleFilePath=sampleInfo.getResourcePath(ALIGNMENT_RESOURCE_DIR);
        File sampleIndexFile = new File(sampleFilePath + ".index");
        if (sampleIndexFile.exists()) {
            logger.info("Adding sample index file to index = " + sampleIndexFile.getAbsolutePath());

            File indexSpecificationFile = getIndexSpecificationFile();

            if (!indexSpecificationFile.exists()) {
                logger.error("Could not find sample specification file="+indexSpecificationFile.getAbsolutePath());
                return false;
            }

            File alignmentResourceDir=new File(ALIGNMENT_RESOURCE_DIR);
            String sampleInfoFilepath=sampleInfo.getResourcePath(alignmentResourceDir.getAbsolutePath());
            File sampleInfoFile=new File(sampleFilePath);
            File sampleDir=sampleInfoFile.getParentFile();

            SystemCall sc = new SystemCall(null /*props*/, sampleDir, logger);


            String commandString = VAA3D_PATH + " -cmd volume-index -mode index -indexSpecificationFile " +
                    indexSpecificationFile.getAbsolutePath() + " -sampleIndexFile " + sampleInfoFilepath+".index";

            if (VAA3D_LIBRARY_PATH!=null && VAA3D_LIBRARY_PATH.length()>0) {
                commandString = "export LD_LIBRARY_PATH=" + VAA3D_LIBRARY_PATH + ":$LD_LIBRARY_PATH;" + commandString;
            }

            logger.info("Executing command with system call: " + commandString);

            int exitValue = 0;

            try {
                exitValue = sc.execute(commandString, true);
            } catch (Exception ex) {
                    logger.error(ex,ex);
                    return false;
            }

            if (exitValue!=0) {
                logger.error("exit value non-zero for generation of sample index file");
                sc.cleanup();
                return false;
            }

            sc.cleanup();

       } else {
            logger.info("Could not find sample index file = " + sampleIndexFile.getAbsolutePath());
            return false;
        }
        return true;
    }

    File getIndexSpecificationFile() {
        File isf = new File(ALIGNMENT_RESOURCE_DIR, "neuron-index.spec");
        return isf;
    }


}
