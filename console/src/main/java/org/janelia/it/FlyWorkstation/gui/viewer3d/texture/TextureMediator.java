package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 1/17/13
 * Time: 2:48 PM
 *
 * This handles interfacing with OpenGL / JOGL for matters regarding textures.  One such mediator represents information
 * regarding a single texture.
 */
public class TextureMediator {
    public static int SIGNAL_TEXTURE_OFFSET = 0;
    public static int MASK_TEXTURE_OFFSET = 1;

    private static int s_textureCount = 0;  // Optional: an assumed sequence of textures is made.

    private int textureName;
    private int textureSymbolicId; // This is an ID like GL.GL_TEXTURE0.
    private int textureOffset; // This will be 0, 1, ...

    private TextureDataI textureData;
    private Logger logger = LoggerFactory.getLogger( TextureMediator.class );

    private static final int INTERPOLATION_METHOD =
            GL2.GL_LINEAR; // blending across voxel edges
    // GL2.GL_NEAREST; // discrete cube shaped voxels

    public static int[] genTextureIds( GL2 gl, int count ) {
        int[] rtnVal = new int[ count ];
        gl.glGenTextures( count, rtnVal, 0 );
        return rtnVal;
    }

    public TextureMediator() {
        // No initialization.
    }

    public TextureMediator( int textureId ) {
        init( textureId, s_textureCount ++ );
    }

    /**
     * Initialize a mediator.  Assumptions that can be made about various identifiers will be made here.
     *
     * @param textureId as generated by @See #genTextureIds
     * @param offset 0, 1, ...
     */
    public void init( int textureId, int offset ) {
        this.textureName = textureId;
        this.textureOffset = offset;
        textureSymbolicId = GL2.GL_TEXTURE0 + offset;
    }

    public void uploadTexture( GL2 gl ) {
        ByteBuffer data = textureData.getTextureData();
        if ( data != null ) {

//            ByteBuffer data = ByteBuffer.allocateDirect( textureDataArr.length * Integer.SIZE );
//            data.order( ByteOrder.LITTLE_ENDIAN );
//            for ( int i = 0; i < textureDataArr.length; i++ ) {
//                data.putInt( textureDataArr[ i ] );
//            }
//            IntBuffer data = IntBuffer.wrap( textureDataArr );
            data.rewind();

            gl.glActiveTexture( textureSymbolicId );
            gl.glEnable( GL2.GL_TEXTURE_3D );

            gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

            gl.glTexImage3D(GL2.GL_TEXTURE_3D,
                    0, // mipmap level
                    getInternalFormat(), // as stored INTO graphics hardware, w/ srgb info (GLint internal format)
                    textureData.getSx(), // width
                    textureData.getSy(), // height
                    textureData.getSz(), // depth
                    0, // border
                    getVoxelComponentOrder(), // voxel component order (GLenum format)
                    getVoxelComponentType(), // voxel component type=packed RGBA values(GLenum type)
                    data
            );
        }
    }

//            if ( textureData.getPixelByteCount() > 777 ) {
//                ByteBuffer rawData = ByteBuffer.allocateDirect( textureDataArr.length * 4 * textureData.getPixelByteCount() );
//                rawData.order( ByteOrder.LITTLE_ENDIAN );
//                data = rawData.asIntBuffer();
//                int hasNonZero = 0;
//                for ( int i = 0; i < textureDataArr.length; i ++ ) {
//                    int nextInt = textureDataArr[ i ];
//                    if ( nextInt > 0 ) {
//                        hasNonZero ++;
//                    }
//                    int[] ints = getInts(nextInt, textureData.getPixelByteCount());
//                    data.put( ints );
//                }
//
//                data.rewind();
//
//
//                // *** TEMP *** wrap up a big colored rect-solid.
//                int[] dummyArr = new int[ textureDataArr.length * textureData.getPixelByteCount() ];
//                for ( int i = 0; i < dummyArr.length; i++ ) {
//                    dummyArr[ i ] = 16;
//                }
//
//                data = IntBuffer.wrap( dummyArr );
//                data.rewind();
//
//                System.out.println("This many ints were nonzero " + hasNonZero);
//            }
//            else {
//                data = IntBuffer.wrap( textureDataArr );
//                data.rewind();
//            }

//    public void uploadTexture( GL2 gl ) {
//        int[] textureDataArr = textureData.getTextureData();
//        if ( textureDataArr != null ) {
//            ShortBuffer data = null;
//            if ( textureData.getPixelByteCount() > 1 ) {
//                ByteBuffer rawData = ByteBuffer.allocate( textureDataArr.length * 4 * textureData.getPixelByteCount() );
//                rawData.order( ByteOrder.LITTLE_ENDIAN );
//                data = rawData.asShortBuffer();
//                int hasNonZero = 0;
//                for ( int i = 0; i < textureDataArr.length; i ++ ) {
//                    int nextInt = textureDataArr[ i ];
//                    if ( nextInt > 0 ) {
//                        hasNonZero ++;
//                    }
//                    short[] ints = getShorts(nextInt, textureData.getPixelByteCount());
//                    data.put(ints);
//                }
//
//                data.rewind();
//                System.out.println("This many ints were nonzero " + hasNonZero);
//            }
//
//            gl.glActiveTexture( textureSymbolicId );
//            gl.glEnable( GL2.GL_TEXTURE_3D );
//
//            gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
//            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
//
//            gl.glTexImage3D(GL2.GL_TEXTURE_3D,
//                    0, // mipmap level
//                    getInternalFormat(), // as stored INTO graphics hardware, w/ srgb info (GLint internal format)
//                    textureData.getSx(), // width
//                    textureData.getSy(), // height
//                    textureData.getSz(), // depth
//                    0, // border
//                    getVoxelComponentOrder(), // voxel component order (GLenum format)
//                    getVoxelComponentType(), // voxel component type=packed RGBA values(GLenum type)
//                    data.rewind()
//            );
//        }
//    }

