package org.janelia.jacs2.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.service.JacsServiceData;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class AbstractExternalProcessComputation extends AbstractServiceComputation {
    @PropertyValue(name = "Executables.ModuleBase")
    @Inject
    private String executablesBaseDir;

    protected abstract List<String> prepareCommandLine(JacsServiceData jacsServiceData);
    protected abstract Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData);

    protected Optional<String> getEnvVar(String varName) {
        return Optional.ofNullable(System.getenv(varName));
    }

    protected String getFullExecutableName(String exeName) {
        StringBuilder cmdBuilder = new StringBuilder();
        if (StringUtils.isNotBlank(executablesBaseDir)) {
            cmdBuilder.append(executablesBaseDir);
            if (cmdBuilder.charAt(cmdBuilder.length() - 1) != '/') {
                cmdBuilder.append('/');
            }
        }
        cmdBuilder.append(exeName);
        return cmdBuilder.toString();
    }
}
