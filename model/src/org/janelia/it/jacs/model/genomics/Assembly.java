
package org.janelia.it.jacs.model.genomics;

import com.google.gwt.user.client.rpc.IsSerializable;

import java.io.Serializable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: tdolafi
 * Date: Aug 4, 2006
 * Time: 10:19:18 AM
 */
public class Assembly implements Serializable, IsSerializable {

    private Long assemblyId;
    private String assemblyAcc;
    private String description;
    private Integer taxonId;
    private String organism;
    private String sampleAcc;
    private Sample sample;
    private String projectSymbol;
    private String status;

    private Set<Library> librarySet;

    public Assembly() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssemblyId() {
        return assemblyId;
    }

    public void setAssemblyId(Long assemblyId) {
        this.assemblyId = assemblyId;
    }

    public String getAssemblyAcc() {
        return assemblyAcc;
    }

    public void setAssemblyAcc(String assemblyAcc) {
        this.assemblyAcc = assemblyAcc;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTaxonId() {
        return taxonId;
    }

    public void setTaxonId(Integer taxonId) {
        this.taxonId = taxonId;
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

    public Set<Library> getLibrarySet() {
        return librarySet;
    }

    public void setLibrarySet(Set<Library> librarySet) {
        this.librarySet = librarySet;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;

        Assembly assembly = (Assembly) o;

        return !(assemblyAcc != null ? !assemblyAcc.equals(assembly.assemblyAcc) : assembly.assemblyAcc != null);

    }

    public int hashCode() {
        return (assemblyAcc != null ? assemblyAcc.hashCode() : 0);
    }

    public String toString() {
        return "assemblyAcc=" + this.assemblyAcc + " assemblyId=" + this.assemblyId;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getProjectSymbol() {
        return projectSymbol;
    }

    public void setProjectSymbol(String projectSymbol) {
        this.projectSymbol = projectSymbol;
    }
}
