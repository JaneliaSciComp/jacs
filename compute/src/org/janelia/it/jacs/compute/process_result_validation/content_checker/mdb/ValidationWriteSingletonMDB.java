package org.janelia.it.jacs.compute.process_result_validation.content_checker.mdb;

import static org.janelia.it.jacs.compute.process_result_validation.ValidationLogger.*;
import org.apache.log4j.Logger;



import javax.ejb.MessageDriven;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJBException;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Enforces a throttling-to-one-thread writeback to avoid shared file problems. This is a somewhat heavy solution
 * for this problem (queue+MDB), so I offer this rationale.
 * Log files written by this MDB are at the Data Set level. Data Sets contain often many samples.  Each sample
 * needs to write to the log of its dataset.
 * There is another MDB/queue for sample validation, so multiple samples may wish to write the same log file.
 * Earlier, I had tried using file locks, but those are applicaible only in multiple-JVM circumstances, meaning that
 * the entire JVM is the file lock holder.  When asking for a log that some other thread in the same JVM already holds,
 * an exception is thrown.  Rather than eat the exception, wait, and try again, I instead opted for this solution which
 * is slightly more JEE kosher.  At bottom, JEE forbids writing to the filesystem at all, but I did not wish to write
 * my own JCA adapter for that.  And finaly, were we using Java EE 1.6 (EJB 3.1) or newer, we could use a singleton
 * EJB for this single-thread-throttling instead of a Queue/MDB.  Hence, am using another MDB with maxSession==1.
 *
 * Created by fosterl on 9/4/14.
 */
@SuppressWarnings("unused")
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destination", propertyValue = NON_CONCURRENT_WRITE_QUEUE ),
        //@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge "),
//        @ActivationConfigProperty(propertyName = "messagingType", propertyValue = "javax.jms.MessageListener"),
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        // This value must remain at 1.  Single use/no concurrency.
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")//,
        //@ActivationConfigProperty(propertyName = "transactionTimeout", propertyValue = "432000")//,
//        @ActivationConfigProperty(propertyName = "DLQMaxResent", propertyValue = "0")
})
//@PoolClass(value  StrictMaxPool.class, maxSize = 1, timeout = 10000)
public class ValidationWriteSingletonMDB implements MessageListener {

    private Logger logger = Logger.getLogger( ValidationWriteSingletonMDB.class );

    @Override
    public void onMessage(Message message) {
        if ( message instanceof  MapMessage ) {
            MapMessage mapMessage = (MapMessage)message;
            try {
                String reportFilePath = mapMessage.getString( REPORT_FILE_PATH_PARAM );
                String fileSectionName = mapMessage.getString( FILE_SECTION_NAME_PARAM );
                String fileContent = mapMessage.getString( FILE_CONTENT_PARAM );

                writeback( reportFilePath, fileSectionName, fileContent );
            } catch ( JMSException jmse ) {
                logger.error("Failed to obtain parameters for validation writeback.");
                jmse.printStackTrace();
                throw new EJBException(jmse);
            }
        }
        else {
            logger.error("Invalid message type delivered.  Expected Map Message, received " + message.getClass().getName());
        }
    }

    /**
     * Appends the content to the input file.
     *
     * @param reportFilePath where the file lie.
     * @param fileSectionName name of this section.
     * @param fileContent this section's content.
     */
    private void writeback( String reportFilePath, String fileSectionName, String fileContent ) {
        try ( PrintWriter fpw = new PrintWriter( new FileOutputStream( new File( reportFilePath ) , true ) ) ) {
            fpw.print( SAMPLE_BREAK_DELIM );
            fpw.println( fileSectionName );
            fpw.print( fileContent );
            fpw.flush();
        } catch ( FileNotFoundException fne ) {
            logger.error("Failed to open file " + reportFilePath + " to post content/" + fileContent + "/");
            fne.printStackTrace();
            throw new EJBException(fne);
        }

    }
}
