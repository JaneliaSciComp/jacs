package org.janelia.it.workstation.gui.framework.progress_meter;

import java.awt.BorderLayout;
import java.util.Properties;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 * Top component which usually slides in from the right side when a background
 * task is executed. Shows progress and "next step" buttons for all 
 * background tasks.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.framework.progress_meter//ProgressTopComponent//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = ProgressTopComponent.PREFERRED_ID,
        iconBase = "images/cog.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "rightSlidingSide", openAtStartup = true, position=30)
@ActionID(category = "Window", id = "ProgressTopComponent")
@ActionReference(path = "Menu/Window", position = 30)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ProgressTopComponentAction",
        preferredID = ProgressTopComponent.PREFERRED_ID
)
@Messages({
    "CTL_ProgressTopComponentAction=Background Tasks",
    "CTL_ProgressTopComponent=Background Tasks",
    "HINT_ProgressTopComponentTopComponent=See progress of background tasks"
})
public final class ProgressTopComponent extends TopComponent {

    public static final String PREFERRED_ID = "ProgressTopComponent";
    
    public ProgressTopComponent() {
        initComponents();
        setName(Bundle.CTL_ProgressTopComponent());
        setToolTipText(Bundle.HINT_ProgressTopComponentTopComponent());
//        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, true);
//        putClientProperty(TopComponent.PROP_SLIDING_DISABLED, false);
//        putClientProperty(TopComponent.PROP_UNDOCKING_DISABLED, true);
//        putClientProperty(TopComponent.PROP_DRAGGING_DISABLED, true);
//        putClientProperty(TopComponent.PROP_MAXIMIZATION_DISABLED, true);
//        putClientProperty(TopComponent.PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, false);
        add(ProgressMeterPanel.getSingletonInstance(), BorderLayout.CENTER);
    }
    
//    @Override
//    public boolean canClose() { 
//        return false; 
//    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    
    @Override
    public void componentOpened() {
    }

    @Override
    public void componentClosed() {
    }

    void writeProperties(Properties p) {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(Properties p) {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
