package org.janelia.jacs2.asyncservice.lightsheetservices;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.common.ComputationTestHelper;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgMatcher;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.asyncservice.common.ServiceProcessorTestHelper;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.asyncservice.containerizedservices.PullSingularityContainerProcessor;
import org.janelia.jacs2.asyncservice.containerizedservices.SimpleRunSingularityContainerProcessor;
import org.janelia.jacs2.asyncservice.utils.FileUtils;
import org.janelia.jacs2.cdi.ApplicationConfigProvider;
import org.janelia.jacs2.cdi.ObjectMapperFactory;
import org.janelia.jacs2.config.ApplicationConfig;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.testhelpers.ListArgMatcher;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.JacsServiceDataBuilder;
import org.janelia.model.service.JacsServiceState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;

public class LightsheetPipelineStepProcessorTest {

    private final static String CONFIG_REFERENCE = "testconfig";

    private final static Map<LightsheetPipelineStep, Map<String, Object>> CLUSTER_STEP_CONFIGS = ImmutableMap.of(
            LightsheetPipelineStep.clusterCS, ImmutableMap.<String, Object>builder()
                    .put("dataType", 2)
                    .put("percentile", new int[]{2, 3, 4})
                    .put("outputType", 1)
                    .put("timepoints", ImmutableMap.of("start", 10, "every", 5, "end", 100))
                    .build(),
            LightsheetPipelineStep.clusterFR, ImmutableMap.<String, Object>builder()
                    .put("filterMode", 1)
                    .put("rangeArray", new int[]{10, 20})
                    .put("timepoints", ImmutableMap.of("start", 10, "every", 5, "end", 100))
                    .build()
            );
    private final static Map<LightsheetPipelineStep, Map<String, Object>> LOCAL_STEP_CONFIGS = ImmutableMap.of(
            LightsheetPipelineStep.localAP, ImmutableMap.<String, Object>builder()
                    .put("dataType", 2)
                    .put("percentile", new int[]{2, 3, 4})
                    .put("outputType", 1)
                    .build(),
            LightsheetPipelineStep.localEC, ImmutableMap.<String, Object>builder()
                    .put("filterMode", 1)
                    .put("rangeArray", new int[]{10, 20})
                    .build()
    );
    private static final Long TEST_SERVICE_ID = 21L;

    private File testDirectory;
    private LightsheetPipelineStepProcessor lightsheetPipelineStepProcessor;
    private PullSingularityContainerProcessor pullContainerProcessor;
    private SimpleRunSingularityContainerProcessor runContainerProcessor;

    @Before
    public void setUp() throws Exception {
        Logger logger = mock(Logger.class);
        ServiceComputationFactory serviceComputationFactory = ComputationTestHelper.createTestServiceComputationFactory(logger);
        JacsServiceDataPersistence jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        pullContainerProcessor = mock(PullSingularityContainerProcessor.class);
        runContainerProcessor = mock(SimpleRunSingularityContainerProcessor.class);

        ServiceProcessorTestHelper.prepareServiceProcessorMetadataAsRealCall(
                pullContainerProcessor,
                runContainerProcessor
        );

        ApplicationConfig applicationConfig = new ApplicationConfigProvider().fromMap(
                ImmutableMap.of(
                        "Container.Registry.URL", "shub://imagecatcher",
                        "ImageProcessing.Collection","{Container.Registry.URL}/imageprocessing",
                        "ImageProcessing.Lightsheet.Version", "1.0",
                        "ImageProcessing.Lightsheet.DataMountPoints", "/groups/lightsheet/lightsheet:/groups/lightsheet/lightsheet,/misc/local,:d1/d1.1,d2:"
                ))
                .build();

        testDirectory = Files.createTempDirectory("testLightsheetStepProcessor").toFile();

        Mockito.when(jacsServiceDataPersistence.findById(any(Number.class))).then(invocation -> {
            JacsServiceData sd = new JacsServiceData();
            sd.setId(invocation.getArgument(0));
            sd.setState(JacsServiceState.SUCCESSFUL);
            return sd;
        });
        Mockito.when(jacsServiceDataPersistence.createServiceIfNotFound(any(JacsServiceData.class))).then(invocation -> {
            JacsServiceData jacsServiceData = invocation.getArgument(0);
            jacsServiceData.setId(TEST_SERVICE_ID);
            jacsServiceData.setState(JacsServiceState.SUCCESSFUL); // mark the service as completed otherwise the computation doesn't return
            return jacsServiceData;
        });

        lightsheetPipelineStepProcessor = new LightsheetPipelineStepProcessor(serviceComputationFactory,
                jacsServiceDataPersistence,
                testDirectory.getAbsolutePath(),
                applicationConfig,
                pullContainerProcessor,
                runContainerProcessor,
                ObjectMapperFactory.instance().getDefaultObjectMapper(),
                logger);
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.deletePath(testDirectory.toPath());
    }

