package org.janelia.it.jacs.compute.service.geci;

/**
 * Created by IntelliJ IDEA.
 * User: saffordt
 * Date: 11/1/11
 * Time: 12:11 PM
 */
public class MatlabHelper {
    public static final String MATLAB_GRID_CONFIG = "-l matlab=1";
    public static final String MATLAB_EXPORT = "export matlabroot=/misc/local/matlab-2010bSP1;" +
            "export PATH=\"$PATH:$matlabroot/bin/\";" +
            "export LD_LIBRARY_PATH=\"$LD_LIBRARY_PATH:$matlabroot/bin/glnxa64:$matlabroot/runtime/glnxa64:" +
            "$matlabroot/sys/os/glnxa64:$matlabroot/sys/java/jre/glnxa64/jre/lib/amd64/native_threads:" +
            "$matlabroot/sys/java/jre/glnxa64/jre/lib/amd64/server:$matlabroot/sys/java/jre/glnxa64/jre/lib/amd64\";" +
            "export XAPPLRESDIR=\"$matlabroot/X11/app-defaults\";export MCR_INHIBIT_CTF_LOCK=1;";

/**
 * We have 4 full floating Matlab licenses available on the cluster with the following toolboxes: Curve Fitting, Image, Optimization, Signal, Statistics, Wavelet, Neural.

Whether you run full Matlab in batch mode using qsub or interactively using qlogin, request a license with "-l matlab=1" option to qsub or qlogin.

Here are some short instructions on how to set up your environment, so that you can use matlab on the cluster.

Add this line to your ~/.bashrc: export PATH="$PATH:/usr/local/matlab/bin"
Then run "source ~/.bashrc" or start a new shell.
After you do this running "which matlab" should return: /usr/local/matlab/bin/matlab
Now matlab binary is in your path.

As I mentioned, we have 8 full floating licenses on the cluster that you can use. However, if you intend to run many matlab jobs, you will need to compile your matlab code. We have two matlab compiler licenses on the cluster. When you submit jobs that run compiled binaries, you don't need to request a license with "-l matlab=1".

Here's an example how you can compile and run your matlab code. Put these two lines:

export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:/usr/local/matlab/bin/glnxa64"
export MCR_INHIBIT_CTF_LOCK=1

in your ~/.bashrc. Then source it or start a new shell.

You would compile the binaries like this  (you can use one of the examples from /usr/local/matlab/extern/examples/compiler/ for testing):

cp /usr/local/matlab/extern/examples/compiler/magicsquare.m . (I'm copying the example to my home directory)
mcc -C -m -R -nojvm -v magicsquare.m

This will generate magicsquare binary that I can now run:

./magicsquare 4
Warning: No display specified.  You will not be able to display graphics on the screen.
m =
    16     2     3    13
     5    11    10     8
     9     7     6    12
     4    14    15     1

If you will be running many compiled MatLab jobs at once you will likely want to add export MCR_CACHE_ROOT=/scratch/<Your_UserName> to your .bashrc and also request the entire node using the -l excl=true flag in your qsub command.  If you intend to do this please submit a ticket to the help desk to have a scratch directory created for you on the cluster.  This will only need to be done once.
 */
}
