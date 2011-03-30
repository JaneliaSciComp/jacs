/**************************************************************************
  Copyright (c) 2010-2011, J. Craig Venter Institute, Inc.

  This file is part of JCVI VICS.

  JCVI VICS is free software; you can redistribute it and/or modify it 
  under the terms and conditions of the Artistic License 2.0.  For 
  details, see the full text of the license in the file LICENSE.txt.  
  No other rights are granted.  Any and all third party software rights 
  to remain with the original developer.

  JCVI VICS is distributed in the hope that it will be useful in 
  bioinformatics applications, but it is provided "AS IS" and WITHOUT 
  ANY EXPRESS OR IMPLIED WARRANTIES including but not limited to implied 
  warranties of merchantability or fitness for any particular purpose.  
  For details, see the full text of the license in the file LICENSE.txt.

  You should have received a copy of the Artistic License 2.0 along with 
  JCVI VICS.  If not, the license can be obtained from 
  "http://www.perlfoundation.org/artistic_license_2_0."
***************************************************************************/

/*
 * this unix command uploads a PANDA .niaa file for processing
 *
./stage_fasta_pep.sh filename dbname
 *
 * this statement will transform the standard PANDA defline into a near-CAMERA formatted defline:
 *
update stage_fasta_pep
  set defline=split_part(defline,'|',1)||'_PEP_'||split_part(split_part(defline,' ',1),'|',3)||
      ' /ACCESSION='||split_part(defline,'|',2)||
      ' /GI='||split_part(split_part(defline,' ',1),'|',3)||
      ' /TAXON_ID='||split_part(split_part(defline,'taxon:',2),' ',1)||
      ' /DESCRIPTION="'||substring(defline from position(' ' in defline)+1)||'"';
 *
 * this statement will summarize the status of the staged sequences:
 *
select case seq.obs_flag when true then 'obsolete' when false then 'current' else 'new' end as status, count(*) as num_sequences
  from stage_fasta_pep pep left outer join sequence_entity seq using(camera_acc)
  group by case seq.obs_flag when true then 'obsolete' when false then 'current' else 'new' end;
 */
------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------
-- massage uploaded PANDA fasta data
------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------
set search_path=panda,camera,public;
set enable_hashjoin=false;
set enable_mergejoin=false;
set enable_seqscan=false;
analyze stage_fasta_pep;
--
-- remove tabs and backspaces from defline, formatdb chokes on them
-- calculate chksum for sequence, we will use this to check if the sequence already exists in BIO_SEQUENCE
-- calculate sequence length
-- extract defline attributes
update stage_fasta_pep set
    defline=replace(replace(defline,chr(9),' '),chr(8),' '),
    sequence_md5=md5(sequence),
    sequence_length=length(sequence),
    owner_id=60,obs_flag=false,source_id=2,entity_type_code=6,
    external_source=split_part(camera_acc,'_',1),
    external_acc=get_def_tag(defline,'/ACCESSION=',' '),
    ncbi_gi_number=get_def_tag(defline,'/GI=',' ')::integer,
    organism=get_def_tag(defline,'/ORGANISM="','"'),
    taxon_id=get_def_tag(defline,'/TAXON_ID=',' ')::integer;
--
-- assign sequence id
-- re-use existing sequence if match found
update stage_fasta_pep f set sequence_id=
  (select min(sequence_id) from bio_sequence s
   where s.sequence_md5=f.sequence_md5 and s.source_id=2 and s.sequence=f.sequence);
update stage_fasta_pep f set sequence_id=
  (select sequence_id from panda_sequence s
   where s.sequence_md5=f.sequence_md5 and s.source_id=2 and s.sequence=f.sequence)
  where sequence_id is null;
update stage_fasta_pep
  set sequence_id=nextval('uid_generated_sequence')
  where sequence_id is null;
--
-- split compound-deflines into individual entries
select normalize_stage_fasta_pep();
--
-- remove any entries already loaded from other niaa files
delete from stage_fasta_pep f
  where exists(select 1 from panda_entity p where p.camera_acc=f.camera_acc);
