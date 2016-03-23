/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.user_data.tiled_microscope_builder;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmWorkspace;

/**
 * Implement this, to interact with the rest of the world (like a database)
 * on behalf of TmModelManipulator, etc.
 *
 * @author fosterl
 */
public interface TmModelAdapter {
    void loadNeurons(TmWorkspace workspace) throws Exception;
    void saveNeuron(TmNeuron neuron) throws Exception;
    TmNeuron refreshFromEntityData(TmNeuron neuron) throws Exception;
    void deleteEntityData(TmNeuron neuron) throws Exception;
}
