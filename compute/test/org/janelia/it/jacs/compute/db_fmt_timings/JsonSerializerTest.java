/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.db_fmt_timings;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.janelia.it.jacs.shared.utils.ThreadUtils;
import org.junit.Test;

/**
 *
 * @author fosterl
 */
public class JsonSerializerTest {
    private static final int THREAD_COUNT = 30;
    private static final int FOLLOW_UP_MIN = 10;
    private ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
    
    protected ExecutorService createExecutor(int numThreads) {
        ExecutorService executor = ThreadUtils.establishExecutor(numThreads, new ThreadFactory() {
            private int threadCounter = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("JsonSerializerTest_" + threadCounter++);
                return t;
            }

        });
        return executor;
    }
    
    protected ExecutorService createExecutor() {
        return createExecutor(10);
    }
    
    @Test
    public void testMultiThreadedRW() throws Exception {
        System.out.println(">>>> Multithreaded R/W");
        List<MockNeuronJsonData> neurons = new MockObjectCreationHelper().createObjects();
        // make fixed size thread pool, 10 threads.  And then write each object to the string/read back from string.
        Long startTime = new Date().getTime();
        final List<String> neuronsAsStrings = new ArrayList<>();
        final List<Future<Void>> callbacks = new ArrayList<>();
        ExecutorService executor = createExecutor(THREAD_COUNT);
        
        for (MockNeuronJsonData neuron : neurons) {
            final MockNeuronJsonData nextNeuron = neuron;
            Callable<Void> callable = new Callable() {
                @Override
                public Void call() {
                    StringWriter sw = new StringWriter(100000);
                    try {
                        mapper.writeValue(sw, nextNeuron);
                        neuronsAsStrings.add(sw.toString());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return null;
                }
            };
            callbacks.add(executor.submit(callable));
        }
        // Need to run down the executor.
        ThreadUtils.followUpExecution(executor, callbacks, FOLLOW_UP_MIN);
        System.out.println("Time required for multi-threaded write-to-mem: " + (new Date().getTime() - startTime) + "ms.");

        callbacks.clear();
        executor = createExecutor(THREAD_COUNT);
        startTime = new Date().getTime();
        for (String neuronString : neuronsAsStrings) {
            final String nextNeuronString = neuronString;
            Callable<Void> callable = new Callable() {
                @Override
                public Void call() {
                    try {
                        StringReader sr = new StringReader(nextNeuronString);
                        mapper.readValue(sr, MockNeuronJsonData.class);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return null;
                }
                
            };
            callbacks.add(executor.submit(callable));
            
        }
        ThreadUtils.followUpExecution(executor, callbacks, FOLLOW_UP_MIN);
        
        long totalJsonSize = 0L;
        for (String neuronString : neuronsAsStrings) {
            totalJsonSize += neuronString.length();
        }
        System.out.println("Total size of JSON=" + totalJsonSize);
        System.out.println("Time required for multi-threaded mem-to-object: " + (new Date().getTime() - startTime) + "ms.");
    }

    //@Test
    public void testWrite() {
        System.out.println(">>>> Single-threaded write");
        
        // Now, attempt to serialize to JSON.
        try {
            Long start = new Date().getTime();
            StringWriter sw = new StringWriter(100000000);
            List<MockNeuronJsonData> neurons = new MockObjectCreationHelper().createObjects();
            mapper.writeValue(sw, neurons);
            System.out.println("Elapsed write time to mem: " + (new Date().getTime() - start) + "ms");                        

            // Write to a new file.
            final File file = new File("MockData.json");
            file.delete();
            start = new Date().getTime();
            mapper.writeValue(file, neurons);
            System.out.println("Elapsed write time to file: " + (new Date().getTime() - start) + "ms.  File size is " + file.length());
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    //@Test
    public void testRead() {
        System.out.println(">>>> Single-threaded read");
        try {
            Long start = new Date().getTime();
            List<MockNeuronJsonData> dataList = mapper.readValue(new File("MockData.json"), ArrayList.class);
            System.out.println("Elapsed read time from file: " + (new Date().getTime() - start) + "ms. Total read count: " + dataList.size());
            
            // Trying to scoop-to-string, and see how long it takes.
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new FileReader("MockData.json"))) {
                String inline = null;
                while (null != (inline = br.readLine())) {
                    sb.append(inline);
                }
                start = new Date().getTime();
                dataList = mapper.readValue(sb.toString(), ArrayList.class);                
                
                System.out.println("Elapsed read time from in-memory string: " + (new Date().getTime() - start) + "ms. Total read count: " + dataList.size());
            } catch (Exception rex) {
                rex.printStackTrace();
                throw rex;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
