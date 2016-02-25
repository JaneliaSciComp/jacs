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

package org.janelia.console.viewerapi.model;

import org.janelia.console.viewerapi.Command;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;

/**
 *
 * @author brunsc
 */
public class AppendNeuronVertexCommand 
extends AbstractUndoableEdit
implements UndoableEdit, Command
{
    private final NeuronModel neuron;
    private NeuronVertex parentVertex;
    private NeuronVertex newVertex = null;
    private final float[] coordinates;
    private final float radius;
    private final AppendNeuronVertexCommand parentCommand;
    
    public AppendNeuronVertexCommand(
            NeuronModel neuron, 
            NeuronVertex parentVertex,
            AppendNeuronVertexCommand parentCommand, // to help unravel serial undo/redo, with replaced parent vertices, in case parentVertex is stale
            float[] micronXyz,
            float radius) 
    {
        this.neuron = neuron;
        this.parentVertex = parentVertex;
        this.parentCommand = parentCommand;
        this.coordinates = micronXyz;
        this.radius = radius;
    }
    
    // Command-like semantics execute is a synonym for redo()
    @Override
    public boolean execute() {
        if (parentCommand != null) { // check in case serial undo/redo made parentVertex stale
            NeuronVertex updatedParent = parentCommand.getAppendedVertex();
            if (updatedParent != parentVertex)
                parentVertex = updatedParent; // update link
        }
        newVertex = neuron.appendVertex(parentVertex, coordinates, radius);
        if (newVertex == null)
            return false;
        return true;
    }
    
    public NeuronVertex getAppendedVertex() {
        return newVertex;
    }
    
    @Override
    public String getPresentationName() {
        return "Append Neuron Vertex";
    }
    
    @Override
    public void redo() {
        super.redo(); // raises exception if canRedo() is false
        execute();
    }
    
    @Override
    public void undo() {
        super.undo(); // raises exception if canUndo() is false
        neuron.deleteVertex(newVertex);
        newVertex = null;
    }

}
