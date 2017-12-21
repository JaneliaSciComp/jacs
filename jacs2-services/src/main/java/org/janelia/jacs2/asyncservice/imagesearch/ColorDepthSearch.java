package org.janelia.jacs2.asyncservice.imagesearch;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.*;
import org.janelia.jacs2.asyncservice.common.resulthandlers.AbstractFileListServiceResultHandler;
import org.janelia.jacs2.asyncservice.common.spark.SparkApp;
import org.janelia.jacs2.asyncservice.common.spark.SparkCluster;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.qualifier.IntPropertyValue;
import org.janelia.jacs2.cdi.qualifier.StrPropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching color depth mask projections at scale by using a Spark service. Multiple directories can be
 * searched. You can perform multiple searches on the same images already in memory by specifying multiple mask files
 * as input.
 *
 * The results are a set of tab-delimited files, one per input mask. The first line of each output file is the
 * filepath of the mask that was used to generated the results. The rest of the lines list matching images in this
 * format;
 * <score>\t<filepath>
 *
 * Depends on a compiled jar from the colordepthsearch project.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
@Named("colorDepthSearch")
public class ColorDepthSearch extends AbstractServiceProcessor<List<File>> {

    public static final String RESULTS_FILENAME_SUFFIX = "_results.txt";

    private final long clusterStartTimeoutInMillis;
    private final long searchTimeoutInMillis;
    private final long clusterIntervalCheckInMillis;
    private final long searchIntervalCheckInMillis;
    private final int numNodes;
    private final String jarPath;
    private final int parallelism;

    static class ColorDepthSearchArgs extends ServiceArgs {
        @Parameter(names = "-inputFiles", description = "Comma-delimited list of mask files", required = true)
        String inputFiles;
        @Parameter(names = "-searchDirs", description = "Comma-delimited list of directories containing the color depth projects to search", required = true)
        String searchDirs;
    }

    @Inject Instance<SparkCluster> clusterSource;

