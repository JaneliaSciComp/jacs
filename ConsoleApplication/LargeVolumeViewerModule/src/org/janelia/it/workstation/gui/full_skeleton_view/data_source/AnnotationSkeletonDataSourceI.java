/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.full_skeleton_view.data_source;

import org.janelia.it.workstation.gui.large_volume_viewer.TileFormat;
import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton;

/**
 * Implement this to provide data to the skeleton view.
 * 
 * @author fosterl
 */
public interface AnnotationSkeletonDataSourceI {
    Skeleton getSkeleton();
    TileFormat getTileFormat();
}
