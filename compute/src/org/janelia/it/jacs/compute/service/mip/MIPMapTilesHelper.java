package org.janelia.it.jacs.compute.service.mip;

import org.janelia.it.jacs.model.common.SystemConfigurationProperties;

/**
 * Created by goinac on 9/23/15.
 */
class MIPMapTilesHelper {
    private static final String EXECUTABLES_BASE = SystemConfigurationProperties.getString("Executables.ModuleBase");
    private static final String MIPMAPS_RETILER_SCRIPT = SystemConfigurationProperties.getString("MipMaps.Retiler.ScriptPath");
    private static final String MIPMAPS_SCALER_SCRIPT = SystemConfigurationProperties.getString("MipMaps.Scaler.ScriptPath");

    static String getMipMapsRetilerCommands() {
        StringBuilder script = new StringBuilder();
        script.append("sh ")
                .append(EXECUTABLES_BASE)
                .append(MIPMAPS_RETILER_SCRIPT);
        return script.toString();
    }

    static String getMipMapsScalerCommands() {
        StringBuilder script = new StringBuilder();
        script.append("sh ")
                .append(EXECUTABLES_BASE)
                .append(MIPMAPS_SCALER_SCRIPT);
        return script.toString();
    }
}
