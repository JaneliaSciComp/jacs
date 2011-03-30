#
# Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.
# 
# This file is part of JCVI VICS.
# 
# JCVI VICS is free software; you can redistribute it and/or modify it 
# under the terms and conditions of the Artistic License 2.0.  For 
# details, see the full text of the license in the file LICENSE.txt.  
# No other rights are granted.  Any and all third party software rights 
# to remain with the original developer.
# 
# JCVI VICS is distributed in the hope that it will be useful in 
# bioinformatics applications, but it is provided "AS IS" and WITHOUT 
# ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
# warranties of merchantability or fitness for any particular purpose.  
# For details, see the full text of the license in the file LICENSE.txt.
# 
# You should have received a copy of the Artistic License 2.0 along with 
# JCVI VICS.  If not, the license can be obtained from 
# "http://www.perlfoundation.org/artistic_license_2_0."
# 

../createConfigDB.pl -C eapDevelConfig.db
../configJobs.pl -C eapDevelConfig.db -J eapPrecomputes.cfg
../importHmmDB.pl -D eapDevelConfig.db -I 1346555862474295989 -N "PFAM/TIGRFAM_HMM" -d "PFAM/TIGRFAM_HMM db"
../importHmmDB.pl -D eapDevelConfig.db -I 1281708109705773437 -N "ACLAME_HMM" -d "ACLAME HMM db"
../importHmmDB.pl -D eapDevelConfig.db -I 1281708109705773436 -N "FRAG_HMM" -d "FRAG HMM database"
../importBlastDB.pl -D eapDevelConfig.db -I 1350168649100427643 -N "ALLGROUP_PEP" -d "ALLGROUP peptide"
../importBlastDB.pl -D eapDevelConfig.db -I 1359311017724609144 -N "ENV_NR" -d "ENV NR peptides"
../importBlastDB.pl -D eapDevelConfig.db -I 1359252827150484088 -N "SANGER_PEP" -d "SANGER Metagenomics peptides"
../importBlastDB.pl -D eapDevelConfig.db -I 1359252225527906936 -N "ACLAME_PEP" -d "ACLAME peptides"
../importRpsDB.pl -D eapDevelConfig.db -I 1281719681970864507 -N "CDD_RPS" -d "cdd rps node"
../importRpsDB.pl -D eapDevelConfig.db -I 1365094104098144893 -N "PRIAM_GENE_RPS" -d "PRIAM gene-oriented dataset, November 2008 release"
../importBlastDB.pl -D eapDevelConfig.db -I 1359306933256847992 -N "ENV_NT" -d "ENV NT nucleotide"
