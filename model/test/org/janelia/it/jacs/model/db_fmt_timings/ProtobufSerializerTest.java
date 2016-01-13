/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.it.jacs.model.db_fmt_timings;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;
import org.janelia.it.jacs.model.util.ThreadUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 *
 * @author fosterl
 */
public class ProtobufSerializerTest {
    
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
    public void taggedTest() throws Exception {
        byte[] protobuf = null;
		LinkedBuffer buffer = LinkedBuffer.allocate();
        final Schema<VerySmallTagged> schema = RuntimeSchema.getSchema(VerySmallTagged.class); 
        try {
            final VerySmallTagged verySmallTagged = new VerySmallTagged();
            protobuf = ProtobufIOUtil.toByteArray(verySmallTagged, schema, buffer);
            VerySmallTagged returned = new VerySmallTagged();
            returned.setId(7);
            returned.setName("A different object");
            ProtobufIOUtil.mergeFrom(protobuf, returned, schema);
            Assert.assertEquals(returned.getId(), verySmallTagged.getId());
        } finally {
        }
    }

	//@Test
	public void aSmallTest() throws Exception {
		// Serialize something really simple.
		java.awt.Dimension d = new java.awt.Dimension(50,90);
		LinkedBuffer buffer = LinkedBuffer.allocate();
		final Schema<java.awt.Dimension> schema = RuntimeSchema.getSchema(java.awt.Dimension.class); 
		byte[] protobuf = ProtobufIOUtil.toByteArray(d, schema, buffer);
		
		// Now deserialize the same value.
		java.awt.Dimension newDimension = schema.newMessage();
		ProtobufIOUtil.mergeFrom(protobuf, newDimension, schema);
		Assert.assertEquals("Dimensions do not match.", d, newDimension);		
	}
	
	@Test
	public void aLargeTest() throws Exception {
		List<MockNeuronJsonData> mockNeurons = new MockObjectCreationHelper().createSmallSetOfObjects();
		final Schema<MockNeuronJsonData> schema = RuntimeSchema.getSchema(MockNeuronJsonData.class); 
		final List<byte[]> neuronsAsArrays = new ArrayList<>();
		for (MockNeuronJsonData nextNeuron: mockNeurons) {
			final LinkedBuffer buffer = LinkedBuffer.allocate();
			// Populate a byte array from serialized data.
			try {
				byte[] protobuf = ProtobufIOUtil.toByteArray(nextNeuron, schema, buffer);
				neuronsAsArrays.add(protobuf);
			} finally {
				buffer.clear();
			}			
		}
		// Populate a list of neurons from deserialized bytes.
		final LinkedBuffer buffer = LinkedBuffer.allocate();
		for (byte[] nextNeuronArray: neuronsAsArrays) {
			try {
				MockNeuronJsonData mockNeuron = schema.newMessage();
				//ByteArrayInputStream inputStream = new ByteArrayInputStream( nextNeuronArray );
				ProtobufIOUtil.mergeFrom(nextNeuronArray, mockNeuron, schema);
				//ProtostuffIOUtil.mergeFrom(inputStream, tmNeuron, schema, buffer);
			} finally {
				buffer.clear();
			}
		}
	}
	
    //@Test
    public void testMultiThreadedRW() throws Exception {
        System.out.println(">>>> Multithreaded R/W");
        List<TmNeuron> neurons = new TmObjectCreationHelper().createObjects();
        // make fixed size thread pool, 10 threads.  And then write each object to the string/read back from string.
        Long startTime = new Date().getTime();
        final List<byte[]> neuronsAsArrays = Collections.synchronizedList(new ArrayList<byte[]>());
        final List<Future<Void>> callbacks = new ArrayList<>();
        ExecutorService executor = createExecutor(20);
		final Schema<TmNeuron> schema = RuntimeSchema.getSchema(TmNeuron.class); 
        for (TmNeuron neuron : neurons) {
            final TmNeuron nextNeuron = neuron;
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() throws Exception {
					final LinkedBuffer buffer = LinkedBuffer.allocate();        
					// Populate a byte array from serialized data.
					try {					
				        byte[] protobuf = ProtobufIOUtil.toByteArray(nextNeuron, schema, buffer);
	                    neuronsAsArrays.add(protobuf);
					} finally {
						buffer.clear();
					}
					
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
        final List<TmNeuron> collectedNeurons = Collections.synchronizedList(new ArrayList<TmNeuron>(neuronsAsArrays.size()));

		// Get rid of some of the excess memory used, to ensure that is not our problem.
		TmNeuron firstCreated = neurons.get(0);
		neurons.clear();

		System.gc();
		System.gc();

//		final List<TmNeuron> collectedNeurons = new ArrayList<TmNeuron>(neuronsAsArrays.size());
        for (byte[] neuronArray : neuronsAsArrays) {
            final byte[] nextNeuronArray = neuronArray;
            Callable<Void> callable = new Callable<Void>() {
                @Override
                public Void call() {
					// Populate a list of neurons from deserialized bytes.
					final LinkedBuffer buffer = LinkedBuffer.allocate();        
					try {
						TmNeuron tmNeuron = schema.newMessage();
						//ByteArrayInputStream inputStream = new ByteArrayInputStream( nextNeuronArray );
						ProtobufIOUtil.mergeFrom(nextNeuronArray, tmNeuron, schema);
						collectedNeurons.add(tmNeuron);
						//ProtostuffIOUtil.mergeFrom(inputStream, tmNeuron, schema, buffer);
					} catch (Exception ex) {
						ex.printStackTrace();
					} finally {
						buffer.clear();
					}
					
                    return null;
                }
                
            };
            callbacks.add(executor.submit(callable));
            
        }
        ThreadUtils.followUpExecution(executor, callbacks, 10);
        System.out.println(collectedNeurons.size() + " neurons collected.");
        
        System.out.println("Time required for multi-threaded mem-to-object: " + (new Date().getTime() - startTime) + "ms.");
		long totalBufferedSize = 0L;
		for (byte[] b: neuronsAsArrays) {
			totalBufferedSize += b.length;
		}
		System.out.println("In memory, as byte arrays, objects required " + totalBufferedSize + " bytes.");
    }

}
