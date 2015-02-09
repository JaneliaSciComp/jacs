/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.img_3d_loader;

import org.bytedeco.javacpp.BytePointer;

/**
 * Implement this to accept bytes coming out of FFMpegLoader
 * @author fosterl
 */
public interface FFMPGByteAcceptor {
    void accept(BytePointer data, int linesize, int width, int height);
}