    public int getTextureOffset() {
        return textureOffset;
    }

    public double[] textureCoordinateFromXyz( double[] xyz ) {
        double[] tc = {xyz[0], xyz[1], xyz[2]}; // micrometers, origin at center
        int[] voxels = { textureData.getSx(), textureData.getSy(), textureData.getSz() };
        Double[] volumeMicrometers = textureData.getVolumeMicrometers();
        Double[] voxelMicrometers = textureData.getVoxelMicrometers();
        for (int i =0; i < 3; ++i) {
            // Move origin to upper left corner
            tc[i] += volumeMicrometers[i] / 2.0; // micrometers, origin at corner
            // Rescale from micrometers to voxels
            tc[i] /= voxelMicrometers[i]; // voxels, origin at corner
            // Rescale from voxels to texture units (range 0-1)
            tc[i] /= voxels[i]; // texture units
        }

        return tc;
    }

    public void setTextureCoordinates( GL2 gl, double tX, double tY, double tZ ) {
        gl.glMultiTexCoord3d(textureSymbolicId, tX, tY, tZ);
    }

    public Double[] getVolumeMicrometers() {
        return textureData.getVolumeMicrometers();
    }

    public Double[] getVoxelMicrometers() {
        return textureData.getVoxelMicrometers();
    }

