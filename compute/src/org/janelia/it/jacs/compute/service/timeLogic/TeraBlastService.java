
package org.janelia.it.jacs.compute.service.timeLogic;

import org.apache.log4j.Logger;
import org.janelia.it.jacs.compute.access.TextFileIO;
import org.janelia.it.jacs.compute.api.EJBFactory;
import org.janelia.it.jacs.compute.engine.data.IProcessData;
import org.janelia.it.jacs.compute.engine.service.IService;
import org.janelia.it.jacs.compute.engine.service.ServiceException;
import org.janelia.it.jacs.compute.service.common.ProcessDataHelper;
import org.janelia.it.jacs.model.tasks.Event;
import org.janelia.it.jacs.model.tasks.Task;
import org.janelia.it.jacs.model.tasks.blast.BlastTask;
import org.janelia.it.jacs.model.tasks.blast.IBlastOutputFormatTask;
import org.janelia.it.jacs.model.tasks.blast.TeraBlastTask;
import org.janelia.it.jacs.model.user_data.FastaFileNode;
import org.janelia.it.jacs.model.user_data.Node;
import org.janelia.it.jacs.model.user_data.blast.BlastDatabaseFileNode;
import org.janelia.it.jacs.model.user_data.blast.BlastResultFileNode;
import org.janelia.it.jacs.shared.blast.*;
import org.janelia.it.jacs.shared.utils.FileUtil;
import org.janelia.it.jacs.shared.utils.SystemCall;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: jhoover
 * Date: Aug 5, 2010
 * Time: 10:24:08 AM
 */
public class TeraBlastService implements IService {

    private Logger logger;
    private Task task;
    private String sessionName;
    private String decypherJobId;
    private long numHits = 0;
    private BlastResultFileNode resultNode;
    private File resultDirectory;
    private File configurationDirectory;
    private File scratchDirectory;

    public static String dcRunShellScriptName ="dc_run.sh";
    public static String dcRunLogFileName = "dc_run.log";
    public static String dcShowLogFileName = "dc_show.log";
    public static String decypherResultFileName = "decypher.tmp";

    public void execute(IProcessData processData) throws ServiceException {
        try {
/*
 * initalization
 */
            initializeJob(processData);
/*
 * submit the blast request to Decypher
 */
            submitToDecypher();
/*
 * poll Decypher for job status until job completed
 */
            waitForDecypher();
/*
 * convert decyhpher results into VICS internal format and save to disk
 */
            formatDecypherResults();
/*
 *  clean up and exit
 */
            finalizeJob();
/*
 * handle exceptions
 */
        } catch (Exception e) {
            logError(e);
            throw new ServiceException(e.getMessage() + " (teraBlastService)");
        }
    }

/****************************************************************************************************
 *  initialized task
 */
    private void initializeJob(IProcessData processData) throws ServiceException {
        try {
            logger= ProcessDataHelper.getLoggerForTask(processData, this.getClass());
            task = ProcessDataHelper.getTask(processData);
            sessionName = ProcessDataHelper.getSessionRelativePath(processData);
            createBlastResultFileNode();
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"initialized");
       } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (init)");
       }
    }

/****************************************************************************************************
 * initialize a result node for the task
 */
    private void createBlastResultFileNode() throws Exception {

        try {
            logger.info("task owner="+task.getOwner());
            if (task.getOwner()==null) {
                logger.info("user is null");
            } else {
                logger.info("user is not null");
            }

            resultNode=new BlastResultFileNode(
                task.getOwner(),
                task,
                "BlastResultFileNode for job="+task.getJobName(),
                "BlastResultFileNode for task="+task.getObjectId(),
                Node.VISIBILITY_PUBLIC,
                sessionName);
            resultNode = (BlastResultFileNode)  EJBFactory.getLocalComputeBean().createNode(resultNode);
//            task.addOutputNode(resultNode);
//            EJBFactory.getLocalComputeBean().saveOrUpdateTask(task);

            FileUtil.ensureDirExists(resultNode.getDirectoryPath());
            resultDirectory =  new File(resultNode.getDirectoryPath());
            configurationDirectory = new File( resultDirectory, "/config" );
            configurationDirectory.mkdir();
            scratchDirectory = new File( resultDirectory, "/scratch" );
            scratchDirectory.mkdir();

        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (createBlastResultFileNode)");
        }
    }

