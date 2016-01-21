NOTE:
This directory will hold workbooks created using Tableau.  In order for Tableau to function, it must have its full workspace established.  Said workspace will
include not only the Workbooks, but also ./Shapes, ./Logs, and ./Datasources.  These latter three have been kept out of the source code 
repository, because they add clutter, and because they should actually remain the same across all runs of Tableau.  That is, nothing unique.

Therefore, in order to use any workbooks found here, you will need to let Tableau create its workspace in this "./doc" directory, 'around'
the Workbooks directory, and somehow preserve the workbooks found.  You can always re-checkout that from git.

Caution: to avoid re-cluttering, always inform git to ignore ./Shapes, ./Logs and ./Datasources.

Les Foster
fosterl@janelia.hhmi.org
x4680
