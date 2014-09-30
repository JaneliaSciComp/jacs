package org.janelia.it.jacs.compute.largevolume;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by fosterl on 9/26/14.
 */
public class RawTiffFetcher {

    public static final String TIFF_0_SUFFIX = "-ngc.0.tif";
    public static final String TIFF_1_SUFFIX = "-ngc.1.tif";
    private Map<List<Integer>, RawDataHandle> centroidToRawDataHandle;
    private CoordinateToRawTransform transform;

    public RawTiffFetcher( TileBase tileBase, File renderedBaseDirectory ) {
        centroidToRawDataHandle = new TileWalker( tileBase ).getCentroidToRawData();
        transform = new CoordinateToRawTransform( renderedBaseDirectory );
    }

    /**
     * Returns the file which should contain the point indicated by the LVV coordinates.  Note that this
     * file is specific to the server, and may need to be cached/resolved on client side.
     *
     * @param lvvCoords from large volume viewer.
     * @return file from the microscope.
     * @throws Exception from called methods.
     */
    public File getMicroscopeFileDir(int[] lvvCoords) throws Exception {
        int[] microScopeCoords = transform.getMicroscopeCoordinate( lvvCoords );
        List<Integer> closestCentroid = getClosestCentroid(microScopeCoords);
        RawDataHandle handle = centroidToRawDataHandle.get( closestCentroid );
        File tiffFile = new File( handle.getBasePath() + handle.getRelativePath() );
        return tiffFile;
    }

    public File[] getMicroscopeFiles( File fileDir ) throws Exception {
        String lastLegDirName = fileDir.getName();
        File[] rtnVal = new File[] {
                new File( fileDir, lastLegDirName + TIFF_0_SUFFIX),
                new File( fileDir, lastLegDirName + TIFF_1_SUFFIX ),
        };

        return rtnVal;
    }

    /**
     * Given microscope/stage coordinates, return the closest centroid from the centroid set from the tilebase.
     *
     * @param microScopeCoords some point expressed in the overall stage space.
     * @return centroid for some raw tiff, in which that point falls.
     */
    public List<Integer> getClosestCentroid(int[] microScopeCoords) {
        List<Integer> closestCentroid = null;
        double squareOfClosestDistance = 0;
        for ( List<Integer> centroid: centroidToRawDataHandle.keySet() ) {
            if ( closestCentroid == null ) {
                closestCentroid = centroid;
            }
            else {
                double squareCentroidDistance = getSquareCentroidDistance(centroid, microScopeCoords);
                if ( squareOfClosestDistance > squareCentroidDistance ) {
                    closestCentroid = centroid;
                    squareOfClosestDistance = squareCentroidDistance;
                }
            }
        }
        return closestCentroid;
    }

    /**
     * Note: do not need actual distance, only to know what is the smallest 'distance'.  Therefore,
     * not bothering to take square root, which would incur more overhead.
     *
     * @param centroid how close _to
     * @param coords how close _is
     * @return square of distance
     */
    public double getSquareCentroidDistance(List<Integer> centroid, int[] coords) {
        int xDist = centroid.get(0) - coords[0];
        int yDist = centroid.get(1) - coords[1];
        int zDist = centroid.get(2) - coords[2];
        return  xDist * xDist +
                yDist * yDist +
                zDist * zDist;
    }
}
