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
package org.janelia.horta.movie;

import java.util.Collection;
import org.janelia.horta.NeuronTracerTopComponent.HortaViewerState;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.horta//MovieMaker//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "MovieMakerTopComponent",
        iconBase = "org/janelia/horta/16-16-8f894f2f6832f3a576bd8f6f8cdf3da0.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.horta.MovieMakerTopComponent")
@ActionReference(path = "Menu/Window/Horta" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_MovieMakerAction",
        preferredID = "MovieMakerTopComponent"
)
@Messages({
    "CTL_MovieMakerAction=MovieMaker",
    "CTL_MovieMakerTopComponent=Movie Maker",
    "HINT_MovieMakerTopComponent=This is a Movie Maker window"
})
public final class MovieMakerTopComponent 
extends TopComponent 
implements LookupListener
{
    private Timeline<HortaViewerState> movieTimeline;
    private MoviePlayState<HortaViewerState> playState;
    private float nextFrameDuration = 2.0f; // seconds
    private final Interpolator<HortaViewerState> defaultInterpolator;

    public MovieMakerTopComponent() {
        initComponents();
        setName(Bundle.CTL_MovieMakerTopComponent());
        setToolTipText(Bundle.HINT_MovieMakerTopComponent());
        defaultInterpolator = new HortaViewerStateInterpolator();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        addFrameButton = new javax.swing.JButton();
        playButton = new javax.swing.JButton();
        saveFramesButton = new javax.swing.JButton();
        realTimeCheckBox = new javax.swing.JCheckBox();
        frameCountLabel = new javax.swing.JLabel();
        saveScriptButton = new javax.swing.JButton();
        durationTextField = new javax.swing.JFormattedTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        fpsTextField = new javax.swing.JFormattedTextField();
        deleteFramesButton = new javax.swing.JButton();

        org.openide.awt.Mnemonics.setLocalizedText(addFrameButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.addFrameButton.text")); // NOI18N
        addFrameButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addFrameButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(playButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.playButton.text")); // NOI18N
        playButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                playButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(saveFramesButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.saveFramesButton.text")); // NOI18N

        realTimeCheckBox.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(realTimeCheckBox, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.realTimeCheckBox.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(frameCountLabel, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.frameCountLabel.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(saveScriptButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.saveScriptButton.text")); // NOI18N

        durationTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        durationTextField.setText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.durationTextField.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jLabel2.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.jLabel1.text")); // NOI18N

        fpsTextField.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0.00"))));
        fpsTextField.setText(org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.fpsTextField.text_2")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(deleteFramesButton, org.openide.util.NbBundle.getMessage(MovieMakerTopComponent.class, "MovieMakerTopComponent.deleteFramesButton.text")); // NOI18N
        deleteFramesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteFramesButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addFrameButton)
                    .addComponent(saveFramesButton)
                    .addComponent(saveScriptButton)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(frameCountLabel)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(durationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel2))))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(fpsTextField)
                            .addComponent(playButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(realTimeCheckBox)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addComponent(deleteFramesButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(addFrameButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(durationTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(frameCountLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(playButton)
                    .addComponent(realTimeCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(fpsTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(saveFramesButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(saveScriptButton)
                .addGap(18, 18, 18)
                .addComponent(deleteFramesButton)
                .addContainerGap(83, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void addFrameButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addFrameButtonActionPerformed
        if (movieSource == null)
            return;
        HortaViewerState viewerState = movieSource.getViewerState();
        if (viewerState == null)
            return;
        KeyFrame<HortaViewerState> keyFrame = new BasicKeyFrame<HortaViewerState>(viewerState, nextFrameDuration);
        if (! movieTimeline.add(keyFrame))
            return;
        updateGui();
    }//GEN-LAST:event_addFrameButtonActionPerformed

    private void playButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_playButtonActionPerformed
        if (playState == null) {
            playButton.setEnabled(false);
            return;
        }            
        boolean realTime = realTimeCheckBox.isSelected();
        float fps = 5.0f;
        String fpsText = fpsTextField.getText();
        if (!fpsText.isEmpty()) {
            Float framesPerSecond = Float.parseFloat(fpsTextField.getText());
            fps = framesPerSecond.floatValue();
        }
        if (realTime)
            playState.playRealTime(fps);
        else
            playState.playEveryFrame(fps);
    }//GEN-LAST:event_playButtonActionPerformed

    private void deleteFramesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteFramesButtonActionPerformed
        if (movieTimeline != null)
            movieTimeline.clear();
        if (playState != null) {
            playState.reset();
        }
        updateGui();
    }//GEN-LAST:event_deleteFramesButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addFrameButton;
    private javax.swing.JButton deleteFramesButton;
    private javax.swing.JFormattedTextField durationTextField;
    private javax.swing.JFormattedTextField fpsTextField;
    private javax.swing.JLabel frameCountLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JButton playButton;
    private javax.swing.JCheckBox realTimeCheckBox;
    private javax.swing.JButton saveFramesButton;
    private javax.swing.JButton saveScriptButton;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
        movieSourcesResult = Utilities.actionsGlobalContext().lookupResult(MovieSource.class);
        movieSourcesResult.addLookupListener(this);
        Collection<? extends MovieSource> allSources = movieSourcesResult.allInstances();
        if (allSources.isEmpty())
            setMovieSource(null);
        else
            setMovieSource(allSources.iterator().next());
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
        movieSourcesResult.removeLookupListener(this);
    }

    void writeProperties(java.util.Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }

    private void updateGui() {
        if (movieTimeline == null) {
            playButton.setEnabled(false);
            frameCountLabel.setText("No frames in movie");
            addFrameButton.setEnabled(false);
        }
        else {
            playButton.setEnabled(movieTimeline.size() > 0);
            frameCountLabel.setText(movieTimeline.size() + " frames in movie");
            addFrameButton.setEnabled(true);
        }
    }
    
    private Lookup.Result<MovieSource> movieSourcesResult = null;
    private MovieSource<HortaViewerState> movieSource = null;

    public MovieSource getMovieSource() {
        return movieSource;
    }

    public void setMovieSource(MovieSource movieSource) 
    {
        if ((movieSource == null) && (this.movieSource == null)) 
        {
            addFrameButton.setEnabled(false); // disable controls
            return;
        }
        if (this.movieSource == movieSource)
            return; // no change
        if (movieSource == null)
            return; // remember the old source, when the new one seems to be null
        this.movieSource = movieSource;
        
        if (movieTimeline == null)
            movieTimeline = new BasicMovieTimeline<>(defaultInterpolator);
        movieTimeline.clear();
        playState = new BasicMoviePlayState(movieTimeline, movieSource);
        
        updateGui();
    }

    public float getPlaybackFramesPerSecond() {
        return playState.getFramesPerSecond();
    }
    
    public void setPlaybackFramesPerSecond(float fps) {
        playState.setFramesPerSecond(fps);
    }
    
    @Override
    public void resultChanged(LookupEvent le) {
        if (movieSourcesResult == null)
            return;
        Collection<? extends MovieSource> sources = movieSourcesResult.allInstances();
        if (sources.isEmpty())
            setMovieSource(null);
        else
            setMovieSource(sources.iterator().next());
    }
}
