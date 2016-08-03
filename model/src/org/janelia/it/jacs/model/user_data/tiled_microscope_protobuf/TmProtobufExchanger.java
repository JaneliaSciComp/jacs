package org.janelia.it.jacs.model.user_data.tiled_microscope_protobuf;

import java.util.logging.Logger;

import javax.annotation.concurrent.ThreadSafe;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.TmNeuron;

/**
 * Exchanges data between byte array and Tile Microscope.  At time of writing,
 * all contents of a TmNeuron are supported.
 * 
 * All operations here must be thread safe.
 *
 * @author fosterl
 */
@ThreadSafe
public class TmProtobufExchanger {

    Logger logger = Logger.getLogger(TmProtobufExchanger.class.getName());

    private Schema<TmNeuron> schema = null;
    
    public TmProtobufExchanger() {
        try {
            schema = RuntimeSchema.getSchema(TmNeuron.class);
        } catch (Exception ex) {
            if (schema == null) {
                throw new RuntimeException("Failed to get schema for " + TmNeuron.class.getCanonicalName(), ex);
            }
        }
    }

    /**
     * Use protobuf mechanism to turn an array of raw data into a fully
     * inflated neuron.
     * 
     * @param protobufData found from serialized source.
     * @return neuron as it should have existed prior to serialization.
     * @throws Exception from any called methods.
     */
    public TmNeuron deserializeNeuron( byte[] protobufData ) throws Exception {
		return deserializeNeuron( protobufData, null );
    }
    
    /**
     * Use protobuf mechanism to turn an array of raw data into a fully
     * inflated neuron.
     * 
     * @param protobufData found from serialized source.
	 * @param oldNeuron existing, pre-instantiated neuron to populate.
     * @return neuron as it should have existed prior to serialization.
     * @throws Exception from any called methods.
     */
    public TmNeuron deserializeNeuron( byte[] protobufData, TmNeuron oldNeuron ) throws Exception {
        TmNeuron tmNeuron = null;
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        try {
			if (oldNeuron == null) {
	            tmNeuron = schema.newMessage();
			}
			else {
				tmNeuron = oldNeuron;
			}
            clearNeuronValues(tmNeuron);
            ProtobufIOUtil.mergeFrom(protobufData, tmNeuron, schema);
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            buffer.clear();
        }
        return tmNeuron;
    }

    /**
     * Turn a neuron into a series of bytes.
     * 
     * @param neuron what to store
     * @return array of bytes, suitable for
     * @see #deserializeNeuron(byte[]) 
     * @throws Exception from any called methods.
     */
    public byte[] serializeNeuron( TmNeuron neuron ) throws Exception {
        // Populate a byte array from serialized data.

        // NOTE: there is an oocasional sporadic concurrent modification
        // exception thrown my io.protostuff.MapSchema, line 341. It looks
        // like there might be a bug in the code whereby the collection is
        // modifying itself. To deal with this, a re-try loop is given
        // here.

        int retries=5;
        byte[] protobuf=null;

        for (;retries>0;retries--) {
            try {
                protobuf = null;
                final LinkedBuffer buffer = LinkedBuffer.allocate();
                if (retries<5) {
                    logger.info("serializeNeuron - starting with retries=" + retries);
                }
                try {
                    protobuf = ProtobufIOUtil.toByteArray(neuron, schema, buffer);
                }
                finally {
                    buffer.clear();
                }
                if (protobuf!=null) {
                    break;
                }
            }
            catch (Throwable t) {
                logger.warning("serializeNeuron failed: " + t.getMessage() + ", retries left=" + retries);
            }
            Thread.sleep(5);
        }
        if (protobuf==null) {
            throw new Exception("serializeNeuron failed and exhausted all retries");
        }
        return protobuf;
    }

    private void clearNeuronValues(TmNeuron tmNeuron) {
        // NOTE: merge-from will cause duplication of collections within
        // the neuron, unless clearing is done first.
        tmNeuron.getAnchoredPathMap().clear();
        tmNeuron.getGeoAnnotationMap().clear();
        tmNeuron.clearRootAnnotations();
        tmNeuron.getStructuredTextAnnotationMap().clear();
    }
    
}
