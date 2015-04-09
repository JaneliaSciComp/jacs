/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.centroid;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author fosterl
 */
public class CentroidCalculator {
    private static final double DISTANCE_DOWN_SCALER = 1000000.0;

    public List<Integer> calculateCentroid(int[][] corners) {
        final int cornerInxCount = corners[0].length;
        List<Integer> centroid = new ArrayList<>();
        int[] farCorner = new int[cornerInxCount];
        int[] nearCorner = new int[cornerInxCount];

        // Find extreme corners.
        for (int i = 0; i < cornerInxCount; i++) {
            nearCorner[i] = Integer.MAX_VALUE;
            farCorner[i] = Integer.MIN_VALUE;
        }

        for (int[] corner : corners) {
            for (int i = 0; i < cornerInxCount; i++) {
                if (corner[i] < nearCorner[i]) {
                    nearCorner[i] = corner[i];
                }
                if (corner[i] > farCorner[i]) {
                    farCorner[i] = corner[i];
                }
            }
        }

        // Find midpoints between extreme corners.
        for (int i = 0; i < cornerInxCount; i++) {
            centroid.add((farCorner[ i] + nearCorner[ i]) / 2);
        }

        return centroid;
    }

    public List<Integer> calculateCentroid( Integer[] originInteger, Integer[] shapeInteger ) {
        if ( shapeInteger.length != originInteger.length ) {
            throw new IllegalArgumentException("Incompatible matrix sizes: " + shapeInteger.length + " vs " + originInteger.length);
        }
        Integer[] farCornerInteger = new Integer[ originInteger.length ];
        for ( int i = 0; i < originInteger.length; i++ ) {
            farCornerInteger[ i ] = originInteger[ i ] + shapeInteger[ i ];
        }

        List<Integer> centroid = new ArrayList<>();
        for ( int i = 0; i < originInteger.length; i++ ) {
            centroid.add((farCornerInteger[ i ] + originInteger[ i ]) / 2);
        }

        return centroid;
    }

    /**
     * Given microscope/stage coordinates, return the closest centroid from the centroid set, from coords.
     *
     * @param microScopeCoords some point expressed in the overall stage space.
     * @param centroids all possible centroids.
     * @return centroid for some raw tiff, in which that point falls.
     */
    public List<Integer> getClosestCentroid(int[] microScopeCoords, Set<List<Integer>> centroids) {
        List<Integer> closestCentroid = null;
        double squareOfClosestDistance = (double)Float.MAX_VALUE;
        for ( List<Integer> centroid: centroids ) {
            if ( closestCentroid == null ) {
                closestCentroid = centroid;
            }
            else {
                double squareCentroidDistance = getCentroidDistanceMetric(centroid, microScopeCoords);
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