--
-- assign entity_id
-- re-use existing entity if match found
update stage_fasta_pep f set entity_id=se.entity_id from sequence_entity se where se.camera_acc=f.camera_acc;
update stage_fasta_pep set entity_id=nextval('uid_generated_sequence') where entity_id is null;
--
-- vacuum/analyze staging tables
vacuum verbose analyze stage_fasta_pep;
--
-- QA stats
select count(*) as numrows, count(entity_id) as numents, count(sequence_id) as numseqs, count(distinct sequence_id) as numunqseqs,
       count(case when taxon_id>0 then taxon_id else null end) as numwithtax, count(organism) as numwithorg
  from stage_fasta_pep;
--
-- restore disabled join methods
set enable_hashjoin=true;
set enable_mergejoin=true;
set enable_seqscan=true;
------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------
-- add fasta data to accumulated PANDA data
------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------
set search_path=panda,camera,public;
set enable_hashjoin=false;
set enable_mergejoin=false;
set enable_seqscan=false;
--
insert into panda_entity(
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,
    dna_acc,dna_begin,dna_end,dna_id,dna_orientation,
    entity_id,entity_type_code,external_acc,external_source,
    library_acc,library_id,locus,ncbi_gi_number,obs_flag,
    orf_acc,orf_id,organism,owner_id,protein_acc,protein_id,
    replaced_by,sample_acc,sample_id,sequence_id,sequence_length,
    sequencing_direction,source_id,stop_3_prime,stop_5_prime,
    taxon_id,template_acc,trace_acc,translation_table,type)
  select
    assembly_acc,assembly_id,camera_acc,clear_range_begin,clear_range_end,comment,defline,
    dna_acc,dna_begin,dna_end,dna_id,dna_orientation,
    entity_id,entity_type_code,external_acc,external_source,
    library_acc,library_id,locus,ncbi_gi_number,obs_flag,
    orf_acc,orf_id,organism,owner_id,protein_acc,protein_id,
    replaced_by,sample_acc,sample_id,sequence_id,sequence_length,
    sequencing_direction,source_id,stop_3_prime,stop_5_prime,
    taxon_id,template_acc,trace_acc,translation_table,type
  from stage_fasta_pep f
  where not exists
    (select entity_id from panda_entity p where p.entity_id=f.entity_id);
--
insert into panda_sequence(sequence_id,sequence_type_code,sequence_md5,sequence,source_id)
  select sequence_id,1,sequence_md5,sequence,2
  from (select distinct sequence_id,sequence_md5,sequence from stage_fasta_pep where sequence>'') x
  where not exists (select sequence_id from panda_sequence p where p.sequence_id=x.sequence_id)
  and not exists (select sequence_id from bio_sequence p where p.sequence_id=x.sequence_id);
--
-- re-analyze PANDA tables after updates
analyze panda_entity;
analyze panda_sequence;
--
-- restore disabled join methods
set enable_hashjoin=true;
set enable_mergejoin=true;
set enable_seqscan=true;
------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------
-- QA accumulated PANDA data
------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------
set search_path=panda,camera,public;
set enable_hashjoin=false;
set enable_mergejoin=false;
set enable_seqscan=false;
--
-- any missing sequences?
select * from panda_sequence where sequence_md5=md5('');
select count(*)
  from panda_entity e
  where not exists(select 1 from bio_sequence s where s.sequence_id=e.sequence_id)
    and not exists(select 1 from panda_sequence s where s.sequence_id=e.sequence_id);
--
-- check for new taxons:
-- add new taxons to the panda_taxonomy table and manually populate the panda_taxonomy_children table.
-- please note: taxon_id=0 indicates an unknown (new to PANDA) taxon. look up the proper taxonomy entry
-- at NCBI and correct the taxon assignment in panda_entity. DO NOT add a taxon_id=0 row to panda_taxonomy.
truncate panda_taxonomy, panda_taxonomy_children;
select taxon_id, max(organism) as maxorg, min(organism) as minorg, count(*) as numseq
  from panda_entity e
  where (taxon_id=0 or not exists (select 1 from taxonomy t where t.taxon_id=e.taxon_id))
  group by taxon_id
  having coalesce(max(organism),'')<>coalesce(min(organism),'');
insert into panda_taxonomy(taxon_id,preferred_name,is_obsolete)
  select taxon_id, max(organism), false from stage_fasta_nuc f
  where taxon_id>0 and organism>'' and not exists (select 1 from taxonomy t where t.taxon_id=f.taxon_id)
  group by taxon_id;
select * from panda_taxonomy limit 5;
--
-- restore disabled join methods
set enable_hashjoin=true;
set enable_mergejoin=true;
set enable_seqscan=true;
