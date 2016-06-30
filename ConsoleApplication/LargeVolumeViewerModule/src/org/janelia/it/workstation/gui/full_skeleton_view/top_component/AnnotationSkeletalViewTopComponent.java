package org.janelia.it.workstation.gui.full_skeleton_view.top_component;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component for landmark annotations.  Used for rapid, "random access"
 * of regions of the volume.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.full_skeleton_view.top_component//AnnotationSkeletalView//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = AnnotationSkeletalViewTopComponent.PREFERRED_ID,
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "properties", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.full_skeleton_view.top_component.AnnotationSkeletalViewTopComponent")
@ActionReference(path = "Menu/Window/Large Volume Viewer" , position = 101)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AnnotationSkeletalViewAction",
        preferredID = AnnotationSkeletalViewTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_AnnotationSkeletalViewAction=Annotation Skeletal View",
    "CTL_AnnotationSkeletalViewTopComponent=" + AnnotationSkeletalViewTopComponent.LABEL_TEXT,
    "HINT_AnnotationSkeletalViewTopComponent=Skeletal View of LVV Annotations"
})
public final class AnnotationSkeletalViewTopComponent extends TopComponent {

    public static final String PREFERRED_ID = "AnnotationSkeletalViewTopComponent";
    public static final String LABEL_TEXT = "  LVV Landmarks  ";
    private final TopComponentPopulator populator = new TopComponentPopulator();
    
    public AnnotationSkeletalViewTopComponent() {
        initComponents();
        setName(Bundle.CTL_AnnotationSkeletalViewTopComponent());
        setToolTipText(Bundle.HINT_AnnotationSkeletalViewTopComponent());

    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        viewPanel = new javax.swing.JPanel();

        viewPanel.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(viewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(viewPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel viewPanel;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        populator.depopulate(viewPanel);
        populator.populate(viewPanel);
    }

    @Override
    public void componentClosed() {
        populator.depopulate(viewPanel);
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
}