/****************************************************************************************************
 submit blast request to Decypher by building and executing a dc_run command
 */
    private void submitToDecypher() throws ServiceException {

    File jobScript;
/*
 * build the dc_run command
 */
        try {
            String command = "dc_run -detach -description \"" + task.getOwner() + " " + task.getJobName() + "\"";

//            command.concat(" -parameters " + ((TeraBlastTask) task).getParameterFile());
            command = command.concat(" -parameters " + ((TeraBlastTask) task).getParameterFile());

            Long nodeId = new Long( task.getParameter(BlastTask.PARAM_query) );
            Node node = EJBFactory.getLocalComputeBean().getNodeById(nodeId);
            command = command.concat(" -query_set " + ((FastaFileNode) node).getFastaFilePath());

            nodeId = new Long( task.getParameter(BlastTask.PARAM_subjectDatabases));
            node = EJBFactory.getLocalComputeBean().getNodeById(nodeId);
            String decypherDbId = ((BlastDatabaseFileNode) node).getDecypherDbId();
            if ( decypherDbId == null ) {
                throw new Exception("blast database " + nodeId + " \"" + node.getName() + "\" is not available to decypher." );
            } else {
                command = command.concat(" -database " + decypherDbId);
            }

            if ( task.parameterDefined(BlastTask.PARAM_matrix) ) {
                if ( ! ((TeraBlastTask) task).getParameterFile().equals("tera-blastn") ) {
                    command = command.concat(" -matrix " + task.getParameter(BlastTask.PARAM_matrix).toLowerCase());
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_wordsize) ) {
                if ( ! task.getParameter(BlastTask.PARAM_wordsize).equals("0") ) {
                    command = command.concat(" -word_size " + task.getParameter(BlastTask.PARAM_wordsize));
                } else if ( ( (TeraBlastTask) task).getParameterFile().equals("tera-blastn") ) {
                    command = command.concat(" -word_size 11");
                } else {
                    command = command.concat(" -word_size 3");
                }
            }

            command = command.concat(" -max_alignments " + task.getParameter(BlastTask.PARAM_databaseAlignments));

            command = command.concat(" -max_scores " + task.getParameter(BlastTask.PARAM_databaseAlignments));

            command = command.concat(" -significance evalue gapped");
            command = command.concat(" -threshold significance=1E" + task.getParameter(BlastTask.PARAM_evalue));
//            command = command.concat(" -alignment_threshold significance=1E" + task.getParameter(BlastTask.PARAM_evalue));

            if ( task.parameterDefined(BlastTask.PARAM_databaseSize) ) {
                if ( ! task.getParameter(BlastTask.PARAM_databaseSize).equals("0") ) {
                    command = command.concat(" -database_size " + task.getParameter(BlastTask.PARAM_databaseSize));
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_filter) ) {
                if ( task.getParameter(BlastTask.PARAM_filter).equals("T") ) {
                    command = command.concat(" -filter_query on");
                } else {
                    command = command.concat(" -filter_query off");
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_lowerCaseFiltering) ) {
                if ( task.getParameter(BlastTask.PARAM_lowerCaseFiltering).equals("true") ) {
                    command = command.concat(" -softmask on");
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_gappedAlignment) ) {
                if ( task.getParameter(BlastTask.PARAM_gappedAlignment).equals("true") ) {
//                    command = command.concat(" -gapped_alignment sw banded optimal");
                    command = command.concat(" -gapped_alignment sw banded");
                } else {
                    command = command.concat(" -gapped_alignment off");
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_gapOpenCost) ) {
                if ( Integer.valueOf(task.getParameter(BlastTask.PARAM_gapOpenCost)) > 0 ) {
                    command = command.concat(" -open_penalty -" + task.getParameter(BlastTask.PARAM_gapOpenCost));
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_gapExtendCost) ) {
                if ( Integer.valueOf(task.getParameter(BlastTask.PARAM_gapExtendCost)) > 0 ) {
                    command = command.concat(" -extend_penalty -" + task.getParameter(BlastTask.PARAM_gapExtendCost));
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_gappedAlignmentDropoff) ) {
                if ( ! task.getParameter(BlastTask.PARAM_gappedAlignmentDropoff).equals("0") ) {
                    command = command.concat(" -x_dropoff " + task.getParameter(BlastTask.PARAM_gappedAlignmentDropoff));
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_hitExtensionThreshold) ) {
                if ( ! task.getParameter(BlastTask.PARAM_hitExtensionThreshold).equals("0") ) {
                    command = command.concat(" -threshold " + task.getParameter(BlastTask.PARAM_hitExtensionThreshold));
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_matchReward) ) {
                if ( ! task.getParameter(BlastTask.PARAM_matchReward).equals("0") ) {
                    command = command.concat(" -match " + task.getParameter(BlastTask.PARAM_matchReward));
                }
            }

            if ( task.parameterDefined(BlastTask.PARAM_mismatchPenalty) ) {
                if ( ! task.getParameter(BlastTask.PARAM_mismatchPenalty).equals("0") ) {
                    command = command.concat(" -mismatch " + task.getParameter(BlastTask.PARAM_mismatchPenalty));
                }
            }

            command = command.concat(" -output_format tab concise fieldrecord groupbylocus -field queryaccession querylocus querytext querylength targetaccession targetlocus targetdescription targetlength significance score queryframe querystart queryend targetframe targetstart targetend algorithm alignmentlength gaps matches similarity alignment");

            jobScript = new File( configurationDirectory.getAbsolutePath(), dcRunShellScriptName );
            jobScript.createNewFile();
            jobScript.setExecutable( true );
            TextFileIO.writeTextFile( jobScript, command );
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"decypher command: " + command);

        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (submitTaskToTeraBlast: build)");
        }
