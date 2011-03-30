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

#
# database to be loaded
db_name=$2
#
# file to be loaded
fasta_file=$1
#load fasta
psql -d $db_name -U postgres -c "truncate table stage_fasta_pep"
#cat $fasta_file | sed 's/\\\([0-9][0-9][0-9]\)\\/\\ \1 \\/g' | sed 's/\\./\\ ./g' | /db/pgsql/scripts/utilities/FASTA_to_Columns.pl -d | psql -d $db_name -U postgres -c "COPY stage_fasta_pep(camera_acc,defline,sequence) FROM STDIN USING DELIMITERS '\t'"
cat $fasta_file | /db/pgsql/scripts/utilities/FASTA_to_Columns.pl -d | sed 's/\\././g' | psql -d $db_name -U postgres -c "COPY stage_fasta_pep(camera_acc,defline,sequence) FROM STDIN USING DELIMITERS '\t'"
