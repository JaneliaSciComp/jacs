/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.skeleton_mesh;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.List;
import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoKey;
import org.janelia.it.workstation.gui.viewer3d.matrix_support.ViewMatrixSupport;

/**
 * Can build up mesh enclosures over lines.  Lines are defined as pairs
 * of endpoints.
 * 
 * @author fosterl
 */
public class LineEnclosureFactory implements TriangleSource {
    
    private List<VertexInfoBean> vertices = new ArrayList<>();
    private List<Triangle> triangles = new ArrayList<>();
    private ViewMatrixSupport matrixUtils = new ViewMatrixSupport();
    
    private int endPolygonSides = -1;
    private double endPolygonRadius; 
    
    private double[][] prototypeEndPolygon;
    
    public LineEnclosureFactory(int endPolygonSides, double endPolygonRadius) {
        this.endPolygonSides = endPolygonSides;
        this.endPolygonRadius = endPolygonRadius;
        this.prototypeEndPolygon = createPrototypeEndPolygon();
    }

    /**
     * Makes a set of triangles (geometry) to make a "pipe" around
     * the pair of starting/ending coords.  May make overhangs for "elbow
     * convenience."
     * 
     * @param startingCoords one end.
     * @param endingCoords other end.
     */
    public void addEnclosure( double[] startingCoords, double[] endingCoords ) {
        if ( startingCoords.length != 3 ) {
            throw new IllegalArgumentException("3-D starting coords only.");
        }
        if (endingCoords.length != 3) {
            throw new IllegalArgumentException("3-D ending coords only.");
        }
        
        double[] deltasOverLength = computeDeltasOverLength( startingCoords, endingCoords );
        double[][] startPoly = makeEndPolygonForPoint( startingCoords, deltasOverLength );
        double[][] endPoly = makeEndPolygonForPoint( endingCoords, deltasOverLength );
        addVertices(startPoly);
        addVertices(endPoly);
        Triangle t = new Triangle();
    }

    //--------------------------------------------IMPLEMENT TriangleSource
    @Override
    public List<VertexInfoBean> getVertices() {
        return vertices;
    }

    @Override
    public List<Triangle> getTriangleList() {
        return triangles;
    }

    //--------------------------------------------HELPERS
    protected void addVertices(double[][] poly) {
        for (int i = 0; i < poly.length; i++) {
            VertexInfoBean bean = new VertexInfoBean();
            VertexInfoKey key = new VertexInfoKey();
            key.setPosition(poly[i]);
            bean.setKey(key);
            vertices.add(bean);
        }
    }

    private double[][] createPrototypeEndPolygon() {
        double[][] prototypeEndPolygon = new double[ endPolygonSides ][];
        prototypeEndPolygon[0] = new double[] { -endPolygonRadius, 0f, 0f };
        double thetaIncrement = 2.0 * Math.PI / endPolygonSides;
        double theta = 0;
        for ( int i = 1; i < endPolygonSides; i++ ) {
            theta += thetaIncrement;
            float x = (float)(Math.cos(theta) * endPolygonRadius);
            float y = (float)(Math.sin(theta) * endPolygonRadius);
            
            prototypeEndPolygon[i] = new double[] { x, y, 0f };
        }
        
        return prototypeEndPolygon;
    }
    
    private double[] computeDeltasOverLength( double[] startCoords, double[] endCoords ) {
        double[] rtnVal = new double[ 3 ];
        double[] lineDelta = getLineDelta(startCoords, endCoords);
        
        double accum = 0.0;
        for (int i = 0; i < 3; i++) {
            accum += lineDelta[i] * lineDelta[i];
        }
        double lineLen = Math.sqrt(accum);

        rtnVal[ 0 ] = lineDelta[0] / lineLen;
        rtnVal[ 1 ] = lineDelta[1] / lineLen;
        rtnVal[ 2 ] = lineDelta[2] / lineLen;
        
        return rtnVal;
    }
    
    private double[][] makeEndPolygonForPoint( double[] coords, double[] deltasOverLength ) {
        return makeEndPolygon(deltasOverLength[0], deltasOverLength[1], deltasOverLength[2], coords);        
    }

    protected double[][] makeEndPolygon(
            double deltaYslashLen, double deltaZslashLen, double deltaXslashLen, 
            double[] coords) {

        double[][] polygon = new double[endPolygonSides][3];
        // Clone the prototype.
        for (int i = 0; i < endPolygonSides; i++) {
            System.arraycopy(prototypeEndPolygon[i], 0, polygon[i], 0, 3);
        }

        // Establish the positioning matrix.
        Matrix transformMatrix = matrixUtils.getTransform3D(
                deltaYslashLen, deltaZslashLen, 
                deltaXslashLen, deltaZslashLen,
                deltaYslashLen, deltaXslashLen, 
                coords[0], coords[1], coords[2]);
        for ( int i = 0; i < polygon.length; i++ ) {
            polygon[i] = matrixUtils.transform( transformMatrix, polygon[i] );
        }
        return polygon;
    }
    
    private double[] getLineDelta( double[] startCoords, double[] endCoords ) {
        double[] delta = new double[ startCoords.length ];
        for ( int i = 0; i < startCoords.length; i++ ) {
            delta[ i ] = startCoords[ i ] - endCoords[ i ];
        }
        return delta;
    }
}
