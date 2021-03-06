package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ContinuationCond;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.JacsServiceState;
import org.janelia.model.service.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Named("vaa3dStitchGrouping")
public class Vaa3dStitchGroupingProcessor extends AbstractBasicLifeCycleServiceProcessor<File, Void> {

    private static final String GROUPS_FILENAME = "igroups.txt";

    static class Vaa3dStitchGroupingArgs extends ServiceArgs {
        @Parameter(names = "-inputDir", description = "Input directory", required = true)
        String inputDir;
        @Parameter(names = "-outputDir", description = "Output directory", required = true)
        String outputDir;
        @Parameter(names = "-refchannel", description = "Reference channel")
        int referenceChannel = 4;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Other plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dPluginProcessor vaa3dPluginProcessor;

    @Inject
    Vaa3dStitchGroupingProcessor(ServiceComputationFactory computationFactory,
                                 JacsServiceDataPersistence jacsServiceDataPersistence,
                                 @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                 Vaa3dPluginProcessor vaa3dPluginProcessor,
                                 Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dPluginProcessor = vaa3dPluginProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(Vaa3dStitchGroupingProcessor.class, new Vaa3dStitchGroupingArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return getGroupsFile(getArgs(depResults.getJacsServiceData())).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceResult<?> depResults) {
                return getGroupsFile(getArgs(depResults.getJacsServiceData())).toFile();
            }
        };
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        return vaa3dPluginProcessor.getErrorChecker();
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            Vaa3dStitchGroupingArgs args = getArgs(jacsServiceData);
            Files.createDirectories(getOutputDir(args));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        Vaa3dStitchGroupingArgs args = getArgs(depResults.getJacsServiceData());
        JacsServiceData vaa3dPluginService = createVaa3dPluginService(args, depResults.getJacsServiceData());
        Path groupsFile = getGroupsFile(getArgs(depResults.getJacsServiceData()));
        return vaa3dPluginProcessor.process(vaa3dPluginService)
                .thenSuspendUntil(vr -> new ContinuationCond.Cond<>(vr, groupsFile.toFile().exists()))
                .thenApply(vr-> depResults);
    }

    private JacsServiceData createVaa3dPluginService(Vaa3dStitchGroupingArgs args, JacsServiceData jacsServiceData) {
        return vaa3dPluginProcessor.createServiceData(new ServiceExecutionContext.Builder(jacsServiceData)
                        .setServiceName(jacsServiceData.getName())
                        .state(JacsServiceState.RUNNING).build(),
                new ServiceArg("-plugin", "imageStitch.so"),
                new ServiceArg("-pluginFunc", "istitch-grouping"),
                new ServiceArg("-input", args.inputDir),
                new ServiceArg("-output", getGroupsFile(args).toString()),
                new ServiceArg("-pluginParams", String.format("#c %d", args.referenceChannel)),
                new ServiceArg("-pluginParams", String.join(",", args.pluginParams))
        );
    }

    private Vaa3dStitchGroupingArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(getJacsServiceArgsArray(jacsServiceData), new Vaa3dStitchGroupingArgs());
    }

    private Path getOutputDir(Vaa3dStitchGroupingArgs args) {
        return Paths.get(args.outputDir);
    }

    private Path getGroupsFile(Vaa3dStitchGroupingArgs args) {
        Path outputDir = getOutputDir(args);
        return outputDir.resolve(GROUPS_FILENAME);
    }
}
