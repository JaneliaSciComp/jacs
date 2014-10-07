package org.janelia.it.jacs.compute.service.activeData.scanner;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.service.activeData.*;
import org.janelia.it.jacs.model.entity.EntityConstants;

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

    public AlignmentSampleScanner() {
        super();
    }

    public AlignmentSampleScanner(List<VisitorFactory> visitorFactoryList) {
        super(visitorFactoryList);
    }

    @Override
    public long[] generateIdList(Object dataResource) throws Exception {
        long[] idArr=generateIdListByEntityType(dataResource, EntityConstants.TYPE_SAMPLE);

        // FOR DEBUGGING, KEEP LIST SHORT
        if (idArr.length<1000) {
            return idArr;
        } else {
            long[] shortArr=new long[1000];
            List<Long> picked=new ArrayList<>();
            Random rand = new Random();
            while(picked.size()<1000) {
                int guess = rand.nextInt(idArr.length);
                Long value=idArr[guess];
                if (!picked.contains(value)) {
                    picked.add(value);
                }
            }
            for (int i=0;i<1000;i++) {
                shortArr[i]=picked.get(i);
            }
            return shortArr;
        }

    }


    public static class SampleInfo {
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
        public Float qualityCentral;
        public Float qualityLeftOpticLobe;
        public Float qualityRightOpticLobe;
        public AlignedStackInfo alignedStackInfo;
        public NeuronSeparationInfo neuronSeparationInfo;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("  ALIGNMENT_RESULT id="+id+"\n");
            sb.append("    SPACE="+spaceDescriptor+"\n");
            sb.append("    QI_BY_CSV="+qiByCsv+"\n");
            sb.append("    QUAL_BY_PROP_NCC="+qualByPropNcc+"\n");
            sb.append("    QUALITY_CENTRAL="+qualityCentral+"\n");
            sb.append("    QUALITY_LEFT_OL="+qualityLeftOpticLobe+"\n");
            sb.append("    QUALITY_RIGHT_OL="+qualityRightOpticLobe+"\n");
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

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("    ALIGNED_STACK id="+id+"\n");
            sb.append("      PATH="+path+"\n");
            sb.append("      OPTICAL_RESOLUTION="+opticalResolution+"\n");
            sb.append("      PIXEL_RESOLUTION="+pixelResolution+"\n");
            return sb.toString();
        }
    }

    public static class NeuronSeparationInfo {
        public Long id;
        public Integer neuronCount;

        public String toString() {
            StringBuilder sb=new StringBuilder();
            sb.append("    NEURON_SEPARATION id="+id+"\n");
            sb.append("      NEURON_COUNT="+neuronCount+"\n");
            return sb.toString();
        }
    }

    @Override
    public void preEpoch(ActiveDataScan scan) throws Exception {
        logger.info("preEpoch() called");
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

        for (Long entityId : orderedEntityIdList) {
            List<ActiveDataEntityEvent> eventList = eventMap.get(entityId);
            for (ActiveDataEntityEvent event : eventList) {
                Object eventData = event.getData();
                if (eventData!=null) {
                    if (eventData instanceof SampleInfo) {
                        bw.append(eventData.toString());
                    }
                }
            }
        }

        bw.close();
    }

}
