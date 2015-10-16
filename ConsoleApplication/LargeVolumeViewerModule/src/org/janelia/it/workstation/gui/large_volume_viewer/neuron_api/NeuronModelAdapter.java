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

package org.janelia.it.workstation.gui.large_volume_viewer.neuron_api;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import org.janelia.console.viewerapi.ComposableObservable;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.workstation.gui.large_volume_viewer.style.NeuronStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronModelAdapter implements NeuronModel
{
    private final TmNeuron neuron;
    private final Long neuronId;
    private final Collection<NeuronVertex> vertexes;
    private final Collection<NeuronEdge> edges = new ArrayList<NeuronEdge>(); // TODO - create adapter
    private final ComposableObservable colorChangeObservable = new ComposableObservable();
    private final ComposableObservable geometryChangeObservable = new ComposableObservable();
    private final ComposableObservable visibilityChangeObservable = new ComposableObservable();
    private Color color = new Color(86, 142, 216); // default color is "neuron blue"
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ObservableInterface membersAddedObservable;
    private ObservableInterface membersRemovedObservable;
    private NeuronStyle neuronStyle;


    public NeuronModelAdapter(TmNeuron neuron, NeuronStyle neuronStyle) {
        this.neuron = neuron;
        this.neuronId = neuron.getId();
        this.neuronStyle = neuronStyle;
        vertexes = new VertexList(neuron.getGeoAnnotationMap());
    }

    @Override
    public String getName()
    {
        return neuron.getName();
    }

    @Override
    public void setName(String name)
    {
        neuron.setName(name);
        // TODO : name change observable?
    }

    @Override
    public Color getColor()
    {
        return neuronStyle.getColor();
    }

    @Override
    public void setColor(Color color)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ObservableInterface getColorChangeObservable()
    {
        return colorChangeObservable;
    }

    @Override
    public Collection<NeuronVertex> getVertexes()
    {
        return vertexes;
    }

    @Override
    public Collection<NeuronEdge> getEdges()
    {
        return edges;
    }

    @Override
    public ObservableInterface getMembersAddedObservable()
    {
        return membersAddedObservable;
    }

    @Override
    public ObservableInterface getMembersRemovedObservable()
    {
        return membersRemovedObservable;
    }

    @Override
    public ObservableInterface getGeometryChangeObservable()
    {
        return geometryChangeObservable;
    }

    @Override
    public ObservableInterface getVisibilityChangeObservable()
    {
        return visibilityChangeObservable;
    }

    @Override
    public boolean isVisible()
    {
        return neuronStyle.isVisible();
    }

    @Override
    public void setVisible(boolean visible)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    // Hash based on database GUID, so distinct object with the same ID hash together
    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 23 * hash + Objects.hashCode(neuronId);
        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NeuronModelAdapter other = (NeuronModelAdapter) obj;
        if (!Objects.equals(this.neuronId, other.neuronId)) {
            return false;
        }
        return true;
    }

    TmNeuron getTmNeuron()
    {
        return neuron;
    }
    
    
    // Adapter to make a Map<Long, TmGeoAnnotation> look like a Collection<NeuronVertex>
    private static class VertexList implements Collection<NeuronVertex> 
    {
        private final Map<Long, TmGeoAnnotation> vertices;
        private final Map<Long, NeuronVertex> cachedVertices = new HashMap<>();

        private VertexList(Map<Long, TmGeoAnnotation> vertices)
        {
            this.vertices = vertices;
        }
        
        @Override
        public int size()
        {
            return vertices.size();
        }

        @Override
        public boolean isEmpty()
        {
            return vertices.isEmpty();
        }

        @Override
        public boolean contains(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Iterator<NeuronVertex> iterator()
        {
            final Iterator<TmGeoAnnotation> it = vertices.values().iterator();
            return new Iterator<NeuronVertex>() {

                @Override
                public boolean hasNext()
                {
                    return it.hasNext();
                }

                @Override
                public NeuronVertex next()
                {
                    TmGeoAnnotation vertex = it.next();
                    Long guid = vertex.getId();
                    if (! cachedVertices.containsKey(guid)) {
                        cachedVertices.put(guid, new NeuronVertexAdapter(vertex));
                    }
                    return cachedVertices.get(guid);
                }

                @Override
                public void remove()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public Object[] toArray()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public <T> T[] toArray(T[] a)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean add(NeuronVertex e)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean remove(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean containsAll(Collection<?> c)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean addAll(Collection<? extends NeuronVertex> c)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean removeAll(Collection<?> c)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean retainAll(Collection<?> c)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
    
}
