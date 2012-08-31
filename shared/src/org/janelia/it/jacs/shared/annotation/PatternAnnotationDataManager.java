package org.janelia.it.jacs.shared.annotation;

import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/21/12
 * Time: 10:22 AM
 * To change this template use File | Settings | File Templates.
 */

public abstract class PatternAnnotationDataManager implements Serializable {

    private Logger logger = Logger.getLogger(PatternAnnotationDataManager.class);

    public static final int STATE_UNDEFINED=0;
    public static final int STATE_STARTING=1;
    public static final int STATE_LOADING=2;
    public static final int STATE_READY=3;
    public static final int STATE_ERROR=4;

    // QS stands for "Quality Summary"
    public static final String QS_NAME_COL = "ScreenSampleName";
    public static final String QS_SAMPLE_ID_COL = "ScreenSampleId";
    public static final String QS_HEATMAP_PATH_COL = "HeatmapImagePath";
    public static final String QS_GLOBAL_PREFIX = "Global";

    public static final List<String> QS_COMPARTMENT_LIST = new ArrayList<String>();
    public static final Map<String,String> QS_DESCRIPTION_MAP = new HashMap<String, String>();

    static {
        QS_COMPARTMENT_LIST.add("AL_L"); QS_DESCRIPTION_MAP.put("AL_L", "Antennal lobe L");
        QS_COMPARTMENT_LIST.add("AL_R"); QS_DESCRIPTION_MAP.put("AL_R", "Antennal lobe R");
        QS_COMPARTMENT_LIST.add("AME_L"); QS_DESCRIPTION_MAP.put("AME_L", "Accessory Medulla L");
        QS_COMPARTMENT_LIST.add("AME_R"); QS_DESCRIPTION_MAP.put("AME_R", "Accessory Medulla R");
        QS_COMPARTMENT_LIST.add("AMMC_L"); QS_DESCRIPTION_MAP.put("AMMC_L", "Antennal Mechanosensory Motor Center L");
        QS_COMPARTMENT_LIST.add("AMMC_R"); QS_DESCRIPTION_MAP.put("AMMC_R", "Antennal Mechanosensory Motor Center R");
        QS_COMPARTMENT_LIST.add("ATL_L"); QS_DESCRIPTION_MAP.put("ATL_L", "Antler L");
        QS_COMPARTMENT_LIST.add("ATL_R"); QS_DESCRIPTION_MAP.put("ATL_R", "Antler R");
        QS_COMPARTMENT_LIST.add("AVLP_L"); QS_DESCRIPTION_MAP.put("AVLP_L", "Anterior Ventrolateral Protocerebrum L");
        QS_COMPARTMENT_LIST.add("AVLP_R"); QS_DESCRIPTION_MAP.put("AVLP_R", "Anterior Ventrolateral Protocerebrum R");
        QS_COMPARTMENT_LIST.add("CRE_L"); QS_DESCRIPTION_MAP.put("CRE_L", "Crepine R");
        QS_COMPARTMENT_LIST.add("CRE_R"); QS_DESCRIPTION_MAP.put("CRE_R", "Crepine L");
        QS_COMPARTMENT_LIST.add("EB"); QS_DESCRIPTION_MAP.put("EB", "Ellipsoid Body");
        QS_COMPARTMENT_LIST.add("EPA_L"); QS_DESCRIPTION_MAP.put("EPA_L", "Epaulette L");
        QS_COMPARTMENT_LIST.add("EPA_R"); QS_DESCRIPTION_MAP.put("EPA_R", "Epaulette R");
        QS_COMPARTMENT_LIST.add("FB"); QS_DESCRIPTION_MAP.put("FB", "Fan-Shaped Body");
        QS_COMPARTMENT_LIST.add("FLA"); QS_DESCRIPTION_MAP.put("FLA", "Flange");
        QS_COMPARTMENT_LIST.add("GA_L"); QS_DESCRIPTION_MAP.put("GA_L", "Gall L");
        QS_COMPARTMENT_LIST.add("GA_R"); QS_DESCRIPTION_MAP.put("GA_R", "Gall R");
        QS_COMPARTMENT_LIST.add("GOR_L"); QS_DESCRIPTION_MAP.put("GOR_L", "Gorget L");
        QS_COMPARTMENT_LIST.add("GOR_R"); QS_DESCRIPTION_MAP.put("GOR_R", "Gorget R");
        QS_COMPARTMENT_LIST.add("IB_L"); QS_DESCRIPTION_MAP.put("IB_L", "Inferior Bridge L");
        QS_COMPARTMENT_LIST.add("IB_R"); QS_DESCRIPTION_MAP.put("IB_R", "Inferior Bridge R");
        QS_COMPARTMENT_LIST.add("ICL_L"); QS_DESCRIPTION_MAP.put("ICL_L", "Inferior Clamp L");
        QS_COMPARTMENT_LIST.add("ICL_R"); QS_DESCRIPTION_MAP.put("ICL_R", "Inferior Clamp R");
        QS_COMPARTMENT_LIST.add("IPS_L"); QS_DESCRIPTION_MAP.put("IPS_L", "Inferior Posterior Slope L");
        QS_COMPARTMENT_LIST.add("IPS_R"); QS_DESCRIPTION_MAP.put("IPS_R", "Inferior Posterior Slope R");
        QS_COMPARTMENT_LIST.add("IVLP_L"); QS_DESCRIPTION_MAP.put("IVLP_L", "Inferior Ventrolateral Protocerebrum L");
        QS_COMPARTMENT_LIST.add("IVLP_R"); QS_DESCRIPTION_MAP.put("IVLP_R", "Inferior Ventrolateral Protocerebrum R");
        QS_COMPARTMENT_LIST.add("LAL_L"); QS_DESCRIPTION_MAP.put("LAL_L", "Lateral Accessory Lobe L");
        QS_COMPARTMENT_LIST.add("LAL_R"); QS_DESCRIPTION_MAP.put("LAL_R", "Lateral Accessory Lobe L");
        QS_COMPARTMENT_LIST.add("LB_L"); QS_DESCRIPTION_MAP.put("LB_L", "Labium L");
        QS_COMPARTMENT_LIST.add("LB_R"); QS_DESCRIPTION_MAP.put("LB_R", "Labium R");
        QS_COMPARTMENT_LIST.add("LH_L"); QS_DESCRIPTION_MAP.put("LH_L", "Lateral Horn L");
        QS_COMPARTMENT_LIST.add("LH_R"); QS_DESCRIPTION_MAP.put("LH_R", "Lateral Horn R");
        QS_COMPARTMENT_LIST.add("LOP_L"); QS_DESCRIPTION_MAP.put("LOP_L", "Lobula Plate L");
        QS_COMPARTMENT_LIST.add("LOP_R"); QS_DESCRIPTION_MAP.put("LOP_R", "Lobula Plate R");
        QS_COMPARTMENT_LIST.add("LO_L"); QS_DESCRIPTION_MAP.put("LO_L", "Lobula L");
        QS_COMPARTMENT_LIST.add("LO_R"); QS_DESCRIPTION_MAP.put("LO_R", "Lobula R");
        QS_COMPARTMENT_LIST.add("MB_L"); QS_DESCRIPTION_MAP.put("MB_L", "Mushroom Body L");
        QS_COMPARTMENT_LIST.add("MB_R"); QS_DESCRIPTION_MAP.put("MB_R", "Mushroom Body R");
        QS_COMPARTMENT_LIST.add("ME_L"); QS_DESCRIPTION_MAP.put("ME_L", "Medulla L");
        QS_COMPARTMENT_LIST.add("ME_R"); QS_DESCRIPTION_MAP.put("ME_R", "Medulla R");
        QS_COMPARTMENT_LIST.add("NO");   QS_DESCRIPTION_MAP.put("NO", "Nodulus");
        QS_COMPARTMENT_LIST.add("OTU_L"); QS_DESCRIPTION_MAP.put("OTU_L", "Optic Tubercle L");
        QS_COMPARTMENT_LIST.add("OTU_R"); QS_DESCRIPTION_MAP.put("OTU_R", "Optic Tubercle R");
        QS_COMPARTMENT_LIST.add("PB"); QS_DESCRIPTION_MAP.put("PB", "Protocerebral Bridge");
        QS_COMPARTMENT_LIST.add("PLP_L"); QS_DESCRIPTION_MAP.put("PLP_L", "Posterior Lateral Protocerebrum L");
        QS_COMPARTMENT_LIST.add("PLP_R"); QS_DESCRIPTION_MAP.put("PLP_R", "Posterior Lateral Protocerebrum R");
        QS_COMPARTMENT_LIST.add("PRW"); QS_DESCRIPTION_MAP.put("PRW", "Prow");
        QS_COMPARTMENT_LIST.add("PVLP_L"); QS_DESCRIPTION_MAP.put("PVLP_L", "Posterior Ventrolateral Protocerebrum L");
        QS_COMPARTMENT_LIST.add("PVLP_R"); QS_DESCRIPTION_MAP.put("PVLP_R", "Posterior Ventrolateral Protocerebrum R");
        QS_COMPARTMENT_LIST.add("SAD"); QS_DESCRIPTION_MAP.put("SAD", "Saddle");
        QS_COMPARTMENT_LIST.add("SCL_L"); QS_DESCRIPTION_MAP.put("SCL_L", "Superior Clamp L");
        QS_COMPARTMENT_LIST.add("SCL_R"); QS_DESCRIPTION_MAP.put("SCL_R", "Superior Clamp R");
        QS_COMPARTMENT_LIST.add("SIP_L"); QS_DESCRIPTION_MAP.put("SIP_L", "Superior Intermediate Protocerebrum L");
        QS_COMPARTMENT_LIST.add("SIP_R"); QS_DESCRIPTION_MAP.put("SIP_R", "Superior Intermediate Protocerebrum R");
        QS_COMPARTMENT_LIST.add("SLP_L"); QS_DESCRIPTION_MAP.put("SLP_L", "Superior Lateral Protocerebrum L");
        QS_COMPARTMENT_LIST.add("SLP_R"); QS_DESCRIPTION_MAP.put("SLP_R", "Superior Lateral Protocerebrum R");
        QS_COMPARTMENT_LIST.add("SMP_L"); QS_DESCRIPTION_MAP.put("SMP_L", "Superior Medial Protocerebrum L");
        QS_COMPARTMENT_LIST.add("SMP_R"); QS_DESCRIPTION_MAP.put("SMP_R", "Superior Medial Protocerebrum R");
        QS_COMPARTMENT_LIST.add("SOG"); QS_DESCRIPTION_MAP.put("SOG", "Subesophageal Ganglion");
        QS_COMPARTMENT_LIST.add("SPS_L"); QS_DESCRIPTION_MAP.put("SPS_L", "Superior Posterior Slope L");
        QS_COMPARTMENT_LIST.add("SPS_R"); QS_DESCRIPTION_MAP.put("SPS_R", "Superior Posterior Slope R");
        QS_COMPARTMENT_LIST.add("VES_L"); QS_DESCRIPTION_MAP.put("VES_L", "Vest L");
        QS_COMPARTMENT_LIST.add("VES_R"); QS_DESCRIPTION_MAP.put("VES_R", "Vest R");
        QS_COMPARTMENT_LIST.add("WED_L"); QS_DESCRIPTION_MAP.put("WED_L", "Wedge L");
        QS_COMPARTMENT_LIST.add("WED_R"); QS_DESCRIPTION_MAP.put("WED_R", "Wedge R");

    }

