package org.janelia.it.jacs.model.user_data.tiledMicroscope;

import org.janelia.it.jacs.model.entity.Entity;
import org.janelia.it.jacs.model.entity.EntityConstants;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/1/13
 * Time: 1:21 PM
 * To change this template use File | Settings | File Templates.
 */

public class TmWorkspace {
    Long id;
    String name;
    String ownerKey;
    List<TmNeuron> neuronList;
    TmPreferences preferences;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public TmPreferences getPreferences() {
        return preferences;
    }

    public void setPreferences(TmPreferences preferences) {
        this.preferences = preferences;
    }

    public TmWorkspace(Long id, String name, String ownerKey, List<TmNeuron> neuronList, TmPreferences preferences) {
        this.id=id;
        this.name=name;
        this.ownerKey=ownerKey;
        this.neuronList=neuronList;
        this.preferences=preferences;
    }

    // This method will typically be used to generate a TmWorkspace object after the Workspace entity has been initially
    // retrieved from the database.

    public TmWorkspace(Entity entity) throws Exception {
        if (entity.getEntityType()==null || !entity.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE)) {
            throw new Exception("Entity type must be="+EntityConstants.TYPE_TILE_MICROSCOPE_WORKSPACE);
        }
        this.id=entity.getId();
        this.name=entity.getName();
        this.ownerKey=entity.getOwnerKey();
        for (Entity child : entity.getChildren()) {
            if (child.getEntityType().getName().equals(EntityConstants.TYPE_TILE_MICROSCOPE_NEURON)) {
                TmNeuron neuron=new TmNeuron(child);
                neuronList.add(neuron);
            } else if (child.getEntityType().getName().equals(EntityConstants.TYPE_PROPERTY_SET)) {
                preferences=new TmPreferences(child);
            }
        }
    }


}
