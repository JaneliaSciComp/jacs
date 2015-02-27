package org.janelia.it.jacs.shared.ffmpeg;
// Used for testing outside of the workstation
//package ffmpeg;

import ch.systemsx.cisd.hdf5.*;

import java.util.List;
import java.util.ListIterator;

import org.janelia.it.jacs.shared.img_3d_loader.FFMPGByteAcceptor;

public class H5JLoader
{
    private String _filename;
    private IHDF5Reader _reader;
    private ImageStack _image;
    
    public H5JLoader(String filename) {
        this._filename = filename;
        IHDF5ReaderConfigurator conf = HDF5Factory.configureForReading(filename);
        conf.performNumericConversions();
        _reader = conf.reader();
    }

    public void close() throws Exception {
        _reader.close();
    }

    public int numberOfChannels() {
        return _reader.object().getAllGroupMembers("/Channels").size();
    }

    public List<String> channelNames() { return _reader.object().getAllGroupMembers("/Channels"); }

    public ImageStack extractAllChannels() {
        _image = new ImageStack();

        List<String> channels = channelNames();
        for (ListIterator<String> iter = channels.listIterator(); iter.hasNext(); )
        {
            String channel_id = iter.next();
            try
            {
                ImageStack frames = extract(channel_id);
                _image.merge( frames );
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return _image;
    }

    public ImageStack extract(String channelID) throws Exception
    {
        IHDF5OpaqueReader channel = _reader.opaque();
        byte[] data = channel.readArray("/Channels/" + channelID);

        FFMpegLoader movie = new FFMpegLoader(data);
        movie.start();
        movie.grab();
        ImageStack stack = movie.getImage();

        return stack;
    }


    public void saveFrame(int iFrame, FFMPGByteAcceptor acceptor)
            throws Exception {
        int width = _image.width();
        int height = _image.height();
        byte[] data = _image.interleave(iFrame, 0, 3);
        int linesize = _image.linesize(iFrame);
        acceptor.accept(data, linesize, width, height);
    }

}