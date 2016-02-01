/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import Jama.Matrix;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A collection of neurons, complete with a version, owner, etc.
 *
 * @author fosterl
 */
public class TmVersionedNeuronCollection {
    
    public static final String ID_TO_TAG_NAME = "id_to_tag";
    
    public static final String DEFAULT_OWNER_KEY = "group:mouselight";
    public static final String COLLECTIONS_FOLDER_NAME = "3D Tile Annotation Collections";
    
    private Long id;
    private String name;
    // defaulted owner key.
    public String ownerKey = DEFAULT_OWNER_KEY;
    private Long sampleID;
    private List<TmNeuron> neuronList;
    private Date createDate;
    private Map<Long,String> neuronIdVsTag;

    private String version;
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSampleID() {
        return sampleID;
    }

    public void setSampleID(Long id) {
        this.sampleID = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getOwnerKey() {
        return ownerKey;
    }

    public void setOwnerKey(String ownerKey) {
        this.ownerKey = ownerKey;
    }

    public List<TmNeuron> getNeuronList() {
        return neuronList;
    }

    public void setNeuronList(List<TmNeuron> neuronList) {
        this.neuronList = neuronList;
    }    

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @return the createDate
     */
    public Date getCreateDate() {
        return createDate;
    }

    /**
     * @param createDate the createDate to set
     */
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    /**
     * @return the neuronIdVsTag
     */
    public Map<Long,String> getNeuronIdVsTag() {
        return neuronIdVsTag;
    }

    /**
     * @param neuronIdVsTag the neuronIdVsTag to set
     */
    public void setNeuronIdVsTag(Map<Long,String> neuronIdVsTag) {
        this.neuronIdVsTag = neuronIdVsTag;
    }
}
