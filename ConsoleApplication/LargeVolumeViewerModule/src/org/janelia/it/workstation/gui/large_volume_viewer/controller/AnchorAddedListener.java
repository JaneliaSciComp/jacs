/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.workstation.gui.large_volume_viewer.controller;

import org.janelia.it.workstation.gui.large_volume_viewer.skeleton.Skeleton.AnchorSeed;

/**
 * Implement this to hear about anchors being added.
 * @author fosterl
 */
public interface AnchorAddedListener {
    void anchorAdded(AnchorSeed seed);
}
