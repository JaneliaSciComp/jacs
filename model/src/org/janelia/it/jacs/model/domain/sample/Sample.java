package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.interfaces.IsParent;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @SearchAttribute(key="age_txt",label="Age",facet="age_s")
    private String age;
    
    @SearchAttribute(key="data_set_txt",label="Data Set",facet="data_set_s")
    private String dataSet;
    
    @SearchAttribute(key="effector_txt",label="Effector")
    private String effector;
    
    @SearchAttribute(key="gender_txt",label="Gender",facet="gender_s")
    private String gender;
    
    @SearchAttribute(key="line_txt",label="Line")
    private String line;
    
    @SearchAttribute(key="slide_code_txt",label="Slide Code")
    private String slideCode;
    
    @SearchAttribute(key="status_txt",label="Status")
    private String status;
    
    @SearchAttribute(key="visited_b",label="Visited")
    private Boolean visited;

    @SearchAttribute(key="compression_txt",label="Compression Type")
    private String compressionType;

    @SearchAttribute(key="tmog_dt",label="TMOG Date")
    private Date tmogDate;
    
    @SearchAttribute(key="completion_dt",label="Completion Date")
    private Date completionDate;
    
    private Map<String, ObjectiveSample> objectives;
    
    public Map<String, ObjectiveSample> getObjectives() {
        return Collections.unmodifiableMap(objectives);
    }

    @JsonProperty
    public void setObjectives(Map<String, ObjectiveSample> objectives) {
        for(String objective : objectives.keySet()) {
            ObjectiveSample objectiveSample = objectives.get(objective);
            objectiveSample.setObjective(objective);
            objectiveSample.setParent(this);
        }
        this.objectives = objectives;
    }
    
    @JsonIgnore
    public void addObjectiveSample(String objective, ObjectiveSample objectiveSample) {
        if (objectives==null) {
            this.objectives = new HashMap<>();
        }
        objectiveSample.setObjective(objective);
        objectiveSample.setParent(this);
        objectives.put(objective, objectiveSample);
    }

    @JsonIgnore
    public void removeObjectiveSample(String objective, ObjectiveSample objectiveSample) {
        if (objectives==null) {
            return;
        }
        objectiveSample.setParent(null);
        objectives.remove(objective);
    }

    @JsonIgnore
    public List<String> getOrderedObjectives() {
        if (objectives==null || objectives.isEmpty()) return null;
        List<String> sortedObjectives = new ArrayList<>(objectives.keySet());
        Collections.sort(sortedObjectives);
        return Collections.unmodifiableList(sortedObjectives);
    }

    @JsonIgnore
    public ObjectiveSample getObjectiveSample(String objective) {
        if (objectives==null) return null;
        return objectives.get(objective);
    }

    @JsonIgnore
    public List<ObjectiveSample> getObjectiveSamples() {
        List<ObjectiveSample> samples = new ArrayList<>();
        for(String objective : getOrderedObjectives()) {
            samples.add(getObjectiveSample(objective));
        }
        return samples;
    }
    
    @JsonIgnore
    public PipelineResult findResultById(Long id) {
        for(String objective : getOrderedObjectives()) {
            ObjectiveSample objectiveSample = getObjectiveSample(objective);
            if (!objectiveSample.hasPipelineRuns()) continue;
            for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                if (!run.hasResults()) continue;
                for(PipelineResult result : run.getResults()) {
                    if (result!=null && result.getId()!=null && result.getId().equals(id)) {
                        return result;
                    }
                }
            }
        }
        return null;
    }
    
    /* EVERYTHING BELOW IS AUTO-GENERATED */
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
