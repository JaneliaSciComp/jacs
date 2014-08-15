/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.geometric_search;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.janelia.it.workstation.gui.geometric_search//GeometricSearch//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "GeometricSearchTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE", 
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "explorer", openAtStartup = false)
@ActionID(category = "Window", id = "org.janelia.it.workstation.gui.geometric_search.GeometricSearchTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_GeometricSearchAction",
        preferredID = "GeometricSearchTopComponent"
)
@Messages({
    "CTL_GeometricSearchAction=Geometric Search",
    "CTL_GeometricSearchTopComponent=Geometric Search",
    "HINT_GeometricSearchTopComponent=This is a Geometric Search window"
})
public final class GeometricSearchTopComponent extends TopComponent {

    public GeometricSearchTopComponent() {
        initComponents();
        setName(Bundle.CTL_GeometricSearchTopComponent());
        setToolTipText(Bundle.HINT_GeometricSearchTopComponent());

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        geometricSearchTabPane = new javax.swing.JTabbedPane();
        searchTabPanel = new javax.swing.JPanel();
        queryPanel = new javax.swing.JPanel();
        queryLabel = new javax.swing.JLabel();
        alignmentBoardPanel = new javax.swing.JPanel();
        resultPanel = new javax.swing.JPanel();
        resultLabel = new javax.swing.JLabel();
        adminTabPanel = new javax.swing.JPanel();

        org.openide.awt.Mnemonics.setLocalizedText(queryLabel, org.openide.util.NbBundle.getMessage(GeometricSearchTopComponent.class, "GeometricSearchTopComponent.queryLabel.text")); // NOI18N

        javax.swing.GroupLayout queryPanelLayout = new javax.swing.GroupLayout(queryPanel);
        queryPanel.setLayout(queryPanelLayout);
        queryPanelLayout.setHorizontalGroup(
            queryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, queryPanelLayout.createSequentialGroup()
                .addContainerGap(20, Short.MAX_VALUE)
                .addComponent(queryLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17))
        );
        queryPanelLayout.setVerticalGroup(
            queryPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(queryPanelLayout.createSequentialGroup()
                .addComponent(queryLabel)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout alignmentBoardPanelLayout = new javax.swing.GroupLayout(alignmentBoardPanel);
        alignmentBoardPanel.setLayout(alignmentBoardPanelLayout);
        alignmentBoardPanelLayout.setHorizontalGroup(
            alignmentBoardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        alignmentBoardPanelLayout.setVerticalGroup(
            alignmentBoardPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 373, Short.MAX_VALUE)
        );

        org.openide.awt.Mnemonics.setLocalizedText(resultLabel, org.openide.util.NbBundle.getMessage(GeometricSearchTopComponent.class, "GeometricSearchTopComponent.resultLabel.text")); // NOI18N

        javax.swing.GroupLayout resultPanelLayout = new javax.swing.GroupLayout(resultPanel);
        resultPanel.setLayout(resultPanelLayout);
        resultPanelLayout.setHorizontalGroup(
            resultPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultPanelLayout.createSequentialGroup()
                .addGap(300, 300, 300)
                .addComponent(resultLabel)
                .addContainerGap(287, Short.MAX_VALUE))
        );
        resultPanelLayout.setVerticalGroup(
            resultPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultPanelLayout.createSequentialGroup()
                .addComponent(resultLabel)
                .addGap(0, 99, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout searchTabPanelLayout = new javax.swing.GroupLayout(searchTabPanel);
        searchTabPanel.setLayout(searchTabPanelLayout);
        searchTabPanelLayout.setHorizontalGroup(
            searchTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(queryPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(searchTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchTabPanelLayout.createSequentialGroup()
                        .addComponent(alignmentBoardPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(resultPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        searchTabPanelLayout.setVerticalGroup(
            searchTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(searchTabPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(searchTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(searchTabPanelLayout.createSequentialGroup()
                        .addComponent(queryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(searchTabPanelLayout.createSequentialGroup()
                        .addComponent(alignmentBoardPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resultPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );

        geometricSearchTabPane.addTab(org.openide.util.NbBundle.getMessage(GeometricSearchTopComponent.class, "GeometricSearchTopComponent.searchTabPanel.TabConstraints.tabTitle"), searchTabPanel); // NOI18N

        javax.swing.GroupLayout adminTabPanelLayout = new javax.swing.GroupLayout(adminTabPanel);
        adminTabPanel.setLayout(adminTabPanelLayout);
        adminTabPanelLayout.setHorizontalGroup(
            adminTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 823, Short.MAX_VALUE)
        );
        adminTabPanelLayout.setVerticalGroup(
            adminTabPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 500, Short.MAX_VALUE)
        );

        geometricSearchTabPane.addTab(org.openide.util.NbBundle.getMessage(GeometricSearchTopComponent.class, "GeometricSearchTopComponent.adminTabPanel.TabConstraints.tabTitle"), adminTabPanel); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(geometricSearchTabPane, javax.swing.GroupLayout.PREFERRED_SIZE, 844, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(geometricSearchTabPane, javax.swing.GroupLayout.PREFERRED_SIZE, 546, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        geometricSearchTabPane.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(GeometricSearchTopComponent.class, "GeometricSearchTopComponent.geometricSearchTabPane.AccessibleContext.accessibleName")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel adminTabPanel;
    private javax.swing.JPanel alignmentBoardPanel;
    private javax.swing.JTabbedPane geometricSearchTabPane;
    private javax.swing.JLabel queryLabel;
    private javax.swing.JPanel queryPanel;
    private javax.swing.JLabel resultLabel;
    private javax.swing.JPanel resultPanel;
    private javax.swing.JPanel searchTabPanel;
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
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
