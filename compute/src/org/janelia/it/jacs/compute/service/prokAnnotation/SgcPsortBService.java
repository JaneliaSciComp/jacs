
package org.janelia.it.jacs.compute.service.prokAnnotation;

/**
 * sgc_psortB.dbi -D <db> -u <user> -p <password> [-a <asmbl_id> -I -L <listfile> -d -h]
 * <p/>
 * =head1 OPTIONS
 * <p/>
 * REQUIRED
 * -D database
 * -u user name
 * -p database password
 * <p/>
 * OPTIONAL
 * -I initialization mode - all ORFs on all molecules are (re)written
 * -a <asmbl_id> runs on all ORFs on supplied asmbl_id
 * -L <list_file> runs on ORFs listed in file (format: <feat_name><tab><asmbl_id>)
 * <p/>
 * -v verbose mode
 * -d debug mode - no changes are written to database
 * -h prints this message
 * <p/>
 * Program will exit with error if you try to combine:
 * -I  and -L or
 * -a and -L
 * User: tsafford
 * Date: Mar 5, 2010
 * Time: 1:46:11 PM
 */
public class SgcPsortBService extends ProkAnnotationBaseService {

    public String getCommandLine() {
        return "sgc_psortB.dbi -D " + _targetDatabase + " -u " + _databaseUser + " -p " + _databasePassword + " -I -v";
    }

}