/*
 * execute job script and parse the resulting decypher job id from the log file and add it to the task
 */
        try {
            File jobLog = new File ( configurationDirectory.getAbsolutePath(), dcRunLogFileName);
            jobScript.createNewFile();
            jobScript.setWritable( true );

            SystemCall system = new SystemCall( logger );
            system.emulateCommandLine( jobScript.getAbsolutePath() + " > " + jobLog.getAbsolutePath(), true );

            String logContents = removeTrailingCrLf( TextFileIO.readTextFile(jobLog.getAbsolutePath()));
            if ( ! logContents.startsWith("OK") ) {
                throw new Exception("unexpected return from dc_run: \"" + logContents + "\"");
            }

            decypherJobId = logContents.substring(logContents.indexOf(" ")+1);
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"submitted to decypher as job " + decypherJobId);
            jobLog.delete();
            jobScript.delete();

        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (submitTaskToTeraBlast: execute)");
        }
    }

/****************************************************************************************************
 * remove end of line characters from string
 */
    private String removeTrailingCrLf( String text ) {
        String newtext = text;
        while ( null != newtext
                && newtext.length()>0
                && ( newtext.substring(newtext.length()-1).equals("\n") ||
                     newtext.substring(newtext.length()-1).equals("\r") ) ) {
            newtext = newtext.substring( 0, newtext.length()-1 );
        }
        return newtext;
    }

