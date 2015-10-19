
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Dec 5, 2006
 * Time: 10:52:14 AM
 */
public class Library implements Serializable, IsSerializable {

    private transient Set samples;

    private Long libraryId;
    private String libraryAcc;
    private String sequencingTechnology;
    private Integer minInsertSize;
    private Integer maxInsertSize;
    private Integer numberOfReads;
    private Set<Read> reads;
    private String sampleAcc;
    private Sample sample;

    public Library() {
    }

    public Long getLibraryId() {
        return libraryId;
    }

    public void setLibraryId(Long libraryId) {
        this.libraryId = libraryId;
    }

    public String getLibraryAcc() {
        return libraryAcc;
    }

    public void setLibraryAcc(String libraryAcc) {
        this.libraryAcc = libraryAcc;
    }

    public String getSequencingTechnology() {
        return sequencingTechnology;
    }

    public void setSequencingTechnology(String sequencingTechnology) {
        this.sequencingTechnology = sequencingTechnology;
    }

    public Integer getMinInsertSize() {
        return minInsertSize;
    }

    public void setMinInsertSize(Integer minInsertSize) {
        this.minInsertSize = minInsertSize;
    }

    public Integer getMaxInsertSize() {
        return maxInsertSize;
    }

    public void setMaxInsertSize(Integer maxInsertSize) {
        this.maxInsertSize = maxInsertSize;
    }

    public Integer getNumberOfReads() {
        return numberOfReads;
    }

    public void setNumberOfReads(Integer numberOfReads) {
        this.numberOfReads = numberOfReads;
    }

    public Set<Read> getReads() {
        return reads;
    }

    public void setReads(Set<Read> reads) {
        this.reads = reads;
    }

    public String getSampleAcc() {
        return sampleAcc;
    }

    public void setSampleAcc(String sampleAcc) {
        this.sampleAcc = sampleAcc;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * **************************************************************************************
     * methods below provided for backwards compatibility
     */
    public String getIntellectualPropertyNotice() {
        return sample.getIntellectualPropertyNotice();
    }

    public Set<Sample> getSamples() {
        if (samples == null) samples = new HashSet();
        samples.clear();
        samples.add(sample);
        return samples;
    }

    public void setSamples(Set<Sample> samples) {
        setSample(samples.iterator().next());
    }
}
