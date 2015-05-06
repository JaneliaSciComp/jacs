/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.mesh_loader.wavefront_obj;

import org.janelia.it.jacs.shared.mesh_loader.Triangle;
import org.janelia.it.jacs.shared.mesh_loader.TriangleSource;
import org.janelia.it.jacs.shared.mesh_loader.VertexInfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Created by fosterl on 5/5/14.
 */
public class OBJWriter {
    public static final String FILE_SUFFIX = ".obj";
    private Logger logger = LoggerFactory.getLogger(OBJWriter.class);

    /**
     * Exposing this method to allow client code to check whether the file they "would" be making later, is going to
     * be possible to use.
     *
     * @param outputLocation directory for file.
     * @param filenamePrefix first part of file name.
     * @param fileSuffix last part of file name.
     * @param key distinguising part of file name.
     * @return file file path.
     */
    public File getVertexFile( File outputLocation, String filenamePrefix, String fileSuffix, Long key ) {
        //NOTE: this may change later.  May need different file name suffixes. OR the prefix may cover it sufficiently.
        File outputFile = new File( outputLocation, filenamePrefix + "_" + key + fileSuffix );
        return outputFile;
    }

    /**
     * Get the relevant data from the factory to write a file, with a standardized name.
     *
     * @param outputLocation directory for file.
     * @param filenamePrefix first part of file name.
     * @param fileSuffix last part of file name.
     * @param key distinguising part of file name.  Must be relevant to the triangle source.
     * @param factory
     * @throws IOException thrown by failed io opertions.
     */
    public void writeVertices(File outputLocation, String filenamePrefix, String fileSuffix, Long key, TriangleSource triangleSource) throws IOException {
        writeVertices(getVertexFile(outputLocation, filenamePrefix, fileSuffix, key), triangleSource);
    }

    /**
     * Get the relevant data from the factory to write a file, with a standardized name.
     *
     * @param outputLocation directory for file.
     * @param filenamePrefix first part of file name.
     * @param fileSuffix last part of file name.
     * @param factory
     * @throws IOException thrown by failed io opertions.
     */
    public void writeVertices(File outputLocationFile, TriangleSource triangleSource) throws IOException {
        PrintWriter objWriter = new PrintWriter( outputLocationFile );

        // Going over vertices twice: once for the geometry values, and once for the normals.
        List<VertexInfoBean> vertices = triangleSource.getVertices();
        for ( VertexInfoBean bean: vertices ) {
            double[] coords = bean.getKey().getPosition();
            objWriter.print("v");
            for ( double coord: coords ) {
                objWriter.print(" ");
                objWriter.print(coord);
            }
            objWriter.println();
        }

        for ( VertexInfoBean bean: vertices ) {
            float[] normal = bean.getKnownAttribute( VertexInfoBean.KnownAttributes.normal );
            if ( normal != null ) {
                objWriter.print("vn");
                for ( float normalElement: normal ) {
                    objWriter.print(" ");
                    objWriter.print(normalElement);
                }
                objWriter.println();
            }
            else {
                logger.warn( "No normals defined for " + bean.getKey() );
            }
        }

        List<Triangle> triangles = triangleSource.getTriangleList();
        for ( Triangle triangle: triangles ) {
            List<VertexInfoBean> triangleVertices = triangle.getVertices();
            objWriter.print("f");
            for ( VertexInfoBean triangleVertex: triangleVertices ) {
                objWriter.print(" ");
                int offset = triangleVertex.getVtxBufOffset() + 1;
                objWriter.print(offset);
            }
            objWriter.println();
        }

        objWriter.close();
    }

}
