/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.process_result_validation.octree;

/**
 * Handle command log string from a peon.jl launch.
 * @author fosterl
 */
public class PeonLogLineHandler implements LineHandler {

    private PeonInfoReciever receiver;
    
    public PeonLogLineHandler(PeonInfoReciever receiver) {
        this.receiver = receiver;
    }
    /**
     * Typical input line: INFO:
     * `/home/fosterl/gitfiles/master/renderer//env/k20/bin/julia \
     * /home/fosterl/gitfiles/master/renderer//src/render/peon.jl \
     * /nobackup/mousebrainmicro/2015-02-27-27-02_RERUN_LLF/set_parameters.jl \
     * 2 1 1290 \
     * 75520776 43356720 15646143 \
     * 75134106 43356720 15646143 \
     * 75520776 42930500 15647139 \
     * 75134106 42930500 15647139 \
     * 75520776 43356720 15896143 \
     * 75134106 43356720 15896143 \
     * 75520776 42930500 15897139 \
     * 75134106 42930500 15897139 \
     * 73827402-40924404-15176205 f16u35.int.janelia.org 2001`
     *
     * These lines are produced via the following Julia code:
     *
     * cmd = `$(ENV["RENDER_PATH"])$(envpath)/bin/julia
     * $(ENV["RENDER_PATH"])/src/render/peon.jl $(ARGS[1]) $(ngpus>0 ? (p-1) %
     * ngpus : NaN) $channel $(in_tiles_idx[locality_idx[tile_idx]])
     * $(transform[in_tiles_idx[locality_idx[tile_idx]]]) $(join(ARGS[3:5],"-"))
     * $hostname2 $port2`
     *
     * @param inline
     * @throws Exception 
     */
    @Override
    public void handleLine(String inline) throws Exception {
        try {
            String[] fields = inline.replace('`', ' ').trim().split(" ");
            // Field 0 is julia bin.
            // Field 1 is includable params julia script.
            // Field 2 is ????.
            // Field 3 is channel num.
            // Field 4 is tile index??
            // Field 5:28 are positions of all 8 corners of target subvolume.
            // Field 29 is concatenated lower corner of whole sample.
            // Field 30 is host
            // Field 31 is port.
            if (! inline.toUpperCase().contains("INFO:")) {
                return;
            }
            //System.out.println("[[[  Parsing  [[[" + inline);
            String portStr = fields[ fields.length - 1 ];
            String hostStr = fields[ fields.length - 2 ];
            int port = Integer.parseInt( portStr );
            int channel = Integer.parseInt( fields[ 6 ] );
            int tileIndex = Integer.parseInt( fields[ 7 ] );

            int[][] corners = new int[8][3];
            int cornerNumber = 0;
            for (int fieldNum = 8; fieldNum < 32; fieldNum += 3 ) {
                int fieldOffset = fieldNum;
                for ( int i = 0; i < 3; i++ ) {
                    corners[ cornerNumber ][ i ] =
                            Integer.parseInt( fields[ fieldOffset ] );
                    fieldOffset ++;
                }
                cornerNumber ++;
            }
            receiver.setCorners(corners);
            receiver.setHost(hostStr);
            receiver.setChannel(channel);
            receiver.setTileIndex(tileIndex);
            receiver.setPort(port);
        } catch (Exception ex) {
            throw new Exception( "Failed to parse " + inline, ex );
        }
    }
    
    public static interface PeonInfoReciever {
        void setCorners(int[][] corners);
        void setHost(String host);
        void setChannel(int channel);
        void setTileIndex(int tileIndex);
        void setPort(int port);
    }

}