    public void setupTexture( GL2 gl ) {
        gl.glActiveTexture( textureSymbolicId );
        gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, INTERPOLATION_METHOD);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, INTERPOLATION_METHOD);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
    }

    public void setTextureData(TextureDataI textureData) {
        this.textureData = textureData;
    }

    //--------------------------- Helpers for glTexImage3D
    private int getVoxelComponentType() {
        int rtnVal = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
        // This: tested vs 1-byte mask.
        if ( textureData.getPixelByteCount()  == 1 ) {
            rtnVal = GL2.GL_UNSIGNED_BYTE;
        }

        // This throws excepx for current read method.
        if ( textureData.getPixelByteCount() == 2 ) {
//            rtnVal = GL2.GL_UNSIGNED_BYTE;
            rtnVal = GL2.GL_UNSIGNED_SHORT;
        }

        logger.info( "Voxel comp type num is {} for GL2.GL_UNSIGNED_INT_8_8_8_8_REV.", GL2.GL_UNSIGNED_INT_8_8_8_8_REV );
        logger.info( "Voxel comp type num is {} for GL2.GL_UNSIGNED_BYTE.", GL2.GL_UNSIGNED_BYTE );
        logger.info( "Voxel comp type num is {} for GL2.GL_UNSIGNED_SHORT.", GL2.GL_UNSIGNED_SHORT );
        logger.info( "Got voxel component type of {} for {}.", rtnVal, textureData.getFilename() );

        return rtnVal;
        // BLACK SCREEN. GL2.GL_UNSIGNED_BYTE_3_3_2,  // BLACK SCREEN for 143/266
        // GL2.GL_UNSIGNED_SHORT_4_4_4_4_REV, // TWO-COLOR SCREEN for 143/266
        // GL2.GL_UNSIGNED_SHORT_5_5_5_1, // 3-Color Screen for 143/266
        // GL2.GL_UNSIGNED_SHORT_1_5_5_5_REV, // Different 3-Color Screen for 143/266
        // GL2.GL_UNSIGNED_SHORT_5_6_5, // BLACK SCREEN for 143/266
        // GL2.GL_UNSIGNED_SHORT_5_6_5_REV, // BLACK SCREEN for 143/266
        // GL2.GL_BYTE, // YBD for 143/266
        // GL2.GL_BYTE, // YBD for 143/266
        // GL2.GL_UNSIGNED_BYTE, // Grey Neurons for 143/266
        // GL2.GL_UNSIGNED_SHORT, // Stack Trace for 143/266
    }

    private int getInternalFormat() {
        int internalFormat = GL2.GL_RGBA;
        if (textureData.getColorSpace() == VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_SRGB)
            internalFormat = GL2.GL_SRGB8_ALPHA8;

        // This: tested against a mask file.
        if (textureData.getChannelCount() == 1)
            internalFormat = GL2.GL_LUMINANCE;

        logger.info("Luminance format num = {}", GL2.GL_LUMINANCE);
        logger.info("Alpha8 format num = {}", GL2.GL_SRGB8_ALPHA8);
        logger.info("RGBA format num = {}", GL2.GL_RGBA);
        logger.info( "internalFormat = {} for {}", internalFormat, textureData.getFilename() );
        return internalFormat;
    }

    private int getVoxelComponentOrder() {
        int rtnVal = GL2.GL_BGRA;
        if ( textureData.getChannelCount() == 1 ) {
            rtnVal = GL2.GL_LUMINANCE;
        }
        return rtnVal;
    }
    //--------------------------- End: Helpers for glTexImage3D

    /** Roll the relevant bytes of the integer down into a byte array. */
    private int[] getInts(int i, int size) {
        int[] rtnVal = new int[ size ];
        int nextPos = 0;
        rtnVal[ nextPos++ ] = (i & 0xff000000) >>> 24;
        if ( size > 1 )
            rtnVal[ nextPos++ ] = (i & 0x00ff0000) >>> 16;
        if ( size > 2 )
            rtnVal[ nextPos++ ] = (i & 0x0000ff00) >>> 8;
        if ( size > 3 )
            rtnVal[ nextPos ] = i & 0x000000ff;

        return rtnVal;
    }

//    private short[] getShorts(int i, int size) {
//        short[] rtnVal = new short[ size ];
//        int nextPos = 0;
//        rtnVal[ nextPos++ ] = (short)((i & 0xff000000) >>> 24);
//        if ( size > 1 )
//            rtnVal[ nextPos++ ] = (short)((i & 0x00ff0000) >>> 16);
//        if ( size > 2 )
//            rtnVal[ nextPos++ ] = (short)((i & 0x0000ff00) >>> 8);
//        if ( size > 3 )
//            rtnVal[ nextPos ] = (short)(i & 0x000000ff);
//
//        return rtnVal;
//    }
}