    public List<String> getCompartmentListInstance() {
        List<String> compartmentListInstance = new ArrayList<String>();
        for (String c : QS_COMPARTMENT_LIST) {
            compartmentListInstance.add(c);
        }
        return compartmentListInstance;
    }

    public static String getCompartmentDescription(String key) {
        return QS_DESCRIPTION_MAP.get(key);
    }

    protected List<DataDescriptor> descriptorList = new ArrayList<DataDescriptor>();

    protected Map<DataDescriptor, Map<Long, List<Float>>> descriptorScoreMap = new HashMap<DataDescriptor, Map<Long, List<Float>>>();
    //protected Map<DataDescriptor, Map<Long, String>> descriptorNameMap = new HashMap<DataDescriptor, Map<Long, String>>();

    ///////////////////////////////////////////////////////////////////////////

    abstract protected void populateScores() throws Exception;

    abstract public String getDataManagerType();

    ///////////////////////////////////////////////////////////////////////////

    public List<DataDescriptor> getDataDescriptors() {
        return descriptorList;
    }

    public Map<Long, List<Float>> getScoreMapByDescriptor(DataDescriptor dataDescriptor) {
        return descriptorScoreMap.get(dataDescriptor);
    }

//    public Map<Long, String> getNameMapByDescriptor(DataDescriptor dataDescriptor) {
//        return descriptorNameMap.get(dataDescriptor);
//    }

