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

create or replace function calculateQryRank(pId bigint, pIdtype text) returns void as $$
declare
  nodeId bigint;
  blkInc integer := 1;
  qryRank integer := 1;
  qryHitRank float4;
  qry record;
  subj record;
  hsp record;
begin
--
-- look up node_id if not provided
  if lower(trim(pIdType))='task' then
    select max(node_id)
      into nodeId
      from node
      where task_id=pId and subclass='BlastResultNode';
    if nodeId is null then return; end if;
  elsif lower(trim(pIdtype))='node' then
    nodeId := pId;
  else
    raise notice 'pIdType must be either task or node';
    return;
  end if;
--
-- fetch query sequences ordered by their BEST evalue
  for qry in select x.query_acc,
                    x.evalue,
                    (select max(b.bit_score) from blast_hit b
                     where b.query_acc=x.query_acc and b.expect_score=x.evalue
                     and b.result_node_id = nodeId) as bitscore
             from (select query_acc, min(expect_score) as evalue
                   from blast_hit where result_node_id = nodeId group by query_acc) x
             order by evalue asc, bitscore desc, query_acc asc
  loop
    qryHitRank := 0.0;
--
-- rank alignments for query by the BEST bitscore
    for subj in select x.subject_acc,
                       x.evalue,
                       (select max(b.bit_score) from blast_hit b
                        where b.query_acc=qry.query_acc and b.subject_acc=x.subject_acc
                        and b.expect_score=x.evalue and b.result_node_id = nodeId) as bitscore
                from (select b.subject_acc, min(b.expect_score) as evalue
                      from blast_hit b
                      where b.query_acc=qry.query_acc and b.result_node_id = nodeId
                      group by b.subject_acc) x
                order by evalue asc, bitscore desc, subject_acc asc
    loop
      qryHitRank := qryHitRank + 1.0;
--
-- order hsps by the position on the query sequence
      for hsp in select blast_hit_id, expect_score, bit_score
                 from blast_hit b
                 where b.query_acc=qry.query_acc and b.subject_acc=subj.subject_acc
                 and b.result_node_id = nodeId
                 order by expect_score asc, bit_score desc, blast_hit_id asc
      loop
        update blast_hit set query_rank=qryRank,query_hit_rank=qryHitRank where blast_hit_id=hsp.blast_hit_id;
        qryHitRank := qryHitRank + 0.001;
      end loop;
      qryHitRank := trunc(qryHitRank-0.001);
    end loop;
    qryRank := qryRank+1;
  end loop;
  return;
end $$ language plpgsql;

