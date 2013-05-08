package org.janelia.it.jacs.model.user_data.tiledMicroscope;

/**
 * Created with IntelliJ IDEA.
 * User: murphys
 * Date: 5/7/13
 * Time: 5:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class TmNeuronDescriptor {
    private Long id;
    private String name;
    private int geometricAnnotationCount;

    public TmNeuronDescriptor(Long id, String name, int geometricAnnotationCount) {
        this.id = id;
        this.name = name;
        this.geometricAnnotationCount = geometricAnnotationCount;
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

    public int getGeometricAnnotationCount() {
        return geometricAnnotationCount;
    }

    public void setGeometricAnnotationCount(int geometricAnnotationCount) {
        this.geometricAnnotationCount = geometricAnnotationCount;
    }
}
