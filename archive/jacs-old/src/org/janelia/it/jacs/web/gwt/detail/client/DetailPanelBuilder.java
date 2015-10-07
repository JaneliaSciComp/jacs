
package org.janelia.it.jacs.web.gwt.detail.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import org.janelia.it.jacs.model.genomics.AccessionIdentifierUtil;
import org.janelia.it.jacs.web.gwt.common.client.service.log.Logger;
import org.janelia.it.jacs.web.gwt.common.client.ui.link.ActionLink;

/**
 * Base class for the different DetailPanelBuilders.  It is responsible for controlling the sequence
 * and timing of operations need to build a DetailSubPanel
 *
 * @author Tareq Nabeel
 */
public abstract class DetailPanelBuilder {

    private static Logger logger = Logger.getLogger("org.janelia.it.jacs.web.gwt.detail.client.DetailPanelBuilder");

    private DetailSubPanel subPanel;
    private ActionLink actionLink;

    /**
     * Go through DetailSubPanelBuilderFactory for getting instance
     */
    protected DetailPanelBuilder(DetailSubPanel subPanel) {
        this.subPanel = subPanel;
    }

    /**
     * Contains the sequence of operations needed to build the sub panel
     */
    protected void build() {
        logger.debug("DetailPanelBuilder build start...");
        buildSkeleton();
        retrieveData();
        logger.debug("DetailPanelBuilder build end");
    }

    /**
     * Creates the panels within the DetailSubPanel and the loading messages
     */
    protected void buildSkeleton() {
        try {
            logger.debug("DetailPanelBuilder buildSkeleton...");

            // Store any user preferences for example before sub panel is blow away
            getSubPanel().preInit();

            // Note: table.clear() method didn't work.  Blowing away the content of the panel is fast enough and a sure bet.
            getSubPanel().clear();

            //Create the panels within the DetailSubPanel and the loading messages
            getSubPanel().createSkeleton();

            // Add the ActionLink to DetailSubPanel e.g. for taking user back to previous page
            getSubPanel().addActionLink(getActionLink());

            // Restore any user preferences
            getSubPanel().postInit();

        }
        catch (RuntimeException e) {
            logger.error("DetailPanelBuilder buildSkeleton caught exception", e);
            throw e;
        }
    }

    /**
     * Retrieves the data needed to build the DetailSubPanel
     */
    protected void retrieveData() {
        logger.debug("DetailPanelBuilder retrieveData retrieving data for Camera Acc: " + getAcc());
        if (AccessionIdentifierUtil.isProjectOrPublication(getAcc())) {
            throw new InvalidAccessionException("Invalid accession: " + getAcc());
        }
        // Timer needed to give labels time to display .... very goofy
        new EntityRetrievalTimer().schedule(200);
    }

    /**
     * Retrieves the panel specific data
     */
    abstract protected void retrieveAndPopulatePanelData();

    /**
     * Gets called on successful completion of retrieveData
     */
    protected abstract void populatePanel();

    /**
     * The DetailService instance to use for retrieving data from database
     *
     * @return DetailServiceAsync instance needed for retrieving data from database
     */
    protected abstract DetailServiceAsync getDetailService();

    /**
     * Delay needed to give UI the chance to render loading label
     */
    private class EntityRetrievalTimer extends Timer {
        public void run() {
            invokeRetrieveData();
        }
    }

    /**
     * The method is invoked in order to retrieve the entity
     */
    protected void
    invokeRetrieveData() {
        getDetailService().getEntity(getAcc(), getRetrieveEntityCallBack());
    }

    /**
     * The method can be used to override the callback to handle the data returned by retrieved entity
     *
     * @return
     */
    protected AsyncCallback
    getRetrieveEntityCallBack() {
        return new GetEntityCallback();
    }

    /**
     * Data handler for the result of the invokeRetrieveData
     */
    private class GetEntityCallback implements AsyncCallback {
        public void onFailure(Throwable throwable) {
            logger.error("DetailPanelBuilder GetEntityCallback failed: ", throwable);
            getSubPanel().setServiceErrorMessage();
        }

        public void onSuccess(Object result) {
            logger.debug("DetailPanelBuilder GetEntityCallback succeeded ");
            try {
                logger.debug("class of subpanel=" + getSubPanel().getClass().getName());
                getSubPanel().getMainLoadingLabel().setVisible(false);
                getSubPanel().setEntity(result);
                if (result != null) {
                    setupPanelsAfterEntity();
                }
                else {
                    getSubPanel().setNotFoundErrorMessage();
                }
            }
            catch (RuntimeException e) {
                logger.error("DetailPanelBuilder GetEntityCallback onSuccess caught exception", e);
                throw e;
            }
        }
    }

    protected void setupPanelsAfterEntity() {
        // before displaying data create the entity specific panels
        getSubPanel().createDetailSpecificPanels();
        // Now that we have the entity instance, display the data
        populatePanel();
    }

    public DetailSubPanel getSubPanel() {
        return subPanel;
    }

    public String getAcc() {
        return subPanel.getAcc();
    }

    public void setAcc(String acc) {
        subPanel.setAcc(acc);
    }

    public ActionLink getActionLink() {
        return actionLink;
    }

    public void setActionLink(ActionLink actionLink) {
        this.actionLink = actionLink;
    }

}
