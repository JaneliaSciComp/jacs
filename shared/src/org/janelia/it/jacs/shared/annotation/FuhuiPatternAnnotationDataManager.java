package org.janelia.it.jacs.shared.annotation;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/23/12
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */

public class FuhuiPatternAnnotationDataManager extends PatternAnnotationDataManager implements Serializable {

    Logger logger= Logger.getLogger(FuhuiPatternAnnotationDataManager.class);

    public static final String FUHUI_TYPE="Fuhui";
    public static final String CATEGORY_DATA="Category";
    public static final String PROBABILITY_DATA="Probability";

    Map<Long, List<Float>> categoryMap;        // ordered by getCompartmentListInstance()
    Map<Long, List<Float>> probabilityMap;     // ditto

    File categorySummaryFile;
    File probabilitySummaryFile;
    File sampleIdFile;

    Map<String, List<Long>> sampleIdMap;

    public FuhuiPatternAnnotationDataManager() {
        DataDescriptor categoryDescriptor = new DataDescriptor(CATEGORY_DATA, 0.0f, 1.0f, DataDescriptor.Type.BOOLEAN);
        DataDescriptor probabilityDescriptor = new DataDescriptor(PROBABILITY_DATA, 0.0f, 1.0f, DataDescriptor.Type.CONTINUOUS);
        descriptorList.add(categoryDescriptor);
        descriptorList.add(probabilityDescriptor);
        String categoryFilename=SystemConfigurationProperties.getString("FlyScreen.FuhuiCategoryPatternAnnotationQuantifierSummaryFile");
        categorySummaryFile=new File(categoryFilename);
        String probabilityFilename=SystemConfigurationProperties.getString("FlyScreen.FuhuiProbabilityPatternAnnotationQuantifierSummaryFile");
        probabilitySummaryFile=new File(probabilityFilename);
        String sampleIdFilename=SystemConfigurationProperties.getString("FlyScreen.FuhuiSampleIdList");
        sampleIdFile=new File(sampleIdFilename);
    }

    public String getDataManagerType() {
        return FUHUI_TYPE;
    }

    public List<String> getCompartmentListInstance() {
        List<String> compartmentListInstance = new ArrayList<String>();
        for (String c : QS_COMPARTMENT_LIST) {
            if (c.equals("ME_R") ||
                c.equals("ME_L") ||
                c.equals("LOP_R") ||
                c.equals("LOP_L") ||
                c.equals("LO_R") ||
                c.equals("LO_L") ||
                c.equals("AME_R") ||
                c.equals("AME_L")) {
                // skip these
            } else {
                compartmentListInstance.add(c);
            }
        }
        return compartmentListInstance;
    }

    protected void populateScores() throws Exception {
        sampleIdMap=loadSampleMap();
        categoryMap=loadCategorySummaryFile();
        probabilityMap=loadProbabilitySummaryFile();
        computePercentiles(probabilityMap, 0.0f, 1.0f);
        DataDescriptor categoryDescriptor=descriptorList.get(0);
        DataDescriptor probabilityDescriptor=descriptorList.get(1);
        logger.info("Adding descriptorScoreMap entries for categoryMap, probabilityMap");
        descriptorScoreMap.put(categoryDescriptor.getName(), categoryMap);
        descriptorScoreMap.put(probabilityDescriptor.getName(), probabilityMap);
    }

    public Map<String, List<Long>> loadSampleMap() throws Exception {
        Map<String, List<Long>> sampleIdMap=new HashMap<String, List<Long>>();
        BufferedReader reader=new BufferedReader(new FileReader(sampleIdFile));
        String line=null;
        while((line=reader.readLine())!=null) {
            String[] comps=line.split(",");
            if (comps.length==2) {
                String lineName=getLineFromFullSampleName(comps[0]);
                List<Long> list=sampleIdMap.get(lineName);
                if (list==null) {
                    list=new ArrayList<Long>();
                    sampleIdMap.put(lineName, list);
                }
                list.add(new Long(comps[1].trim()));
            }
        }
        reader.close();
        return sampleIdMap;
    }

