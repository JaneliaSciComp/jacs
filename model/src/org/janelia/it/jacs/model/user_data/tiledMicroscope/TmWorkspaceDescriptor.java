package org.janelia.it.jacs.model.user_data.tiledMicroscope;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/7/13
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TmWorkspaceDescriptor {
    private Long id;
    private String name;
    int neuronCount;

    public TmWorkspaceDescriptor(Long id, String name, int neuronCount) {
        this.id = id;
        this.name = name;
        this.neuronCount = neuronCount;
    }

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

    public int getNeuronCount() {
        return neuronCount;
    }

    public void setNeuronCount(int neuronCount) {
        this.neuronCount = neuronCount;
    }
}
