package org.janelia.it.jacs.model.domain.sample;

import java.util.Date;
import java.util.List;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.support.MongoMapped;

/**
 * A release definition which controls how Samples are released and published to the external website. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="release",label="Fly Line Release")
public class LineRelease extends AbstractDomainObject {

    private Date releaseDate;
    private Integer lagTimeMonths;
    private boolean sageSync;    
    private List<String> dataSets;
    private List<String> annotators;
    private List<String> subscribers;

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    
    public Date getReleaseDate() {
        return releaseDate;
    }
    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
    public Integer getLagTimeMonths() {
        return lagTimeMonths;
    }
    public void setLagTimeMonths(Integer lagTimeMonths) {
        this.lagTimeMonths = lagTimeMonths;
    }
    public boolean isSageSync() {
        return sageSync;
    }
    public void setSageSync(boolean sageSync) {
        this.sageSync = sageSync;
    }
    public List<String> getDataSets() {
        return dataSets;
    }
    public void setDataSets(List<String> dataSets) {
        this.dataSets = dataSets;
    }
    public List<String> getAnnotators() {
        return annotators;
    }
    public void setAnnotators(List<String> annotators) {
        this.annotators = annotators;
    }
    public List<String> getSubscribers() {
        return subscribers;
    }
    public void setSubscribers(List<String> subscribers) {
        this.subscribers = subscribers;
    }
}