    protected String getLineFromFullSampleName(String sampleName) {
        String[] comps=sampleName.split("-");
        return comps[0];
    }

    public Map<Long, List<Float>> loadCategorySummaryFile() throws Exception {
        Long startTime=new Date().getTime();
        logger.info("loadCategorySummaryFile start()");
        BufferedReader bw=new BufferedReader(new FileReader(categorySummaryFile));
        String firstLine=bw.readLine();
        if (firstLine==null) {
            throw new Exception("Could not read first line of file="+categorySummaryFile.getAbsolutePath());
        }
        String[] firstLineColumnNames=firstLine.split(",");
        int expectedColumnCount=1 /* line name */ + 60 /* 0 or 1 per compartment */;
        if (firstLineColumnNames.length!=expectedColumnCount) {
            throw new Exception("Expected columnCount="+expectedColumnCount+" but found "+firstLineColumnNames.length);
        }
        Map<Long, List<Float>> categoryMap=new HashMap<Long, List<Float>>();
        String quantifierLine=null;
        List<String> compartmentList=getCompartmentListInstance();
        while((quantifierLine=bw.readLine())!=null) {
            String[] qArr=quantifierLine.split(",");
            if (qArr.length>0) {
                String lineName = qArr[0];
                Map<String, Float> cMap=new HashMap<String, Float>();
                cMap.put("FB", new Float(qArr[1].trim()));
                cMap.put("EB", new Float(qArr[2].trim()));
                cMap.put("SAD", new Float(qArr[3].trim()));
                cMap.put("NO", new Float(qArr[4].trim()));
                cMap.put("SOG", new Float(qArr[5].trim()));
                cMap.put("PB", new Float(qArr[6].trim()));
                cMap.put("CRE_R", new Float(qArr[7].trim()));
                cMap.put("EPA_R", new Float(qArr[8].trim()));
                cMap.put("VES_R", new Float(qArr[9].trim()));
                cMap.put("ATL_R", new Float(qArr[10].trim()));
                cMap.put("PLP_R", new Float(qArr[11].trim()));
                cMap.put("AVLP_R", new Float(qArr[12].trim()));
                cMap.put("AL_R", new Float(qArr[13].trim()));
                cMap.put("GOR_R", new Float(qArr[14].trim()));
                cMap.put("SCL_R", new Float(qArr[15].trim()));
                cMap.put("FLA", new Float(qArr[16].trim()));
                cMap.put("ICL_R", new Float(qArr[17].trim()));
                cMap.put("MB_R", new Float(qArr[18].trim()));
                cMap.put("PVLP_R", new Float(qArr[19].trim()));
                cMap.put("OTU_R", new Float(qArr[20].trim()));
                cMap.put("WED_R", new Float(qArr[21].trim()));
                cMap.put("SMP_R", new Float(qArr[22].trim()));
                cMap.put("LH_R", new Float(qArr[23].trim()));
                cMap.put("SLP_R", new Float(qArr[24].trim()));
                cMap.put("LB_R", new Float(qArr[25].trim()));
                cMap.put("SIP_R", new Float(qArr[26].trim()));
                cMap.put("IB_R", new Float(qArr[27].trim()));
                cMap.put("IVLP_R", new Float(qArr[28].trim()));
                cMap.put("IPS_R", new Float(qArr[29].trim()));
                cMap.put("SPS_R", new Float(qArr[30].trim()));
                cMap.put("LAL_R", new Float(qArr[31].trim()));
                cMap.put("PRW", new Float(qArr[32].trim()));
                cMap.put("GA_R", new Float(qArr[33].trim()));
                cMap.put("CRE_L", new Float(qArr[34].trim()));
                cMap.put("EPA_L", new Float(qArr[35].trim()));
                cMap.put("VES_L", new Float(qArr[36].trim()));
                cMap.put("ATL_L", new Float(qArr[37].trim()));
                cMap.put("PLP_L", new Float(qArr[38].trim()));
                cMap.put("AVLP_L", new Float(qArr[39].trim()));
                cMap.put("AL_L", new Float(qArr[40].trim()));
                cMap.put("GOR_L", new Float(qArr[41].trim()));
                cMap.put("SCL_L", new Float(qArr[42].trim()));
                cMap.put("ICL_L", new Float(qArr[43].trim()));
                cMap.put("MB_L", new Float(qArr[44].trim()));
                cMap.put("PVLP_L", new Float(qArr[45].trim()));
                cMap.put("OTU_L", new Float(qArr[46].trim()));
                cMap.put("WED_L", new Float(qArr[47].trim()));
                cMap.put("SMP_L", new Float(qArr[48].trim()));
                cMap.put("LH_L", new Float(qArr[49].trim()));
                cMap.put("SLP_L", new Float(qArr[50].trim()));
                cMap.put("LB_L", new Float(qArr[51].trim()));
                cMap.put("SIP_L", new Float(qArr[52].trim()));
                cMap.put("IB_L", new Float(qArr[53].trim()));
                cMap.put("IVLP_L", new Float(qArr[54].trim()));
                cMap.put("IPS_L", new Float(qArr[55].trim()));
                cMap.put("SPS_L", new Float(qArr[56].trim()));
                cMap.put("LAL_L", new Float(qArr[57].trim()));
                cMap.put("GA_L", new Float(qArr[58].trim()));
                cMap.put("AMMC_L", new Float(qArr[59].trim()));
                cMap.put("AMMC_R", new Float(qArr[60].trim()));
                List<Float> cList=new ArrayList<Float>();
                for (String c : compartmentList) {
                    cList.add(cMap.get(c));
                }
                List<Long> sampleIdList=sampleIdMap.get(lineName);
                if (sampleIdList != null) {
                    for (Long sampleId : sampleIdList) {
                        categoryMap.put(sampleId, cList);
                    }
                }
            }
        }
        bw.close();
        Long elapsedTime=new Date().getTime()-startTime;
        logger.info("loadCategorySummaryFile end() elapsedTime="+elapsedTime);
        return categoryMap;
    }

