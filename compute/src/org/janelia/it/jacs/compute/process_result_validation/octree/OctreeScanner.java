/**
 * Created by hand
 * User: fosterl
 * Date: Mar 25, 2015
 * Time: 1:41 PM
 *
 * Scans log output from LVV-targeted Octree build runs.
 */
package org.janelia.it.jacs.compute.process_result_validation.octree;

import java.io.*;
import java.util.*;
import org.janelia.it.jacs.compute.centroid.CentroidCalculator;

import org.janelia.it.jacs.compute.largevolume.TileBaseReader;
import org.janelia.it.jacs.compute.largevolume.model.Shape;
import org.janelia.it.jacs.compute.largevolume.model.Tile;
import org.janelia.it.jacs.compute.largevolume.model.TileBase;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;

public class OctreeScanner {

    private File infile;
    private int levels;

    private long expectedSize = -1;

    private byte[][][] volume;
    private int dim;
    
    private CoordinateToRawTransform transform;
    private CentroidCalculator centroidCalculator = new CentroidCalculator();

    public static void main(String[] args) throws Exception {
        boolean runMissingTifScan = false;
        if (args.length < 2) {
            throw new IllegalArgumentException("USAGE: java octree_scan.OctreeScanner <directory base location> <num levels> [1|T=missing-tif scan]");
        }
        else if ( args.length >= 3 ) {
            runMissingTifScan = args[ 2 ].startsWith("1") || args[ 2 ].toLowerCase().startsWith("t");
        }
        File infile = new File( args[0] );
        if (! infile.exists() ) {
            throw new IllegalStateException("Failed to locate " + infile.getAbsolutePath());
        }
        else if ( ! infile.isDirectory() ) {
            throw new IllegalArgumentException( infile + " is not a directory" );
        }
        int levels = Integer.parseInt(args[1]);
        new OctreeScanner(infile, levels).scan(runMissingTifScan);
    }

    public OctreeScanner(File infile, int levels) {
        this.infile = infile;
        transform = new CoordinateToRawTransform( infile );
        this.levels = levels;
        this.dim = (int)Math.pow(2.0, levels);
        this.volume = new byte[dim][dim][dim];
    }

    public void scan( boolean runMissingTifScan ) throws Exception {
        if (runMissingTifScan) {
            scanForMissingTifs(this.infile);
        }
        String baseOctreeCoords = "";
        tileExpectationScan( this.infile, new int[] {0, 0, 0}, Math.pow( 2, levels ), baseOctreeCoords );
        reportSheets();
    }

    private void reportSheets() throws Exception {
        List<String> emptyCoords = new ArrayList<>();
        List<int[]> emptyExpanded = new ArrayList<>();
        Map<int[],String> emptyExpandedToString = new HashMap<>();
        TileBase tileBase = new TileBaseReader().readTileBase( getTilebaseStream() );
        //String path = tileBase.getPath();
        Tile tile = tileBase.getTiles().get(0);
        String basePathForTiles = tileBase.getPath();
        Integer[] shape = tile.getAabb().getShape();

        for ( int z = 0; z < dim; z++ ) {
            System.out.println("===============Sheet " + z);
            for ( int y = 0; y < dim; y++ ) {
                StringBuilder bldr = new StringBuilder();
                for ( int x = 0; x < dim; x++ ) {
                    int volVal = volume[ x ][ y ][ z ];
                    if ( volVal == 3 ) {
                        bldr.append( 'X' );
                    }
                    else if ( volVal == 2 ) {
                        bldr.append( '1' );
                    } 
                    else if ( volVal == 1 ) {
                        bldr.append( '0' );
                    }
                    else {
                        bldr.append( ' ' );

                        if (isIsolated(x, y, z)) {
                            // Shape is actually just the "box dimensions".
                            int[] expandedCoords = new int[] {
                                    (transform.getOrigin()[0] + shape[0] * x ),//(int)(x * scaleMultiplier[0])),
                                    (transform.getOrigin()[1] + shape[1] * y ),//(int)(y * scaleMultiplier[1])),
                                    (transform.getOrigin()[2] + shape[2] * z ),//(int)(z * scaleMultiplier[2])),
                            };
                            emptyExpanded.add(expandedCoords);
                            String emptyCoord = "" + expandedCoords[0] + "," +
                                    expandedCoords[1] + "," + expandedCoords[2];
                            emptyCoords.add(emptyCoord);
                            
                            emptyExpandedToString.put( expandedCoords, emptyCoord );
                        }
                    }
                }
                System.out.println( bldr.toString() );
            }
        }
        
        System.out.println("\nThe following " + emptyCoords.size() + " nanometer coordinates had no representation:");
        for (String emptyCoord: emptyCoords) {
            System.out.println(emptyCoord);
        }

        LocationHostScanner locHostScanner = new LocationHostScanner(infile);
        Map<List<Integer>,List<LocationHostScanner.LocationInfo>> centroidToLocation = locHostScanner.scan();
        // Find nearest centroids for all the no-result expanded coords.  Print their
        // file info for later scrutiny.
        System.out.println("Aside: there are " + centroidToLocation.size() + " centroid->location mappings.");
        System.out.println("\nHere are all locations of nanometer coordinates with no representation in octree:");

        // This collection will hold full paths for all 'bad' input tiles.
        Collection<File> inputTileFolders = new HashSet<>();
        Collection<Integer> inputTileIndexes = new HashSet<>();

        for (int[] expandedCoords: emptyExpanded) {
            List<Integer> centroid = 
                    centroidCalculator.getClosestCentroid(expandedCoords, centroidToLocation.keySet());
            List<LocationHostScanner.LocationInfo> locationInfo = centroidToLocation.get(centroid);
            if (locationInfo == null) {
                System.out.println("Failed to find nearest centroid to " + emptyExpandedToString.get(expandedCoords));
            }
            else {
                for (LocationHostScanner.LocationInfo li: locationInfo) {
                    tile = tileBase.getTiles().get(li.getTileIndex());
                    String tilePath = null;
                    if (tile != null ) {
                        tilePath = tile.getPath();
                    }
                    else {
                        tilePath = "Unknown-path";
                    }
                    System.out.println(
                            emptyExpandedToString.get(expandedCoords) +
                                    " location(s) " + li.toString() + ". Input tile path " + tilePath
                    );

                    inputTileFolders.add( new File( basePathForTiles, tilePath ) );
                    inputTileIndexes.add( li.getTileIndex() );
                }
            }
        }

        System.out.println("\nThe following folders contain input tiles involved in failures.");
        for (File folder: inputTileFolders) {
            System.out.println( folder );
        }

        System.out.println("\nThe following tile indexes were involved in failures.");
        for (Integer index: inputTileIndexes) {
            System.out.println( index );
        }
    }

