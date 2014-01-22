package org.janelia.it.jacs.shared.annotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;
import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/16/12
 * Time: 9:52 AM
 * To change this template use File | Settings | File Templates.
 */

public class RelativePatternAnnotationDataManager extends PatternAnnotationDataManager implements Serializable {

    Logger logger = Logger.getLogger(RelativePatternAnnotationDataManager.class);

    public static final List<String> QS_Z_INDEX_LIST = new ArrayList<String>();
    public static final List<String> QS_C_INDEX_LIST = new ArrayList<String>();
    public static final String RELATIVE_TYPE="Relative";
    public static final String INTENSITY_DATA="Intensity";
    public static final String DISTRIBUTION_DATA="Distribution";

    Map<Long, List<Float>> intensityScoreMap;        // ordered by getCompartmentListInstance()
    Map<Long, List<Float>> distributionScoreMap;     // ditto

    static {
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

    File summaryFile;

    public RelativePatternAnnotationDataManager() {
        DataDescriptor intensityDescriptor = new DataDescriptor(INTENSITY_DATA, 0.0f, 100.0f, DataDescriptor.Type.CONTINUOUS);
        DataDescriptor distributionDescriptor = new DataDescriptor(DISTRIBUTION_DATA, 0.0f, 100.0f, DataDescriptor.Type.CONTINUOUS);
        descriptorList.add(intensityDescriptor);
        descriptorList.add(distributionDescriptor);
        summaryFile=getPatternAnnotationSummaryFile();
    }

    private static File getPatternAnnotationSummaryFile() {
        String resourceDirString=SystemConfigurationProperties.getString("FileStore.CentralDir.Archived")+
                SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationResourceDir");
        String quantifierSummaryFilename=SystemConfigurationProperties.getString("FlyScreen.PatternAnnotationQuantifierSummaryFile");
        File patternAnnotationSummaryFile=new File(resourceDirString, quantifierSummaryFilename);
        return patternAnnotationSummaryFile;
    }

    public String getDataManagerType() {
        return RELATIVE_TYPE;
    }

    protected void populateScores() throws Exception {
        Object[] quantifierMapArr = loadPatternAnnotationQuantifierSummaryFile();
        Map<Long, List<Float>> quantifierInfoMap = (Map<Long, List<Float>>) quantifierMapArr[1];
        computeScores(quantifierInfoMap);
        computePercentiles(intensityScoreMap, 0.0f, 100.0f);
        computePercentiles(distributionScoreMap, 0.0f, 100.0f);
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
    public Object[] loadPatternAnnotationQuantifierSummaryFile() throws Exception {
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
        Map<Long, List<Float>> quantifierInfoMap=new HashMap<Long, List<Float>>();
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
                List<Float> qList = new ArrayList<Float>();
                for (int columnIndex = 3; columnIndex < firstLineColumnNames.length; columnIndex++) {
                    qList.add(new Float(qArr[columnIndex]));
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
    public static Float[] getCompartmentScoresByQuantifiers(List<Float> globalList, List<Float> quantifierList) {

        // Global info
        float gt0=globalList.get(0);  // 1st-stage threshold (hard-coded to 31)
        float gt1=globalList.get(1);  // <average-low>
        float gt2=globalList.get(2);  // <overall average>
        float gt3=globalList.get(3);  // <average high>

        float gz0=globalList.get(4);  // 0-31 hard-coded intensity, as a ratio
        float gz1=globalList.get(5);  // 32-<average low>, as ratio
        float gz2=globalList.get(6);  // <average-low> - <overall average above 31>, as ratio
        float gz3=globalList.get(7);  // <overall average above 31> - <average high>, as ratio
        float gz4=globalList.get(8);  // <average high> - max, as ratio

// Compartment info
        float z0=quantifierList.get(0); // 0-31 hard-coded intensity, as a ratio
        float z1=quantifierList.get(1); // 32-<average low>, as ratio
        float z2=quantifierList.get(2); // <average-low> - <overall average above 31>, as ratio
        float z3=quantifierList.get(3); // <overall average above 31> - <average high>, as ratio
        float z4=quantifierList.get(4); // <average high> - max, as ratio

// Compartment info as 5x5x5 cubes
        float c0=quantifierList.get(5); // 0-31 hard-coded intensity, as a ratio
        float c1=quantifierList.get(6); // 32-<average low>, as ratio
        float c2=quantifierList.get(7); // <average-low> - <overall average above 31>, as ratio
        float c3=quantifierList.get(8); // <overall average above 31> - <average high>, as ratio
        float c4=quantifierList.get(9); // <average high> - max, as ratio

        float gd = gz3+gz4;

// Intensity-Score
        float intensityScore = 0.0f;
        if (gz4>0.0) {
            intensityScore=(c3 + c4) / gd;
        }

        // Distribution Score
        float distributionScore = 0.0f;
        if (gd > 0.0) {
            distributionScore = (z3 + z4) / gd;
        }

        Float[] returnArr = new Float[2];
        returnArr[0]=intensityScore;
        returnArr[1]=distributionScore;

        return returnArr;
    }

    public static String getCompartmentDescription(String key) {
        return QS_DESCRIPTION_MAP.get(key);
    }

    protected void computeScores(Map<Long, List<Float>> quantifierInfoMap) {
        long totalComputeCount=0;
        List<String> compartmentAbbreviationList=getCompartmentListInstance();
        DataDescriptor intensityDescriptor=descriptorList.get(0);
        DataDescriptor distributionDescriptor=descriptorList.get(1);

        // Populate descriptorScoreMap
        intensityScoreMap=descriptorScoreMap.get(intensityDescriptor);
        distributionScoreMap=descriptorScoreMap.get(distributionDescriptor);
        if (intensityScoreMap==null) {
            logger.info("Adding descriptorScoreMap entry for intensityScoreMap");
            intensityScoreMap=new HashMap<Long, List<Float>>();
            descriptorScoreMap.put(intensityDescriptor.getName(), intensityScoreMap);
        }
        if (distributionScoreMap==null) {
            logger.info("Adding descriptorScoreMap entry for distributionScoreMap");
            distributionScoreMap=new HashMap<Long, List<Float>>();
            descriptorScoreMap.put(distributionDescriptor.getName(), distributionScoreMap);
        }

        for (Long sampleId : quantifierInfoMap.keySet()) {

            // Populate Scores
            List<Float> quantifierList = quantifierInfoMap.get(sampleId);
            List<Float> intensityList = new ArrayList<Float>();
            List<Float> distributionList = new ArrayList<Float>();
            List<Float> globalList = new ArrayList<Float>();
            List<Float> compartmentList = new ArrayList<Float>();

            // We assume the compartment list here matches the order of the quantifierList
            final int GLOBAL_LIST_SIZE=9;
            for (int g=0;g<GLOBAL_LIST_SIZE;g++) {
                globalList.add(quantifierList.get(g));
            }
            int compartmentCount=0;
            for (String compartmentAbbreviation : compartmentAbbreviationList) {
                compartmentList.clear();
                int startPosition=GLOBAL_LIST_SIZE + compartmentCount*10;
                int endPosition=startPosition+10;
                for (int c=startPosition;c<endPosition;c++) {
                    compartmentList.add(quantifierList.get(c));
                }
                Object[] scores=getCompartmentScoresByQuantifiers(globalList, compartmentList);
                totalComputeCount++;
                intensityList.add((Float)scores[0]);
                distributionList.add((Float) scores[1]);
                compartmentCount++;
            }
            intensityScoreMap.put(sampleId, intensityList);
            distributionScoreMap.put(sampleId, distributionList);
        }
        System.out.println("Total calls to getCompartmentScoresByQuantifiers() = "+totalComputeCount);
    }

}