    public Map<Long, List<Float>> loadProbabilitySummaryFile() throws Exception {
        Long startTime=new Date().getTime();
        logger.info("loadProbabilitySummaryFile start()");
        BufferedReader bw=new BufferedReader(new FileReader(probabilitySummaryFile));
        String firstLine=bw.readLine();
        if (firstLine==null) {
            throw new Exception("Could not read first line of file="+probabilitySummaryFile.getAbsolutePath());
        }
        String[] firstLineColumnNames=firstLine.split(",");
        int expectedColumnCount=1 /* line name */ + 60 /* 0 or 1 per compartment */;
        if (firstLineColumnNames.length!=expectedColumnCount) {
            throw new Exception("Expected columnCount="+expectedColumnCount+" but found "+firstLineColumnNames.length);
        }
        Map<Long, List<Float>> probabilityMap=new HashMap<Long, List<Float>>();
        List<String> compartmentList=getCompartmentListInstance();
        String quantifierLine=null;
        while((quantifierLine=bw.readLine())!=null) {
            String[] qArr=quantifierLine.split(",");
            if (qArr.length>0) {
                String lineName = qArr[0];
                Map<String, Float> cMap=new HashMap<String, Float>();
                cMap.put("FB", new Float(qArr[1].trim()));
                cMap.put("EB", new Float(qArr[2].trim()));
                cMap.put("SAD", new Float(qArr[3].trim()));
                cMap.put("NO", new Float(qArr[4].trim()));
                cMap.put("SOG", new Float(qArr[5].trim()));
                cMap.put("PB", new Float(qArr[6].trim()));
                cMap.put("CRE_R", new Float(qArr[7].trim()));
                cMap.put("EPA_R", new Float(qArr[8].trim()));
                cMap.put("VES_R", new Float(qArr[9].trim()));
                cMap.put("ATL_R", new Float(qArr[10].trim()));
                cMap.put("PLP_R", new Float(qArr[11].trim()));
                cMap.put("AVLP_R", new Float(qArr[12].trim()));
                cMap.put("AL_R", new Float(qArr[13].trim()));
                cMap.put("GOR_R", new Float(qArr[14].trim()));
                cMap.put("SCL_R", new Float(qArr[15].trim()));
                cMap.put("FLA", new Float(qArr[16].trim()));
                cMap.put("ICL_R", new Float(qArr[17].trim()));
                cMap.put("MB_R", new Float(qArr[18].trim()));
                cMap.put("PVLP_R", new Float(qArr[19].trim()));
                cMap.put("OTU_R", new Float(qArr[20].trim()));
                cMap.put("WED_R", new Float(qArr[21].trim()));
                cMap.put("SMP_R", new Float(qArr[22].trim()));
                cMap.put("LH_R", new Float(qArr[23].trim()));
                cMap.put("SLP_R", new Float(qArr[24].trim()));
                cMap.put("LB_R", new Float(qArr[25].trim()));
                cMap.put("SIP_R", new Float(qArr[26].trim()));
                cMap.put("IB_R", new Float(qArr[27].trim()));
                cMap.put("IVLP_R", new Float(qArr[28].trim()));
                cMap.put("IPS_R", new Float(qArr[29].trim()));
                cMap.put("SPS_R", new Float(qArr[30].trim()));
                cMap.put("LAL_R", new Float(qArr[31].trim()));
                cMap.put("PRW", new Float(qArr[32].trim()));
                cMap.put("GA_R", new Float(qArr[33].trim()));
                cMap.put("CRE_L", new Float(qArr[34].trim()));
                cMap.put("EPA_L", new Float(qArr[35].trim()));
                cMap.put("VES_L", new Float(qArr[36].trim()));
                cMap.put("ATL_L", new Float(qArr[37].trim()));
                cMap.put("PLP_L", new Float(qArr[38].trim()));
                cMap.put("AVLP_L", new Float(qArr[39].trim()));
                cMap.put("AL_L", new Float(qArr[40].trim()));
                cMap.put("GOR_L", new Float(qArr[41].trim()));
                cMap.put("SCL_L", new Float(qArr[42].trim()));
                cMap.put("ICL_L", new Float(qArr[43].trim()));
                cMap.put("MB_L", new Float(qArr[44].trim()));
                cMap.put("PVLP_L", new Float(qArr[45].trim()));
                cMap.put("OTU_L", new Float(qArr[46].trim()));
                cMap.put("WED_L", new Float(qArr[47].trim()));
                cMap.put("SMP_L", new Float(qArr[48].trim()));
                cMap.put("LH_L", new Float(qArr[49].trim()));
                cMap.put("SLP_L", new Float(qArr[50].trim()));
                cMap.put("LB_L", new Float(qArr[51].trim()));
                cMap.put("SIP_L", new Float(qArr[52].trim()));
                cMap.put("IB_L", new Float(qArr[53].trim()));
                cMap.put("IVLP_L", new Float(qArr[54].trim()));
                cMap.put("IPS_L", new Float(qArr[55].trim()));
                cMap.put("SPS_L", new Float(qArr[56].trim()));
                cMap.put("LAL_L", new Float(qArr[57].trim()));
                cMap.put("GA_L", new Float(qArr[58].trim()));
                cMap.put("AMMC_L", new Float(qArr[59].trim()));
                cMap.put("AMMC_R", new Float(qArr[60].trim()));
                List<Float> cList=new ArrayList<Float>();
                for (String c : compartmentList) {
                    cList.add(cMap.get(c));
                }
                List<Long> sampleIdList=sampleIdMap.get(lineName);
                if (sampleIdList != null) {
                    for (Long sampleId : sampleIdList) {
                        probabilityMap.put(sampleId, cList);
                    }
                }
            }
        }
        bw.close();
        Long elapsedTime=new Date().getTime()-startTime;
        logger.info("loadProbabilitySummaryFile end() elapsedTime="+elapsedTime);
        return probabilityMap;
    }

    public static String getCompartmentDescription(String key) {
        return QS_DESCRIPTION_MAP.get(key);
    }



}