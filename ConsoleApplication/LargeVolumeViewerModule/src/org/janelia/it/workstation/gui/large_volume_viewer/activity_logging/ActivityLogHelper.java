package org.janelia.it.workstation.gui.large_volume_viewer.activity_logging;

import java.util.Date;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmGeoAnnotation;
import org.janelia.it.jacs.shared.annotation.metrics_logging.ActionString;
import org.janelia.it.jacs.shared.annotation.metrics_logging.CategoryString;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.workstation.gui.large_volume_viewer.TileIndex;
import static org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponentDynamic.LVV_LOGSTAMP_ID;

/**
 * Keep all the logging code in one place, to declutter.
 *
 * @author fosterl
 */
public class ActivityLogHelper {
    private static final CategoryString LIX_CATEGORY_STRING = new CategoryString("loadTileIndexToRam:elapsed");
    private static final CategoryString LONG_TILE_LOAD_CATEGORY_STRING = new CategoryString("longRunningTileIndexLoad");
    private static final CategoryString LVV_SESSION_CATEGORY_STRING = new CategoryString("openFolder");
    private static final CategoryString LVV_ADD_ANCHOR_CATEGORY_STRING = new CategoryString("addAnchor");
    private static final CategoryString LVV_MERGE_NEURITES_CATEGORY_STRING = new CategoryString("mergeNeurites");
    
    private static final int LONG_TIME_LOAD_LOG_THRESHOLD = 5 * 1000;

    public void logTileLoad(int relativeSlice, TileIndex tileIndex, final double elapsedMs, long folderOpenTimestamp) {
        final ActionString actionString = new ActionString(
                folderOpenTimestamp + ":" + relativeSlice + ":" + tileIndex.toString() + ":elapsed_ms=" + elapsedMs
        );
        // Use the by-category granularity for these.
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID,
                LIX_CATEGORY_STRING,
                actionString,
                elapsedMs,
                Double.MAX_VALUE
        );
        // Use the elapsed cutoff for this parallel category.
        SessionMgr.getSessionMgr().logToolThresholdEvent(
                LVV_LOGSTAMP_ID,
                LONG_TILE_LOAD_CATEGORY_STRING,
                actionString,
                new Date().getTime(),
                elapsedMs,
                LONG_TIME_LOAD_LOG_THRESHOLD
        );
    }

    public void logFolderOpen(String remoteBasePath, long folderOpenTimestamp) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_SESSION_CATEGORY_STRING, 
                new ActionString(remoteBasePath + ":" + folderOpenTimestamp)
        );
    }
    
    public void logAddAnchor(TmGeoAnnotation anchor) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_ADD_ANCHOR_CATEGORY_STRING, 
                new ActionString(anchor.getNeuronId() + ":" + anchor.getX() + "," + anchor.getY() + "," + anchor.getZ())
        );
    }
    
    public void logReparentedAnchor(TmGeoAnnotation anchor) {
        SessionMgr.getSessionMgr().logToolEvent(
                LVV_LOGSTAMP_ID, 
                LVV_MERGE_NEURITES_CATEGORY_STRING, 
                new ActionString(anchor.getNeuronId() + ":" + anchor.getX() + "," + anchor.getY() + "," + anchor.getZ())
        );
    }
}
