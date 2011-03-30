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

set search_path=camera;
-----------------------------------------------------------------------------------------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION deletetask(p_taskid bigint) RETURNS integer
    AS $$
begin
--
-- delete blast hits
     delete from camera.blast_hit where result_node_id in
         (select node_id from camera.node where task_id=p_taskId);
--
-- delete user nodes
     delete from camera.task_input_node where task_id=p_taskId;
     delete from camera.node where task_id=p_taskId;
--
-- delete user tasks
     delete from camera.task_event where task_id=p_taskId;
     delete from camera.task where task_id=p_taskId;
   return 1;
 exception
   when others then
--     rollback;
     raise notice 'delete failed';
     return -1;
 end;
 $$ LANGUAGE plpgsql;

DROP FUNCTION FUNCTION fn_trig_bse_view_pre_iud_stmt();

CREATE OR REPLACE FUNCTION fnblastresultsubjectlist(pblastresultnode bigint) RETURNS text
    AS $$
declare
  vSubjectList text = '';
  vSubjectHit record;
begin
  for vSubjectHit in select bse.camera_acc as subject_acc
                    from camera.blast_hit bh inner join camera.bse bse on bse.entity_id=bh.subject_id
                    where bh.result_node_id=pBlastResultNode
                    group by bse.camera_acc
                    order by max( (bh.number_identical+bh.number_similar)/bh.length_alignment *
                                  (bse.sequence_length-bh.subject_number_unalignable)/bse.sequence_length ) loop
    vSubjectList := vSubjectList||vSubjectHit.subject_acc||chr(10);
  end loop;
  return substring(vSubjectList from 1 for char_length(vSubjectList)-2);
end $$ LANGUAGE plpgsql;

DROP FUNCTION fulltext_basesequenceentity(text);

CREATE OR REPLACE FUNCTION get_def_tag(p_defline character varying, p_tag character varying, p_eod character varying) RETURNS character varying
    AS $$
 declare
   startpos int;
   datalen int;
   taglen int := char_length( p_tag );
   deflen int := char_length( p_defline );
 begin
   if p_defline is null then return null; end if;
   startpos := position( p_tag in p_defline );
   if startpos=0 then return null; end if;
   startpos := startpos+taglen;
   datalen := position( p_eod in substring( p_defline, startpos, deflen-startpos+1 ) );
   if datalen is null or datalen=1 then
     return null;
   elseif datalen=0 then
     datalen := deflen-startpos+1;
   else
     datalen := datalen-1;
   end if;
   return trim('"' from trim(' ' from substring(p_defline from startpos for datalen )));
 end;
 $$ LANGUAGE plpgsql;

--
-- Name: nodeidtoname(character varying); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE OR REPLACE FUNCTION nodeidtoname(pidstring character varying) RETURNS text[]
    AS $$
declare
  work text := replace(pIdString,' ','');
  pos integer;
  result text[];
  n integer := 0;
  nodename text;
  id bigint;
begin
  while work is not null and work<>'' loop
    pos := position(',' in work);
    if pos=0 then pos := length(work)+1; end if;
    id := substring(work from 1 for pos-1);
    select max(name) into nodename from camera.node where node_id=id;
    n := n + 1;
    result[n] := nodename;
    work := substring(work from pos+1);
  end loop;
  return result;
end $$ LANGUAGE plpgsql;

DROP TABLE aa_sequence;

DROP TABLE alignment_type CASCADE;

DROP TABLE annotation_set;


