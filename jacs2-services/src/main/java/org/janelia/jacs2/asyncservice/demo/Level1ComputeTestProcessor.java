package org.janelia.jacs2.asyncservice.demo;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.*;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

@Named("level1ComputeTest")
public class Level1ComputeTestProcessor extends AbstractBasicLifeCycleServiceProcessor<Long, Void> {

    private static final int DEFAULT_INTEGER_SERVICES=10;
    private static final int DEFAULT_FLOAT_SERVICES=5;

    public static class Level1ComputeTestArgs extends ServiceArgs {
        @Parameter(names="-integerServiceCount", description="Number of concurrent IntegerComputeTest", required=false)
        Integer integerServiceCount=DEFAULT_INTEGER_SERVICES;
        @Parameter(names="-floatServiceCount", description="Number of concurrent FloatComputeTest", required=false)
        Integer floatServiceCount=DEFAULT_FLOAT_SERVICES;
        @Parameter(names = "-testName", description = "Optional unique test name", required=false)
        String testName="Level1ComputeTest";
    }

    private long resultComputationTime;

    private final IntegerComputeTestProcessor integerComputeTestProcessor;
    private final FloatComputeTestProcessor floatComputeTestProcessor;

    @Inject
    public Level1ComputeTestProcessor(ServiceComputationFactory computationFactory,
                                      JacsServiceDataPersistence jacsServiceDataPersistence,
                                      @PropertyValue(name="service.DefaultWorkingDir") String defaultWorkingDir,
                                      Logger logger,
                                      IntegerComputeTestProcessor integerComputeTestProcessor,
                                      FloatComputeTestProcessor floatComputeTestProcessor) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.integerComputeTestProcessor=integerComputeTestProcessor;
        this.floatComputeTestProcessor=floatComputeTestProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(Level1ComputeTestProcessor.class, new Level1ComputeTestProcessor.Level1ComputeTestArgs());
    }

    @Override
    public ServiceComputation<JacsServiceResult<Long>> process(JacsServiceData jacsServiceData) {
        String serviceName=getArgs(jacsServiceData).testName;
        logger.info(serviceName+" start processing");
        long startTime=new Date().getTime();
        Level1ComputeTestArgs args=getArgs(jacsServiceData);

        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenApply(jsd -> {
                    logger.info("Beginning loop to create IntegerTests and save to queue");
                    for (int i = 0; i < args.integerServiceCount; i++) {
                        String testName = args.testName + ".IntegerTest" + i;
                        JacsServiceData j = integerComputeTestProcessor.createServiceData(new ServiceExecutionContext.Builder(jsd).build());
                        j.addArg("-testName");
                        j.addArg(testName);
                        logger.info("adding integerComputeTest " + testName);
                        jacsServiceDataPersistence.saveHierarchy(j);
                    }
                    return jsd;
                })
                .thenSuspendUntil(this.suspendCondition(jacsServiceData))
                .thenApply(jsd -> {
                    logger.info("Beginning loop to create FloatTests and save to queue");
                    for (int i = 0; i < args.floatServiceCount; i++) {
                        String testName = args.testName + ".FloatTest" + i;
                        JacsServiceData j = floatComputeTestProcessor.createServiceData(new ServiceExecutionContext.Builder(jsd).build());
                        j.addArg("-testName");
                        j.addArg(testName);
                        logger.info("adding floatComputeTest " + testName);
                        jacsServiceDataPersistence.saveHierarchy(j);
                    }
                    return jsd;
                })
                .thenSuspendUntil(this.suspendCondition(jacsServiceData))
                .thenApply(jsd -> {
                    logger.info("All tests complete for service " + serviceName);
                    long endTime = new Date().getTime();
                    resultComputationTime = endTime - startTime;
                    logger.info(serviceName + " end processing, processing time= " + resultComputationTime);
                    return new JacsServiceResult<>(jsd, endTime);
                })
                ;
    }

    @Override
    protected ServiceComputation<JacsServiceResult<Void>> processing(JacsServiceResult depsResult) {
        return null;
    }

    @Override
    public ServiceResultHandler<Long> getResultHandler() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceErrorChecker getErrorChecker() {
        throw new UnsupportedOperationException();
    }

    private Level1ComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(getJacsServiceArgsArray(jacsServiceData), new Level1ComputeTestArgs());
    }

}
