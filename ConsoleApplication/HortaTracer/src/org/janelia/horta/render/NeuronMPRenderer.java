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

package org.janelia.horta.render;

import java.awt.Color;
import java.awt.geom.Point2D;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.BrightnessModel;
import org.janelia.gltools.BasicScreenBlitActor;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.LightingBlitActor;
import org.janelia.gltools.MultipassRenderer;
import org.janelia.gltools.RemapColorActor;
import org.janelia.gltools.RenderPass;
import org.janelia.gltools.RenderTarget;
import org.janelia.horta.actors.SwcActor;

/**
 *
 * @author Christopher Bruns
 */
public class NeuronMPRenderer
extends MultipassRenderer
{
    private final GLAutoDrawable drawable;
    // private final ColorBackgroundActor backgroundActor;
    
    private final BackgroundRenderPass backgroundRenderPass;
    private final OpaqueRenderPass opaqueRenderPass;
    private final VolumeRenderPass volumeRenderPass;
    
    public NeuronMPRenderer(GLAutoDrawable drawable, final BrightnessModel brightnessModel) 
    {
        this.drawable = drawable;
        
        backgroundRenderPass = new BackgroundRenderPass();
        add(backgroundRenderPass);
        
        // CMB September 2015 begin work on opaque render pass
        opaqueRenderPass = new OpaqueRenderPass(drawable);
        add(opaqueRenderPass);
        
        volumeRenderPass = new VolumeRenderPass(drawable);

        // pass depth texture from opaque render pass as input to volume render pass
        // using trivial intermediate render pass
        add(new RenderPass(null) {
            @Override
            public void display(GL3 gl, AbstractCamera camera) {
                volumeRenderPass.setOpaqueDepthTexture(
                        opaqueRenderPass.getDepthTarget(),
                        opaqueRenderPass.getZNear(),
                        opaqueRenderPass.getZFar());
                super.display(gl, camera);
            }
        });
        
        add(volumeRenderPass);
        
        // 2.5 blit opaque geometry to screen
        add(new RenderPass(null) {
            { // constructor
                addActor(new BasicScreenBlitActor(opaqueRenderPass.getColorTarget()));
            }
        });
        
        // 3) Colormap volume onto screen
        add(new RenderPass(null) { // render to screen
            private GL3Actor lightingActor = new LightingBlitActor(
                    volumeRenderPass.getIntensityTexture()); // for isosurface
            private final GL3Actor colorMapActor = new RemapColorActor(
                    volumeRenderPass.getIntensityTexture(), brightnessModel); // for MIP, occluding

            {
                // addActor(lightingActor); // TODO - use for isosurface
                addActor(colorMapActor); // Use for MIP and occluding
                // lightingActor.setVisible(false);
                // colorMapActor.setVisible(true);
                // addActor(new BasicScreenBlitActor(volumeRenderPass.getIntensityTexture()));
            }
            
            @Override
            protected void renderScene(GL3 gl, AbstractCamera camera) {
                gl.glEnable(GL3.GL_BLEND);
                gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
                super.renderScene(gl, camera);
            }
        });
        
    }
    
    public void addVolumeActor(GL3Actor boxMesh) {
        volumeRenderPass.addActor(boxMesh);
        setIntensityBufferDirty();
    }
    
    public void clearVolumeActors() {
        volumeRenderPass.clearActors();
        setIntensityBufferDirty();
    }
    
    @Override
    public void init(GL3 gl) {
        super.init(gl);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
    }
    
    public int pickIdForScreenXy(Point2D xy) {
        return valueForScreenXy(xy, volumeRenderPass.getPickTexture().getAttachment(), 0);
    }

    public int intensityForScreenXy(Point2D xy) {
        int result = valueForScreenXy(xy, volumeRenderPass.getIntensityTexture().getAttachment(), 0);
        if (result <= 0) {
            return -1;
        }
        return result;
    }
    
    public float relativeDepthOffsetForScreenXy(Point2D xy, AbstractCamera camera) {
        float result = 0;
        int intensity = intensityForScreenXy(xy);
        if (intensity == -1) {
            return result;
        }
        if (volumeRenderPass.getFramebuffer() == null) {
            return result;
        }
        RenderTarget intensityDepthTarget = volumeRenderPass.getPickTexture();
        if (intensityDepthTarget == null) {
            return result;
        }
        int relDepth = intensityDepthTarget.getIntensity(
                drawable,
                (int) xy.getX(),
                // y convention is opposite between screen and texture buffer
                intensityDepthTarget.getHeight() - (int) xy.getY(),
                1); // channel index
        result = 2.0f * (relDepth / 65535.0f - 0.5f); // range [-1,1]
        return result;
    }

    private int valueForScreenXy(Point2D xy, int glAttachment, int channel) {
        int result = -1;
        if (volumeRenderPass.getFramebuffer() == null) {
            return result;
        }
        RenderTarget target = volumeRenderPass.getFramebuffer().getRenderTarget(glAttachment);
        if (target == null) {
            return result;
        }
        int intensity = target.getIntensity(
                drawable,
                (int) Math.round(xy.getX()),
                // y convention is opposite between screen and texture buffer
                target.getHeight() - (int) Math.round(xy.getY()),
                channel); // channel index
        return intensity;
    }
    
    public void setIntensityBufferDirty() {
        for (RenderTarget rt : new RenderTarget[] {
            volumeRenderPass.getIntensityTexture(), 
            volumeRenderPass.getPickTexture()}) 
            rt.setDirty(true);
    }

    public Iterable<GL3Actor> getVolumeActors()
    {
        return volumeRenderPass.getActors();
    }
    
    public void setBackgroundColor(Color topColor, Color bottomColor) {
        backgroundRenderPass.setColor(topColor, bottomColor);
    }

    public void removeOpaqueActor(GL3Actor actor)
    {
        opaqueRenderPass.removeActor(actor);
    }

    public void addOpaqueActor(SwcActor na)
    {
        opaqueRenderPass.addActor(na);
    }

}