    @Test
    public void processWithNonDefaultContainerLocation() {
        LOCAL_STEP_CONFIGS.forEach((step, config) -> {
            int stepIndex = 1;
            JacsServiceData testServiceData = createTestService(step, stepIndex, 10, config);
            testServiceData.getDictionaryArgs().put("containerImage", "shub://otherregistry/imageprocessing/" + step.name().toLowerCase() + ":latest");
            prepareResultHandlers(step);
            ServiceComputation<JacsServiceResult<Void>> stepComputation = lightsheetPipelineStepProcessor.process(testServiceData);
            @SuppressWarnings("unchecked")
            Consumer<JacsServiceResult<Void>> successful = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> failure = mock(Consumer.class);
            stepComputation
                    .thenApply(r -> {
                        successful.accept(r);
                        File stepConfigFile = new File(
                                testDirectory,
                                testServiceData.getId() + "/" + "stepConfig_" + CONFIG_REFERENCE + "_" + stepIndex + "_" + step + ".json");
                        assertTrue(stepConfigFile.exists());
                        Mockito.verify(runContainerProcessor).createServiceData(
                                any(ServiceExecutionContext.class),
                                argThat(new ListArgMatcher<>(
                                        ImmutableList.of(
                                                new ServiceArgMatcher(new ServiceArg("-containerLocation", new File(testDirectory, step.name().toLowerCase() + ".simg").getAbsolutePath())),
                                                new ServiceArgMatcher(new ServiceArg("-bindPaths",
                                                        stepConfigFile.getParentFile().getAbsolutePath() + ":" + stepConfigFile.getParentFile().getAbsolutePath() + "," +
                                                                "/groups/lightsheet/lightsheet:/groups/lightsheet/lightsheet" + "," +
                                                                "/misc/local" + "," +
                                                                "d1/d1.1:d1/d1.1" + "," +
                                                                "d2"
                                                )),
                                                new ServiceArgMatcher(new ServiceArg("-appArgs", stepConfigFile.getAbsolutePath())),
                                                new ServiceArgMatcher(new ServiceArg("-batchJobArgs", ""))
                                        )
                                ))
                        );
                        return r;
                    })
                    .exceptionally(exc -> {
                        failure.accept(exc);
                        fail(exc.toString());
                        return null;
                    })
            ;
            Mockito.verify(successful).accept(any());
        });
    }

    @Test
    public void processLocalStepJob() {
        LOCAL_STEP_CONFIGS.forEach((step, config) -> {
            int stepIndex = 1;
            JacsServiceData testServiceData = createTestService(step, stepIndex, 10, config);
            prepareResultHandlers(step);
            ServiceComputation<JacsServiceResult<Void>> stepComputation = lightsheetPipelineStepProcessor.process(testServiceData);
            @SuppressWarnings("unchecked")
            Consumer<JacsServiceResult<Void>> successful = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> failure = mock(Consumer.class);
            stepComputation
                    .thenApply(r -> {
                        successful.accept(r);
                        File stepConfigFile = new File(
                                testDirectory,
                                testServiceData.getId() + "/" + "stepConfig_" + CONFIG_REFERENCE + "_" + stepIndex + "_" + step + ".json");
                        assertTrue(stepConfigFile.exists());
                        Mockito.verify(runContainerProcessor).createServiceData(
                                any(ServiceExecutionContext.class),
                                argThat(new ListArgMatcher<>(
                                        ImmutableList.of(
                                                new ServiceArgMatcher(new ServiceArg("-containerLocation", new File(testDirectory, step.name().toLowerCase() + ".simg").getAbsolutePath())),
                                                new ServiceArgMatcher(new ServiceArg("-bindPaths",
                                                        stepConfigFile.getParentFile().getAbsolutePath() + ":" + stepConfigFile.getParentFile().getAbsolutePath() + "," +
                                                                "/groups/lightsheet/lightsheet:/groups/lightsheet/lightsheet" + "," +
                                                                "/misc/local" + "," +
                                                                "d1/d1.1:d1/d1.1" + "," +
                                                                "d2"
                                                )),
                                                new ServiceArgMatcher(new ServiceArg("-appArgs", stepConfigFile.getAbsolutePath())),
                                                new ServiceArgMatcher(new ServiceArg("-batchJobArgs", ""))
                                        )
                                ))
                        );
                        return r;
                    })
                    .exceptionally(exc -> {
                        failure.accept(exc);
                        fail(exc.toString());
                        return null;
                    })
            ;
            Mockito.verify(successful).accept(any());
        });
    }