/****************************************************************************************************
 * poll decypher for the status of a job, until job completes or fails
 * update the task event to reflect changes in job status and return final status to caller
 */
    private void waitForDecypher() throws ServiceException {
        int waitTotal = 0;
        int waitMarker = 0;

        int[] waitMilestone = new int[4];
        waitMilestone[0] = 900000;         // 15 minutes
        waitMilestone[1] = 3600000;        // 1 hour
        waitMilestone[2] = 4 * 3600000;    // 4 hours
        waitMilestone[3] = 24 * 3600000;   // 24 hours

        int[] waitTime = new int[4];
        waitTime[0] = 60000;         // 60 seconds
        waitTime[1] = 120000;         // 2 minutes
        waitTime[2] = 10 * 60000;     // 10 minutes
        waitTime[3] = 30 * 60000;    // 30 minutes

        int maxUnknownResponses = 5;
        int numUnknownResponses = 0;

        try {
            String status = "pending";
            String message = "";

            File statusLog = new File ( configurationDirectory.getAbsolutePath(), dcShowLogFileName );
            statusLog.createNewFile();
            statusLog.setWritable( true );
            while (!Task.isDone(status)) {
                Thread.sleep(waitTime[waitMarker]);
                waitTotal += waitTime[waitMarker];
                if ( waitMarker < 4 && waitTotal > waitMilestone[waitMarker] ) {
                    waitMarker++;
                }
                SystemCall system = new SystemCall( logger );
                system.emulateCommandLine( "dc_show -job " + decypherJobId + " > " + statusLog.getAbsolutePath(), true );

                String logContents = removeTrailingCrLf(TextFileIO.readTextFile(statusLog.getAbsolutePath()));
                String[] tmpstatus = parseDecypherStatus( logContents, statusLog ).split("\t");
                String newStatus = tmpstatus[0];
                String newMessage = tmpstatus[1];

                if ( newStatus.equals("unknown") ) {
                    numUnknownResponses++;
                    if ( numUnknownResponses > maxUnknownResponses ) {
                        throw new Exception("unrecognized response from decypher: " + newMessage );
                    }
                    if ( ! message.equals(newMessage) ) {
                        message = newMessage;
                        EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(), message);
                    }
                    Thread.sleep( 5 * 60000 );
                } else if ( ! status.equals(newStatus) ) {
                    if ( ! newStatus.equals(Event.COMPLETED_EVENT)) {
                        EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(),newStatus,newMessage, nowTimeStamp());
                    }
                    status = newStatus;
                    message = newMessage;
                    EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(), message);
                    waitMarker = 0;
                    waitTotal = 0;
                } else if ( ! message.equals(newMessage) ) {
                    message = newMessage;
                    EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(), message);
                    waitMarker = 0;
                    waitTotal = 0;
                }
            }

            if ( status.equals(Event.ERROR_EVENT) ) {
                throw new Exception("decypher error: ".concat(message));
            }
            statusLog.delete();
        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (waitForJob)");
        }
    }

/****************************************************************************************************
 * return current date/time as timestamp
 */
    private Timestamp nowTimeStamp() {
        Date today = new Date();
        return new Timestamp( today.getTime() );
    }

/****************************************************************************************************
 * parse VICs status from decypher status string
 */
    private String parseDecypherStatus( String message, File statusLog ) throws ServiceException {
        try {
            if ( message.contains("processing") || message.contains("Alignment") ) {
                String newMessage = message.replaceFirst("The job","Decypher job "+decypherJobId).replaceAll("\n *"," ");
                return "running\t" + newMessage;
            } else if ( message.contains("waiting") ) {
                String newMessage = message.replaceFirst("The job","Decypher job "+decypherJobId);
                return "pending\t" + newMessage;
            } else if ( message.startsWith("Completed:") ) {
                SystemCall system = new SystemCall( logger );
                system.emulateCommandLine( "dc_get -keep -err -job " + decypherJobId + " > " + statusLog.getAbsolutePath(), true );
                String newMessage = removeTrailingCrLf(TextFileIO.readTextFile(statusLog.getAbsolutePath())).replaceAll("\n *"," ");
                if ( ! newMessage.contains("no errors") ) {
                    return "error\t" + "Decypher job " + decypherJobId + " failed: " + newMessage;
                }
                return "completed\t" + "Decypher job " + decypherJobId + " completed: " + newMessage;
            } else if ( message.contains("Unknown job name:") ) {
                return "error\t" + "Decypher job " + decypherJobId + " has been deleted.";
            } else {
                String newMessage = removeTrailingCrLf(TextFileIO.readTextFile(statusLog.getAbsolutePath())).replaceAll("\n *"," ");
                return "unknown\t" + "Decypher job " + decypherJobId + " completed: " + newMessage;
            }
        } catch(Exception e) {
            throw new ServiceException(e);
        }
    }

