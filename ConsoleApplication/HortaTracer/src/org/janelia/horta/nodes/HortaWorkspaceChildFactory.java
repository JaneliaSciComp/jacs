/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.horta.nodes;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.HortaWorkspace;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author Christopher Bruns
 */
class HortaWorkspaceChildFactory extends ChildFactory<NeuronSet>
{
    private final HortaWorkspace workspace;
    private final Observer refresher;

    public HortaWorkspaceChildFactory(HortaWorkspace workspace)  {
        this.workspace = workspace;
        refresher = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                refresh(false);
            }
        };
        workspace.addObserver(refresher);
    }

    @Override
    protected boolean createKeys(List<NeuronSet> toPopulate)
    {
        for (NeuronSet neuronList : workspace.getNeuronSets()) {
            // Only show neuron lists with, you know, neurons in them.
            neuronList.getMembershipChangeObservable().deleteObserver(refresher);
            if (neuronList.size() == 0) {
                // Listen for changes to empty neuron list content
                neuronList.getMembershipChangeObservable().addObserver(refresher);
            }
            else {
                toPopulate.add(neuronList);
            }
        }
        return true;
    }
    
    @Override
    protected Node createNodeForKey(NeuronSet key)
    {
        return new NeuronSetNode(key);
    }
    
}
