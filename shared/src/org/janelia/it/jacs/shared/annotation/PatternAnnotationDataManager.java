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

    static {
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_R");
        QS_COMPARTMENT_LIST.add("AME_L");
        QS_COMPARTMENT_LIST.add("AME_R");
        QS_COMPARTMENT_LIST.add("AMMC_L");
        QS_COMPARTMENT_LIST.add("AMMC_R");
        QS_COMPARTMENT_LIST.add("ATL_L");
        QS_COMPARTMENT_LIST.add("ATL_R");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AL_L");
        QS_COMPARTMENT_LIST.add("AVLP_L");
        QS_COMPARTMENT_LIST.add("AVLP_R");
        QS_COMPARTMENT_LIST.add("CRE_L");
        QS_COMPARTMENT_LIST.add("CRE_R");
        QS_COMPARTMENT_LIST.add("EB");
        QS_COMPARTMENT_LIST.add("EPA_L");
        QS_COMPARTMENT_LIST.add("EPA_R");
        QS_COMPARTMENT_LIST.add("FB");
        QS_COMPARTMENT_LIST.add("FLA");
        QS_COMPARTMENT_LIST.add("GA_L");
        QS_COMPARTMENT_LIST.add("GA_R");
        QS_COMPARTMENT_LIST.add("GOR_L");
        QS_COMPARTMENT_LIST.add("GOR_R");
        QS_COMPARTMENT_LIST.add("IB_L");
        QS_COMPARTMENT_LIST.add("IB_R");
        QS_COMPARTMENT_LIST.add("ICL_L");
        QS_COMPARTMENT_LIST.add("ICL_R");
        QS_COMPARTMENT_LIST.add("IPS_L");
        QS_COMPARTMENT_LIST.add("IPS_R");
        QS_COMPARTMENT_LIST.add("IVLP_L");
        QS_COMPARTMENT_LIST.add("IVLP_R");
        QS_COMPARTMENT_LIST.add("LAL_L");
        QS_COMPARTMENT_LIST.add("LAL_R");
        QS_COMPARTMENT_LIST.add("LB_L");
        QS_COMPARTMENT_LIST.add("LB_R");
        QS_COMPARTMENT_LIST.add("LH_L");
        QS_COMPARTMENT_LIST.add("LH_R");
        QS_COMPARTMENT_LIST.add("LOP_L");
        QS_COMPARTMENT_LIST.add("LOP_R");
        QS_COMPARTMENT_LIST.add("LO_L");
        QS_COMPARTMENT_LIST.add("LO_R");
        QS_COMPARTMENT_LIST.add("MB_L");
        QS_COMPARTMENT_LIST.add("MB_R");
        QS_COMPARTMENT_LIST.add("ME_L");
        QS_COMPARTMENT_LIST.add("ME_R");
        QS_COMPARTMENT_LIST.add("NO");
        QS_COMPARTMENT_LIST.add("OTU_L");
        QS_COMPARTMENT_LIST.add("OTU_R");
        QS_COMPARTMENT_LIST.add("PB");
        QS_COMPARTMENT_LIST.add("PLP_L");
        QS_COMPARTMENT_LIST.add("PLP_R");
        QS_COMPARTMENT_LIST.add("PRW");
        QS_COMPARTMENT_LIST.add("PVLP_L");
        QS_COMPARTMENT_LIST.add("PVLP_R");
        QS_COMPARTMENT_LIST.add("SAD");
        QS_COMPARTMENT_LIST.add("SCL_L");
        QS_COMPARTMENT_LIST.add("SCL_R");
        QS_COMPARTMENT_LIST.add("SIP_L");
        QS_COMPARTMENT_LIST.add("SIP_R");
        QS_COMPARTMENT_LIST.add("SLP_L");
        QS_COMPARTMENT_LIST.add("SLP_R");
        QS_COMPARTMENT_LIST.add("SMP_L");
        QS_COMPARTMENT_LIST.add("SMP_R");
        QS_COMPARTMENT_LIST.add("SOG");
        QS_COMPARTMENT_LIST.add("SPS_L");
        QS_COMPARTMENT_LIST.add("SPS_R");
        QS_COMPARTMENT_LIST.add("VES_L");
        QS_COMPARTMENT_LIST.add("VES_R");
        QS_COMPARTMENT_LIST.add("WED_L");
        QS_COMPARTMENT_LIST.add("WED_R");

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

            // Numerical info
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
        int columnHeaderIndex=0;
        for (int columnIndex=0;columnIndex<firstLineColumnNames.length;columnIndex++) {

        }
        bw.close();
        System.out.println("loadPatternAnnotationQuantifierSummaryFile end()");
        return null;
    }

}
