package org.janelia.jacs2.asyncservice.fileservices;

import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ComputationTestUtils;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.cdi.ApplicationConfigProvider;
import org.janelia.jacs2.config.ApplicationConfig;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileCopyProcessorTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private JacsServiceDataPersistence jacsServiceDataPersistence;
    private String libraryPath = "testLibrary";
    private String scriptName = "testScript";
    private String defaultWorkingDir = "testWorking";
    private String executablesBaseDir = "testTools";

    private FileCopyProcessor testProcessor;
    private File testDirectory;

    @Before
    public void setUp() throws IOException {
        jacsServiceDataPersistence = mock(JacsServiceDataPersistence.class);
        Logger logger = mock(Logger.class);
        ServiceComputationFactory serviceComputationFactory = ComputationTestUtils.createTestServiceComputationFactory(logger);

        ApplicationConfig applicationConfig = new ApplicationConfigProvider().fromMap(
                ImmutableMap.of("Executables.ModuleBase", executablesBaseDir))
                .build();

        testProcessor = new FileCopyProcessor(serviceComputationFactory,
                jacsServiceDataPersistence,
                null, // serviceRunners are not essential for these unit tests
                defaultWorkingDir,
                libraryPath,
                scriptName,
                ComputationTestUtils.createTestThrottledProcessesQueue(),
                applicationConfig,
                logger);
        testDirectory = Files.createTempDirectory("testFileCopy").toFile();
    }

    @After
    public void tearDown() throws IOException {
        Files.delete(testDirectory.toPath());
    }

    @Test
    public void successfulPreprocessing() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .setName("fileCopy")
                .addArg("-src", "/home/testSource")
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        testProcessor.prepareProcessing(testServiceData);
        assertTrue(testDestFile.getParentFile().exists());
    }

    @Test
    public void missingRequiredParameter() throws ExecutionException, InterruptedException {
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .setName("fileCopy")
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        verifyCompletionWithException(testServiceData);
    }

    @Test
    public void emptySourceOrTarget() throws ExecutionException, InterruptedException {
        JacsServiceDataBuilder testServiceDataBuilder = new JacsServiceDataBuilder(null);
        verifyCompletionWithException(testServiceDataBuilder
                        .addArg("-dst", "dst") // the arg order is important here in order to capture the execution branch
                        .addArg("-src", "")
                        .build());
        verifyCompletionWithException(testServiceDataBuilder
                        .clearArgs()
                        .addArg("-src", "src")
                        .addArg("-dst", "")
                        .build());
   }

    private void verifyCompletionWithException(JacsServiceData testServiceData) throws ExecutionException, InterruptedException {
        expectedException.expect(ComputationException.class);
        testProcessor.prepareProcessing(testServiceData);
    }

    @Test
    public void deleteSourceWhenDone() throws IOException {
        Path testSourcePath = Files.createTempFile(testDirectory.toPath(), "testFileCopySource", ".test");
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSourcePath.toString())
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        assertTrue(Files.exists(testSourcePath));
        testProcessor.postProcessing(new JacsServiceResult<>(testServiceData, testDestFile));
        assertTrue(Files.notExists(testSourcePath));
    }

    @Test
    public void cannotDeleteSourceWhenDone() throws IOException {
        Path testSourcePath = Files.createTempFile(testDirectory.toPath(), "testFileCopySource", ".test");
        try {
            File testDestFile = new File(testDirectory, "testDest");
            JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                    .addArg("-src", testDirectory.getAbsolutePath()) // pass in a non-empty dir so that it cannot be deleted
                    .addArg("-dst", testDestFile.getAbsolutePath())
                    .addArg("-mv")
                    .addArg("-convert8")
                    .build();
            expectedException.expect(UncheckedIOException.class);
            testProcessor.postProcessing(new JacsServiceResult<>(testServiceData, testDestFile));
            assertTrue(Files.exists(testSourcePath));
        } finally {
            Files.deleteIfExists(testSourcePath);
        }
    }

    @Test
    public void doNotDeleteSourceWhenDone() throws IOException {
        Path testSourcePath = Files.createTempFile(testDirectory.toPath(), "testFileCopySource", ".test");
        try {
            File testDestFile = new File(testDirectory, "testDest");
            JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                    .addArg("-src", testSourcePath.toString())
                    .addArg("-dst", testDestFile.getAbsolutePath())
                    .addArg("-convert8")
                    .build();
            testProcessor.postProcessing(new JacsServiceResult<>(testServiceData, testDestFile));
            assertTrue(Files.exists(testSourcePath));
        } finally {
            Files.deleteIfExists(testSourcePath);
        }
    }

    @Test
    public void cmdArgs() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .build();
        ExternalCodeBlock copyScript = testProcessor.prepareExternalScript(testServiceData);
        assertThat(copyScript.toString(),
                equalTo(executablesBaseDir + "/" + scriptName + " " + testSource + " " + testDestFile.getAbsolutePath() + " \n"));
    }

    @Test
    public void cmdArgsWithConvertFlag() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        ExternalCodeBlock copyScript = testProcessor.prepareExternalScript(testServiceData);
        assertThat(copyScript.toString(),
                equalTo(executablesBaseDir + "/" + scriptName + " " + testSource + " " + testDestFile.getAbsolutePath() + " 8 \n"));
    }

    @Test
    public void prepareEnv() {
        String testSource = "/home/testSource";
        File testDestFile = new File(testDirectory, "testDest");
        JacsServiceData testServiceData = new JacsServiceDataBuilder(null)
                .addArg("-src", testSource)
                .addArg("-dst", testDestFile.getAbsolutePath())
                .addArg("-mv")
                .addArg("-convert8")
                .build();
        Map<String, String> env = testProcessor.prepareEnvironment(testServiceData);
        assertThat(env, hasEntry(equalTo("LD_LIBRARY_PATH"), containsString(libraryPath)));
    }

}