    @Test
    public void processSingleClusterStepJob() {
        CLUSTER_STEP_CONFIGS.forEach((step, config) -> {
            int stepIndex = 1;
            int timePointsPerJob = 10;
            JacsServiceData testServiceData = createTestService(step, stepIndex, timePointsPerJob, config);
            prepareResultHandlers(step);
            ServiceComputation<JacsServiceResult<Void>> stepComputation = lightsheetPipelineStepProcessor.process(testServiceData);
            @SuppressWarnings("unchecked")
            Consumer<JacsServiceResult<Void>> successful = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> failure = mock(Consumer.class);
            stepComputation
                    .thenApply(r -> {
                        successful.accept(r);
                        File stepConfigFile = new File(
                                testDirectory,
                                testServiceData.getId() + "/" + "stepConfig_" + CONFIG_REFERENCE + "_" + stepIndex + "_" + step + ".json");
                        assertTrue(stepConfigFile.exists());
                        Mockito.verify(runContainerProcessor).createServiceData(
                                any(ServiceExecutionContext.class),
                                argThat(new ListArgMatcher<>(
                                        ImmutableList.of(
                                                new ServiceArgMatcher(new ServiceArg("-containerLocation", new File(testDirectory, step.name().toLowerCase() + ".simg").getAbsolutePath())),
                                                new ServiceArgMatcher(new ServiceArg("-bindPaths",
                                                        stepConfigFile.getParentFile().getAbsolutePath() + ":" + stepConfigFile.getParentFile().getAbsolutePath() + "," +
                                                                "/groups/lightsheet/lightsheet:/groups/lightsheet/lightsheet" + "," +
                                                                "/misc/local" + "," +
                                                                "d1/d1.1:d1/d1.1" + "," +
                                                                "d2"
                                                )),
                                                new ServiceArgMatcher(new ServiceArg("-appArgs", stepConfigFile.getAbsolutePath() + "," + String.valueOf(timePointsPerJob))),
                                                new ServiceArgMatcher(new ServiceArg("-batchJobArgs", "1,2"))
                                        )
                                ))
                        );
                        return r;
                    })
                    .exceptionally(exc -> {
                        failure.accept(exc);
                        fail(exc.toString());
                        return null;
                    })
            ;
            Mockito.verify(successful).accept(any());
        });
    }

    @Test
    public void processMultipleClusterStepJob() {
        CLUSTER_STEP_CONFIGS.forEach((step, config) -> {
            int stepIndex = 1;
            int timePointsPerJob = 10;
            JacsServiceData testServiceData = createTestService(step, stepIndex, timePointsPerJob, config);
            prepareResultHandlers(step);
            ServiceComputation<JacsServiceResult<Void>> stepComputation = lightsheetPipelineStepProcessor.process(testServiceData);
            @SuppressWarnings("unchecked")
            Consumer<JacsServiceResult<Void>> successful = mock(Consumer.class);
            @SuppressWarnings("unchecked")
            Consumer<Throwable> failure = mock(Consumer.class);
            stepComputation
                    .thenApply(r -> {
                        successful.accept(r);
                        File stepConfigFile = new File(
                                testDirectory,
                                testServiceData.getId() + "/" + "stepConfig_" + CONFIG_REFERENCE + "_" + stepIndex + "_" + step + ".json");
                        assertTrue(stepConfigFile.exists());
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> timepoints = (Map<String, Integer>) config.get("timepoints");
                        int timePoints = (timepoints.get("end") - timepoints.get("start")) / timepoints.get("every");
                        int numJobs = (int) Math.ceil((double)timePoints / timePointsPerJob);
                        for (int j = 0; j < numJobs; j++) {
                            Mockito.verify(runContainerProcessor).createServiceData(
                                    any(ServiceExecutionContext.class),
                                    argThat(new ListArgMatcher<>(
                                            ImmutableList.of(
                                                    new ServiceArgMatcher(new ServiceArg("-containerLocation", new File(testDirectory, step.name().toLowerCase() + ".simg").getAbsolutePath())),
                                                    new ServiceArgMatcher(new ServiceArg("-bindPaths",
                                                            stepConfigFile.getParentFile().getAbsolutePath() + ":" + stepConfigFile.getParentFile().getAbsolutePath() + "," +
                                                                    "/groups/lightsheet/lightsheet:/groups/lightsheet/lightsheet" + "," +
                                                                    "/misc/local" + "," +
                                                                    "d1/d1.1:d1/d1.1" + "," +
                                                                    "d2"
                                                    )),
                                                    new ServiceArgMatcher(new ServiceArg("-appArgs", stepConfigFile.getAbsolutePath() + "," + String.valueOf(timePointsPerJob))),
                                                    new ServiceArgMatcher(new ServiceArg("-batchJobArgs", "1,2"))
                                            )
                                    ))
                            );
                        }
                        return r;
                    })
                    .exceptionally(exc -> {
                        failure.accept(exc);
                        fail(exc.toString());
                        return null;
                    })
            ;
            Mockito.verify(successful).accept(any());
        });
    }

