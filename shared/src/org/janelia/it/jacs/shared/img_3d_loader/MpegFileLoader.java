package org.janelia.it.jacs.shared.img_3d_loader;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IOpenCoderEvent;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

import java.awt.image.BufferedImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 2/6/13
 * Time: 3:33 PM
 *
 * Pull MPEG / MP4 file contents inot memory.
 */
public class MpegFileLoader extends LociFileLoader {
    private int imagesRead = 0;
    
    @Override
    public void loadVolumeFile( String fileName ) {
        setUnCachedFileName(fileName);
        loadMpegVideo( fileName );
    }

    private boolean loadMpegVideo(String fileName) {
        IMediaReader mediaReader = ToolFactory.makeReader(fileName);
        // use premultiplied alpha for this opengl mip technique
        mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
        mediaReader.addListener(new VolumeFrameListener());
        while (mediaReader.readPacket() == null);
        return true;
    }

    @Override
    public int getSz() {
        if (imagesRead < super.getSz()) {
            Logger log = LoggerFactory.getLogger( MpegFileLoader.class );
            log.warn(
                "Encountered MPEG file {}, which has 'dead planes' in Z.  Expected Z was {}.  Number of planes read {}.",
                getUnCachedFileName(), super.getSz(), imagesRead
            );
        }
        return super.getSz();
    }
    
    private class VolumeFrameListener extends MediaListenerAdapter {
        // mpeg loading state variables
        private int mVideoStreamIndex = -1;
        private int frameIndex = 0;

        @Override
        public void onOpenCoder(IOpenCoderEvent event) {
            IContainer container = ((IMediaReader) event.getSource()).getContainer();
            // Duration might be useful for computing number of frames
            long duration = container.getDuration(); // microseconds
            int numStreams = container.getNumStreams();
            for (int i = 0; i < numStreams; ++i) {
                IStream stream = container.getStream(i);
                IStreamCoder coder = stream.getStreamCoder();
                ICodec.Type type = coder.getCodecType();
                if (type != ICodec.Type.CODEC_TYPE_VIDEO)
                    continue;
                double frameRate = coder.getFrameRate().getDouble();
                frameIndex = 0;
                setSx(coder.getWidth());
                setSy(coder.getHeight());
                setSz((int)(frameRate * duration / 1e6 + 0.5));
                initArgbTextureIntArray();
                setChannelCount(3);
                setPixelBytes(4);
                return;
            }
        }

        @Override
        public void onVideoPicture(IVideoPictureEvent event) {
            if (event.getStreamIndex() != mVideoStreamIndex) {
                // if the selected video stream id is not yet set, go ahead an
                // select this lucky video stream
                if (mVideoStreamIndex == -1)
                    mVideoStreamIndex = event.getStreamIndex();
                    // no need to show frames from this video stream
                else
                    return;
            }
            storeFramePixels(frameIndex, event.getImage());
            ++frameIndex;
            imagesRead ++;
        }
    }

    private void storeFramePixels(int frameIndex, BufferedImage image) {
        // System.out.println("Reading frame " + frameIndex);
        int sx = getSx();
        int sy = getSy();
        int offset = frameIndex * sx * sy;
        image.getRGB(0, 0, sx, sy,
                getArgbTextureIntArray(),
                offset, sx);        
    }

    private void zeroColors() {
        int[] argbTextureIntArray = getArgbTextureIntArray();
        int numVoxels = argbTextureIntArray.length;
        for (int v = 0; v < numVoxels; ++v)
            argbTextureIntArray[v] = 0;
    }

}
