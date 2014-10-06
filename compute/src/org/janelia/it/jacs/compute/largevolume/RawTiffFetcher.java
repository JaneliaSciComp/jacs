package org.janelia.it.jacs.compute.largevolume;

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

    private static final double DISTANCE_DOWN_SCALER = 1000000.0;
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
        double squareOfClosestDistance = (double)Float.MAX_VALUE;
int debugI = 0;
        for ( List<Integer> centroid: centroidToRawDataHandle.keySet() ) {
            if ( closestCentroid == null ) {
                closestCentroid = centroid;
            }
            else {
                double squareCentroidDistance = getCentroidDistanceMetric(centroid, microScopeCoords);
//if (++debugI % 100 == 0)
//System.out.println(String.format(
//        "InputCoords: [%,d %,d %,d].  TestedCentroid: [%,d %,d %,d].  Square of Closest distance is: %f. ("+debugI+")",
//        microScopeCoords[0],microScopeCoords[1],microScopeCoords[2],
//        centroid.get(0), centroid.get(1), centroid.get(2),
//        squareOfClosestDistance
//    )
//);
                if ( squareOfClosestDistance > squareCentroidDistance ) {
                    closestCentroid = centroid;
                    squareOfClosestDistance = squareCentroidDistance;
                }
            }
        }
        // debug
//        System.out.println(String.format(
//                        "InputCoords: [%,d %,d %,d].  ClosestCentroid: [%,d %,d %,d].  SquareOfClosestDistance: %f.",
//                        microScopeCoords[0],microScopeCoords[1],microScopeCoords[2],
//                        closestCentroid.get(0), closestCentroid.get(1), closestCentroid.get(2),
//                        squareOfClosestDistance
//                )
//        );
        return closestCentroid;
    }

    /**
     * Note: do not need actual distance, only to know what is the smallest 'distance'.  Therefore,
     * not bothering to take square root, which would incur more overhead.  However, do need to scale all the
     * values by some large number to prevent double overflow.  Distances are significant enough to cause that
     * if squared.
     *
     * @param centroid how close _to
     * @param coords how close _is
     * @return square of distance
     */
    public double getCentroidDistanceMetric(List<Integer> centroid, int[] coords) {
        double xDist = (centroid.get(0) - coords[0]) / DISTANCE_DOWN_SCALER;
        double yDist = (centroid.get(1) - coords[1]) / DISTANCE_DOWN_SCALER;
        double zDist = (centroid.get(2) - coords[2]) / DISTANCE_DOWN_SCALER;
        return  (xDist * xDist) +
                (yDist * yDist) +
                (zDist * zDist);
    }
}
