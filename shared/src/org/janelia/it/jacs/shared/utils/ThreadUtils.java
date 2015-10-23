/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.shared.utils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Holds methods usable in multi-threading operations.
 *
 * @author fosterl
 */
public class ThreadUtils {
    /**
     * Given the executor service, await shutdown and throw back any exceptions
     * from the callable.  Only for no-return-value futures.
     * 
     * @param executorService as which runs the tasks that generated the list of futures
     * @param callbacks futures for everything that runs in the service
     * @param awaitMin how long to wait for the shutdown.
     * @throws Exception thrown by any task in the service.
     */
    public static void followUpExecution(ExecutorService executorService, List<Future<Void>> callbacks, int awaitMin) throws Exception {
        // Follow-up.
        executorService.shutdown();
        boolean completed = executorService.awaitTermination(awaitMin, TimeUnit.MINUTES);
        if (! completed) {
            throw new Exception("One or more operations were not completed as of shutdown.  More time than "+awaitMin+"min may be needed.");
        }

        for (Future<Void> future : callbacks) {
            future.get(); // 'Enjoy' any exceptions encountered.
        }
    }

    /**
     * Creates a service to execute tasks, with the number-of-threads given, and thread factory.
     * 
     * @param threadCount
     * @param threadFactory
     * @return 
     */
    public static ExecutorService establishExecutor(int threadCount, ThreadFactory threadFactory) {
        // Iterate over all the traced segments, and add enclosures for each.
        ExecutorService executorService = Executors.newFixedThreadPool(
                threadCount, threadFactory
        );
        return executorService;
    }

}