/****************************************************************************************************
 * read decypger's tab-delimited results file and transform to format(s) requested by user
 */
    private void formatDecypherResults() throws ServiceException {
    long rownum = 0;
    String step = "init";
    try {
            EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(),"format","Formatting blast results", nowTimeStamp());
            boolean writeTop = true;
            long queryIteration = 0;
            long numHSPs = 0;
            ParsedBlastResultCollection blastResults = new ParsedBlastResultCollection();
/*
 *  fetch the decypher results to disk
 */
            step = "fetch";
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"Fetching results from Decypher.");
            File resultTmp = new File ( scratchDirectory.getAbsolutePath(), decypherResultFileName);
            resultTmp.createNewFile();
            resultTmp.setWritable( true );

            SystemCall system = new SystemCall( logger );
            system.emulateCommandLine( "dc_get -keep -job " + decypherJobId + " > " + resultTmp.getAbsolutePath(), true );

/*
 *  initialize blast format writers
 */
            step = "getwriters";
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"Formatting results.");
            HashMap<String,BlastWriter> writerHash = new HashMap();
            String formatString = task.getParameter(BlastTask.PARAM_formatTypesCsv);
            if ( formatString == null || formatString.length() == 0 ) {
                formatString = "xml,btab";
            } else {
                formatString = formatString.concat(",xml,btab");
            }
            for ( String format: formatString.split(",") ) {
                format = format.trim().toLowerCase();
                if ( ! writerHash.containsKey(format) ) {
                    BlastWriter writer = BlastWriterFactory.getWriterForFormat(resultDirectory, format);
                    File resultFile = new File(resultDirectory, "blastResults."+format);
                    writer.init(resultFile);
                    resultFile.setReadable(true);
                    resultFile.setWritable(true);
                    writerHash.put(format,writer);

// delete mysterious extra file created by getWriter
                    File extraFile =new File( resultDirectory, "parsedBlastResultsCollection."+format);
                    if ( extraFile.exists() ) {
                        extraFile.delete();
                    }
                }
            }
            Collection<BlastWriter> writerList = writerHash.values();
/*
 *  read the decypher results and construct VICS blast result objects
 */
            step = "parse header";
            BufferedReader input =  new BufferedReader(new FileReader(resultTmp.getAbsolutePath()));
            String[] header = nextRow(input);
            rownum++;

            ParsedBlastResult blastHit = null;
            String[] row;
            while (( row = nextRow(input) ) != null) {

                rownum++;
                step = "make rawhsp";
                HashMap<String,String> rawhsp = makeRawHsp(header,row);
                if ( rawhsp.get("ALIGNMENT").length() > 0 ) {   // patch to exclude rows missing alignment data

/****
 *  initialize first hit
 */
                step = "handle rawhsp";
                if ( blastHit == null ) {
                    step = "init hit";
                    blastHit = initBlastHit( rawhsp );
                    numHSPs = 1;
/****
 *  starting a new query sequence
 *  add last hit for current query sequence
 */
                } else if ( ! blastHit.getQueryId().equals(rawhsp.get("QUERYACCESSION")) ) {
                    step = "add hit Q";
                    blastResults.addParsedBlastResult(blastHit);
                    numHits++;
                    if ( ( numHits < 10 ) || ( numHits < 100 && numHits% 10 == 0 ) || ( numHits < 1000 && numHits% 100 == 0 ) || ( numHits % 1000 == 0 ) ) {
                        EJBFactory.getLocalComputeBean().setTaskNote(task.getObjectId(), "Formatting results: " + Long.toString(numHits) + " hits");
                    }
                    step = "addDeflines Q";
                    blastResults.addDefline(blastHit.getQueryId(),blastHit.getQueryDefline());
                    blastResults.addDefline(blastHit.getSubjectId(),blastHit.getSubjectDefline());
/*
 * write current query sequence's hits
 */
                    step = "sort hits";
                    blastResults.sort();

                    step = "format hits";
                    Iterator writerIter = writerList.iterator();
                    while ( writerIter.hasNext() ) {
                        BlastWriter writer = (BlastWriter) writerIter.next();
                        writer.setBlastDataSources(blastResults, (IBlastOutputFormatTask) task);
                        if ( writeTop ) {
                            writer.writeTopPortion();
                        }
                        writer.startQueryPortion();
                        queryIteration++;
                        writer.writeSingleQueryPortion(blastHit.getQueryId(),blastResults.getParsedBlastResults(),queryIteration);
                        writer.endQueryPortion();
                    }
                    writeTop = false;
/*
 * initialize first hit for new query sequence
 */
                    step = "init hit Q";
                    blastResults = new ParsedBlastResultCollection();
                    blastHit = initBlastHit( rawhsp );
                    numHSPs = 1;
                    blastResults.addDefline(blastHit.getQueryId(),blastHit.getQueryDefline());
/****
 *  starting a hit for new subject sequence against current query sequence
 *  add current hit to buffer
 */
                } else if ( ! blastHit.getSubjectId().equals(rawhsp.get("TARGETACCESSION")) ) {
                    step = "add hit S";
                    blastResults.addParsedBlastResult(blastHit);
                    numHits++;
                    if ( ( numHits < 10 ) || ( numHits < 100 && numHits% 10 == 0 ) || ( numHits < 1000 && numHits% 100 == 0 ) || ( numHits % 1000 == 0 ) ) {
                        EJBFactory.getLocalComputeBean().setTaskNote(task.getObjectId(), "Formatting results: " + Long.toString(numHits) + " hits");
                    }
                    step = "addDefline S";
                    blastResults.addDefline(blastHit.getSubjectId(),blastHit.getSubjectDefline());
/*
 *  initialize new hit for current query sequence
 */
                    step = "init hit S";
                    blastHit = initBlastHit( rawhsp );
                    numHSPs = 1;
/****
 *  current query and subject sequence, add HSP to current hit
 */
                } else {
                    step = "add HSP";
                    blastHit.getHspList().add(makeHSP(numHSPs++,rawhsp));
                }
            }
            }   // patch to exclude rows missing alignment data
