package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.volume_builder;

import org.janelia.it.FlyWorkstation.gui.viewer3d.masking.VolumeDataI;
import org.janelia.it.FlyWorkstation.gui.viewer3d.volume_builder.VolumeDataChunk;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 9/22/13
 * Time: 11:11 PM
 *
 * This probably stop-gap impl will just split up the volume into multiple planes or slices.  The classic
 * single-voxel-width slice can be bundled into multiples.  This is very convenient for dealing with a segmentation
 * of the 1D version of the "array" into small enough sub-arrays.
 */
public class VeryLargeVolumeData implements VolumeDataI {

    public static final int DEFAULT_NUM_SLABS = 64;
    private int slabExtent = 0;
    private long volumeExtent = 0L;

    private byte[][] slabs;
    private VolumeDataChunk[] chunks;

    public VeryLargeVolumeData( int sizeX, int sizeY, int sizeZ, int bytesPerVoxel ) {
        this( sizeX, sizeY, sizeZ, bytesPerVoxel, DEFAULT_NUM_SLABS );
    }

    /**
     * Construct with eno info to figure out how big the single-voxel-thick slice is, including voxel
     * byte multiple.  The slice size must not exceed Integer.MAX.
     *
     * @param sizeX ct x
     * @param sizeY ct y
     * @param sizeZ ct z
     * @param bytesPerVoxel how many bytes for each voxel.
     * @param numSlabs how many divisions of the original volume are wanted?
     */
    public VeryLargeVolumeData( int sizeX, int sizeY, int sizeZ, int bytesPerVoxel, int numSlabs ) {
        int sliceSize = sizeX * sizeY * bytesPerVoxel;
        volumeExtent = sliceSize * sizeZ;
        long slicesPerSlab = (long) sizeZ / (long) numSlabs;
        slabExtent = (int)((long) sliceSize * slicesPerSlab);
        slabs = new byte[ numSlabs ][];
        chunks = new VolumeDataChunk[ numSlabs ];
        long slabEnd = 0L;
        int lastSlabIndex = numSlabs - 1;
        for ( int slabIndex = 0; slabIndex < lastSlabIndex; slabIndex++ ) {
            slabs[ slabIndex ] = new byte[ slabExtent ];
            slabEnd += slabExtent;
            VolumeDataChunk chunk = getVolumeDataChunk( slicesPerSlab, slabIndex );
            chunks[ slabIndex ] = chunk;
        }
        if ( slabEnd < volumeExtent ) {
            slabs[ lastSlabIndex ] = new byte[ (int)(volumeExtent - slabEnd) ];
            VolumeDataChunk chunk = getVolumeDataChunk( slicesPerSlab, lastSlabIndex );
            chunks[ lastSlabIndex ] = chunk;
        }
    }

    @Override
    public boolean isVolumeAvailable() {
        return true;
    }

    @Override
    public VolumeDataChunk[] getVolumeChunks() {
        return chunks;
    }

    @Override
    public byte getValueAt(long location) {
        int slabNo = getSlabNo( location );
        byte[] slab = slabs[ slabNo ];
        return slab[ getLocInSlab( location, slabNo ) ];
    }

    @Override
    public void setValueAt(long location, byte value) {
        int slabNo = getSlabNo( location );
        byte[] slab = slabs[ slabNo ];
        slab[ getLocInSlab( location, slabNo ) ] = value;
    }

    @Override
    public long length() {
        return volumeExtent;
    }

    /**
     * Helper for creating the final slabs to be cached, and returned later.
     *
     * @param slicesPerSlab for Z coord.
     * @param slabNumber which slab
     * @return fully-characterized volume chunk.
     */
    private VolumeDataChunk getVolumeDataChunk(long slicesPerSlab, int slabNumber) {
        VolumeDataChunk chunk = new VolumeDataChunk();
        chunk.setData( slabs[ slabNumber ] );
        chunk.setStartX( 0 );
        chunk.setStartY( 0 );
        chunk.setStartZ( (int)(slicesPerSlab * slabNumber) );
        return chunk;
    }

    private int getSlabNo(long location) {
        return (int)(location / slabExtent);
    }

    private int getLocInSlab(long location, int slabNo) {
        return (int)(location - ( (long)slabNo * slabExtent ) );
    }
}

