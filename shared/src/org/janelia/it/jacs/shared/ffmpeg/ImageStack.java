/**
 * Created by bolstadm on 10/29/14.
 */
package org.janelia.it.jacs.shared.ffmpeg;

import java.util.ArrayList;
import org.bytedeco.javacpp.BytePointer;

/**
 * A stack of video frames read from FFmpeg
 */
public class ImageStack
{
    /**
     * The width of the images in the stack
     *
     * @return the width of an individual image
     */
    public int width()
    {
        return _width;
    }

    /**
     * Setter method for width
     *
     * @param width - the width of an individual image
     */
    public void set_width(int width)
    {
        this._width = width;
    }

    /**
     * The height of the image in the stack
     *
     * @return the height of an individual image (in pixels)
     */
    public int height()
    {
        return _height;
    }

    /**
     * Setter method for height
     * @param height - the height of an individual image
     */
    public void set_height(int height)
    {
        this._height = height;
    }

    /**
     * The number of frames in the stack
     *
     * @return the number of individual frames
     */
    public int get_num_frames()
    {
        return _image.size();
    }

    public int get_num_components()
    {
        return _num_components;
    }

    public void set_num_components(int num_components)
    {
        this._num_components = num_components;
    }

    public int get_bytes_per_pixel()
    {
        return _bytes_per_pixel;
    }

    public void set_bytes_per_pixel(int bytes_per_pixel)
    {
        this._bytes_per_pixel = bytes_per_pixel;
    }

    /**
     * Return a pointer to the pixels of the ith frame/image in the stack
     * @param i - image index
     * @return a pointer to the bytes representing the image
     */
    public BytePointer image(int i) { return _image.get(i).image.data(0); }

    /**
     * Returns a Frame of the ith image in the stack
     * @param i - image index
     * @return the individual frame
     */
    public Frame frame(int i) { return _image.get(i); }

    /**
     * A convenience routine for the size of a line of data (FFmpeg specific)
     * @param i - image index
     * @return the size of a line of data
     */
    public int linesize(int i) { return _image.get(i).image.linesize(0); }

    /**
     * Add a Frame to the end of the stack
     * @param f - The Frame to add
     */
    public void add(Frame f) { _image.add(f); }

    /**
     * Release the resources used by this class
     * @throws Exception
     */
    public void release() throws Exception
    {
        for (int i = 0; i < _image.size(); i++)
        {
            _image.get(i).release();
        }

        _image.clear();

        _height = 0;
        _width = 0;
    }

    private int _height;
    private int _width;

    private int _num_components;

    private int _bytes_per_pixel;

    private ArrayList<Frame> _image = new ArrayList<Frame>();
}

