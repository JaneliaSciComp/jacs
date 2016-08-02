/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.wsrest.mouselight;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.api.ComputeBeanLocal;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.model.user_data.UserToolEvent;
import org.janelia.it.jacs.shared.lvv.TileIndex;

/**
 * Helper: pump any/all logging pertinent to the restful services through here.
 *
 * @author fosterl
 */
class ActivityLogHelper {
    // Singleton support.
    private static ActivityLogHelper activityLogHelper = new ActivityLogHelper();
    private ActivityLogHelper() {}
    public static ActivityLogHelper getInstance() { return activityLogHelper; }
    
    private static final Logger logger = Logger.getLogger(ActivityLogHelper.class);
    
    public void logTileLoad(String relativeSlice, TileIndex tileIndex, final double elapsedMs, String sampleIdentifier) {
        String actionString = sampleIdentifier + ":" + relativeSlice + ":" + tileIndex.toString() + ":elapsed_ms=" + elapsedMs;
        // Use the by-category granularity for these.
        UserToolEvent event = new UserToolEvent();
        event.setSessionId(null);
        event.setUserLogin("system");
        event.setToolName("restService");
        event.setCategory("getSample2DTile:elapsed");        
        event.setAction(actionString);
        try {
            ComputeBeanLocal computeBeanLocal = EJBFactory.getLocalComputeBean();
            UserToolEvent postedEvent = computeBeanLocal.addEventToSession(event);
            if (postedEvent == null) {
                throw new Exception("Null returned from add-attempt.");
            }
        } catch (Exception ex) {
            logger.error("Failed to log user tool event for action: " + actionString);
            ex.printStackTrace();
        }
    }
}
