
package org.janelia.it.jacs.shared.tasks;

import java.util.Arrays;
import java.util.List;

/**
 * @author Michael Press
 */
public class RecruitableJobInfo extends JobInfo {
    public static final String QUERY_SORT = "query";
    public static final String NAME_SORT = "jobName";
    public static final String LENGTH_SORT = "refEnd";
    public static final String SUBJECT_SORT = "subject";
    public static final String HITS_SORT = "numHits";
    public static final String SORT_BY_GENOME_LENGTH = "length";

    private String _recruitableNodeId;
    private String _recruitementResultsFileNodeId;
    private int _pctIdMin;
    private int _pctIdMax;
    private long _refAxisCoordBegin;
    private long _refAxisCoordEnd;
    private String _genomeLengthFormatted;
    private String pathToSourceData;
    private String giNumberOfSourceData;
    private String mateInfo;
    // Comma separated list of samples recruited
    private String samplesRecruited;
    private String annotationFilterString;
    private String mateSpanPoint;
    private String colorizationType;

    public RecruitableJobInfo() {
        super();
    }

    public void setRecruitableNodeId(String id) {
        _recruitableNodeId = id;
    }

    public String getRecruitableNodeId() {
        return _recruitableNodeId;
    }

    public void setRecruitmentResultsFileNodeId(String id) {
        _recruitementResultsFileNodeId = id;
    }

    public String getRecruitmentResultsFileNodeId() {
        return _recruitementResultsFileNodeId;
    }

    public int getPercentIdentityMax() {
        return _pctIdMax;
    }

    public void setPercentIdentityMax(int pctIdMax) {
        _pctIdMax = pctIdMax;
    }

    public int getPercentIdentityMin() {
        return _pctIdMin;
    }

    public void setPercentIdentityMin(int pctIdMin) {
        _pctIdMin = pctIdMin;
    }

    public long getRefAxisBeginCoord() {
        return _refAxisCoordBegin;
    }

    public void setRefAxisBeginCoord(long refAxisCoordBegin) {
        _refAxisCoordBegin = refAxisCoordBegin;
    }

    public long getRefAxisEndCoord() {
        return _refAxisCoordEnd;
    }

    public void setRefAxisEndCoord(long refAxisCoordEnd) {
        _refAxisCoordEnd = refAxisCoordEnd;
    }

    public void setGenomeLengthFormatted(String genomeLength) {
        _genomeLengthFormatted = genomeLength;
    }

    public String getGenomeLengthFormatted() {
        return _genomeLengthFormatted;
    }

    public String getPathToSourceData() {
        return pathToSourceData;
    }

    public void setPathToSourceData(String pathToSourceData) {
        this.pathToSourceData = pathToSourceData;
    }

    public String getGiNumberOfSourceData() {
        return giNumberOfSourceData;
    }

    public void setGiNumberOfSourceData(String giNumberOfSourceData) {
        this.giNumberOfSourceData = giNumberOfSourceData;
    }

    public String getMateInfo() {
        return mateInfo;
    }

    public void setMateInfo(String mateInfo) {
        this.mateInfo = mateInfo;
    }

    public String getSamplesRecruited() {
        return samplesRecruited;
    }

    /**
     * @return list of sample names
     */
    public List<String> getSamplesRecruitedAsList() {
        String[] samples = samplesRecruited.split(",");
        return Arrays.asList(samples);
    }

    public void setSamplesRecruited(String samplesRecruited) {
        this.samplesRecruited = samplesRecruited;
    }

    public String getAnnotationFilterString() {
        if (null == annotationFilterString) return "";
        return annotationFilterString;
    }

    public void setAnnotationFilterString(String annotationFilterString) {
        this.annotationFilterString = annotationFilterString;
    }

    public String getMateSpanPoint() {
        return mateSpanPoint;
    }

    public void setMateSpanPoint(String mateSpanPoint) {
        this.mateSpanPoint = mateSpanPoint;
    }

    public String getColorizationType() {
        return colorizationType;
    }

    public void setColorizationType(String colorizationType) {
        this.colorizationType = colorizationType;
    }
}