/*
 * close and delete decypher results file
 */
            rownum = -1;
            step = "close/delete";
            input.close();
            resultTmp.delete();
/*
 *  write last query sequence's hits
 */
            step = "add last hit";
            blastResults.addParsedBlastResult(blastHit);
            numHits++;
            EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(), "Formatting results: " + Long.toString(numHits) + " hits");

            step = "add last deflines";
            blastResults.addDefline(blastHit.getQueryId(),blastHit.getQueryDefline());
            blastResults.addDefline(blastHit.getSubjectId(),blastHit.getSubjectDefline());

            step = "sort last hits";
            blastResults.sort();

            step = "format last hits";
            Iterator writerIter = writerList.iterator();
            while ( writerIter.hasNext() ) {
                BlastWriter writer = (BlastWriter) writerIter.next();
                writer.setBlastDataSources(blastResults, (IBlastOutputFormatTask) task);
                if ( writeTop ) {
                    writer.writeTopPortion();
                }
                writer.startQueryPortion();
                queryIteration++;
                writer.writeSingleQueryPortion(blastHit.getQueryId(),blastResults.getParsedBlastResults(),queryIteration);
                writer.endQueryPortion();
                writer.writeBottomPortion();
                writer.finish();
            }
/*
 *  update blastHitCount in result node
 */
            step = "set hit count";
            EJBFactory.getLocalComputeBean().setBlastHitsForNode(resultNode.getObjectId(),numHits);
/*
 *  handle exceptions
 */
        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (formatDecypherResults on step \"" + step + "\" for row " + Long.toString(rownum) + ")");
        }
   }

   private String[] nextRow(BufferedReader input) throws Exception {
        String line = input.readLine();
        if ( line == null ) {
            return null;
        } else if ( line.length() == 0 ) {
            return null;
        } else {
            return line.split("\t");
       }
   }

