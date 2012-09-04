package org.janelia.it.jacs.shared.annotation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 8/31/12
 * Time: 3:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilterResult implements Serializable {

    Map<String, Long> countMap;
    List<Long> sampleList;
    Long totalSampleCount;

    public FilterResult() {
        countMap=new HashMap<String, Long>();
        sampleList=new ArrayList<Long>();
    }

    public FilterResult(Map<String, Long> countMap, List<Long> sampleList, Long totalSamplesCount) {
        this.countMap = countMap;
        this.sampleList = sampleList;
        this.totalSampleCount = totalSamplesCount;
    }

    public Map<String, Long> getCountMap() {
        return countMap;
    }

    public void setCountMap(Map<String, Long> countMap) {
        this.countMap = countMap;
    }

    public List<Long> getSampleList() {
        return sampleList;
    }

    public void setSampleList(List<Long> sampleList) {
        this.sampleList = sampleList;
    }

    public Long getTotalSampleCount() {
        return totalSampleCount;
    }

    public void setTotalSampleCount(Long totalSampleCount) {
        this.totalSampleCount = totalSampleCount;
    }

    public void clear() {
        countMap.clear();
        sampleList.clear();
        totalSampleCount=0L;
    }
}
