/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.compute.db_fmt_timings;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.util.ThreadUtils;
import org.junit.Test;

/**
 *
 * @author fosterl
 */
public class JavaSerializationSerializerTest {
    
    protected ExecutorService createExecutor(int numThreads) {
        ExecutorService executor = ThreadUtils.establishExecutor(numThreads, new ThreadFactory() {
            private int threadCounter = 1;
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("BinarySerializerTest_" + threadCounter++);
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
        List<TmNeuron> neurons = new TmObjectCreationHelper().createObjects();
        // make fixed size thread pool, 10 threads.  And then write each object to the string/read back from string.
        Long startTime = new Date().getTime();
        final List<byte[]> neuronsAsArrays = Collections.synchronizedList(new ArrayList<byte[]>());
        final List<Future<Void>> callbacks = new ArrayList<>();
        ExecutorService executor = createExecutor(20);
        
        for (TmNeuron neuron : neurons) {
            final TmNeuron nextNeuron = neuron;
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(byteArrayOutputStream))) {
                        oos.writeObject(nextNeuron);
                        oos.flush();
                    }
                    
                    neuronsAsArrays.add(byteArrayOutputStream.toByteArray());
                    return null;
                }
            };
            callbacks.add(executor.submit(callable));
        }
                
        // Need to run down the executor.
        ThreadUtils.followUpExecution(executor, callbacks, 10);
        System.out.println(neuronsAsArrays.size() + " Neurons created.");
        System.out.println("Time required for multi-threaded write-to-mem: " + (new Date().getTime() - startTime) + "ms.");

        callbacks.clear();
        executor = createExecutor(20);
        startTime = new Date().getTime();
        final List<TmNeuron> collectedNeurons = Collections.synchronizedList(new ArrayList<TmNeuron>());
        for (byte[] neuronArray : neuronsAsArrays) {
            final byte[] nextNeuronArray = neuronArray;
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() {
                    try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream( new ByteArrayInputStream(nextNeuronArray)))) {
                        TmNeuron tmNeuron = (TmNeuron)ois.readObject();
                        collectedNeurons.add(tmNeuron);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                    return null;
                }
                
            };
            callbacks.add(executor.submit(callable));
            
        }
        ThreadUtils.followUpExecution(executor, callbacks, 10);
        System.out.println(collectedNeurons.size() + " neurons collected.");
        
        long totalBytesSize = 0L;
        for (byte[] neuronArray : neuronsAsArrays) {
            totalBytesSize += neuronArray.length;
        }
        System.out.println("Total size of serialized bytes=" + totalBytesSize);
        System.out.println("Time required for multi-threaded mem-to-object: " + (new Date().getTime() - startTime) + "ms.");
    }

}
