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

package org.janelia.horta.actors;

import java.util.Observable;
import java.util.Observer;
import org.janelia.geometry3d.MeshGeometry;
import org.janelia.geometry3d.Vector3;
import org.janelia.geometry3d.Vertex;
import org.janelia.gltools.BasicGL3Actor;
import org.janelia.gltools.MeshActor;
import org.janelia.gltools.material.WireframeMaterial;
import org.janelia.horta.modelapi.NeuronReconstruction;
import org.janelia.horta.modelapi.NeuronVertex;

/**
 *
 * @author Christopher Bruns
 */
public class NeuriteLineActor extends BasicGL3Actor
{
    private final WireframeMaterial material;
    private final MeshGeometry meshGeometry;
    private final MeshActor meshActor;
    
    public NeuriteLineActor(final NeuronReconstruction neuron) {
        super(null);
        material = new WireframeMaterial();
        meshGeometry = new MeshGeometry();
        meshActor = new MeshActor(meshGeometry, material, this);
        this.addChild(meshActor);
        buildMesh(neuron);
        neuron.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg)
            {
                buildMesh(neuron);
                meshGeometry.notifyObservers();
            }
        });
    }
    
    private void buildMesh(NeuronReconstruction neuron) {
        meshGeometry.clear();
        for (NeuronVertex neuronVertex : neuron.getVertexes()) {
            NeuronVertex parent = neuronVertex.getParentVertex();
            if (parent == null)
                continue;
            Vector3 pt1 = neuronVertex.getLocation();
            Vector3 pt2 = parent.getLocation();
            Vertex meshVertex1 = meshGeometry.addVertex(pt1);
            Vertex meshVertex2 = meshGeometry.addVertex(pt2);
            // Add first point again, to reuse already written riangle code...
            meshGeometry.addVertex(pt1);
        }
    }
}