    public InputStream getTilebaseStream() throws Exception {
        File resourceFile = new File(infile, TileBaseReader.STD_TILE_BASE_FILE_NAME);
        return new FileInputStream( resourceFile );
    }

    /**
     * Report whether the coordinates given, are 'surrounded' by non-empty values.
     *
     * @param x y z - coordinates of some point in volume.
     * @return true if surrounded by non-empty values.
     */
    private boolean isIsolated( int x, int y, int z ) {
        boolean rtnVal = true;
        // Algorithm: walk through volume in all cardinal directions (above/below/left/right).
        if ( x < 1  ||  y < 1  ||  z < 1 ) {
            // Not isolated: bound by edge.
            rtnVal = false;
        }
        else if ( x == dim - 1  ||  y == dim - 1  ||  z == dim - 1 ) {
            rtnVal = false;
        }
        else {
            boolean toEdge = true;
            for ( int zOffs = z - 1; zOffs >= 0; zOffs-- ) {
                if (volume[ x ][ y ][ zOffs ] > 0) {
                    toEdge = false;
                }
            }

            if (! toEdge) {
                for (int zOffs = z + 1; zOffs < dim; zOffs++) {
                    if (volume[x][y][zOffs] > 0) {
                        toEdge = false;
                    }
                }
            }

            if (! toEdge) {
                for (int yOffs = y - 1; yOffs >= 0; yOffs--) {
                    if (volume[x][yOffs][z] > 0) {
                        toEdge = false;
                    }
                }
            }

            if (! toEdge) {
                for (int yOffs = 0; yOffs < dim; yOffs++) {
                    if (volume[x][yOffs][z] > 0) {
                        toEdge = false;
                    }
                }
            }
            if (! toEdge) {
                for (int xOffs = x - 1; xOffs >= 0; xOffs--) {
                    if (volume[xOffs][y][z] > 0) {
                        toEdge = false;
                    }
                }
            }
            if (! toEdge) {
                for (int xOffs = 0; xOffs < dim; xOffs++) {
                    if (volume[xOffs][y][z] > 0) {
                        toEdge = false;
                    }
                }
            }

            if (toEdge) {
                rtnVal = false;
            }

        }
        return rtnVal;
    }

