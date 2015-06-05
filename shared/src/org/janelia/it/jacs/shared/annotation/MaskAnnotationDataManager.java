package org.janelia.it.jacs.shared.annotation;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;

/**
 * Created by IntelliJ IDEA.
 * User: murphys
 * Date: 3/21/12
 * Time: 10:22 AM
 * To change this template use File | Settings | File Templates.
 */

public class MaskAnnotationDataManager {

    // QS stands for "Quality Summary"
    public static final String QS_NAME_COL = "ScreenSampleName";
    public static final String QS_SAMPLE_ID_COL = "ScreenSampleId";
    public static final String QS_HEATMAP_PATH_COL = "HeatmapImagePath";
    public static final String QS_GLOBAL_PREFIX = "Global";
    public static final List<String> QS_Z_INDEX_LIST = new ArrayList<String>();
    public static final List<String> QS_C_INDEX_LIST = new ArrayList<String>();

    public final List<String> QS_COMPARTMENT_LIST = new ArrayList<String>();
    public final Map<String,String> QS_DESCRIPTION_MAP = new HashMap<String, String>();
    public final Map<String, Integer> QS_RED_MAP = new HashMap<String, Integer>();
    public final Map<String, Integer> QS_GREEN_MAP = new HashMap<String, Integer>();
    public final Map<String, Integer> QS_BLUE_MAP = new HashMap<String, Integer>();

    public static Pattern nameListPattern;

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

        nameListPattern=Pattern.compile("(\\S+)\\s+(\\S+)\\s+\"(.+)\"\\s+\\(\\s*(\\d+)\\s+(\\d+)\\s+(\\d+)\\s*\\)");
    }

    public static List<String> parseMaskNameIndexLine(String line) {
        List<String> returnList=new ArrayList<String>();
        Matcher m=nameListPattern.matcher(line.trim());
        if (m.matches()) {
            for (int i=0;i<(m.groupCount()+1);i++) {
                returnList.add(m.group(i));
            }
        }
        return returnList;
    }

    public void loadMaskCompartmentList(InputStream maskNameIndexStream) {
        try {
            
            BufferedReader br = new BufferedReader(new InputStreamReader(maskNameIndexStream));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.trim().length() > 0) {
                    List<String> groupList = parseMaskNameIndexLine(line);
                    if (groupList.size() > 6) {
                        int index = new Integer(groupList.get(1));
                        String name = groupList.get(2);
                        String description = groupList.get(3);
                        int red = new Integer(groupList.get(4));
                        int green = new Integer(groupList.get(5));
                        int blue = new Integer(groupList.get(6));
                        QS_COMPARTMENT_LIST.add(name);
                        QS_DESCRIPTION_MAP.put(name, description);
                        QS_RED_MAP.put(name, red);
                        QS_GREEN_MAP.put(name, green);
                        QS_BLUE_MAP.put(name, blue);
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getCompartmentListInstance() {
        List<String> compartmentListInstance = new ArrayList<String>();
        for (String c : QS_COMPARTMENT_LIST) {
            compartmentListInstance.add(c);
        }
        return compartmentListInstance;
    }

    public void createMaskAnnotationQuantifierSummaryFile(File patternSummaryFile, Map<Entity, Map<String, Double>> entityQuantifierMap) throws Exception {
        if (QS_COMPARTMENT_LIST==null || QS_COMPARTMENT_LIST.size()==0) {
            throw new Exception("Compartment list must be loaded");
        }
        List<Entity> screenSampleKeyList=new ArrayList<Entity>();
        screenSampleKeyList.addAll(entityQuantifierMap.keySet());
        Collections.sort(screenSampleKeyList, new Comparator<Entity>() {
            @Override
            public int compare(Entity o, Entity o1) {
                return o.getName().compareTo(o1.getName());
            }
        });

        // Initialize output file
        BufferedWriter bw=new BufferedWriter(new FileWriter(patternSummaryFile));

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
                if (heatmapFilepath==null) {
                    heatmapFilepath=screenSample.getValueByAttributeName(EntityConstants.ATTRIBUTE_DEFAULT_2D_IMAGE);
                }
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
    public Object[] loadMaskSummaryFile(InputStream patternAnnotationSummaryStream) throws Exception {
//        System.out.println("loadMaskSummaryFile start()");
        if (QS_COMPARTMENT_LIST.size()==0) {
            throw new Exception("Compartment list must be loaded");
        }
//        Long startTime=new Date().getTime();
//        System.out.println("Reading mask annotation summary file="+patternAnnotationSummaryFile.getAbsolutePath());

        BufferedReader bw = new BufferedReader(new InputStreamReader(patternAnnotationSummaryStream));
        String firstLine=bw.readLine();
        if (firstLine==null) {
            throw new Exception("Could not read first line of file");
        }
        String[] firstLineColumnNames=firstLine.split(",");
        int qsoSize=QS_COMPARTMENT_LIST.size();
        int qszSize=QS_Z_INDEX_LIST.size();
        int qscSize=QS_C_INDEX_LIST.size();
        int expectedColumnCount=3 /* path */ + 9 /* Global */ + (QS_COMPARTMENT_LIST.size() * (QS_Z_INDEX_LIST.size() + QS_C_INDEX_LIST.size()));
        if (firstLineColumnNames.length!=expectedColumnCount) {
            throw new Exception("Expected columnCount="+expectedColumnCount+" but found "+firstLineColumnNames.length+" , qso="+qsoSize+" qsz="+qszSize+" qsc="+qscSize);
        }
        Map<Long, Map<String, String>> sampleInfoMap=new HashMap<Long, Map<String, String>>();
        Map<Long, List<Double>> quantifierInfoMap=new HashMap<Long, List<Double>>();
        String quantifierLine=null;
        long lineCount=0;
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
            lineCount++;
        }
        bw.close();
        Object[] returnArr=new Object[2];
        returnArr[0]=sampleInfoMap;
        returnArr[1]=quantifierInfoMap;
//        Long elapsedTime=new Date().getTime()-startTime;
//        System.out.println("loadMaskSummaryFile end() elapsedTime="+elapsedTime+" lineCount="+lineCount+" sampleInfoMap="+sampleInfoMap.size()+" quantifierInfoMap="+quantifierInfoMap.size());
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

    public String getCompartmentDescription(String key) {
        return QS_DESCRIPTION_MAP.get(key);
    }


}
