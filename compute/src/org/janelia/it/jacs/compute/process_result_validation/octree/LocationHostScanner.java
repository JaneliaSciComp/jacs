/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.process_result_validation.octree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.janelia.it.jacs.compute.centroid.CentroidCalculator;
import org.janelia.it.jacs.compute.process_result_validation.octree.PeonLogLineHandler.PeonInfoReciever;

/**
 * Will take input path, scan it for logs named [0-9]+.log, and query them
 * for lines telling what paths were executed on which hosts, etc.
 *
 * @author fosterl
 */
public class LocationHostScanner {
    private File input;
    private Map<List<Integer>,List<LocationInfo>> centroidToLocation;
    
    public LocationHostScanner( File input ) {
        this.input = input;
    }
    
    public Map<List<Integer>,List<LocationInfo>> scan() throws Exception {
        // Get the centroid-to-data map.
        centroidToLocation = new HashMap<>();

        // Read the logs in the target directory, looking for the lines
        // of interest.
        File[] digitLogFiles = input.listFiles( new OScanDigitLogFilter() );
        for ( File f: digitLogFiles ) {
            scanLog(f);
        }
        
        return centroidToLocation;
    }

    /**
     * Go through log files, assigning locations to hosts.
     * 
     * @param logFile
     * @throws Exception 
     */
    private void scanLog( File logFile ) throws Exception {
        PeonInfoReciever receiver = new ScannerPeonReceiver(centroidToLocation);

        PeonLogLineHandler peonLogLineHandler = new PeonLogLineHandler(receiver);
        try (BufferedReader br = new BufferedReader( new FileReader( logFile ) )) {
            String inline = null;
            while (null != (inline = br.readLine())) {
                // Looking at candidate lines.
                if (inline.contains("/src/render/peon.jl")) {
                    peonLogLineHandler.handleLine(inline);
                }
            }
        } catch ( Exception ex ) {
            ex.printStackTrace();
            throw ex;
        }
        
        // Now, we have the centroid vs location info.
    }
    
    private static class ScannerPeonReceiver implements PeonInfoReciever {

        private Map<List<Integer>, List<LocationInfo>> centroidToLocation;
        private CentroidCalculator centroidCalculator = new CentroidCalculator();
        private LocationInfo currentLI;

        public ScannerPeonReceiver(Map<List<Integer>,List<LocationInfo>> centroidToLocation) {
            this.centroidToLocation = centroidToLocation;
        }
        
        /**
         * Order Dependency Alert: this method must be called on the
         * PeonInfoReceiver, _first_ of all setters, for a given LocationInfo.
         * 
         * @param corners the corners to set
         */
        @Override
        public void setCorners(int[][] corners) {
            currentLI = new LocationInfo();
            currentLI.setCorners(corners);
        }

        @Override
        public void setHost(String host) {
            currentLI.setHost(host);
        }

        @Override
        public void setChannel(int channel) {
            currentLI.setChannel(channel);
        }

        @Override
        public void setTileIndex(int tileIndex) {
            currentLI.setTileIndex(tileIndex);
        }

        /**
         * Order Dependency Alert: this method must be called on the
         * PeonInfoReceiver, after setCorners, for a given LocationInfo.
         * @param port 
         */
        @Override
        public void setPort(int port) {
            currentLI.setPort(port);
            
            List<Integer> centroid = centroidCalculator.calculateCentroid( currentLI.getCorners() );
            List<LocationInfo> locations = centroidToLocation.get( centroid );
            if (locations == null) {
                locations = new ArrayList<>();
                centroidToLocation.put( centroid, locations );
            }
            locations.add(currentLI);
        }

    }
    
    public static class LocationInfo {
        private int[][] corners;
        private String host;
        private int port;
        private int channel;
        private int tileIndex;

        /**
         * @return the corners
         */
        public int[][] getCorners() {
            return corners;
        }

        /**
         * @param corners the corners to set
         */
        public void setCorners(int[][] corners) {
            this.corners = corners;
        }

        /**
         * @return the host
         */
        public String getHost() {
            return host;
        }

        /**
         * @param host the host to set
         */
        public void setHost(String host) {
            this.host = host;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return port;
        }

        /**
         * @param port the port to set
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Format for printability
         * @return coords as string: [ x1 y1 z1 ]; [ x2 y2 z2 ]; ...
         */
        public String toString() {
            StringBuilder outbuf = new StringBuilder();
            for (int[] corner: getCorners()) {
                if (outbuf.length() > 0) {
                    outbuf.append("; ");
                }
                outbuf.append("[ ");
                for (int coord: corner) {
                    outbuf.append(coord);
                    outbuf.append(" ");
                }
                outbuf.append("]");
            }
            outbuf.insert(0, "channel " + getChannel() + " ran on host: " + getHost() + " and port " + getPort() + " " + " from tile index: " + getTileIndex() + " ");
            return outbuf.toString();
        }

        public int getChannel() {
            return channel;
        }

        public void setChannel(int channel) {
            this.channel = channel;
        }

        public int getTileIndex() {
            return tileIndex;
        }

        public void setTileIndex(int tileIndex) {
            this.tileIndex = tileIndex;
        }
    }
}
