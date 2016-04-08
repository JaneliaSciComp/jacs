package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SAGEAttribute;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

/**
 * All the processing results of a particular specimen. Uniqueness of a Sample is determined by a combination 
 * of data set and slide code. A single sample may include many LSMs. For example, it may include images taken 
 * at multiple objectives (e.g. 20x/63x), of different anatomical areas (e.g. Brain/VNC), and of different 
 * tile regions which are stitched together.   
 *
 * Contains references to NeuronFragment objects in the fragment collection.  
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@MongoMapped(collectionName="sample",label="Sample")
@SearchType(key="sample",label="Sample")
public class Sample extends AbstractDomainObject implements IsParent {

    @SAGEAttribute(cvName="light_imagery", termName="age")
    @SearchAttribute(key="age_txt",label="Age",facet="age_s")
    private String age;

    @SAGEAttribute(cvName="light_imagery", termName="data_set")
    @SearchAttribute(key="data_set_txt",label="Data Set",facet="data_set_s")
    private String dataSet;

    @SAGEAttribute(cvName="fly", termName="effector")
    @SearchAttribute(key="effector_txt",label="Effector")
    private String effector;

    @SAGEAttribute(cvName="line", termName="flycore_alias")
    @SearchAttribute(key="fcalias_s",label="Fly Core Alias")
    private String flycoreAlias;

    @SAGEAttribute(cvName="light_imagery", termName="gender")
    @SearchAttribute(key="gender_txt",label="Gender",facet="gender_s")
    private String gender;

    @SAGEAttribute(cvName="image_query", termName="line")
    @SearchAttribute(key="line_txt",label="Line")
    private String line;

    @SAGEAttribute(cvName="light_imagery", termName="slide_code")
    @SearchAttribute(key="slide_code_txt",label="Slide Code")
    private String slideCode;
    
    @SearchAttribute(key="status_txt",label="Status")
    private String status;
    
    @SearchAttribute(key="visited_b",label="Visited")
    private Boolean visited = false;
    
    @SearchAttribute(key="sage_synced",label="SAGE Synchronized")
    private Boolean sageSynced = false;

    @SearchAttribute(key="compression_txt",label="Compression Type")
    private String compressionType;

    @SearchAttribute(key="tmog_dt",label="TMOG Date")
    private Date tmogDate;
    
    @SearchAttribute(key="completion_dt",label="Completion Date")
    private Date completionDate;
    
    private List<ObjectiveSample> objectiveSamples = new ArrayList<>();

    @JsonProperty
    public List<ObjectiveSample> getObjectiveSamples() {
        return objectiveSamples;
    }

    @JsonProperty
    public void setObjectiveSamples(List<ObjectiveSample> objectiveSamples) {
        if (objectiveSamples==null) throw new IllegalArgumentException("Property cannot be null");
        this.objectiveSamples = objectiveSamples;
        for(ObjectiveSample objectiveSample : objectiveSamples) {
            objectiveSample.setParent(this);
        }
        resortObjectiveSamples();
    }
    
    @JsonIgnore
    public void addObjectiveSample(ObjectiveSample objectiveSample) {
        objectiveSample.setParent(this);
        objectiveSamples.add( objectiveSample);
        resortObjectiveSamples();
    }

    @JsonIgnore
    private void resortObjectiveSamples() {
        Collections.sort(objectiveSamples, new Comparator<ObjectiveSample>() {
            @Override
            public int compare(ObjectiveSample o1, ObjectiveSample o2) {
                return ComparisonChain.start()
                        .compare(o1.getObjective(), o2.getObjective(), Ordering.natural().nullsLast())
                        .result();
            }
        });
    }

    @JsonIgnore
    public void removeObjectiveSample(ObjectiveSample objectiveSample) {
        if (objectiveSamples.remove(objectiveSample)) {
            objectiveSample.setParent(null);
        }
    }

    @JsonIgnore
    public ObjectiveSample getObjectiveSample(String objective) {
        for(ObjectiveSample objectiveSample : objectiveSamples) {
            if (objectiveSample.getObjective().equals(objective)) {
                return objectiveSample;
            }
        }
        return null;
    }
    
    @JsonIgnore
    public List<String> getObjectives() {
        List<String> objectives = new ArrayList<>();
        for(ObjectiveSample objectiveSample : objectiveSamples) {
            objectives.add(objectiveSample.getObjective());
        }
        return objectives;
    }
    
    @JsonIgnore
    public <T extends PipelineResult> List<T> getResultsById(Class<T> resultClass, Long resultEntityId) {
        List<T> results = new ArrayList<>();
        for(ObjectiveSample objectiveSample : getObjectiveSamples()) {
            results.addAll(objectiveSample.getResultsById(resultClass, resultEntityId));
        }
        return results;
    }

    @JsonIgnore
	public List<Reference> getLsmReferences() {
		List<Reference> refs = new ArrayList<>();
        for(ObjectiveSample objectiveSample : getObjectiveSamples()) {
        	for(SampleTile sampleTile : objectiveSample.getTiles()) {
        		refs.addAll(sampleTile.getLsmReferences());
        	}
        }
		return refs;
	}

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getEffector() {
        return effector;
    }

    public void setEffector(String effector) {
        this.effector = effector;
    }
    
    public String getFlycoreAlias() {
        return flycoreAlias;
    }

    public void setFlycoreAlias(String flycoreAlias) {
        this.flycoreAlias = flycoreAlias;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getSlideCode() {
        return slideCode;
    }

    public void setSlideCode(String slideCode) {
        this.slideCode = slideCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getVisited() {
        return visited;
    }

    public void setVisited(Boolean visited) {
        this.visited = visited;
    }

    public Boolean getSageSynced() {
		return sageSynced;
	}

	public void setSageSynced(Boolean sageSynced) {
		this.sageSynced = sageSynced;
	}
	
    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }
    
    public Date getTmogDate() {
        return tmogDate;
    }

    public void setTmogDate(Date tmogDate) {
        this.tmogDate = tmogDate;
    }

    public Date getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(Date completionDate) {
        this.completionDate = completionDate;
    }

}
