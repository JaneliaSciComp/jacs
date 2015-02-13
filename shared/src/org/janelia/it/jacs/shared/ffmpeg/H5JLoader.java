package org.janelia.it.jacs.shared.ffmpeg;
//package FFmpeg;

import ch.systemsx.cisd.hdf5.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class H5JLoader
{
    private String _filename;
    private IHDF5Reader _reader;
    
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

    public ImageStack extract(String channelID) throws Exception
    {
        // create a temp file
        File temp = File.createTempFile(channelID, ".tmp");
        String path = temp.getAbsolutePath();
        //System.err.println("Temp file created: " + path);

        // open a handle to it
        FileOutputStream fh = new FileOutputStream(temp);
        //System.err.println("Temp file opened for writing.");

        IHDF5OpaqueReader channel = _reader.opaque();
        byte[] data = channel.readArray("/Channels/" + channelID);
        fh.write(data);
        fh.close();

        FFMpegLoader movie = new FFMpegLoader(path);
        movie.start();
        movie.grab();
        ImageStack frames = movie.get_image();

        // try to delete the file.
        // The file will be deleted when all the open handles have been
        // closed.
        boolean deleted = false;
        try
        {
            deleted = temp.delete();
        } catch (SecurityException e)
        {
            // ignore
        }

        // else delete the file when the program ends
        if (!deleted)
            temp.deleteOnExit();

        return frames;
    }
}