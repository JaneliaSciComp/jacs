package org.janelia.it.workstation.gui.dataview;

import java.awt.BorderLayout;
import java.util.Properties;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;

import javax.swing.GroupLayout;
import javax.swing.JPanel;

/**
 * Top component for the entity-based data viewer. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.dataview//DataViewer//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DataViewerTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@Messages({
    "CTL_DataViewerAction=Data Viewer",
    "CTL_DataViewerTopComponent=DataViewer Window",
    "HINT_DataViewerTopComponent=This is a DataViewer window"
})
public final class DataViewerTopComponent extends TopComponent {

    private DataViewer dataViewer;
    
    public DataViewerTopComponent() {
        initComponents();
        dataViewer = new DataViewer();
        setName(Bundle.CTL_DataViewerTopComponent());
        setToolTipText(Bundle.HINT_DataViewerTopComponent());
        establishEntityAcceptor();

    }

    //------------------------------------------HELPERS
    private void establishEntityAcceptor() {
        Launcher launcher = new Launcher();
        this.associateLookup( Lookups.singleton( launcher ) );
    }
    
    private void initMyComponents() {
        contentPanel.setLayout( new BorderLayout() );
        contentPanel.add( dataViewer, BorderLayout.CENTER );
    }
    
    public void openDataViewer(Long entityId) {
        dataViewer.getEntityPane().performSearchById(entityId);
    }
    
    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentPanel = new JPanel();

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
            contentPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        contentPanelLayout.setVerticalGroup(
            contentPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(contentPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(contentPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JPanel contentPanel;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        initMyComponents();
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
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