    public void setup() throws Exception {
        populateScores();
    }

    protected void computePercentiles(Map<Long, List<Float>> scoreMap, Float scaleMin, Float scaleMax) {

        class PercentileScore implements Comparable {

            public Long sampleId;
            public Float score;

            @Override
            public int compareTo(Object o) {
                PercentileScore other=(PercentileScore)o;
                if (score > other.score) {
                    return 1;
                } else if (score < other.score) {
                    return -1;
                } else {
                    return 0;
                }
            }
        }

        List<String> compartmentAbbreviationList=getCompartmentListInstance();
        List<PercentileScore> sortList = new ArrayList<PercentileScore>();

        int compartmentIndex=0;
        for (String compartmentAbbreviation : compartmentAbbreviationList) {
            sortList.clear();
            for (Long sampleId : scoreMap.keySet()) {
                List<Float> intensityList = scoreMap.get(sampleId);
                PercentileScore ps=new PercentileScore();
                ps.sampleId=sampleId;
                ps.score=intensityList.get(compartmentIndex);
                sortList.add(ps);
            }
            Collections.sort(sortList);
            float listLength=sortList.size()-1.0f;

            float index=0.0f;
            for (PercentileScore ps : sortList) {
                float sortScore = index / listLength;
                sortScore=(sortScore*(scaleMax-scaleMin))+scaleMin;
                List<Float> scoreList=scoreMap.get(ps.sampleId);
                scoreList.set(compartmentIndex, sortScore);
                index+=1.0;
            }
            compartmentIndex++;
        }
    }