/****************************************************************************************************
 *  create a VICs HSP from the raw decypher data
 */
    private ParsedBlastHSP makeHSP(long hspNum, HashMap<String,String> rawhsp) throws ServiceException {
        try {
            ParsedBlastHSP hsp = new ParsedBlastHSP();

            hsp.setHspOrd(hspNum);
            hsp.setBitScore(new Float(rawhsp.get("SCORE")));
            hsp.setExpectScore(new Double(rawhsp.get("SIGNIFICANCE")));
            hsp.setEntropy(new Float(-1.0));
            hsp.setHspScore(new Float(-1.0));

            hsp.setLengthAlignment(new Integer(rawhsp.get("ALIGNMENTLENGTH")));
            hsp.setNumberIdentical(new Integer(rawhsp.get("MATCHES")));
            hsp.setNumberSimilar(new Integer(rawhsp.get("SIMILARITIES")));

            String rawAlignment = rawhsp.get("ALIGNMENT");
            rawAlignment = rawAlignment.replaceAll("\"\",","");
            rawAlignment = rawAlignment.replaceAll("\"","");
            String[] alignment = rawAlignment.split(",");
            hsp.setQueryAlignString(alignment[0]);
            hsp.setMidline(alignment[1]);
            hsp.setSubjectAlignString(alignment[2]);

            Integer[] gaps = analyzeGaps(alignment[0]);
            hsp.setQueryGaps(gaps[0]);
            hsp.setQueryGapRuns(gaps[1]);
            gaps = analyzeGaps(alignment[2]);
            hsp.setSubjectGaps(gaps[0]);
            hsp.setSubjectGapRuns(gaps[1]);

            if ( rawhsp.get("QUERYFRAME").equals("D") ) {
                hsp.setQueryFrame(1);
            } else if ( rawhsp.get("QUERYFRAME").equals("C") ) {
                hsp.setQueryFrame(-1);
            } else {
                hsp.setQueryFrame(new Integer(rawhsp.get("QUERYFRAME")));
            }
            hsp.setQueryBegin(new Integer(rawhsp.get("QUERYSTART")));
            hsp.setQueryEnd(new Integer(rawhsp.get("QUERYEND")));
            hsp.setQueryStops(0);

            if ( rawhsp.get("TARGETFRAME").equals("D") ) {
                hsp.setSubjectFrame(1);
            } else if ( rawhsp.get("TARGETFRAME").equals("C") ) {
                hsp.setSubjectFrame(-1);
            } else {
                hsp.setSubjectFrame(new Integer(rawhsp.get("TARGETFRAME")));
            }
            hsp.setSubjectBegin(new Integer(rawhsp.get("TARGETSTART")));
            hsp.setSubjectEnd(new Integer(rawhsp.get("TARGETEND")));
            hsp.setSubjectStops(0);

            if ( hsp.getQueryFrame().longValue() < 0 ) {
                hsp.setQueryOrientation(-1);
            } else {
                hsp.setQueryOrientation(1);
            }
            if ( hsp.getSubjectFrame().longValue() < 0 ) {
                hsp.setSubjectOrientation(new Integer(-1));
            } else {
                hsp.setSubjectOrientation(new Integer(1));
            }

            int numUnaligned = (new Integer(rawhsp.get("QUERYLENGTH"))).intValue() - hsp.getNumberSimilar().intValue();
            hsp.setQueryNumberUnalignable(numUnaligned);
            numUnaligned = (new Integer(rawhsp.get("TARGETLENGTH"))).intValue() - hsp.getNumberSimilar().intValue();
            hsp.setSubjectNumberUnalignable(numUnaligned);
            return hsp;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (makeHSP)");
        }
    }

/****************************************************************************************************
 * count total number of gaps (-) and number of gap runs in alignment string
 */
    private Integer[] analyzeGaps(String alignment) {
        Integer[] gaps = new Integer[2];

        if ( alignment == null || alignment.length() == 0 ) {
            gaps[0] = 0;
            gaps[1] = 0;
            return gaps;
        }

        gaps[0] = -1;
        gaps[1] = -1;
        String[] chunks = alignment.split("-");
        int numchunks = 0;
        for ( int i = 0; i < chunks.length; i++ ) {
            if ( chunks[i] != null && chunks[i].length() > 0 ) {
                gaps[1]++;
            }
            gaps[0]++;
        }
        return gaps;
    }