    @Inject
    ColorDepthSearch(ServiceComputationFactory computationFactory,
                     JacsServiceDataPersistence jacsServiceDataPersistence,
                     @StrPropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                     @IntPropertyValue(name = "service.colorDepthSearch.clusterStartTimeoutInSeconds", defaultValue = 3600) int clusterStartTimeoutInSeconds,
                     @IntPropertyValue(name = "service.colorDepthSearch.searchTimeoutInSeconds", defaultValue = 1200) int searchTimeoutInSeconds,
                     @IntPropertyValue(name = "service.colorDepthSearch.clusterIntervalCheckInMillis", defaultValue = 2000) int clusterIntervalCheckInMillis,
                     @IntPropertyValue(name = "service.colorDepthSearch.searchIntervalCheckInMillis", defaultValue = 5000) int searchIntervalCheckInMillis,
                     @IntPropertyValue(name = "service.colorDepthSearch.numNodes", defaultValue = 6) Integer numNodes,
                     @IntPropertyValue(name = "service.colorDepthSearch.parallelism", defaultValue = 300) Integer parallelism,
                     @StrPropertyValue(name = "service.colorDepthSearch.jarPath") String jarPath,
                     Logger log) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, log);
        this.clusterStartTimeoutInMillis = clusterStartTimeoutInSeconds * 1000;
        this.searchTimeoutInMillis = searchTimeoutInSeconds * 1000;
        this.clusterIntervalCheckInMillis = clusterIntervalCheckInMillis;
        this.searchIntervalCheckInMillis = searchIntervalCheckInMillis;
        this.numNodes = numNodes;
        this.parallelism = parallelism;
        this.jarPath = jarPath;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(ColorDepthSearch.class, new ColorDepthSearchArgs());
    }

    @Override
    public ServiceResultHandler<List<File>> getResultHandler() {
        return new AbstractFileListServiceResultHandler() {
            @Override
            public boolean isResultReady(JacsServiceResult<?> depResults) {
                return true;
            }
            @Override
            public List<File> collectResult(JacsServiceResult<?> depResults) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private SparkCluster startCluster(JacsServiceData sd) {
        // TODO: Should cache this somehow so it doesn't need to get recomputed each time
        Path workingDir = getWorkingDirectory(sd);
        try {
            SparkCluster cluster = clusterSource.get();
            cluster.startCluster(workingDir, numNodes);
            logger.info("Waiting until Spark cluster is ready...");
            return cluster;
        }
        catch (Exception e) {
            throw new ComputationException(sd, e);
        }
    }

    private SparkApp runApp(JacsServiceData jacsServiceData, SparkCluster cluster) {
        ColorDepthSearchArgs args = getArgs(jacsServiceData);
        logger.info("Using args="+args);

        Path workingDir = getWorkingDirectory(jacsServiceData);

        List<String> inputFiles = new ArrayList<>();
        List<String> outputFiles = new ArrayList<>();
        Set<Path> outputPaths = new HashSet<>();

        for(String inputFile : args.inputFiles.split(",")) {
            inputFiles.add(inputFile);
            String name = FileUtils.getFileNameOnly(inputFile);

            int i = 1;
            Path outputPath;
            do {
                String discriminator = i == 1 ? "" : "_"+i;
                outputPath = workingDir.resolve(name + discriminator + RESULTS_FILENAME_SUFFIX);
                i++;
            } while (outputPaths.contains(outputPath));

            outputPaths.add(outputPath);
            outputFiles.add(outputPath.toFile().getAbsolutePath());
        }

        List<String> appArgs = new ArrayList<>();
        appArgs.add("-p");
        appArgs.add(""+parallelism);
        appArgs.add("-m");
        appArgs.addAll(inputFiles);
        appArgs.add("-i");
        appArgs.add(args.searchDirs);
        appArgs.add("-o");
        appArgs.addAll(outputFiles);
        String[] appArgsArr = appArgs.toArray(new String[appArgs.size()]);

        try {
            logger.info("Starting Spark application");
            return cluster.runApp(null, jarPath, appArgsArr);
        }
        catch (Exception e) {
            throw new ComputationException(jacsServiceData, e);
        }
    }

    @Override
    public ServiceComputation<JacsServiceResult<List<File>>> process(JacsServiceData jacsServiceData) {

        // Create the working directory
        // TODO: this should be managed by a FileNode interface, which has not yet been ported from JACSv1
        Path workingDir = getWorkingDirectory(jacsServiceData);
        try {
            Files.createDirectories(workingDir);
        }
        catch (IOException e) {
            throw new ComputationException(jacsServiceData, e);
        }

        SparkCluster sparkCluster = startCluster(jacsServiceData);

        return computationFactory.newCompletedComputation(sparkCluster)

                // Wait until the cluster has started
                .thenSuspendUntil2((SparkCluster cluster) -> continueWhenTrue(cluster.isReady(), cluster),
                        clusterIntervalCheckInMillis, clusterStartTimeoutInMillis)

                // Now run the search
                .thenApply((SparkCluster cluster) -> runApp(jacsServiceData, cluster))

                // Wait until the search has completed
                .thenSuspendUntil2((SparkApp app) -> continueWhenTrue(app.isDone(), app),
                        searchIntervalCheckInMillis, searchTimeoutInMillis)

                // This is the "finally" block. We must always kill the cluster no matter what happens above.
                // We don't attempt to extract the cluster from cond, because that may be null if there's an exception.
                // Instead, we use the instance from the surrounding closure, which is guaranteed to work.
                .whenComplete((app, exc) -> {
                    sparkCluster.stopCluster();
                })

                // Deal with the results
                .thenApply((app) -> {

                    List<File> resultsFiles = FileUtils.lookupFiles(
                                workingDir, 1, "glob:**/*"+RESULTS_FILENAME_SUFFIX)
                            .map(Path::toFile)
                            .collect(Collectors.toList());

                    for(File resultsFile : resultsFiles) {

                        String maskFile;
                        ColorDepthSearchResults results = new ColorDepthSearchResults();
                        try (Scanner scanner = new Scanner(resultsFile)) {
                            maskFile = scanner.nextLine();
                            while (scanner.hasNext()) {
                                String line = scanner.nextLine();
                                String[] s = line.split("\t");
                                float score = Float.parseFloat(s[0].trim());
                                String filepath = s[1].trim();
                                results.getResultList().add(new ColorDepthSearchResults.ColorDepthSearchResult(score, filepath));
                            }
                        } catch (IOException e) {
                            throw new ComputationException(jacsServiceData, e);
                        }

                        logger.info("First five results for mask {}:", maskFile);
                        int c = 0;
                        for (ColorDepthSearchResults.ColorDepthSearchResult result : results.getResultList()) {
                            logger.info(result.getScore() + ": " + result.getFilename());
                            if (c++ > 4) break;
                        }
                    }

                    return updateServiceResult(jacsServiceData, resultsFiles);
                });
    }

    protected JacsServiceData prepareProcessing(JacsServiceData jacsServiceData) {
        JacsServiceData jacsServiceDataHierarchy = jacsServiceDataPersistence.findServiceHierarchy(jacsServiceData.getId());
        if (jacsServiceDataHierarchy == null) {
            jacsServiceDataHierarchy = jacsServiceData;
        }
        setOutputAndErrorPaths(jacsServiceDataHierarchy);
        return jacsServiceDataHierarchy;
    }

    private ColorDepthSearchArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(getJacsServiceArgsArray(jacsServiceData), new ColorDepthSearchArgs());
    }
}