    private void tileExpectationScan(File infile, int[] parentCoords, double levelMultiplier, String octreeCoordStr) {
        FileFilter digitDirFilter = new OScanDigitFileFilter();
        File[] digitFiles = infile.listFiles( digitDirFilter );
        Map<Integer,File> nameToFile = new HashMap<>();

        // Check: is this a leaf node?
        if (digitFiles.length == 0) {
            //System.out.println("Visiting leaf octree coords: (" + octreeCoordStr + ")");
            if (levelMultiplier > 1) {
                System.out.println("# WARN: not expected leaf.  Level multiplier is " + levelMultiplier);
            }
            // No more OBJ coords. System.out.println("v "+parentCoords[0]+" "+parentCoords[1]+" "+parentCoords[2]);
            double halfLevelMultiplier = levelMultiplier / 2.0;
            int digit = Integer.parseInt( infile.getName() );
            int[] coords = calculateCoords( parentCoords, halfLevelMultiplier, digit );

            FileFilter filter = new OScanTiffFileFilter();
            File[] tifFiles = infile.listFiles( filter );
            boolean has0 = false;
            boolean has1 = false;
            for ( File f: tifFiles ) {
                if (f.getName().equals("default.0.tif"))  has0 = true;
                if (f.getName().equals("default.1.tif"))  has1 = true;
            }

            boolean hasBoth = has0 && has1;
            volume[coords[0]][coords[1]][coords[2]] = hasBoth ? (byte)3 : has1 ? (byte)2 : has0 ? (byte)1 : (byte)0;

            System.out.println("cartesian coords: [" + coords[0] + "," + coords[1] + "," + coords[2] + "]  for octree coords: " + octreeCoordStr);
        }

        for ( File digitFile: digitFiles ) {
            int digit = Integer.parseInt( digitFile.getName() );
            nameToFile.put( digit, digitFile );
            if ( nameToFile.keySet().contains( digit ) ) {
                double halfLevelMultiplier = levelMultiplier / 2.0;
                if (halfLevelMultiplier == levelMultiplier || halfLevelMultiplier == 0) {
                    throw new IllegalArgumentException("Wrong level count given.");
                }
                int[] coords = calculateCoords( parentCoords, halfLevelMultiplier, digit );
                //System.out.println("Calculated cartesion coords: [" + coords[0] + "," + coords[1] + "," + coords[2] + "]");
                tileExpectationScan(digitFile, coords, halfLevelMultiplier, octreeCoordStr + '/' + digit);
            }
        }

        /*
         *  No dump performed.
        for (int i = 1; !nameToFile.isEmpty()  &&  i <= 8; i++) {
            if (! nameToFile.keySet().contains( i ) ) {
                System.out.println("Directory " + infile + " does not contain index directory " + i);
            }
        }
        */
    }

    private static final int[][] OCTANT_COORDS = {
          { 0,0,0 }, { 1,0,0 },
          { 0,1,0 }, { 1,1,0 },
          { 0,0,1 }, { 1,0,1 },
          { 0,1,1 }, { 1,1,1 },
    };

    /**
     * Use this to build up to the current coordinate value, as each level's coords are calculated from
     * those of their parents.
     */
    private int[] calculateCoords( int[] parentCoords, double levelMultiplier, int octantNumber ) {
        int[] rtnVal = new int[3];
        int[] octantCoords = OCTANT_COORDS[ octantNumber - 1 ];
        for (int i = 0; i < 3; i++) {
            rtnVal[ i ] = parentCoords[ i ] + octantCoords[ i ] * (int)levelMultiplier;
        }
        return rtnVal;
    }

    private class OScanTiffFileFilter implements FileFilter {
        public boolean accept( File candidate ) {
            boolean rtnVal = false;
            String fileName = candidate.getName();
            if (candidate.isFile()  &&  fileName.startsWith("default.")  &&  fileName.endsWith(".tif") ) {
                rtnVal = true;
            }

            return rtnVal;
        }
    }

    private void scanForMissingTifs(File infile) {
        FileFilter digitDirFilter = new OScanDigitFileFilter();
        FileFilter defaultTifFilter = new OScanTiffFileFilter();

        File[] digitDirectories = infile.listFiles( digitDirFilter );
        File[] tifFiles = infile.listFiles( defaultTifFilter );

        if (tifFiles.length < 2) {
            System.out.println("Directory: " + infile + " does not contain both default.0.tif and default.1.tif. Contents follow:");
            for ( File tifFile: tifFiles ) {
                System.out.println("    " + tifFile);
            }
        }
        if (expectedSize == -1  &&  tifFiles[0].length() > 0) {
            expectedSize = tifFiles[0].length();
            System.out.println("Getting expected tif file size from file " + tifFiles[0] );
        }

        for ( File tifFile: tifFiles ) {
            if (tifFile.length() != expectedSize) {
                System.out.println( "Tif File " + tifFile + " has length " + tifFile.length() + " which does not match the expected length of " + expectedSize );
            }
        }

        // Recurse into the sub directories.
        for ( File digitDirectory: digitDirectories) {
            scanForMissingTifs(digitDirectory);
        }
    }
}
