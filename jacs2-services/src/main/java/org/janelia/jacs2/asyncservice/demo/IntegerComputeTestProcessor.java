package org.janelia.jacs2.asyncservice.demo;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.JacsServiceResult;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceErrorChecker;
import org.janelia.jacs2.asyncservice.common.ServiceResultHandler;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.model.service.JacsServiceData;
import org.janelia.model.service.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.LongStream;

@Named("integerComputeTest")
public class IntegerComputeTestProcessor extends AbstractServiceProcessor<Long> {

    private final static int DEFAULT_MATRIX_SIZE = 700;
    private final static int DEFAULT_ITERATIONS = 10;

    static class IntegerComputeTestArgs extends ServiceArgs {
        @Parameter(names = "-matrixSize", description = "Size of matrix NxN", required = false)
        Integer matrixSize = DEFAULT_MATRIX_SIZE;
        @Parameter(names = "-iterations", description = "Iterations per matrix multiply", required = false)
        Integer iterations = DEFAULT_ITERATIONS;
        @Parameter(names = "-testName", description = "Optional unique test name", required = false)
        String testName="IntegerComputeTest";
    }

    private long resultComputationTime;

    @Inject
    public IntegerComputeTestProcessor(ServiceComputationFactory computationFactory,
                                       @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                       Logger logger) {
        super(computationFactory, null, defaultWorkingDir, logger);
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(IntegerComputeTestProcessor.class, new IntegerComputeTestArgs());
    }

    @Override
    public ServiceComputation<JacsServiceResult<Long>> process(JacsServiceData jacsServiceData) {
        logger.debug("process() start");
        IntegerComputeTestArgs args = getArgs(jacsServiceData);
        int matrixSize = args.matrixSize;
        int iterations = args.iterations;
        logger.debug("matrixSize=" + matrixSize + ", iterations=" + iterations);
        long startTime = new Date().getTime();
        jacsServiceData.getArgs();
        Random random = new Random(startTime);
        LongStream longStream = random.longs(0L, 100L);
        PrimitiveIterator.OfLong iterator = longStream.iterator();
        long[] matrix1 = new long[matrixSize * matrixSize];
        long[] matrix2 = new long[matrixSize * matrixSize];
        long[] result = new long[matrixSize * matrixSize];
        int position = 0;
        // Create matrices
        logger.debug("Creating matrices");
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                matrix1[position] = iterator.nextLong();
                matrix2[position] = iterator.nextLong();
                position++;
            }
        }
        // Do multiply
        logger.debug("Doing matrix multiply");
        for (int i = 0; i < iterations; i++) {
            logger.debug("Starting iteration " + i + " of " + iterations);
            for (int column2 = 0; column2 < matrixSize; column2++) {
                for (int row1 = 0; row1 < matrixSize; row1++) {
                    long sum = 0L;
                    int row2 = 0;
                    for (int column1 = 0; column1 < matrixSize; column1++) {
                        sum += matrix1[row1 * matrixSize + column1] * matrix2[row2 * matrixSize + column2];
                        row2++;
                    }
                    result[row1 * matrixSize + column2] = sum;
                }
            }
        }
        long doneTime = new Date().getTime();
        resultComputationTime = doneTime - startTime;
        logger.debug("localProcessData() end, elapsed time="+resultComputationTime+" ms");
        return computationFactory.newCompletedComputation(new JacsServiceResult<Long>(jacsServiceData, resultComputationTime));
    }

    @Override
    public ServiceResultHandler<Long> getResultHandler() {
        throw new UnsupportedOperationException();
    }

    private IntegerComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return ServiceArgs.parse(getJacsServiceArgsArray(jacsServiceData), new IntegerComputeTestArgs());
    }

}
