package org.janelia.it.jacs.compute.service.mip;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by goinac on 9/23/15.
 */
class MIPMapTilesHelper {
    private static final String MIPMAP_TILER_SCRIPT =
            SystemConfigurationProperties.getString("Executables.ModuleBase") +
            SystemConfigurationProperties.getString("MipMapTiles.ScriptPath");

    static String getMipMapTilesCommands() {
        StringBuilder script = new StringBuilder();
        script.append("sh ")
                .append(MIPMAP_TILER_SCRIPT);
        return script.toString();
    }

}
