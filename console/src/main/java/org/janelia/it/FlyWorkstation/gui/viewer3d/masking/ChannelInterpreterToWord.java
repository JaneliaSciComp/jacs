package org.janelia.it.FlyWorkstation.gui.viewer3d.masking;

import org.janelia.it.FlyWorkstation.gui.viewer3d.loader.ChannelMetaData;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/10/13
 * Time: 2:09 PM
 *
 * This decides how to deal with incoming channel data, for a volume builder.  It is set aside as its own object
 * so that the complex decisions of downsampling within a channle, can be isolated, and setting up fixed (over whole
 * loop) values can be done in only one place.
 */
public class ChannelInterpreterToWord implements ChannelInterpreterI {
    private ChannelMetaData channelMetaData;
    private byte[] volumeData;

    private int[] orderedRgbIndexes = new int[ 3 ];

    public ChannelInterpreterToWord(ChannelMetaData channelMetaData, byte[] volumeData) {
        this.channelMetaData = channelMetaData;
        this.volumeData = volumeData;

        if ( channelMetaData.byteCount != 2 ) {
            throw new IllegalStateException(
                    "This interpreter is for word values only."
            );
        }

        orderedRgbIndexes[ 0 ] = channelMetaData.redChannelInx;
        orderedRgbIndexes[ 1 ] = channelMetaData.greenChannelInx;
        orderedRgbIndexes[ 2 ] = channelMetaData.blueChannelInx;
    }

    @Override
    public void interpretChannelBytes(byte[] channelData, int targetPos) {
        for ( int i = 0; i < channelMetaData.rawChannelCount; i++ ) {
            for ( int j = 0; j < channelMetaData.byteCount; j++ ) {
                //  block of in-memory, interleaving the channels as the offsets follow.
                int channelInx = orderedRgbIndexes[ i ];

                int channelDataOffset = channelInx * channelMetaData.byteCount + j;
                volumeData[(targetPos + channelDataOffset)] =
                        channelData[ channelDataOffset ];
            }

        }

        // Pad out to the end, to create the alpha byte(s).
        if ( channelMetaData.channelCount > channelMetaData.rawChannelCount ) {
            for ( int j = channelMetaData.rawChannelCount; j < channelMetaData.channelCount; j++ ) {
                for ( int byteOffs = 0; byteOffs < channelMetaData.byteCount; byteOffs ++ ) {
                    volumeData[ targetPos + (j * channelMetaData.byteCount) + byteOffs ] = (byte)255;
                }
            }
        }
    }

    @Override
    public void close() {}
}
