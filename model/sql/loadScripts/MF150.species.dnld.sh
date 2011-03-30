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

export PGPASSWORD=$4
#
# make sure hierarchy node and download directory exists for data files
psql $2 -U $3 -c "insert into hierarchy_node values(7,'Downloads',null)"
psql $2 -U $3 -c "delete from hierarchy_node_data_file_link where hierarchy_node_id=7 and position=9999"
mkdir ${1}/mf
chmod 755 ${1}/mf
cd ${1}/mf
#
# process each sample in list
for sample in A20C1 A5410 ABOONEI ADG881 AND4 AS9601 ATW7 B14911 BAL199 BBAL3 BBFL7 BL107 BOGUAY BSG1 CA2559 CAT7 CDSM653 CMTB2 CSC01 CY0110 DCDF1 DSM11836 DSM3645 DSM6158 EE36 ESD21 FB2170 FBALC1 FBBAL38 FP2506 GP2143 GP2207 GPB2148 HG1285 HPDFL43 ISM JNB KAOT1 KT71 KT99 L8106 LMED105 M23134 MADE MC7420 MDG893 MED121 MED134 MED152 MED193 MED217  MED222 MED297 MED92 MELB17 MFACE2 MGP2080 MITS9220 MPKA3 N9414 NAP1 NAS141 NATL1A NB231 NB311A NOC27 OA2633 OA307 OB2597 OBOE OG2516 OIHEL45 OM2255 OS145 P3TCK P700755 P9202 P9211 P9301 P9303 P9515 PB2503 PBAL39 PCNPT3 PE36 PI23P PJE062 PM8797T PPSIR1 PTD2 PU1002 R2601 RAZWK3B RB2501 RB2654 RED65 RG210 ROS217 RR11 RS9916 RS9917 RSK20926 RTM1035 S7335 SADFL11 SCB49 SI859A1 SIAM614 SKA34 SKA53 SKA58 SM111 SM167 SPV1 SSKA14 TAM4 V12B01 V12G01 VAS14 VDG1235 VFMJ11 VSAK1 VSWAT3 WH5701 WH7805
do
#
# purge old files and working directory
  rm -f -r ${sample}.tar.*
  rm -f -r $sample
#
# make new working directory
  mkdir $sample
  cd $sample
#
# download and unzip annotation file from MF website
  wget -v --no-check-certificate "https://research.venterinstitute.org/moore/FileServer?speciesTag=${sample}&DataType=MOORE_DATA"
  mv *MOORE_DATA ${sample}.zip
  unzip ${sample}.zip
  rm -f ${sample}.zip
#
# download NUCL/PEPT fasta files
  wget -v --no-check-certificate "https://research.venterinstitute.org/moore/FileServer?speciesTag=${sample}&DataType=NUCL_FASTA"
  mv *NUCL_FASTA nucl.fasta
  wget -v --no-check-certificate "https://research.venterinstitute.org/moore/FileServer?speciesTag=${sample}&DataType=PEPT_FASTA"
  mv *PEPT_FASTA pept.fasta
#
# download annotation statistics
  wget -v --no-check-certificate "https://research.venterinstitute.org/moore/FileServer?speciesTag=${sample}&DataType=ANNO_CSV"
  mv *ANNO_CSV anno.csv
#
#   tar contents and save file size
  cd ..
  tar cvf ${sample}.tar $sample
  chmod 755 ${sample}.tar
  fsize=`wc -c ${sample}.tar | awk ' BEGIN { FS=" "; OFS=" " }{ print $1 }'`
#
# zip and gzip tar file
  zip ${sample}.tar.zip ${sample}.tar
  gzip ${sample}.tar
  chmod 755 ${sample}.tar.*
#
# remove working directory
  rm -f -r $sample
#
# update database
  psql $2 -U $3 -c "delete from data_file_sample_link where data_file_id in (select oid from data_file where path = '/mf/${sample}.tar')"
  psql $2 -U $3 -c "delete from hierarchy_node_data_file_link where data_file_id in (select oid from data_file where path = '/mf/${sample}.tar')"
  psql $2 -U $3 -c "delete from data_file where path = '/mf/${sample}.tar'"
  psql $2 -U $3 -c "insert into data_file(oid,path,description,size,multifile_archive) select nextval('uid_generated_sequence'),'/mf/${sample}.tar',organism,${fsize},false from assembly where assembly_acc='MF_ASM_${sample}'"
  psql $2 -U $3 -c "insert into data_file_sample_link(data_file_id,sample_id) select df.oid,s.sample_id from data_file df, bio_sample s where df.path='/mf/${sample}.tar' and s.sample_acc='MF_SMPL_${sample}'"
  psql $2 -U $3 -c "insert into hierarchy_node_data_file_link(hierarchy_node_id,data_file_id,position) select 7,oid,99999 from data_file where path='/mf/${sample}.tar'"
  psql $2 -U $3 -c "update hierarchy_node_data_file_link l set position=10000+(select count(*) from data_file d1, data_file d2 where d1.oid=l.data_file_id and d2.path like '/mf/%.tar' and lower(d2.description||' '||d2.path) < lower(d1.description||' '||d1.path)) where hierarchy_node_id=7"
  psql $2 -U $3 -c "update hierarchy_node_data_file_link l set position=position-10000 where hierarchy_node_id=7"
done
psql $2 -U $3 -c "vacuum full verbose analyze data_file"
psql $2 -U $3 -c "vacuum full verbose analyze hierarchy_node_data_file_link"