    public FilterResult getFilteredResults(Map<DataDescriptor, Set<DataFilter>> filterMap) {
        List<String> compartmentList=getCompartmentListInstance();
        Map<String, Integer> compartmentIndex=new HashMap<String, Integer>();
        for (int i=0;i<compartmentList.size();i++) {
            compartmentIndex.put(compartmentList.get(i), i);
        }
        Set<DataDescriptor> dataDescriptors=filterMap.keySet();
        List<Long> sampleResultSet=new ArrayList<Long>();
        Map<String, Long> countMap=new HashMap<String, Long>();
        for (DataDescriptor d : dataDescriptors) {
            Map<Long, List<Float>> sampleMap=descriptorScoreMap.get(d);
            Set<Long> samples=sampleMap.keySet();
            Set<DataFilter> filterSet=filterMap.get(d);
            List<DataFilter> filterList=new ArrayList<DataFilter>();
            filterList.addAll(filterSet);
            List<Float> minList=new ArrayList<Float>();
            List<Float> maxList=new ArrayList<Float>();
            long[] countArray=new long[filterList.size()];
            int i=0;
            for (DataFilter f : filterList) {
                countArray[i]=0;
                minList.add(f.getMin());
                maxList.add(f.getMax());
                i++;
            }
            for (Long sampleId : samples) {
                List<Float> scores=sampleMap.get(sampleId);
                int fi=0;
                int filterListSize=filterList.size();
                Long excludeCount=0L;
                while(fi<filterListSize) {
                    DataFilter f = filterList.get(fi);
                    String filterName=f.getName();
                    int index=compartmentIndex.get(filterName);
                    float score=scores.get(index);
                    float min=minList.get(fi);
                    float max=maxList.get(fi);
                    if (score<min || score>max) {
                        excludeCount++;
                    } else {
                        countArray[fi]++;
                    }
                    fi++;
                }
                if (excludeCount==0) {
                    sampleResultSet.add(sampleId);
                }
            }
            for (i=0;i<filterList.size();i++) {
                String name=filterList.get(i).getName();
                countMap.put(name, (countMap.get(name)==null?0:countMap.get(name))+countArray[i]);
            }
        }
        return new FilterResult(countMap, sampleResultSet);
    }

}