    private void prepareMockPathsThatExists(String... pnames) {
        Map<String, Path> pathStringMap = new HashMap<>();
        for (String pname : pnames) {
            Path mockPath = Mockito.mock(Path.class);
            File mockFile = Mockito.mock(File.class);
            Mockito.when(mockPath.toFile()).thenReturn(mockFile);
            Mockito.when(mockFile.exists()).thenReturn(true);
            pathStringMap.put(pname, mockPath);
            Mockito.when(mockPath.toString()).thenReturn(pname);
        }
        Mockito.when(Paths.get(anyString())).then(invocation -> {
            String pname = invocation.getArgument(0);
            if (pathStringMap.keySet().contains(pname)) {
                return pathStringMap.get(pname);
            } else {
                return invocation.callRealMethod();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private WebTarget prepareConfigEnpointTestTarget() {
        WebTarget configEndpoint = Mockito.mock(WebTarget.class);
        Invocation.Builder configRequestBuilder = Mockito.mock(Invocation.Builder.class);
        Response configResponse = Mockito.mock(Response.class);
        Mockito.when(configEndpoint.queryParam(anyString(), anyString())).thenReturn(configEndpoint);
        Mockito.when(configEndpoint.request()).thenReturn(configRequestBuilder);
        Mockito.when(configRequestBuilder.get()).thenReturn(configResponse);
        Mockito.when(configResponse.getStatus()).thenReturn(200);
        String testData = "{\"key\": \"val\"}";
        Mockito.when(configResponse.readEntity(any(GenericType.class)))
                .then(invocation -> ObjectMapperFactory.instance().newObjectMapper().readValue(testData, new TypeReference<Map<String, Object>>(){}));
        return configEndpoint;
    }

    private void prepareResultHandlers(LightsheetPipelineStep step) {
        @SuppressWarnings("unchecked")
        ServiceResultHandler<File> pullContainerResultHandler = mock(ServiceResultHandler.class);
        Mockito.when(pullContainerResultHandler.getServiceDataResult(any(JacsServiceData.class)))
                .then(invocation -> new File(testDirectory, step.name().toLowerCase() + ".simg"));
        Mockito.when(pullContainerProcessor.getResultHandler()).thenReturn(pullContainerResultHandler);
        Mockito.when(runContainerProcessor.getResultHandler()).thenCallRealMethod();
    }

    private JacsServiceData createTestService(LightsheetPipelineStep step, int stepIndex, int timePointsPerJob, Map<String, Object> stepParameters) {
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .setWorkspace(testDirectory.getAbsolutePath())
                .addArgs("-step", step.name())
                .addArgs("-stepIndex", String.valueOf(stepIndex))
                .addArgs("-configReference", CONFIG_REFERENCE)
                .addArgs("-timePointsPerJob", String.valueOf(timePointsPerJob))
                .setDictionaryArgs(ImmutableMap.of("parameters", stepParameters))
                .build();
        testServiceData.setId(TEST_SERVICE_ID);
        return testServiceData;
    }

}
