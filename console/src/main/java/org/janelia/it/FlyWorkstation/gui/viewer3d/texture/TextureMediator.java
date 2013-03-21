package org.janelia.it.FlyWorkstation.gui.viewer3d.texture;

import org.janelia.it.FlyWorkstation.gui.viewer3d.VolumeDataAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.media.opengl.GL2;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

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
    public static int COLOR_MAP_TEXTURE_OFFSET = 2;

    private int textureName;
    private int textureSymbolicId; // This is an ID like GL.GL_TEXTURE0.
    private int textureOffset; // This will be 0, 1, ...

    private boolean isInitialized = false;

    private TextureDataI textureData;
    private Logger logger = LoggerFactory.getLogger( TextureMediator.class );

    private Map<Integer,String> glConstantToName;

    public static int[] genTextureIds( GL2 gl, int count ) {
        int[] rtnVal = new int[ count ];
        gl.glGenTextures( count, rtnVal, 0 );
        return rtnVal;
    }

    public TextureMediator() {
        // No initialization.
    }

    /**
     * Initialize a mediator.  Assumptions that can be made about various identifiers will be made here.
     *
     * @param textureId as generated by @See #genTextureIds
     * @param offset 0, 1, ...
     */
    public void init( int textureId, int offset ) {
        if ( ! isInitialized ) {
            this.textureName = textureId;
            this.textureOffset = offset;
            textureSymbolicId = GL2.GL_TEXTURE0 + offset;
        }
        isInitialized = true;
    }

    public void uploadTexture( GL2 gl ) {
        if ( ! isInitialized ) {
            logger.error("Attempted to upload texture before mediator was initialized.");
            throw new RuntimeException("Failed to upload texture");
        }

        // DEBUG
        //testRawBufferContents( textureData.getPixelByteCount(), textureData.getTextureData() );
        ByteBuffer data = ByteBuffer.wrap( textureData.getTextureData() );
        //System.out.println( "Loading texture data of capacity: " + data.capacity() );
        if ( data != null ) {
            data.rewind();

            logger.info(
                    "[" +
                            textureData.getFilename() +
                            "]: Coords are " + textureData.getSx() + " * " + textureData.getSy() + " * " + textureData.getSz()
            );
            int maxCoord = getMaxTexCoord(gl);
            if ( textureData.getSx() > maxCoord  || textureData.getSy() > maxCoord || textureData.getSz() > maxCoord ) {
                logger.warn(
                        "Exceeding max coord in one or more size of texture data {}.  Results unpredictable.",
                        textureData.getFilename()
                );
            }

            int expectedRemaining = textureData.getSx() * textureData.getSy() * textureData.getSz()
                    * textureData.getPixelByteCount() * textureData.getChannelCount();
            if ( expectedRemaining != data.remaining() ) {
                logger.warn( "Invalid remainder vs texture data dimensions.  Sx=" + textureData.getSx() +
                             " Sy=" + textureData.getSy() + " Sz=" + textureData.getSz() +
                             " storageFmtReq=" + getStorageFormatMultiplier() +
                             " pixelByteCount=" + textureData.getPixelByteCount() +
                             ";  total remaining is " +
                             data.remaining() + " " + textureData.getFilename() +
                             ";  expected remaining is " + expectedRemaining
                );
            }
            //else {
            //    logger.info( "Remainder vs texture data dimensions matches.  Sx=" + textureData.getSx() +
            //            " Sy=" + textureData.getSy() + " Sz=" + textureData.getSz() +
            //            " storageFmtReq=" + getStorageFormatMultiplier() +
            //            " pixelByteCount=" + textureData.getPixelByteCount() +
            //            ";  total remaining is " +
            //            data.remaining() + " " + textureData.getFilename() +
            //            ";  expected remaining is " + expectedRemaining
            //    );
            //}
            data.rewind();

            gl.glActiveTexture( textureSymbolicId );
            reportError( "glActiveTexture", gl );

            gl.glEnable( GL2.GL_TEXTURE_3D );
            reportError( "glEnable", gl );

            gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
            reportError( "glBindTexture", gl );

            gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
            reportError( "glTexEnv MODE-REPLACE", gl );

            try {
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

            } catch ( Exception exGlTexImage ) {
                logger.error(
                        "Exception reported during texture upload of NAME:OFFS={}, FORMAT:COMP-ORDER:MULTIPLIER={}",
                        this.textureName + ":" + this.getTextureOffset(),
                        this.getInternalFormat() + ":" + this.getVoxelComponentOrder() + ":" +
                        this.getStorageFormatMultiplier()
                );
                exGlTexImage.printStackTrace();
            }
            reportError( "glTexImage", gl );

            // DEBUG
            //if ( expectedRemaining < 1000000 )
            //    testTextureContents(gl);
        }

    }

    /** Release the texture data memory from the GPU. */
    public void deleteTexture( GL2 gl ) {
        IntBuffer textureNameBuffer = IntBuffer.allocate( 1 );
        textureNameBuffer.put( textureName );
        textureNameBuffer.rewind();
        gl.glDeleteTextures( 1, textureNameBuffer );
    }

    public int getTextureOffset() {
        return textureOffset;
    }

    public double[] textureCoordFromVoxelCoord(double[] voxelCoord) {
        double[] tc = {voxelCoord[0], voxelCoord[1], voxelCoord[2]}; // micrometers, origin at center
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

    /**
     * Set the coords for the texture.  Note that we may not call glGetError here, as this is done
     * between glBegin and glGetEnd calls.
     */
    public void setTextureCoordinates( GL2 gl, double tX, double tY, double tZ ) {
        float[] coordCoverage = textureData.getCoordCoverage();
        logger.debug( "Tex Coords: (" + tX + "," + tY + "," + tZ + ")");
        gl.glMultiTexCoord3d(
                textureSymbolicId, tX * coordCoverage[ 0 ], tY * coordCoverage[ 1 ], tZ * coordCoverage[ 2 ]
        );
    }

    public Double[] getVolumeMicrometers() {
        return textureData.getVolumeMicrometers();
    }

    public Double[] getVoxelMicrometers() {
        return textureData.getVoxelMicrometers();
    }

    public void setupTexture( GL2 gl ) {
        logger.debug( "Texture Data for {} has interp of {}.", textureData.getFilename(),
                getConstantName( textureData.getInterpolationMethod() ) );

        if ( ! isInitialized ) {
            logger.error("Attempting to setup texture before mediator has been initialized.");
            throw new RuntimeException( "Texture setup failed." );
        }
        gl.glActiveTexture( textureSymbolicId );
        reportError( "setupTexture glActiveTexture", gl );
        gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
        reportError( "setupTexture glBindTexture", gl );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, textureData.getInterpolationMethod() );
        reportError( "setupTexture glTexParam MIN FILTER", gl );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, textureData.getInterpolationMethod() );
        reportError( "setupTexture glTexParam MAG_FILTER", gl );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        reportError( "setupTexture glTexParam TEX-WRAP-R", gl );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        reportError( "setupTexture glTexParam TEX-WRAP-S", gl );
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        reportError( "setupTexture glTexParam TEX-WRAP-T", gl );

    }

    /**
     * This debugging method will take the assumptions inherent in this mediator, and use them to
     * grab the stuff in the graphics memory.  If it has been loaded, and contains non-zero data
     * (any), this method will tell that, and any other kinds of checks that may be coded below.
     *
     * Please do not remove this "apparently dead" code, as it may be called to debug difficult-to
     * -track problems with texture loading.
     *
     * @param gl for invoking the required OpenGL method.
     */
    public void testTextureContents( GL2 gl ) {
        gl.glActiveTexture( textureSymbolicId );
        reportError( "testTextureContents glActiveTexture", gl );
        gl.glBindTexture( GL2.GL_TEXTURE_3D, textureName );
        reportError( "testTextureContents glBindTexture", gl );

        int pixelByteCount = textureData.getPixelByteCount();
        int bufferSize = textureData.getSx() * textureData.getSy() * textureData.getSz() *
                pixelByteCount * textureData.getChannelCount();

        byte[] rawBuffer = new byte[ bufferSize ];
        ByteBuffer buffer = ByteBuffer.wrap(rawBuffer);
        gl.glGetTexImage( GL2.GL_TEXTURE_3D, 0, getVoxelComponentOrder(), getVoxelComponentType(), buffer );
        reportError( "TEST: Getting texture for testing", gl );

        buffer.rewind();

        testRawBufferContents(pixelByteCount, rawBuffer);

    }

    /** This should be called immediately after some openGL call, to check error status. */
    public void setTextureData( TextureDataI textureData ) {
        this.textureData = textureData;
    }

    private int getStorageFormatMultiplier() {
        int orderId =  getVoxelComponentOrder();
        if ( orderId == GL2.GL_BGRA ) {
            return 4;
        }
        else {
            return 1;
        }
    }

    private void reportError( String operation, GL2 gl ) {
        int errorNum = gl.glGetError();
        String hexErrorNum = Integer.toHexString( errorNum );
        if ( errorNum > 0 ) {
            logger.error( "Error " + errorNum + "/x0" + hexErrorNum + " during " + operation +
                          " on texture (by 'name' id) " + textureName );
            //new Exception().printStackTrace(); // *** DEBUG ***
        }

    }
    //--------------------------- Helpers for glTexImage3D
    private int getVoxelComponentType() {
        int rtnVal = GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
        if ( textureData.getExplicitVoxelComponentType() != TextureDataI.UNSET_VALUE ) {
            rtnVal = textureData.getExplicitVoxelComponentType();
        }
        else {
            if ( textureData.getChannelCount() == 3 ) {
                rtnVal = GL2.GL_UNSIGNED_INT_8_8_8_8;
            }
            else if ( textureData.getPixelByteCount()  == 1 ) {
                // This: tested vs 1-byte mask.
                rtnVal = GL2.GL_UNSIGNED_BYTE;
            }

            // This throws excepx for current read method.
            if ( textureData.getPixelByteCount() == 2 ) {
                rtnVal = GL2.GL_UNSIGNED_SHORT;
            }
        }

        logger.info( "Got voxel component type of {} for {}.", getConstantName( rtnVal ), textureData.getFilename() );

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

    private int getMaxTexCoord(GL2 gl) {
        IntBuffer rtnBuf = IntBuffer.allocate( 1 );
        rtnBuf.rewind();
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, rtnBuf);
        int[] rtnVals = rtnBuf.array();
        return rtnVals[ 0 ];
    }

    private int getInternalFormat() {
        int internalFormat = GL2.GL_RGBA;
        if ( textureData.getExplicitInternalFormat() != TextureDataI.UNSET_VALUE ) {
            internalFormat = textureData.getExplicitInternalFormat();
        }
        else {
            if (textureData.getColorSpace() == VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_SRGB)
                internalFormat = GL2.GL_SRGB8_ALPHA8;

            // This: tested against a mask file.
            if (textureData.getChannelCount() == 1) {
                internalFormat = GL2.GL_LUMINANCE;

                if (textureData.getPixelByteCount() == 2) {
                    internalFormat = GL2.GL_LUMINANCE16;
                }
            }

            if (textureData.getColorSpace() == VolumeDataAcceptor.TextureColorSpace.COLOR_SPACE_RGB) {
                internalFormat = GL2.GL_RGB;
            }

            if ( textureData.getChannelCount() == 3 ) {
                if ( textureData.getPixelByteCount() == 1 ) {
                    internalFormat = GL2.GL_SRGB8_ALPHA8;
                }
                else {
                    internalFormat = GL2.GL_RGBA16;
                }
            }
        }

        logger.info( "internalFormat = {} for {}", getConstantName( internalFormat ), textureData.getFilename() );
        return internalFormat;
    }

    private int getVoxelComponentOrder() {
        int rtnVal = GL2.GL_BGRA;
        if ( textureData.getExplicitVoxelComponentOrder() != TextureDataI.UNSET_VALUE ) {
            rtnVal = textureData.getExplicitVoxelComponentOrder();
        }
        else {
            if ( textureData.getChannelCount() == 1 ) {
                rtnVal = GL2.GL_LUMINANCE;
            }
            else if ( textureData.getChannelCount() == 3 ) {
                if ( textureData.getPixelByteCount() == 1 )
                    rtnVal = GL2.GL_BGRA;
                else
                    rtnVal = GL2.GL_BGRA;
            }
        }

        logger.info( "Voxel Component order/glTexImage3D 'format' {} for {}.", getConstantName( rtnVal ), textureData.getFilename() );
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

    private void testRawBufferContents(int pixelByteCount, byte[] rawBuffer) {
        java.util.Map<Integer,Integer> allFoundFrequencies = new java.util.HashMap<Integer,Integer>();

        int nonZeroCount = 0;
        for ( int i = 0; i < rawBuffer.length; i++ ) {
            if ( rawBuffer[ i ] != 0 ) {
                nonZeroCount ++;
            }
        }
        if ( nonZeroCount == 0 ) {
            logger.warn( "TEST: All-zero texture loaded for {} by name.", textureName );
        }
        else {
            logger.info( "TEST: Found {} non-zero bytes in texture {} by name.", nonZeroCount, textureName );

            byte[] voxel = new byte[pixelByteCount];
            int leftByteNonZeroCount = 0;
            int rightByteNonZeroCount = 0;
            for ( int i = 0; i < rawBuffer.length; i += pixelByteCount ) {
                boolean voxelNonZero = false;
                for ( int voxOffs = 0; voxOffs < pixelByteCount; voxOffs ++ ) {
                    voxel[ voxOffs ] = rawBuffer[ i+voxOffs ];
                    if ( voxel[ voxOffs ] > 0 ) {
                        voxelNonZero = true;
                    }

                }
                if ( voxelNonZero ) {
                    if ( voxel[ 0 ] != 0 ) {
                        leftByteNonZeroCount ++;
                    }
                    else if ( voxel[ pixelByteCount - 1 ] > 0 ) {
                        rightByteNonZeroCount ++;
                    }
                }

                for ( int j = 0; j < pixelByteCount; j++ ) {
                    Integer count = allFoundFrequencies.get( (int)voxel[ j ] );
                    if ( count == null ) {
                        count = 0;
                    }
                    allFoundFrequencies.put( (int)voxel[ j ], ++count );
                }
            }

            logger.info( "TEST: There are {} nonzero left-most bytes.", leftByteNonZeroCount );
            logger.info( "TEST: There are {} nonzero right-most bytes.", rightByteNonZeroCount );

        }

        logger.info("Texture Values Dump---------------------");
        for ( Integer key: allFoundFrequencies.keySet() ) {
            int foundValue = key;
            if ( foundValue < 0 ) {
                foundValue = 256 + key;
            }
            logger.info("Found {}  occurrences of {}.", allFoundFrequencies.get( key ), foundValue );
        }
        logger.info("End: Texture Values Dump---------------------");
    }

    /** Gets a string name of an OpenGL constant used in this class.  For debugging purposes. */
    private String getConstantName( Integer openGlEnumConstant ) {
        String rtnVal = null;
        if ( glConstantToName == null ) {
            glConstantToName = new HashMap<Integer,String>();
            glConstantToName.put( GL2.GL_UNSIGNED_INT_8_8_8_8_REV, "GL2.GL_UNSIGNED_INT_8_8_8_8_REV" );
            glConstantToName.put( GL2.GL_UNSIGNED_INT_8_8_8_8, "GL2.GL_UNSIGNED_INT_8_8_8_8" );
            glConstantToName.put( GL2.GL_UNSIGNED_BYTE, "GL2.GL_UNSIGNED_BYTE" );
            glConstantToName.put( GL2.GL_UNSIGNED_SHORT, "GL2.GL_UNSIGNED_SHORT" );

            glConstantToName.put( GL2.GL_LUMINANCE, "GL2.GL_LUMINANCE" );
            glConstantToName.put( GL2.GL_SRGB8_ALPHA8, "GL2.GL_SRGB8_ALPHA8" );
            glConstantToName.put( GL2.GL_LUMINANCE16, "GL2.GL_LUMINANCE16" );
            glConstantToName.put( GL2.GL_RGBA, "GL2.GL_RGBA" );
            glConstantToName.put( GL2.GL_RGB, "GL2.GL_RGB" );

            glConstantToName.put( GL2.GL_LINEAR, "GL2.GL_LINEAR" );
            glConstantToName.put( GL2.GL_NEAREST, "GL2.GL_NEAREST" );

            glConstantToName.put( GL2.GL_UNSIGNED_BYTE_3_3_2, "GL2.GL_UNSIGNED_BYTE_3_3_2" );
            glConstantToName.put( GL2.GL_UNSIGNED_SHORT_4_4_4_4_REV, "GL2.GL_UNSIGNED_SHORT_4_4_4_4_REV" );
            glConstantToName.put( GL2.GL_UNSIGNED_SHORT_5_5_5_1, "GL2.GL_UNSIGNED_SHORT_5_5_5_1" );
            glConstantToName.put( GL2.GL_UNSIGNED_SHORT_5_6_5, "GL2.GL_UNSIGNED_SHORT_5_6_5" );
            glConstantToName.put( GL2.GL_UNSIGNED_SHORT_1_5_5_5_REV, "GL2.GL_UNSIGNED_SHORT_1_5_5_5_REV" );
            glConstantToName.put( GL2.GL_BYTE, "GL2.GL_BYTE" );
            glConstantToName.put( GL2.GL_UNSIGNED_BYTE, "GL2.GL_UNSIGNED_BYTE" );
            glConstantToName.put( GL2.GL_UNSIGNED_SHORT, "GL2.GL_UNSIGNED_SHORT" );

            glConstantToName.put( GL2.GL_BGRA, "GL2.GL_BGRA" );
            glConstantToName.put( GL2.GL_RGBA16, "GL2.GL_RGBA16");
            //glConstantToName.put( , "" );
            //glConstantToName.put( , "" );

        }
        rtnVal = glConstantToName.get( openGlEnumConstant );
        if ( rtnVal == null ) {
            rtnVal = "::Unknown " + openGlEnumConstant + "/"+ Integer.toHexString( openGlEnumConstant );
        }

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