/****************************************************************************************************
 *  initialize VICS blast hit object from raw decypher data
 */
    private ParsedBlastResult initBlastHit(HashMap<String,String> rawhsp) throws ServiceException {
        try {
            ParsedBlastResult blastHit = new ParsedBlastResult();

            blastHit.setProgramUsed(rawhsp.get("ALGORITHM"));

            blastHit.setQueryId(rawhsp.get("QUERYACCESSION"));
            if ( rawhsp.get("QUERYLOCUS").equals(rawhsp.get("QUERYTEXT")) ) {
                blastHit.setQueryDefline(rawhsp.get("QUERYLOCUS"));
            } else if ( rawhsp.get("QUERYTEXT").startsWith(rawhsp.get("QUERYLOCUS")+" ") ) {
                    blastHit.setQueryDefline(rawhsp.get("QUERYTEXT"));
            } else {
                blastHit.setQueryDefline(rawhsp.get("QUERYLOCUS")+" "+rawhsp.get("QUERYTEXT"));
            }
            blastHit.setQueryLength(new Integer(rawhsp.get("QUERYLENGTH")));

            blastHit.setSubjectId(rawhsp.get("TARGETACCESSION"));
            if ( rawhsp.get("TARGETLOCUS").equals(rawhsp.get("TARGETDESCRIPTION")) ) {
                blastHit.setSubjectDefline(rawhsp.get("TARGETLOCUS"));
            } else {
                blastHit.setSubjectDefline(rawhsp.get("TARGETLOCUS").concat(" ").concat(rawhsp.get("TARGETDESCRIPTION")));
            }
            blastHit.setSubjectLength(new Integer(rawhsp.get("TARGETLENGTH")));

            blastHit.setBestExpectScore(new Double(rawhsp.get("SIGNIFICANCE")));

            blastHit.getHspList().add(makeHSP(1,rawhsp));

            return blastHit;
        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (initBlastHit)");
        }
    }

/****************************************************************************************************
 * parse the decypher raw data and save as a hash map]
 */
    private HashMap<String,String> makeRawHsp(String[] header, String[] row) throws Exception {
        HashMap<String,String> rawhsp = new HashMap();

/*
        if ( header.length != row.length ) {
            throw new Exception("makeRawHsp: header/row length mismatch" );
        }
 */
        for ( int i = 0; i < header.length; i++ ) {
// patch to handle rows missing alignment data
            if ( i < row.length ) {
                rawhsp.put(header[i], row[i] );
            } else {
                rawhsp.put(header[i], "" );
            }
        }
        if ( rawhsp.get("QUERYACCESSION").length() == 0 ) {
            rawhsp.put("QUERYACCESSION",rawhsp.get("QUERYLOCUS"));
        }
        if ( rawhsp.get("TARGETACCESSION").length() == 0 ) {
            rawhsp.put("TARGETACCESSION",rawhsp.get("TARGETLOCUS"));
        }

        return rawhsp;
    }

/****************************************************************************************************
 * record error event
 */
    private void logError(Exception e) {
        if ( task != null ) {
            try {
                EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(),Event.ERROR_EVENT,e.getMessage(), nowTimeStamp());
                EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"error: " + e.getMessage());
            } catch (Exception ignored) {
            }
        }
    }

    /****************************************************************************************************
 *  purge job from decypher
 *  clean-up config and scratch space
 *  mark VICs job completed
 */
    private void finalizeJob() throws ServiceException {
        try {
            SystemCall system = new SystemCall( logger );
            system.emulateCommandLine( "dc_get -purge -job " + decypherJobId + " > /dev/null", true );

            EJBFactory.getLocalComputeBean().saveEvent(task.getObjectId(), Event.COMPLETED_EVENT,"result location: " + resultDirectory.getAbsolutePath(),nowTimeStamp());
            if ( numHits == 1 ) {
                EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"completed: 1 hit");
            } else {
                EJBFactory.getLocalComputeBean().addTaskNote(task.getObjectId(),"completed: " + Long.toString(numHits) + " hits");
            }

            configurationDirectory.delete();
            scratchDirectory.delete();

        } catch (Exception e) {
            throw new ServiceException(e.getMessage() + " (finalize)");
        }
    }
}