/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.alignment_board_viewer.top_component;

import com.google.common.eventbus.Subscribe;
import java.awt.BorderLayout;
import java.util.Properties;
import javax.swing.GroupLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.Launcher;
import org.janelia.it.workstation.gui.alignment_board_viewer.AlignmentBoardPanel;
import org.janelia.it.workstation.gui.viewer3d.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.gui.viewer3d.events.AlignmentBoardOpenEvent;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.Lookups;
import org.openide.windows.TopComponentGroup;
import org.openide.windows.WindowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.alignment_board_viewer.top_component//AlignmentBoard//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "AlignmentBoardTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false, position = 300)
@ActionID(category = "Window", id = "AlignmentBoardTopComponent")
@ActionReference(path = "Menu/Window", position = 300 )
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_AlignmentBoardAction",
        preferredID = "AlignmentBoardTopComponent"
)
@Messages({
    "CTL_AlignmentBoardAction=Alignment Board",
    "CTL_AlignmentBoardTopComponent=Alignment Board",
    "HINT_AlignmentBoardTopComponent=Shows how neurons align to each other in a common space"
})
public final class AlignmentBoardTopComponent extends TopComponent {

    private AlignmentBoardPanel alignmentBoardPanel;
    private Logger logger = LoggerFactory.getLogger( AlignmentBoardTopComponent.class );
            
    public AlignmentBoardTopComponent() {
        initComponents();
        alignmentBoardPanel = new AlignmentBoardPanel();
        setName(Bundle.CTL_AlignmentBoardTopComponent());
        putClientProperty(TopComponent.PROP_CLOSING_DISABLED, Boolean.FALSE);
        setToolTipText(Bundle.HINT_AlignmentBoardTopComponent());
        establishEntityAcceptor();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleBoardOpened(AlignmentBoardOpenEvent event) {
        alignmentBoardPanel.handleBoardOpened(event);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void handleItemChanged(AlignmentBoardItemChangeEvent event) {
        alignmentBoardPanel.handleItemChanged(event);
    }

    //------------------------------------------HELPERS
    private void establishEntityAcceptor() {
        Launcher launcher = new Launcher();
        this.associateLookup( Lookups.singleton( launcher ) );
        logger.info("Established acceptor");
    }
    
    private void initMyComponents() {

        contentPanel.setLayout( new BorderLayout() );
        contentPanel.add( alignmentBoardPanel, BorderLayout.CENTER );

    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        contentPanel = new JPanel();

        GroupLayout contentPanelLayout = new GroupLayout(contentPanel);
        contentPanel.setLayout(contentPanelLayout);
        contentPanelLayout.setHorizontalGroup(
            contentPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 814, Short.MAX_VALUE)
        );
        contentPanelLayout.setVerticalGroup(
            contentPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGap(0, 864, Short.MAX_VALUE)
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
        ModelMgr.getModelMgr().registerOnEventBus(this);
        initMyComponents();
    }

    @Override
    public void componentClosed() {
        ModelMgr.getModelMgr().unregisterOnEventBus(this);        
        alignmentBoardPanel.close();
        Runnable runnable = new Runnable() {
            public void run() {
                TopComponentGroup tcg = WindowManager.getDefault().findTopComponentGroup(
                        "alignment_board_plugin"
                );
                if (tcg != null) {
                    tcg.close();
                }
            }
        };
        if ( SwingUtilities.isEventDispatchThread() ) {
            runnable.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait( runnable );
            } catch ( Exception ex ) {
                logger.error(ex.getMessage());
                ex.printStackTrace();
            }
        }
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
