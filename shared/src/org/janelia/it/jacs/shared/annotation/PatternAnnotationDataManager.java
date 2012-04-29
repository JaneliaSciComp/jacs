package org.janelia.it.jacs.shared.annotation;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/21/12
 * Time: 10:22 AM
 * To change this template use File | Settings | File Templates.
 */

public class PatternAnnotationDataManager {

    // QS stands for "Quality Summary"
    public static final String QS_NAME_COL = "ScreenSampleName";
    public static final String QS_SAMPLE_ID_COL = "ScreenSampleId";
    public static final String QS_HEATMAP_PATH_COL = "HeatmapImagePath";
    public static final String QS_GLOBAL_PREFIX = "Global";

    public static final List<String> QS_COMPARTMENT_LIST = new ArrayList<String>();
    public static final List<String> QS_Z_INDEX_LIST = new ArrayList<String>();
    public static final List<String> QS_C_INDEX_LIST = new ArrayList<String>();
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

        QS_Z_INDEX_LIST.add(".z0");
        QS_Z_INDEX_LIST.add(".z1");
        QS_Z_INDEX_LIST.add(".z2");
        QS_Z_INDEX_LIST.add(".z3");
        QS_Z_INDEX_LIST.add(".z4");

        QS_C_INDEX_LIST.add(".c0");
        QS_C_INDEX_LIST.add(".c1");
        QS_C_INDEX_LIST.add(".c2");
        QS_C_INDEX_LIST.add(".c3");
        QS_C_INDEX_LIST.add(".c4");

    }
    
    public static List<String> getCompartmentListInstance() {
        List<String> compartmentListInstance = new ArrayList<String>();
        for (String c : QS_COMPARTMENT_LIST) {
            compartmentListInstance.add(c);
        }
        return compartmentListInstance;
    }

    public static File getPatternAnnotationSummaryFile() {
        String resourceDirString= SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationResourceDir");
        String quantifierSummaryFilename=SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
        File patternAnnotationSummaryFile=new File(resourceDirString, quantifierSummaryFilename);
        return patternAnnotationSummaryFile;
    }

    public static void createPatternAnnotationQuantifierSummaryFile(Map<Entity, Map<String, Double>> entityQuantifierMap) throws Exception {
        List<Entity> screenSampleKeyList=new ArrayList<Entity>();
        screenSampleKeyList.addAll(entityQuantifierMap.keySet());
        Collections.sort(screenSampleKeyList, new Comparator<Entity>() {
            @Override
            public int compare(Entity o, Entity o1) {
                return o.getName().compareTo(o1.getName());
            }
        });

        // Initialize output file
        File patternAnnotationSummaryFile=getPatternAnnotationSummaryFile();
        BufferedWriter bw=new BufferedWriter(new FileWriter(patternAnnotationSummaryFile));

        // Create header line
        bw.write(QS_NAME_COL+",");
        bw.write(QS_SAMPLE_ID_COL+",");
        bw.write(QS_HEATMAP_PATH_COL+",");
        bw.write(QS_GLOBAL_PREFIX+".t0,");
        bw.write(QS_GLOBAL_PREFIX+".t1,");
        bw.write(QS_GLOBAL_PREFIX+".t2,");
        bw.write(QS_GLOBAL_PREFIX+".t3,");
        bw.write(QS_GLOBAL_PREFIX+".z0,");
        bw.write(QS_GLOBAL_PREFIX+".z1,");
        bw.write(QS_GLOBAL_PREFIX+".z2,");
        bw.write(QS_GLOBAL_PREFIX+".z3,");
        bw.write(QS_GLOBAL_PREFIX+".z4"); // missing comma is intentional

        for (String compartmentAbbreviation : QS_COMPARTMENT_LIST) {
            for (String zIndex : QS_Z_INDEX_LIST) {
                bw.write(","+compartmentAbbreviation+zIndex);
            }
            for (String cIndex : QS_C_INDEX_LIST) {
                bw.write(","+compartmentAbbreviation+cIndex);
            }
        }
        bw.write("\n");

        // Iterate over screenSamples and write info
        for (Entity screenSample : screenSampleKeyList) {

            Map<String, Double> entityValueMap = entityQuantifierMap.get(screenSample);

            // General screen sample info
            bw.write(screenSample.getName()+",");
            bw.write(screenSample.getId()+",");
            String heatmapFilepath=screenSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE_FILE_PATH);
//                if (heatmapFilepath==null) {
//                    String imageEntityIdString=screenSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
//                    Entity imageEntity=EJBFactory.getLocalAnnotationBean().getEntityById(imageEntityIdString);
//                    heatmapFilepath=imageEntity.getValueByAttributeName(EntityConstants.ATTRIBUTE_FILE_PATH);
//                }
            if (heatmapFilepath==null) {
                throw new Exception("Could not find default image filepath for screenSample id="+screenSample.getId());
            }
            bw.write(heatmapFilepath); // comma intentionally not included
            
            // Global info
            Double g0=entityValueMap.get(QS_GLOBAL_PREFIX+".t0"); bw.write(","+g0);
            Double g1=entityValueMap.get(QS_GLOBAL_PREFIX+".t1"); bw.write(","+g1);
            Double g2=entityValueMap.get(QS_GLOBAL_PREFIX+".t2"); bw.write(","+g2);
            Double g3=entityValueMap.get(QS_GLOBAL_PREFIX+".t3"); bw.write(","+g3);
            Double g4=entityValueMap.get(QS_GLOBAL_PREFIX+".z0"); bw.write(","+g4);
            Double g5=entityValueMap.get(QS_GLOBAL_PREFIX+".z1"); bw.write(","+g5);
            Double g6=entityValueMap.get(QS_GLOBAL_PREFIX+".z2"); bw.write(","+g6);
            Double g7=entityValueMap.get(QS_GLOBAL_PREFIX+".z3"); bw.write(","+g7);
            Double g8=entityValueMap.get(QS_GLOBAL_PREFIX+".z4"); bw.write(","+g8);

            // Compartmental Numerical info
            for (String compartmentAbbreviation : QS_COMPARTMENT_LIST) {
                for (String zIndex : QS_Z_INDEX_LIST) {
                    Double value=entityValueMap.get(compartmentAbbreviation+zIndex);
                    if (value==null) {
                        throw new Exception("Could not find value for key="+compartmentAbbreviation+zIndex+" in sample="+screenSample.getName());
                    }
                    bw.write(","+value.toString());
                }
                for (String cIndex : QS_C_INDEX_LIST) {
                    Double value=entityValueMap.get(compartmentAbbreviation+cIndex);
                    if (value==null) {
                        throw new Exception("Could not find value for key="+compartmentAbbreviation+cIndex+" in sample="+screenSample.getName());
                    }
                    bw.write(","+value.toString());
                }
            }
            bw.write("\n");
        }
        bw.close();
    }

    // This method returns: Map<Long, Map<String, String>> sampleInfoMap, Map<Long, List<Double>> quantifierInfoMap
    public static Object[] loadPatternAnnotationQuantifierSummaryFile() throws Exception {
        File patternAnnotationSummaryFile=getPatternAnnotationSummaryFile();
        Long startTime=new Date().getTime();
        System.out.println("loadPatternAnnotationQuantifierSummaryFile start()");
        BufferedReader bw=new BufferedReader(new FileReader(patternAnnotationSummaryFile));

        String firstLine=bw.readLine();
        if (firstLine==null) {
            throw new Exception("Could not read first line of file="+patternAnnotationSummaryFile.getAbsolutePath());
        }
        String[] firstLineColumnNames=firstLine.split(",");
        int expectedColumnCount=3 /* path */ + 9 /* Global */ + (QS_COMPARTMENT_LIST.size() * (QS_Z_INDEX_LIST.size() + QS_C_INDEX_LIST.size()));
        if (firstLineColumnNames.length!=expectedColumnCount) {
            throw new Exception("Expected columnCount="+expectedColumnCount+" but found "+firstLineColumnNames.length);
        }
        Map<Long, Map<String, String>> sampleInfoMap=new HashMap<Long, Map<String, String>>();
        Map<Long, List<Double>> quantifierInfoMap=new HashMap<Long, List<Double>>();
        String quantifierLine=null;
        while((quantifierLine=bw.readLine())!=null) {
            String[] qArr=quantifierLine.split(",");
            if (qArr.length>0) {
                Long qId = new Long(qArr[1]);
                Map<String, String> sampleInfo = new HashMap<String, String>();
                sampleInfo.put(QS_NAME_COL, qArr[0]);
                sampleInfo.put(QS_SAMPLE_ID_COL, qArr[1]);
                sampleInfo.put(QS_HEATMAP_PATH_COL, qArr[2]);
                sampleInfoMap.put(qId, sampleInfo);
                List<Double> qList = new ArrayList<Double>();
                for (int columnIndex = 3; columnIndex < firstLineColumnNames.length; columnIndex++) {
                    qList.add(new Double(qArr[columnIndex]));
                }
                quantifierInfoMap.put(qId, qList);
            }
        }
        bw.close();
        Object[] returnArr=new Object[2];
        returnArr[0]=sampleInfoMap;
        returnArr[1]=quantifierInfoMap;
        Long elapsedTime=new Date().getTime()-startTime;
        System.out.println("loadPatternAnnotationQuantifierSummaryFile end() elapsedTime="+elapsedTime);
        return returnArr;
    }

    // Returns 0-intensity 1-distribution
    public static Double[] getCompartmentScoresByQuantifiers(List<Double> globalList, List<Double> quantifierList) {

        // Global info
        double gt0=globalList.get(0);  // 1st-stage threshold (hard-coded to 31)
        double gt1=globalList.get(1);  // <average-low>
        double gt2=globalList.get(2);  // <overall average>
        double gt3=globalList.get(3);  // <average high>

        double gz0=globalList.get(4);  // 0-31 hard-coded intensity, as a ratio
        double gz1=globalList.get(5);  // 32-<average low>, as ratio
        double gz2=globalList.get(6);  // <average-low> - <overall average above 31>, as ratio
        double gz3=globalList.get(7);  // <overall average above 31> - <average high>, as ratio
        double gz4=globalList.get(8);  // <average high> - max, as ratio

        // Compartment info
        double z0=quantifierList.get(0); // 0-31 hard-coded intensity, as a ratio
        double z1=quantifierList.get(1); // 32-<average low>, as ratio
        double z2=quantifierList.get(2); // <average-low> - <overall average above 31>, as ratio
        double z3=quantifierList.get(3); // <overall average above 31> - <average high>, as ratio
        double z4=quantifierList.get(4); // <average high> - max, as ratio

        // Compartment info as 5x5x5 cubes
        double c0=quantifierList.get(5); // 0-31 hard-coded intensity, as a ratio
        double c1=quantifierList.get(6); // 32-<average low>, as ratio
        double c2=quantifierList.get(7); // <average-low> - <overall average above 31>, as ratio
        double c3=quantifierList.get(8); // <overall average above 31> - <average high>, as ratio
        double c4=quantifierList.get(9); // <average high> - max, as ratio

        double gd = gz3+gz4;

        // Intensity-Score
        double intensityScore = 0.0;
        if (gz4>0.0) {
            intensityScore=(c3 + c4) / gd;
        }

        // Distribution Score
        double distributionScore = 0.0;
        if (gd > 0.0) {
            distributionScore = (z3 + z4) / gd;
        }

        Double[] returnArr = new Double[2];
        returnArr[0]=intensityScore;
        returnArr[1]=distributionScore;

        return returnArr;
    }

    public static String getCompartmentDescription(String key) {
        return QS_DESCRIPTION_MAP.get(key);
    }


}
