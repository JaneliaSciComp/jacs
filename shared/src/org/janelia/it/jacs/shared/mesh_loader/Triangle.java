package org.janelia.it.jacs.shared.mesh_loader;

import java.util.ArrayList;
import java.util.List;

/**
 * This essentially tracks which vertices are in a given triangle.  It is necessary to keep track of triangles
 * so that the vertices, themselves could have including-triangle-averaged normal vectors calculated for them.
 *
 * Created by fosterl on 4/3/14.
 */
public class Triangle {
    private final List<VertexInfoBean> vertices = new ArrayList<>();
    private AxialNormalDirection normalVector = AxialNormalDirection.NOT_APPLICABLE;
    private double[] customNormal; // Only needed if the axial normal is N/A.
    
    public void addVertex( VertexInfoBean bean ) {
        vertices.add( bean );
        // Need to ensure all is consistent.
        bean.addIncludingTriangle( this );
    }

    public AxialNormalDirection getNormalVector() {
        return normalVector;
    }
    
    /**
     * Call this only if the
     * @see #getNormalVector() returns 'not applicable' enum value.
     * @return array of double representing normal unit vector.
     */
    public double[] getCustomNormal() {
        if (customNormal == null) {            
            customNormal = NormalCompositor.computeNormal( vertices );
        }
        return customNormal;
    }

    /**
     * Calling this is optional.  Some triangles may not adhere to an
     * axial normal direction (aligned with x, y or z).
     * 
     * @param normalVector one of the axial directions, with unit vector normal.
     */
    public void setNormalVector(AxialNormalDirection normalVector) {
        this.normalVector = normalVector;
    }
    public List<VertexInfoBean> getVertices() { return vertices; }
        
}
