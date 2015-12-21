package org.janelia.it.jacs.model.domain.sample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.janelia.it.jacs.model.domain.AbstractDomainObject;
import org.janelia.it.jacs.model.domain.support.MongoMapped;
import org.janelia.it.jacs.model.domain.support.SearchAttribute;
import org.janelia.it.jacs.model.domain.support.SearchType;

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
public class Sample extends AbstractDomainObject {

    @SearchAttribute(key="age_txt",label="Age",facet=true)
    private String age;
    
    @SearchAttribute(key="chan_spec_s",label="Channel Specification",facet=true)
    private String chanSpec;
    
    @SearchAttribute(key="data_set_txt",label="Data Set",facet=true)
    private String dataSet;
    
    @SearchAttribute(key="effector_txt",label="Effector")
    private String effector;
    
    @SearchAttribute(key="gender_s",label="Gender",facet=true)
    private String gender;
    
    @SearchAttribute(key="line_txt",label="Line")
    private String line;
    
    @SearchAttribute(key="slide_code_txt",label="Slide Code")
    private String slideCode;
    
    @SearchAttribute(key="status_txt",label="Status")
    private String status;
    
    @SearchAttribute(key="visited_s",label="Visited")
    private String visited;
    
    private Map<String, ObjectiveSample> objectives;

    public List<String> getOrderedObjectives() {
        if (objectives==null || objectives.isEmpty()) return null;
        List<String> sortedObjectives = new ArrayList<>(objectives.keySet());
        Collections.sort(sortedObjectives);
        return sortedObjectives;
    }
    
    public ObjectiveSample getObjectiveSample(String objective) {
        if (objectives==null) return null;
        return objectives.get(objective);
    }

    /* EVERYTHING BELOW IS AUTO-GENERATED */
    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getChanSpec() {
        return chanSpec;
    }

    public void setChanSpec(String chanSpec) {
        this.chanSpec = chanSpec;
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

    public String getVisited() {
        return visited;
    }

    public void setVisited(String visited) {
        this.visited = visited;
    }

    public Map<String, ObjectiveSample> getObjectives() {
        return objectives;
    }

    public void setObjectives(Map<String, ObjectiveSample> objectives) {
        this.objectives = objectives;
    }
}
