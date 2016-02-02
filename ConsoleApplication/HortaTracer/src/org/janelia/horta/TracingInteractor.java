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
package org.janelia.horta;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPopupMenu;
import org.janelia.console.viewerapi.model.NeuronEdge;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.console.viewerapi.model.NeuronVertex;
import org.janelia.geometry3d.Vector3;
import org.janelia.gltools.GL3Actor;
import org.janelia.horta.actors.DensityCursorActor;
import org.janelia.horta.actors.ParentVertexActor;
import org.janelia.horta.actors.SpheresActor;
import org.janelia.horta.actors.VertexHighlightActor;
import org.janelia.horta.nodes.BasicNeuronEdge;
import org.janelia.horta.nodes.BasicNeuronModel;
import org.janelia.horta.nodes.BasicSwcVertex;
import org.openide.awt.StatusDisplayer;

/**
 * Adapted from C:\Users\brunsc\Documents\Fiji_Plugins\Auto_Trace\Semi_Trace.java
 * @author Christopher Bruns
 */
public class TracingInteractor extends MouseAdapter
        implements MouseListener, MouseMotionListener, KeyListener
{

    private final VolumeProjection volumeProjection;
    // private NeuriteAnchor sourceAnchor; // origin of series of provisional anchors
    // private final Stack<NeuriteAnchor> provisionalModel = new Stack<>();
    // private final List<NeuriteAnchor> persistedModel = new ArrayList<>();
    private final int max_tol = 5; // pixels
        
    // For selection affordance
    // For GUI feedback on existing model, contains zero or one vertex.
    // Larger yellow overlay over an existing vertex under the mouse pointer.
    private final NeuronModel highlightHoverModel = new BasicNeuronModel("Hover highlight");
    private NeuronVertex cachedHighlightVertex = null;
    
    // For Tracing
    // Larger blueish vertex with a "P" for current selected persisted parent
    private final NeuronModel parentVertexModel = new BasicNeuronModel("Selected parent vertex"); // TODO: begin point of auto tracing
    private NeuronVertex cachedParentVertex = null;
    
    // White ghost vertex for potential new vertex under cursor 
    // TODO: design this.
    // Maybe color RED until a good path from parent is found
    // This is the new neuron cursor
    private final NeuronModel densityCursorModel = new BasicNeuronModel("Hover density");
    private Vector3 cachedDensityCursorXyz = null;
    
    RadiusEstimator radiusEstimator = 
            new TwoDimensionalRadiusEstimator();
            // new ConstantRadiusEstimator(5.0f);
    private StatusDisplayer.Message previousHoverMessage;
    
    @Override
    public void keyTyped(KeyEvent keyEvent) {
        // System.out.println("KeyTyped");
        // System.out.println(keyEvent.getKeyCode()+", "+KeyEvent.VK_ESCAPE);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // System.out.println("KeyPressed");
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        // System.out.println("KeyReleased");
    }

    public TracingInteractor(
            VolumeProjection volumeProjection) 
    {
        this.volumeProjection = volumeProjection;
        connectMouseToComponent();
    }

    private void connectMouseToComponent() {
        volumeProjection.getMouseableComponent().addMouseListener(this);
        volumeProjection.getMouseableComponent().addMouseMotionListener(this);
        volumeProjection.getMouseableComponent().addKeyListener(this);
    }
    
    public List<GL3Actor> createActors() {
        List<GL3Actor> result = new ArrayList<>();

        // Create actors in the order that they should be rendered;

        // Create a special single-vertex actor for highlighting the selected parent vertex
        SpheresActor parentActor = new ParentVertexActor(parentVertexModel);
        parentActor.setMinPixelRadius(5.0f);
        result.add(parentActor);
        
        // Create a special single-vertex actor for highlighting the vertex under the cursor
        SpheresActor highlightActor = new VertexHighlightActor(highlightHoverModel);
        highlightActor.setMinPixelRadius(7.0f);
        result.add(highlightActor);
        
        // Create a special single-vertex actor for highlighting the vertex under the cursor
        SpheresActor densityCursorActor = new DensityCursorActor(densityCursorModel);
        densityCursorActor.setMinPixelRadius(1.0f);
        result.add(densityCursorActor);
        
        return result;
    }
    
    // Mouse clicking for recentering, selection, and tracing
    @Override
    public void mouseClicked(MouseEvent event) {
        // System.out.println("Mouse clicked in tracer");

        // single click on primary (left) button
        if ( (event.getClickCount() == 1) && (event.getButton() == MouseEvent.BUTTON1) )
        {
            // Shift-clicking might add a new vertex to the neuron model
            if (event.isShiftDown() && densityIsHovered()) {
                if (parentIsSelected()) {
                    // TODO: add vertex to existing model
                    System.out.println("append neuron vertex (TODO)");
                    NeuronVertexIndex vix = volumeProjection.getVertexIndex();
                    NeuronModel neuron = vix.neuronForVertex(cachedParentVertex);
                    NeuronVertex templateVertex = densityCursorModel.getVertexes().iterator().next();
                    NeuronVertex addedVertex = neuron.appendVertex(cachedParentVertex, templateVertex.getLocation(), templateVertex.getRadius());
                    if (addedVertex != null) {
                        selectParentVertex(addedVertex);
                    }
                }
                else {
                    // TODO: create a new neuron
                    System.out.println("create new neuron (TODO)");
                }
            }
            else {
                // Click on highlighted vertex to make it the next parent
                if (volumeProjection.isNeuronModelAt(event.getPoint())) {
                    if (cachedHighlightVertex != null)
                        selectParentVertex(cachedHighlightVertex);
                }
                // Click away from existing neurons to clear parent point
                else {
                    if (clearParentVertex())
                        parentVertexModel.getMembersRemovedObservable().notifyObservers();
                }
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        // System.out.println("mouse exited");
        
        // keep showing hover highlight cursor, if dragging, even when mouse exits
        int buttonsDownMask = MouseEvent.BUTTON1_DOWN_MASK 
                | MouseEvent.BUTTON2_DOWN_MASK 
                | MouseEvent.BUTTON3_DOWN_MASK;
        if ( (event.getModifiersEx() & buttonsDownMask) != 0 )
            return;
        
        // Stop displaying hover highlight when cursor exits the viewport
        if (clearHighlightHoverVertex()) {
            highlightHoverModel.getMembersRemovedObservable().notifyObservers(); // repaint
        }

    }

    private boolean selectParentVertex(NeuronVertex vertex)
    {
        if (vertex == null) return false;
        
        if (cachedParentVertex == vertex)
            return false;
        cachedParentVertex = vertex;
        
        // Remove any previous vertex
        parentVertexModel.getVertexes().clear();
        parentVertexModel.getEdges().clear();

        NeuronVertexIndex vix = volumeProjection.getVertexIndex();
        NeuronModel neuron = vix.neuronForVertex(vertex);
        float loc[] = vertex.getLocation();

        // Create a modified vertex to represent the enlarged, highlighted actor
        BasicSwcVertex parentVertex = new BasicSwcVertex(loc[0], loc[1], loc[2]); // same center location as real vertex
        // Set parent actor radius X% larger than true vertex radius, and at least 2 pixels larger
        float startRadius = 1.0f;
        if (vertex.hasRadius())
            startRadius = vertex.getRadius();
        float parentRadius = startRadius * 1.15f;
        // plus at least 2 pixels bigger - this is handled in actor creation time
        parentVertex.setRadius(parentRadius);
        // blend neuron color with pale yellow parent color
        float parentColor[] = {0.5f, 0.6f, 1.0f, 0.5f}; // pale blue and transparent
        float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
        if (neuron != null) {
            neuronColor = neuron.getColor().getColorComponents(neuronColor);
        }
        float parentBlend = 0.75f;
        Color blendedColor = new Color(
                neuronColor[0] - parentBlend * (neuronColor[0] - parentColor[0]),
                neuronColor[1] - parentBlend * (neuronColor[1] - parentColor[1]),
                neuronColor[2] - parentBlend * (neuronColor[2] - parentColor[2]),
                parentColor[3] // always use the same alpha transparency value
                );
        parentVertexModel.setVisible(true);
        parentVertexModel.setColor(blendedColor);
        // parentVertexModel.setColor(Color.MAGENTA); // for debugging

        parentVertexModel.getVertexes().add(parentVertex);
        parentVertexModel.getMembersAddedObservable().setChanged();

        parentVertexModel.getMembersAddedObservable().notifyObservers();     
        parentVertexModel.getColorChangeObservable().notifyObservers();
        
        return true; 
    }
    
    private boolean parentIsSelected() {
        if (parentVertexModel == null) return false;
        if (parentVertexModel.getVertexes() == null) return false;
        if (parentVertexModel.getVertexes().isEmpty()) return false;
        return true;
    }
    
    private boolean densityIsHovered() {
        if (densityCursorModel == null) return false;
        if (densityCursorModel.getVertexes() == null) return false;
        if (densityCursorModel.getVertexes().isEmpty()) return false;
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearParentVertex() 
    {
        if (parentVertexModel.getVertexes().isEmpty()) {
            return false;
        }
        parentVertexModel.getVertexes().clear();
        parentVertexModel.getEdges().clear();
        parentVertexModel.getMembersRemovedObservable().setChanged();
        cachedParentVertex = null;
        return true;
    }
    
    private boolean setDensityCursor(Vector3 xyz)
    {
        if (xyz == null) return false;
        
        if (cachedDensityCursorXyz == xyz)
            return false;
        cachedDensityCursorXyz = xyz;
        
        // Remove any previous vertex
        densityCursorModel.getVertexes().clear();
        densityCursorModel.getEdges().clear();

        // Create a modified vertex to represent the enlarged, highlighted actor
        BasicSwcVertex densityVertex = new BasicSwcVertex(xyz.getX(), xyz.getY(), xyz.getZ()); // same center location as real vertex
        densityVertex.setRadius(1.0f); // TODO: measure radius and set this rationally
        // blend neuron color with white(?) provisional vertex color
        Color vertexColor = new Color(0.2f, 1.0f, 0.8f, 0.5f);

        densityCursorModel.setVisible(true);
        densityCursorModel.setColor(vertexColor);
        // densityCursorModel.setColor(Color.MAGENTA); // for debugging

        densityCursorModel.getVertexes().add(densityVertex);
        densityCursorModel.getMembersAddedObservable().setChanged();

        densityCursorModel.getMembersAddedObservable().notifyObservers();     
        densityCursorModel.getColorChangeObservable().notifyObservers();
        
        return true; 
    }
    
    // Clear display of existing vertex highlight
    private boolean clearDensityCursor()
    {
        if (densityCursorModel.getVertexes().isEmpty()) {
            return false;
        }
        densityCursorModel.getVertexes().clear();
        densityCursorModel.getEdges().clear();
        densityCursorModel.getMembersRemovedObservable().setChanged();
        cachedDensityCursorXyz = null;
        return true;
    }
    
    // GUI feedback for hovering existing vertex under cursor
    // returns true if a previously unhighlighted vertex is highlighted
    private boolean highlightHoverVertex(NeuronVertex vertex) 
    {
        if (vertex == null) return false;
        
        if (cachedHighlightVertex == vertex)
            return false; // No change
        cachedHighlightVertex = vertex;
        
        NeuronVertexIndex vix = volumeProjection.getVertexIndex();
        NeuronModel neuron = vix.neuronForVertex(vertex);
        float loc[] = vertex.getLocation();
        
        boolean doShowStatusMessage = true;
        if (doShowStatusMessage) {
            String message = "";
            if (neuron != null) {
                message += neuron.getName() + ": ";
            }
            message += " XYZ = [" + loc[0] + ", " + loc[1] + ", " + loc[2] + "]";
            message += "; Vertex Object ID = " + System.identityHashCode(vertex);
            if (message.length() > 0)
                previousHoverMessage = StatusDisplayer.getDefault().setStatusText(message, 2);            
        }
        
        boolean doShowVertexActor = true;
        if (doShowVertexActor) {
            // Remove any previous vertex
            highlightHoverModel.getVertexes().clear();
            highlightHoverModel.getEdges().clear();

            // Create a modified vertex to represent the enlarged, highlighted actor
            BasicSwcVertex highlightVertex = new BasicSwcVertex(loc[0], loc[1], loc[2]); // same center location as real vertex
            // Set highlight actor radius X% larger than true vertex radius, and at least 2 pixels larger
            float startRadius = 1.0f;
            if (vertex.hasRadius())
                startRadius = vertex.getRadius();
            float highlightRadius = startRadius * 1.30f;
            // plus at least 2 pixels bigger - this is handled in actor creation time
            highlightVertex.setRadius(highlightRadius);
            // blend neuron color with pale yellow highlight color
            float highlightColor[] = {1.0f, 1.0f, 0.6f, 0.5f}; // pale yellow and transparent
            float neuronColor[] = {1.0f, 0.0f, 1.0f, 1.0f};
            if (neuron != null) {
                neuronColor = neuron.getColor().getColorComponents(neuronColor);
            }
            float highlightBlend = 0.75f;
            Color blendedColor = new Color(
                    neuronColor[0] - highlightBlend * (neuronColor[0] - highlightColor[0]),
                    neuronColor[1] - highlightBlend * (neuronColor[1] - highlightColor[1]),
                    neuronColor[2] - highlightBlend * (neuronColor[2] - highlightColor[2]),
                    highlightColor[3] // always use the same alpha transparency value
                    );
            highlightHoverModel.setVisible(true);
            highlightHoverModel.setColor(blendedColor);
            // highlightHoverModel.setColor(Color.MAGENTA); // for debugging
            
            highlightHoverModel.getVertexes().add(highlightVertex);
            highlightHoverModel.getMembersAddedObservable().setChanged();
            
            highlightHoverModel.getMembersAddedObservable().notifyObservers();     
            highlightHoverModel.getColorChangeObservable().notifyObservers();
        }
        
        return true;
    }
    
    // Clear display of existing vertex highlight
    private boolean clearHighlightHoverVertex() 
    {
        if (highlightHoverModel.getVertexes().isEmpty()) {
            return false;
        }
        highlightHoverModel.getVertexes().clear();
        highlightHoverModel.getEdges().clear();
        highlightHoverModel.getMembersRemovedObservable().setChanged();
        previousHoverPoint = null;
        cachedHighlightVertex = null;
        return true;
    }
    
    @Override
    public void mouseMoved(MouseEvent event) 
    {
        // TODO: update old provisional tracing behavior
        moveHoverCursor(event.getPoint());
    }

    // Show provisional Anchor radius and position for current mouse location
    private Point previousHoverPoint = null;
    public void moveHoverCursor(Point screenPoint) {
        if (screenPoint == previousHoverPoint)
            return; // no change from last time
        previousHoverPoint = screenPoint;
        
        // Question: Which of these three locations is the current mouse cursor in?
        //  1) upon an existing neuron model vertex
        //  2) upon a region of image density
        //  3) neither
        
        // 1) (maybe) Highlight existing neuron annotation model vertex
        Point hoverPoint = screenPoint;
        boolean foundGoodHighlightVertex = true; // start optimistic...
        NeuronVertex nearestVertex = null;
        if (volumeProjection.isNeuronModelAt(hoverPoint)) { // found an existing annotation model under the cursor
            Vector3 cursorXyz = volumeProjection.worldXyzForScreenXy(hoverPoint);
            NeuronVertexIndex vix = volumeProjection.getVertexIndex();
            nearestVertex = vix.getNearest(cursorXyz);
            if (nearestVertex == null) // no vertices to be found?
                foundGoodHighlightVertex = false;
            else {
                // Is cursor too far from closest vertex?
                Vector3 vertexXyz = new Vector3(nearestVertex.getLocation());
                float dist = vertexXyz.distance(cursorXyz);
                float radius = 1.0f;
                if (nearestVertex.hasRadius())
                    radius = nearestVertex.getRadius();
                // TODO: accept vertices within a certain number of pixels too
                if (dist > 5.0f * radius)
                    foundGoodHighlightVertex = false;
            }
        }
        if (nearestVertex == null)
            foundGoodHighlightVertex = false;
        if (foundGoodHighlightVertex) {
            highlightHoverVertex(nearestVertex);
        }
        else {
            if (clearHighlightHoverVertex())
                highlightHoverModel.getMembersRemovedObservable().notifyObservers(); // repaint
            // Clear previous vertex message, if necessary
            if (previousHoverMessage != null) {
                previousHoverMessage.clear(2);
                previousHoverMessage = null;
            }
        }
        
        // 2) (maybe) show provisional anchor at current image density
        if ( (! foundGoodHighlightVertex) && (volumeProjection.isVolumeDensityAt(hoverPoint))) 
        {
            // Find nearby brightest point
            // screenPoint = optimizePosition(screenPoint); // TODO: disabling optimization for now
            Point optimizedPoint = optimizePosition(hoverPoint);
            Vector3 cursorXyz = volumeProjection.worldXyzForScreenXy(optimizedPoint);
            setDensityCursor(cursorXyz);
        }
        else {
            if (clearDensityCursor())
                densityCursorModel.getMembersRemovedObservable().notifyObservers();
        }
        
        // TODO: build up from current parent toward current mouse position
    }

    private Point optimizePosition(Point screenPoint) {
        // TODO - this method is pretty crude; but maybe good enough?
        screenPoint = optimizeX(screenPoint, max_tol);
        screenPoint = optimizeY(screenPoint, max_tol);
        // Refine to local maximum
        Point prevPoint = new Point(screenPoint.x, screenPoint.y);
        int stepCount = 0;
        do {
            stepCount += 1;
            if (stepCount > 5) 
                break;
            screenPoint = optimizeX(screenPoint, 2);
            screenPoint = optimizeY(screenPoint, 2);
        } while (! prevPoint.equals(screenPoint));
        return screenPoint;
    }
    
    // Find a nearby brighter pixel
    private Point optimizeX(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, -1, 0, max);
        Point p2 = searchOptimizeBrightness(p, 1, 0, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point optimizeY(Point p, int max) {
        Point p1 = searchOptimizeBrightness(p, 0, -1, max);
        Point p2 = searchOptimizeBrightness(p, 0, 1, max);
        double intensity1 = volumeProjection.getIntensity(p1);
        double intensity2 = volumeProjection.getIntensity(p2);
        if (intensity1 > intensity2) {
            return p1;
        } else {
            return p2;
        }
    }

    private Point searchOptimizeBrightness(Point point, int dx, int dy, int max_step) {
        double i_orig = volumeProjection.getIntensity(point);
        double best_i = i_orig;
        int best_t = 0;
        double max_drop = 10 + 0.05 * i_orig;
        for (int t = 1; t <= max_step; ++t) {
            double i_test = volumeProjection.getIntensity(new Point(
                    point.x + t * dx, 
                    point.y + t * dy));
            if (i_test > best_i) {
                best_i = i_test;
                best_t = t;
            } else if (i_test < (best_i - max_drop)) {
                break; // Don't cross valleys
            }
        }
        if (best_t == 0) {
            return point;
        } else {
            return new Point(point.x + best_t * dx, point.y + best_t * dy);
        }
    }

}
