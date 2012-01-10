
package org.janelia.it.jacs.compute.drmaa;

import org.apache.log4j.Logger;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.Version;
import org.janelia.it.jacs.shared.utils.IOUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA.
 * User: lkagan
 * Date: Apr 2, 2009
 * Time: 11:45:09 AM
 *
 */
public class DrmaaTest
{
    private static int delay; // seconds to sleep on grid job
    private static String pid = getPid();
    private static String scriptFileNameBase="drmaaTestScript." + pid;
    private static String jobTemplateFileName="SerializableJobTemplate." + pid + ".oos";
    private DrmaaHelper drmaa;
    private File currDir;

    public DrmaaTest() throws DrmaaException
    {
        drmaa = new DrmaaHelper(Logger.getLogger(this.getClass()));
        currDir = new File(System.getProperty("user.dir"));
    }

    public void printVersion() throws DrmaaException
    {
        Version ver = drmaa.getVersion();
        System.out.println("DRMAA verion: " + ver.toString());
    }

    public  SerializableJobTemplate createTemplate() throws DrmaaException, IOException
    {
        SerializableJobTemplate jt = drmaa.createJobTemplate();

        File jobScript = createJobScriptFile();
        jt.setRemoteCommand("bash");
        jt.setArgs(Arrays.asList(jobScript.getAbsolutePath()));
        jt.setWorkingDirectory(currDir.getAbsolutePath());
        jt.setErrorPath(":" + currDir.getAbsolutePath() + File.separator + scriptFileNameBase + ".err");
        jt.setOutputPath(":" + currDir.getAbsolutePath() + File.separator + scriptFileNameBase + ".out");
        // Apply a RegEx to replace any non-alphanumeric character with "_".  SGE is finicky that way.
        jt.setJobName("DrmaaTest_" + pid);
        jt.setNativeSpecification("-P 08020" );
        jt.setNativeSpecification("-l fast" );

        return jt;
    }

    private File createJobScriptFile() throws IOException
    {
        File jobScript = new File(currDir, scriptFileNameBase + ".sh");
        jobScript.setExecutable(true, false);
        FileWriter writer = new FileWriter(jobScript);
        writer.write("sleep " + delay + "s\n");
        writer.close();
        return jobScript;
    }

    public File saveTemplate(SerializableJobTemplate jt) throws Exception
    {
        File jtFile = new File(currDir, jobTemplateFileName);

        ObjectOutputStream objStream = new ObjectOutputStream (new FileOutputStream(jtFile));

        // Write object out to disk
        objStream.writeObject (jt);
        System.out.println(jt.toString());
        System.out.println("----------------------------");
        objStream.close();
        drmaa.deleteJobTemplate(jt);
        return jtFile;
    }

    public SerializableJobTemplate loadTemplate(File templateFile) throws Exception
    {
        // Read object using ObjectInputStream
        ObjectInputStream objStream = new ObjectInputStream (new FileInputStream(templateFile));

        // Read an object
        Object obj = objStream.readObject();
        SerializableJobTemplate sjt = (SerializableJobTemplate)obj;
        objStream.close();
        System.out.println("Loading template:");
        System.out.println(sjt.toString());
        System.out.println("----------------------------");
        return drmaa.createJobTemplate(sjt);
    }

    public void sumbitJob(SerializableJobTemplate jt) throws Exception
    {
        System.out.println("Submitting job... waiting...");

        drmaa.setShellReturnMethod(DrmaaSubmitter.OPT_RETURN_VIA_SYSTEM_VAL);
        Process shell = drmaa.runJobThroughShell(-1L, "lkagan", currDir.getAbsolutePath(), jt);
        InputStream shellOut = shell.getInputStream(); // this is actual process output!
        InputStream shellErr = shell.getErrorStream(); // this is actual process output!
        int shellExitStatus = shell.waitFor();

        if (shellExitStatus == 0)
        {
            // output contains grid job ID
            String jobID = IOUtils.readInputStream(shellOut);
            System.out.println("Job " + jobID + " has completed" );
        }
        else
        {
            System.err.println("Shell Drmaa Submitter returned ERROR: ");
            String errorText = IOUtils.readInputStream(shellErr);
            System.err.println(shellErr);
        }

        drmaa.deleteJobTemplate(jt);
    }


    public static void main(String[] args) throws Exception
    {
        if (pid == null)
            System.exit(2);

        delay = Integer.parseInt(args[0]);

        DrmaaTest dt = new DrmaaTest();
        dt.printVersion();

        // create and save template
        SerializableJobTemplate jt = dt.createTemplate();
//        File jtFile = dt.saveTemplate(jt);
//
//        // read out template
//        SerializableJobTemplate jt2 = dt.loadTemplate(jtFile);

        // submit a job
        dt.sumbitJob(jt);
        // drop into a shell and execute drmaa




    }
    /**
    * Gets a string representing the pid of this program - Java VM
     * @return returns the process id string
     */
    public static String getPid()
    {
        try
        {
            Vector<String> commands=new Vector<String>();
            commands.add("/bin/bash");
            commands.add("-c");
            commands.add("echo $PPID");
            ProcessBuilder pb=new ProcessBuilder(commands);

            Process pr=pb.start();
            pr.waitFor();
            if (pr.exitValue()==0)
            {
                BufferedReader outReader=new BufferedReader(new InputStreamReader(pr.getInputStream()));
                return outReader.readLine().trim();
            }
            else
            {
                System.out.println("Error while getting PID");
                return null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}

