package org.janelia.jacs2.asyncservice.fileservices;

import com.beust.jcommander.Parameter;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.common.AbstractBasicLifeCycleServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractSingleFileServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.model.service.JacsServiceData;
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

@Named("fileMove")
public class FileMoveProcessor extends AbstractBasicLifeCycleServiceProcessor<File, Void> {

    public static class FileMoveArgs extends ServiceArgs {
        @Parameter(names = {"-input", "-source"}, description = "Source name", required = true)
        String source;
        @Parameter(names = {"-target"}, description = "Target name or location", required = true)
        String target;
        @Parameter(names = {"-errorIfExists"}, description = "Error if a link already exists, otherwise simply overwrite it", required = false)
        boolean errorIfExists;
    }

    @Inject
    FileMoveProcessor(ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(FileMoveProcessor.class, new FileMoveArgs());
    }

    @Override
    public ServiceResultHandler<File> getResultHandler() {
        return new AbstractSingleFileServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return getTargetFile(getArgs(depResults.getJacsServiceData())).toFile().exists();
            }

            @Override
            public File collectResult(JacsServiceResult<?> depResults) {
                return getTargetFile(getArgs(depResults.getJacsServiceData())).toFile();
            }
        };
    }

    @Override
    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        try {
            FileMoveArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.source)) {
                throw new ComputationException(jacsServiceData, "Source file name must be specified");
            } else if (StringUtils.isBlank(args.target)) {
                throw new ComputationException(jacsServiceData, "Target file name must be specified");
            } else {
                Path targetFile = getTargetFile(args);
                Files.createDirectories(targetFile.getParent());
            }
        } catch (ComputationException e) {
            throw e;
        } catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
        return super.prepareProcessing(jacsServiceData);
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult<Void> depResults) {
        return computationFactory.newCompletedComputation(depResults)
                .thenApply(pd -> {
                    try {
                        FileMoveArgs args = getArgs(pd.getJacsServiceData());
                        Path sourcePath = getSourceFile(args);
                        Path targetPath = getTargetFile(args);
                        if (!sourcePath.toAbsolutePath().startsWith(targetPath.toAbsolutePath())) {
                            if (Files.exists(targetPath)) {
                                if (args.errorIfExists) {
                                    throw new ComputationException(pd.getJacsServiceData(), "File " + targetPath + " already exists");
                                } else {
                                    Files.deleteIfExists(targetPath);
                                }
                            }
                            Files.move(sourcePath, targetPath);
                        }
                        return pd;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }

    private FileMoveArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(getJacsServiceArgsArray(jacsServiceData), new FileMoveArgs());
    }

    private Path getSourceFile(FileMoveArgs args) {
        return Paths.get(args.source);
    }

    private Path getTargetFile(FileMoveArgs args) {
        return Paths.get(args.target);
    }

}
