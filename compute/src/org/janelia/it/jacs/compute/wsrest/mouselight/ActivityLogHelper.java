/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.wsrest.mouselight;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.ComputeDAO;
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
    
    private ComputeDAO computeDAO = new ComputeDAO(logger);
    
    public void logTileLoad(String relativeSlice, TileIndex tileIndex, final double elapsedMs, String sampleIdentifier) {        
        String actionString = sampleIdentifier + ":" + relativeSlice + ":" + tileIndex.toString() + ":elapsed_ms=" + elapsedMs;
        // Use the by-category granularity for these.
        UserToolEvent event = new UserToolEvent();
        event.setSessionId(1L);
        event.setUserLogin("system");
        event.setToolName("restService");
        event.setCategory("getSample2DTile:elapsed");        
        event.setAction(actionString);
        try {
            computeDAO.addEventToSession(event);
        } catch (Exception daoE) {
            logger.error("Failed to log user tool event for action: " + actionString);
        }
    }
}
