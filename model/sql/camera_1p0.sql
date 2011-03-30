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

--
-- PostgreSQL database dump
--

SET client_encoding = 'UTF8';
SET check_function_bodies = false;
SET client_min_messages = warning;

--
-- Name: camera; Type: SCHEMA; Schema: -; Owner: camapp
--

CREATE SCHEMA camera;


ALTER SCHEMA camera OWNER TO camapp;

SET search_path = camera, pg_catalog;

--
-- Name: degree_to_num(character varying); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION degree_to_num(p_string character varying) RETURNS real
    AS $$
 declare
   v_real real := 0;
   v_idx integer;
   v_len integer;
   v_work varchar(255);
 begin
   v_work := p_string;
   v_len := char_length(v_work);
   v_idx := position('d' in v_work);
   if v_idx>0 then
     v_real := to_number(substring(v_work from 1 for v_idx-1),'99999.99999');
     v_work := substring(v_work from v_idx+1);
     v_len := v_len-v_idx;
   end if;
   v_idx := position('''' in v_work);
   if v_idx>0 then
     v_real := v_real + to_number(substring(v_work from 1 for v_idx-1),'99999.99999')/60;
     v_work := substring(v_work from v_idx+1);
     v_len := v_len-v_idx;
   end if;
   v_idx := position('"' in v_work);
   if v_idx>0 then
     v_real := v_real + to_number(substring(v_work from 1 for v_idx-1),'99999.99999')/60/60;
     v_work := trim(both ' ' from substring(v_work from v_idx+1));
     v_len := v_len-v_idx;
   end if;
   if v_work in ('s','w') then v_real := -1*v_real; end if;
   return v_real;
 end;
 $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.degree_to_num(p_string character varying) OWNER TO postgres;

--
-- Name: delete_user_data(character varying, boolean); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION delete_user_data(p_user_login character varying, p_delete_user boolean) RETURNS integer
    AS $$
 declare
   l_user_id bigint;
   rec record;
 begin
--   start transaction;
--
-- get user id
     select user_id into l_user_id from camera.camera_user where user_login=p_user_login;
     if l_user_id is null then
       raise notice 'user does not exist';
       return 0;
     end if;       
--
-- delete user sequences
     for rec in
       select distinct f.feature_id, b.entity_id, b.sequence_id
         from camera.node n
              left outer join camera.data_node_feature f on f.node_id=n.node_id
              left outer join camera.bse b on b.entity_id=f.feature_id
         where n.user_id=l_user_id loop
       if rec.feature_id is not null then
         delete from camera.data_node_feature where feature_id=rec.feature_id;
       end if;
       if rec.entity_id is not null then
         delete from camera.bse where entity_id=rec.entity_id;
         if rec.sequence_id is not null then
           delete from camera.bio_sequence where sequence_id=rec.sequence_id;
         end if;
       end if;
     end loop;
--
-- delete user alignments
     delete from camera.blast_hit where alignment_id in
       (select alignment_id from camera.alignment where result_node_id in
         (select node_id from camera.node where user_id=l_user_id));
     delete from camera.alignment where result_node_id in
       (select node_id from camera.node where user_id=l_user_id);
--
-- delete user nodes
     delete from camera.task_input_node where task_id in
       (select task_id from camera.task where user_id=l_user_id or task_owner=p_user_login);
     delete from camera.node where user_id=l_user_id;
--
-- delete user tasks
     delete from camera.task_event where task_id in
       (select task_id from camera.task where user_id=l_user_id or task_owner=p_user_login);
     delete from camera.task where user_id=l_user_id or task_owner=p_user_login;
--
-- delete user login (if requested)
     if p_delete_user then
       delete from camera.camera_user where user_id=l_user_id;
     end if;
--   commit;
   return 1;
 exception
   when others then
--     rollback;
     raise notice 'delete failed';
     return -1;
 end;
 $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.delete_user_data(p_user_login character varying, p_delete_user boolean) OWNER TO postgres;

--
-- Name: deletetask(bigint); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION deletetask(p_taskid bigint) RETURNS integer
    AS $$
begin
--
-- delete alignments
     delete from camera.blast_hit where alignment_id in
       (select alignment_id from camera.alignment where result_node_id in
         (select node_id from camera.node where task_id=p_taskId));
     delete from camera.alignment where result_node_id in
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
 $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.deletetask(p_taskid bigint) OWNER TO postgres;

--
-- Name: deleteusertaskbefore(text, date); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION deleteusertaskbefore(p_userlogin text, p_beforedate date) RETURNS integer
    AS $$
declare
  curTaskId bigint := -1;
  n integer := 0;
  status integer;
  t task;
begin
  for t in (select task.* from task_event event inner join task on task.task_id=event.task_id
            where task.task_owner=p_userLogin and event.event_timestamp<p_beforeDate::timestamp) loop
    curTaskId := t.task_id;
    select deleteTask(curTaskId) into status;
    if status<>1 then raise exception 'delete failed on task %', curTaskId; end if;
    n := n + 1;
  end loop;
  return n;
exception when others then
  raise notice 'deletion failed on task %', curTaskId;
  return -1;
end;
$$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.deleteusertaskbefore(p_userlogin text, p_beforedate date) OWNER TO postgres;

--
-- Name: fn_max_ts(); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION fn_max_ts() RETURNS timestamp without time zone
    AS $$
begin
  return to_timestamp('31-dec-9999','dd-mon-yyyy');
end;
$$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.fn_max_ts() OWNER TO postgres;

--
-- Name: fn_trig_auditing(); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION fn_trig_auditing() RETURNS "trigger"
    AS $$
begin
  delete from tmp_transaction_timestamp;
  insert into tmp_transaction_timestamp values(now());
  return null;
end;
$$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.fn_trig_auditing() OWNER TO postgres;

--
-- Name: fn_trig_bio_sequence_iu(); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION fn_trig_bio_sequence_iu() RETURNS "trigger"
    AS $$
  begin
    new.sequence_md5=md5(new.sequence);
    return new;
  end;
  $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.fn_trig_bio_sequence_iu() OWNER TO postgres;

--
-- Name: fn_trig_bse_view_pre_iud_stmt(); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION fn_trig_bse_view_pre_iud_stmt() RETURNS "trigger"
    AS $$
begin
  delete from tmp_transaction_timestamp;
  insert into tmp_transaction_timestamp values(now());
end;
$$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.fn_trig_bse_view_pre_iud_stmt() OWNER TO postgres;

--
-- Name: fnblastresultsubjectlist(bigint); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION fnblastresultsubjectlist(pblastresultnode bigint) RETURNS text
    AS $$
declare
  vSubjectList text = '';
  vSubjectHit record;
begin
  for vSubjectHit in select bse.camera_acc as subject_acc
                    from camera.alignment a
                    inner join camera.blast_hit bh on bh.alignment_id=a.alignment_id
                    inner join camera.bse bse on bse.entity_id=a.subject_id
                    where a.result_node_id=pBlastResultNode
                    group by bse.camera_acc
                    order by max( (bh.number_identical+bh.number_similar)/bh.length_alignment *
                                  (bse.sequence_length-bh.subject_number_unalignable)/bse.sequence_length ) loop
    vSubjectList := vSubjectList||vSubjectHit.subject_acc||chr(10);
  end loop;
  return substring(vSubjectList from 1 for char_length(vSubjectList)-2);
end $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.fnblastresultsubjectlist(pblastresultnode bigint) OWNER TO postgres;

--
-- Name: fulltext_basesequenceentity(text); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION fulltext_basesequenceentity(p_camera_acc text) RETURNS text
    AS $$
declare
  entityId bigint;
  entityTypeCode integer;
  tempText text;
  fullText text;
begin
  select
      bse.entity_id,
      bse.entity_type_code,
      'Entity Type: '||(select det.name from entity_type det where det.code=bse.entity_type_code)||chr(10)||
      'Camera Accession: '||camera_acc||chr(10)||
      'External Accession: '||coalesce(external_acc,'')||chr(10)||
      'Description: '||coalesce(description,'')||chr(10)||
      'Owner: '||(select det.user_login from camera_user det where det.user_id=bse.owner_id)||chr(10)||
      'Sequence Length: '||sequence_length::text
    into
      entityId, entityTypeCode, fullText
    from
      camera.bse
    where
      camera_acc=p_camera_acc;
  if entityTypeCode=1 then
    select
        'Notes:'||
          case when c.is_complete then ' complete' else '' end||
          case when c.is_plasmid then ' plasmid' else '' end||chr(10)||
        'Organism: '||o.organism_acc||' '||o.name||chr(10)||
        'NCBI taxon #:'||case when o.taxon_id is not null then ' '||o.taxon_id::text else '' end
      into
        tempText
      from
        camera.chromosome c
        inner join camera.organism o on o.organism_id=c.organism_id
      where
        c.entity_id=entityId;
  elsif entityTypeCode=2 then
    select
        'Assembly '||a.assembly_acc||' '||a.name||case when a.is_finished then '(finished)' else '' end
      into
        tempText
      from
        camera.scaffold s
        inner join camera.assembly a on a.assembly_id=s.assembly_id
      where
        s.entity_id=entityId;
  elsif entityTypeCode=3 then
    select
        'Trace:'||coalesce(' '||r.trace_acc,'')||chr(10)||
        'Template:'||coalesce(' '||r.template_acc,'')||chr(10)||
        'Clear Range:'||
           coalesce(' '||r.clear_range_begin||'-'||r.clear_range_end,'')||
           coalesce(' ('||r.sequencing_direction||')','')||chr(10)||
        'Library: '||l.library_acc||chr(10)||
        'Sequencing Technology:'||coalesce(' '||l.sequencing_technology,'')||chr(10)||
        'Insert Size:'||
          case when l.max_insert_size=l.min_insert_size then ' '||l.max_insert_size::text 
               else coalesce(' '||l.min_insert_size::text||'-'||l.max_insert_size::text,'')
            end||
        coalesce(chr(10)||'Intellectual Property Notice: '||l.intellectual_property_notice,'')
      into
        tempText
      from
        camera.read r
        inner join camera.library l on l.library_id=r.library_id
      where
        r.entity_id=entityId;
  end if;
  if tempText is not null then fullText = fullText||chr(10)||tempText; end if;
  return fullText;
end $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.fulltext_basesequenceentity(p_camera_acc text) OWNER TO postgres;

--
-- Name: get_def_tag(character varying, character varying, character varying); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION get_def_tag(p_defline character varying, p_tag character varying, p_eod character varying) RETURNS character varying
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
   return substring( p_defline from startpos for datalen );
 end;
 $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.get_def_tag(p_defline character varying, p_tag character varying, p_eod character varying) OWNER TO postgres;

--
-- Name: getsubjectnode(character varying); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION getsubjectnode(p_parameters character varying) RETURNS bigint
    AS $$
declare
  nodeId bigint;
  workSpace text;
begin
  workSpace = replace(substring(p_parameters from position('subject databases' in p_parameters)),'<PVOMUT.D><PVOMUT.V>','<PVOMUT.D>');
  workSpace = split_part(split_part(workSpace,'>',2),'<',1);
  nodeId = workSpace::bigint;
  return nodeId;
exception when others then
  return null;
end $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.getsubjectnode(p_parameters character varying) OWNER TO postgres;

--
-- Name: nodeidtoname(character varying); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION nodeidtoname(pidstring character varying) RETURNS text[]
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
end $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.nodeidtoname(pidstring character varying) OWNER TO postgres;

--
-- Name: subsequence(integer, text, integer, integer, integer); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION subsequence(p_seqtype integer, p_sequence text, p_begin integer, p_end integer, p_ori integer) RETURNS text
    AS $$
 declare
   sequence text;
   rsequence text;
   ch text;
   seqtype integer;
 begin
   sequence = substring(p_sequence from p_begin+1 for p_end-p_begin);
   if p_ori=1 then
     return sequence;
   else
     rsequence = '';
     for i in 1..length(sequence) loop
       ch = substring(sequence from i for 1);
       if p_seqtype=2 or p_seqtype=3 then
         if ch='A' then
           if p_seqtype=2 then ch='T';
           else ch = 'U';
           end if;
         elsif ch='C' then ch='G';
         elsif ch='G' then ch='C';
         elsif ch='T' then ch='A';
         elsif ch='U' then ch='A';
         end if;
       end if; 
       rsequence = ch||rsequence;
     end loop;
     return rsequence;
   end if;
 end;
 $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.subsequence(p_seqtype integer, p_sequence text, p_begin integer, p_end integer, p_ori integer) OWNER TO postgres;

--
-- Name: timeouttasks(); Type: FUNCTION; Schema: camera; Owner: postgres
--

CREATE FUNCTION timeouttasks() RETURNS void
    AS $$
begin
  insert into task_event(task_id,event_no,event_timestamp,event_type,description)
    select task_id, last_event_no+1, current_timestamp, 'error', 'Task timed out.'
    from task_status
    where upper(status) not in ('ERROR','COMPLETED') and current_date-start_timestamp::date>=2;
end $$
    LANGUAGE plpgsql;


ALTER FUNCTION camera.timeouttasks() OWNER TO postgres;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: aa_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE aa_sequence (
    entity_acc character varying(255) NOT NULL,
    "sequence" text NOT NULL,
    sequence_md5 text NOT NULL,
    partition_flag integer
);


ALTER TABLE camera.aa_sequence OWNER TO postgres;

--
-- Name: alignment; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE alignment (
    alignment_id bigint NOT NULL,
    alignment_type_code integer NOT NULL,
    query_id bigint NOT NULL,
    query_begin integer NOT NULL,
    query_end integer NOT NULL,
    query_orientation integer NOT NULL,
    subject_id bigint NOT NULL,
    subject_begin integer NOT NULL,
    subject_end integer NOT NULL,
    subject_orientation integer NOT NULL,
    result_node_id bigint,
    mate_category_code integer,
    result_rank integer,
    CONSTRAINT alignment_chk_result CHECK ((((result_node_id IS NOT NULL) AND (result_rank IS NOT NULL)) OR ((result_node_id IS NULL) AND (result_rank IS NULL))))
);


ALTER TABLE camera.alignment OWNER TO postgres;

--
-- Name: alignment_type; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE alignment_type (
    code integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255)
);


ALTER TABLE camera.alignment_type OWNER TO postgres;

--
-- Name: annotation_set; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE annotation_set (
    annotation_id integer NOT NULL,
    annotation_name character varying(80) NOT NULL,
    description character varying(255),
    date_last_updated date,
    number_of_entries integer,
    data_source_id integer NOT NULL,
    annotation_version character varying(20),
    entity_type integer NOT NULL,
    number_loaded integer
);


ALTER TABLE camera.annotation_set OWNER TO postgres;

--
-- Name: asdf; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE asdf (
    sdf character varying(25)
);


ALTER TABLE camera.asdf OWNER TO postgres;

--
-- Name: assembly; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE assembly (
    assembly_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    is_finished boolean,
    site_id bigint,
    sample_id bigint,
    assembly_acc character varying(255)
);


ALTER TABLE camera.assembly OWNER TO postgres;

--
-- Name: author; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE author (
    name character varying(255) NOT NULL
);


ALTER TABLE camera.author OWNER TO postgres;

--
-- Name: bio_material; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE bio_material (
    material_id bigint NOT NULL,
    material_acc character varying(40) NOT NULL,
    description character varying(255) NOT NULL,
    project_symbol character varying(40) NOT NULL,
    collection_location_id bigint NOT NULL,
    collection_start_time timestamp without time zone NOT NULL,
    collection_stop_time timestamp without time zone
);


ALTER TABLE camera.bio_material OWNER TO postgres;

--
-- Name: bio_material_sample; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE bio_material_sample (
    material_id bigint NOT NULL,
    sample_id bigint NOT NULL
);


ALTER TABLE camera.bio_material_sample OWNER TO postgres;

--
-- Name: bio_sample; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE bio_sample (
    sample_id bigint NOT NULL,
    sample_acc character varying(255),
    filter_min double precision,
    filter_max double precision,
    sample_depth real,
    sample_name character varying(255)
);


ALTER TABLE camera.bio_sample OWNER TO postgres;

--
-- Name: bio_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE bio_sequence (
    sequence_id bigint NOT NULL,
    sequence_type_code integer NOT NULL,
    "sequence" text NOT NULL,
    sequence_md5 text
);


ALTER TABLE camera.bio_sequence OWNER TO postgres;

--
-- Name: blast_fasta; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE blast_fasta (
    node_id bigint,
    accession character varying(80),
    defline character varying(4000),
    "sequence" text
);


ALTER TABLE camera.blast_fasta OWNER TO postgres;

--
-- Name: blast_hit; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE blast_hit (
    alignment_id bigint NOT NULL,
    bit_score double precision NOT NULL,
    "comment" character varying(255),
    number_identical integer NOT NULL,
    number_similar integer NOT NULL,
    length_alignment integer NOT NULL,
    query_gaps integer NOT NULL,
    subject_gaps integer NOT NULL,
    query_length integer NOT NULL,
    subject_length integer NOT NULL,
    query_number_unalignable integer NOT NULL,
    subject_number_unalignable integer NOT NULL,
    program_used character varying(255) NOT NULL,
    query_stops integer NOT NULL,
    subject_stops integer NOT NULL,
    expect_score double precision,
    entropy double precision,
    subject_gap_runs integer,
    query_gap_runs integer,
    hsp_score double precision,
    query_frame integer,
    midline_align_string text,
    query_align_string text,
    subject_frame integer,
    subject_align_string text
);


ALTER TABLE camera.blast_hit OWNER TO postgres;

--
-- Name: blast_result_node_defline_map; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE blast_result_node_defline_map (
    node_id bigint NOT NULL,
    camera_acc character varying(255) NOT NULL,
    defline character varying(4000) NOT NULL
);


ALTER TABLE camera.blast_result_node_defline_map OWNER TO postgres;

--
-- Name: blastdataset_node_members; Type: TABLE; Schema: camera; Owner: camapp; Tablespace: 
--

CREATE TABLE blastdataset_node_members (
    dataset_node_id bigint NOT NULL,
    blastdb_filenode_id bigint NOT NULL
);


ALTER TABLE camera.blastdataset_node_members OWNER TO camapp;

--
-- Name: bse; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE bse (
    entity_id bigint NOT NULL,
    entity_type_code integer NOT NULL,
    camera_acc character varying(255) NOT NULL,
    external_acc character varying(255),
    description character varying(4000),
    owner_id bigint NOT NULL,
    sequence_id bigint NOT NULL,
    sequence_length integer NOT NULL
);


ALTER TABLE camera.bse OWNER TO postgres;

--
-- Name: camera_user; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE camera_user (
    user_login character varying(255) NOT NULL,
    fullname character varying(255),
    email character varying(255),
    user_id bigint NOT NULL
);


ALTER TABLE camera.camera_user OWNER TO postgres;

--
-- Name: chromosome; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE chromosome (
    entity_id bigint NOT NULL,
    is_complete boolean,
    is_plasmid boolean,
    organism_id bigint NOT NULL
);


ALTER TABLE camera.chromosome OWNER TO postgres;

--
-- Name: chromosome_entity; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE chromosome_entity (
    entity_acc character varying(255),
    name character varying(255),
    taxon_id integer,
    is_complete boolean,
    is_plasmid boolean
);


ALTER TABLE camera.chromosome_entity OWNER TO postgres;

--
-- Name: collection_location; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE collection_location (
    location_id bigint NOT NULL,
    longitude character varying(20),
    latitude character varying(20),
    altitude real,
    depth real,
    arrival_time timestamp without time zone,
    departure_time timestamp without time zone,
    path_id bigint,
    location_type character varying(12) NOT NULL,
    CONSTRAINT geo_location_chk_subclass CHECK (CASE WHEN ((location_type)::text = 'GeoPath'::text) THEN ((((((longitude IS NULL) AND (latitude IS NULL)) AND (altitude IS NULL)) AND (depth IS NULL)) AND (arrival_time IS NULL)) AND (departure_time IS NULL)) WHEN (((location_type)::text = 'GeoPoint'::text) OR ((location_type)::text = 'GeoTimePoint'::text)) THEN ((((longitude IS NOT NULL) AND (latitude IS NOT NULL)) AND ((altitude IS NULL) OR (depth IS NULL))) AND CASE WHEN ((location_type)::text = 'GeoTimePoint'::text) THEN ((arrival_time IS NOT NULL) AND (departure_time IS NOT NULL)) ELSE ((arrival_time IS NULL) AND (departure_time IS NULL)) END) ELSE false END)
);


ALTER TABLE camera.collection_location OWNER TO postgres;

--
-- Name: collection_observation; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE collection_observation (
    material_id bigint NOT NULL,
    observation_no integer NOT NULL,
    observation_type character varying(40) NOT NULL,
    value character varying(255),
    units character varying(12),
    instrument character varying(40),
    "comment" character varying(4000),
    CONSTRAINT collection_observation_chk_content CHECK (((((value IS NOT NULL) OR ("comment" IS NOT NULL)) AND ((units IS NULL) OR (value IS NOT NULL))) AND ((instrument IS NULL) OR (value IS NOT NULL))))
);


ALTER TABLE camera.collection_observation OWNER TO postgres;

--
-- Name: constant_type; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE constant_type (
    oid bigint NOT NULL,
    discriminator character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255)
);


ALTER TABLE camera.constant_type OWNER TO postgres;

--
-- Name: core_cluster; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE core_cluster (
    core_cluster_id bigint NOT NULL,
    core_cluster_acc character varying(255) NOT NULL,
    final_cluster_id bigint,
    ffas_profile text NOT NULL,
    psiblast_profile bytea NOT NULL,
    longest_peptide_id bigint
);


ALTER TABLE camera.core_cluster OWNER TO postgres;

--
-- Name: core_cluster_peptide; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE core_cluster_peptide (
    core_cluster_id bigint NOT NULL,
    peptide_id bigint NOT NULL,
    is_redundant boolean NOT NULL,
    is_representative boolean NOT NULL
);


ALTER TABLE camera.core_cluster_peptide OWNER TO postgres;

--
-- Name: data_file; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE data_file (
    oid bigint NOT NULL,
    path character varying(255),
    info_location character varying(255),
    description character varying(2000),
    size bigint,
    multifile_archive boolean
);


ALTER TABLE camera.data_file OWNER TO postgres;

--
-- Name: data_file_sample_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE data_file_sample_link (
    data_file_id bigint NOT NULL,
    sample_id bigint NOT NULL
);


ALTER TABLE camera.data_file_sample_link OWNER TO postgres;

--
-- Name: data_node_feature; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE data_node_feature (
    node_id bigint NOT NULL,
    feature_id bigint NOT NULL
);


ALTER TABLE camera.data_node_feature OWNER TO postgres;

--
-- Name: data_source; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE data_source (
    source_id bigint NOT NULL,
    source_name character varying(100),
    data_version character varying(20)
);


ALTER TABLE camera.data_source OWNER TO postgres;

--
-- Name: entity_type; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE entity_type (
    code integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255),
    seq_type character varying(255),
    abbrev character varying(4) NOT NULL,
    sequence_type integer NOT NULL
);


ALTER TABLE camera.entity_type OWNER TO postgres;

--
-- Name: experiment; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE experiment (
    experiment_id bigint NOT NULL,
    experiment_acc character varying(255) NOT NULL,
    path_id bigint,
    experiment_type_id bigint NOT NULL,
    description character varying(255)
);


ALTER TABLE camera.experiment OWNER TO postgres;

--
-- Name: external_aa_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE external_aa_sequence (CONSTRAINT external_aa_sequence_chk CHECK ((partition_flag = 2))
)
INHERITS (aa_sequence);


ALTER TABLE camera.external_aa_sequence OWNER TO postgres;

--
-- Name: seq_entity; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE seq_entity (
    entity_acc character varying(255) NOT NULL,
    def_line character varying(4000),
    owner_id bigint NOT NULL,
    sequence_length integer NOT NULL,
    partition_flag integer
);


ALTER TABLE camera.seq_entity OWNER TO postgres;

--
-- Name: external_entity; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE external_entity (
    partition_flag integer DEFAULT 2,
    CONSTRAINT external_entity_chk CHECK ((partition_flag = 2)),
    CONSTRAINT external_entity_chk_is_system CHECK ((owner_id = 60))
)
INHERITS (seq_entity);


ALTER TABLE camera.external_entity OWNER TO postgres;

--
-- Name: na_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE na_sequence (
    entity_acc character varying(255) NOT NULL,
    "sequence" text NOT NULL,
    sequence_md5 text NOT NULL,
    partition_flag integer
);


ALTER TABLE camera.na_sequence OWNER TO postgres;

--
-- Name: external_na_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE external_na_sequence (CONSTRAINT external_na_sequence_chk CHECK ((partition_flag = 2))
)
INHERITS (na_sequence);


ALTER TABLE camera.external_na_sequence OWNER TO postgres;

--
-- Name: final_cluster; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE final_cluster (
    final_cluster_id bigint NOT NULL,
    final_cluster_acc character varying(255) NOT NULL,
    is_spurious boolean NOT NULL
);


ALTER TABLE camera.final_cluster OWNER TO postgres;

--
-- Name: final_cluster_go_term; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE final_cluster_go_term (
    final_cluster_id bigint,
    go_acc character varying(255)
);


ALTER TABLE camera.final_cluster_go_term OWNER TO postgres;

--
-- Name: gene; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE gene (
    entity_id bigint NOT NULL,
    annotation_id integer NOT NULL,
    is_pseudogene boolean
);


ALTER TABLE camera.gene OWNER TO postgres;

--
-- Name: go_term; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE go_term (
    go_term_acc character varying(255) NOT NULL,
    alt_go_term_acc_list character varying(12)[],
    name character varying(255),
    namespace character varying(255),
    definition text
);


ALTER TABLE camera.go_term OWNER TO postgres;

--
-- Name: hierarchy_node; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE hierarchy_node (
    oid bigint NOT NULL,
    name character varying(255),
    description character varying(255)
);


ALTER TABLE camera.hierarchy_node OWNER TO postgres;

--
-- Name: hierarchy_node_data_file_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE hierarchy_node_data_file_link (
    hierarchy_node_id bigint NOT NULL,
    data_file_id bigint NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.hierarchy_node_data_file_link OWNER TO postgres;

--
-- Name: hierarchy_node_to_children_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE hierarchy_node_to_children_link (
    parent_id bigint NOT NULL,
    child_id bigint NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.hierarchy_node_to_children_link OWNER TO postgres;

--
-- Name: id_generated_sequence; Type: SEQUENCE; Schema: camera; Owner: postgres
--

CREATE SEQUENCE id_generated_sequence
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE camera.id_generated_sequence OWNER TO postgres;

--
-- Name: internal_aa_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE internal_aa_sequence (CONSTRAINT internal_aa_sequence_chk CHECK ((partition_flag = 1))
)
INHERITS (aa_sequence);


ALTER TABLE camera.internal_aa_sequence OWNER TO postgres;

--
-- Name: internal_entity; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE internal_entity (
    partition_flag integer DEFAULT 1,
    CONSTRAINT internal_entity_chk CHECK ((partition_flag = 1)),
    CONSTRAINT internal_entity_chk_is_system CHECK ((owner_id = 60))
)
INHERITS (seq_entity);


ALTER TABLE camera.internal_entity OWNER TO postgres;

--
-- Name: internal_na_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE internal_na_sequence (CONSTRAINT internal_na_sequence_chk CHECK ((partition_flag = 1))
)
INHERITS (na_sequence);


ALTER TABLE camera.internal_na_sequence OWNER TO postgres;

--
-- Name: library; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE library (
    library_id bigint NOT NULL,
    library_acc character varying(40) NOT NULL,
    number_of_reads integer,
    sequencing_technology character varying(60),
    min_insert_size integer,
    max_insert_size integer,
    intellectual_property_notice character varying(1000)
);


ALTER TABLE camera.library OWNER TO postgres;

--
-- Name: library_sample; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE library_sample (
    library_id bigint NOT NULL,
    sample_id bigint NOT NULL
);


ALTER TABLE camera.library_sample OWNER TO postgres;

--
-- Name: location; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE "location" (
    location_id bigint NOT NULL,
    discriminator character varying(255) NOT NULL,
    latitude real NOT NULL,
    longitude real NOT NULL,
    depth real
);


ALTER TABLE camera."location" OWNER TO postgres;

--
-- Name: mate_category; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE mate_category (
    code integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(255)
);


ALTER TABLE camera.mate_category OWNER TO postgres;

--
-- Name: measurement; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE measurement (
    experiment_id bigint NOT NULL,
    value character varying(255) NOT NULL,
    unit_type_id bigint,
    measurement_type_id bigint NOT NULL,
    instrument_type_id bigint,
    measurement_date timestamp without time zone NOT NULL,
    measurement_no integer NOT NULL,
    "comment" character varying(255)
);


ALTER TABLE camera.measurement OWNER TO postgres;

--
-- Name: multi_select_choices; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE multi_select_choices (
    choice_id bigint NOT NULL,
    choice_element character varying(255) NOT NULL,
    choice_position integer NOT NULL
);


ALTER TABLE camera.multi_select_choices OWNER TO postgres;

--
-- Name: multi_select_values; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE multi_select_values (
    value_id bigint NOT NULL,
    value_element character varying(255) NOT NULL,
    value_position integer NOT NULL
);


ALTER TABLE camera.multi_select_values OWNER TO postgres;

--
-- Name: node; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE node (
    node_id bigint NOT NULL,
    description character varying(255),
    visibility character varying(255),
    data_type character varying(255),
    node_owner character varying(255),
    name character varying(255),
    node_type integer DEFAULT 0 NOT NULL,
    user_id bigint,
    task_id bigint,
    sequence_type character varying(20),
    length bigint,
    partition_count integer,
    is_replicated boolean,
    data_source_id bigint,
    is_assembled_data boolean,
    num_sequences integer,
    num_loaded integer
);


ALTER TABLE camera.node OWNER TO postgres;

--
-- Name: node_data_type; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE node_data_type (
    data_type character varying(255) NOT NULL,
    description character varying,
    has_sequence boolean
);


ALTER TABLE camera.node_data_type OWNER TO postgres;

--
-- Name: observation; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE observation (
    experiment_id bigint NOT NULL,
    "comment" character varying(255) NOT NULL,
    observation_date timestamp without time zone NOT NULL,
    observation_type_id bigint NOT NULL,
    observation_no integer NOT NULL
);


ALTER TABLE camera.observation OWNER TO postgres;

--
-- Name: orf; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE orf (
    entity_id bigint NOT NULL,
    translation_table character varying(255),
    peptide_id bigint NOT NULL,
    annotation_id integer NOT NULL
);


ALTER TABLE camera.orf OWNER TO postgres;

--
-- Name: organism; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE organism (
    organism_id bigint NOT NULL,
    name character varying(255) NOT NULL,
    taxon_id integer,
    organism_acc character varying(255) NOT NULL
);


ALTER TABLE camera.organism OWNER TO postgres;

--
-- Name: parameter_vo; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE parameter_vo (
    oid bigint NOT NULL,
    discriminator character varying(255) NOT NULL,
    boolean_value boolean,
    double_min_value double precision,
    double_max_value double precision,
    double_value double precision,
    long_min_value bigint,
    long_max_value bigint,
    long_value bigint,
    description character varying(255),
    text_max_length integer,
    text_value character varying(255)
);


ALTER TABLE camera.parameter_vo OWNER TO postgres;

--
-- Name: path; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE path (
    oid bigint NOT NULL,
    discriminator character varying(255) NOT NULL
);


ALTER TABLE camera.path OWNER TO postgres;

--
-- Name: path_point; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE path_point (
    oid bigint NOT NULL,
    discriminator character varying(255) NOT NULL,
    location_id bigint,
    path_id bigint,
    list_position integer,
    acquisition_date character varying(255),
    timepoint timestamp without time zone
);


ALTER TABLE camera.path_point OWNER TO postgres;

--
-- Name: peptide; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE peptide (
    entity_id bigint NOT NULL,
    annotation_id integer NOT NULL
);


ALTER TABLE camera.peptide OWNER TO postgres;

--
-- Name: project; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE project (
    symbol character varying(255) NOT NULL,
    description text,
    principal_investigators character varying(255),
    organization character varying(255),
    email character varying(255),
    website_url character varying(255),
    name character varying(255),
    released boolean DEFAULT false,
    funded_by character varying(1024),
    institutional_affiliation character varying(255)
);


ALTER TABLE camera.project OWNER TO postgres;

--
-- Name: project_publication_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE project_publication_link (
    project_id character varying(255) NOT NULL,
    publication_id bigint NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.project_publication_link OWNER TO postgres;

--
-- Name: protein_cluster_version; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE protein_cluster_version (
    version_id integer,
    description character varying(255),
    run_date date
);


ALTER TABLE camera.protein_cluster_version OWNER TO postgres;

--
-- Name: protein_final_cluster; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE protein_final_cluster (
    cluster_acc character varying(50),
    first_version_id integer,
    last_version_id integer
);


ALTER TABLE camera.protein_final_cluster OWNER TO postgres;

--
-- Name: publication; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE publication (
    oid bigint NOT NULL,
    abstractofpublication character varying(15000),
    summary character varying(15000),
    title character varying(1000),
    subjectdocument character varying(3000),
    description_html text,
    supplemental_text character varying(3000),
    pub_date timestamp without time zone,
    journal_entry character varying(300),
    publication_acc character varying(50) NOT NULL
);


ALTER TABLE camera.publication OWNER TO postgres;

--
-- Name: publication_author_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE publication_author_link (
    publication_id bigint NOT NULL,
    author_id character varying(255) NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.publication_author_link OWNER TO postgres;

--
-- Name: publication_combined_archives_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE publication_combined_archives_link (
    publication_id bigint NOT NULL,
    data_file_id bigint NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.publication_combined_archives_link OWNER TO postgres;

--
-- Name: publication_hierarchy_node_link; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE publication_hierarchy_node_link (
    publication_id bigint NOT NULL,
    hierarchy_node_id bigint NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.publication_hierarchy_node_link OWNER TO postgres;

--
-- Name: read; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE "read" (
    entity_id bigint NOT NULL,
    trace_acc character varying,
    template_acc character varying(255),
    sequencing_direction character varying(255),
    clear_range_begin integer,
    clear_range_end integer,
    library_id bigint NOT NULL
);


ALTER TABLE camera."read" OWNER TO postgres;

--
-- Name: read_entity; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE read_entity (
    entity_acc character varying(255),
    trace_acc character varying,
    template_acc character varying(255),
    sequencing_direction character varying(255),
    clear_range_begin integer,
    clear_range_end integer,
    library_acc character varying(40)
);


ALTER TABLE camera.read_entity OWNER TO postgres;

--
-- Name: sample; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE sample (
    sample_id bigint NOT NULL,
    sample_acc character varying(255) NOT NULL,
    subclass character varying(255) NOT NULL,
    experiment_acc character varying(255),
    filter_min double precision,
    filter_max double precision,
    sample_depth real,
    sample_name character varying(255)
);


ALTER TABLE camera.sample OWNER TO postgres;

--
-- Name: sample_comment; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE sample_comment (
    sample_id bigint NOT NULL,
    comment_text character varying(255),
    comment_no integer NOT NULL
);


ALTER TABLE camera.sample_comment OWNER TO postgres;

--
-- Name: sample_component; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE sample_component (
    combined_sample_id bigint NOT NULL,
    component_sample_id bigint NOT NULL
);


ALTER TABLE camera.sample_component OWNER TO postgres;

--
-- Name: scaffold; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE scaffold (
    entity_id bigint NOT NULL,
    assembly_id bigint NOT NULL
);


ALTER TABLE camera.scaffold OWNER TO postgres;

--
-- Name: sequence_type; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE sequence_type (
    code integer NOT NULL,
    name character varying(8) NOT NULL,
    description character varying(255) NOT NULL,
    elements character varying(255) NOT NULL,
    complements character varying(255) NOT NULL
);


ALTER TABLE camera.sequence_type OWNER TO postgres;

--
-- Name: single_select_values; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE single_select_values (
    value_id bigint NOT NULL,
    value_element character varying(255) NOT NULL,
    "position" integer NOT NULL
);


ALTER TABLE camera.single_select_values OWNER TO postgres;

--
-- Name: site; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE site (
    site_acc character varying(255) NOT NULL,
    subclass character varying(255) NOT NULL,
    sample_location character varying(255),
    country character varying(255),
    geographic_location character varying(255),
    leg character varying(255),
    longitude character varying(255),
    latitude character varying(255),
    data_timestamp timestamp without time zone,
    sample_depth character varying(255),
    water_depth character varying(255),
    habitat_type character varying(255),
    chlorophyll_density character varying(255),
    fluorescence character varying(255),
    transmission character varying(255),
    dissolved_oxygen character varying(255),
    temperature character varying(255),
    salinity character varying(255),
    project character varying(255),
    volume_filtered character varying(255),
    biomass character varying(255),
    dissolved_inorganic_carbon character varying(255),
    dissolved_inorganic_phosphate character varying(255),
    dissolved_organic_carbon character varying(255),
    nitrate_plus_nitrite character varying(255),
    geographic_location_detail character varying(4000),
    number_of_samples_pooled character varying(4000),
    number_of_stations_sampled character varying(4000),
    start_time timestamp without time zone,
    stop_time timestamp without time zone
);


ALTER TABLE camera.site OWNER TO postgres;

--
-- Name: site_blast_node; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE site_blast_node (
    site_acc character varying(20) NOT NULL,
    node_id bigint NOT NULL,
    experiment_id bigint
);


ALTER TABLE camera.site_blast_node OWNER TO postgres;

--
-- Name: site_sample; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE site_sample (
    site_acc character varying(255) NOT NULL,
    sample_id bigint NOT NULL
);


ALTER TABLE camera.site_sample OWNER TO postgres;

--
-- Name: task; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE task (
    task_id bigint NOT NULL,
    subclass character varying(255) NOT NULL,
    task_owner character varying(255),
    user_id bigint,
    task_name character varying(255),
    job_name character varying(40),
    parameters text
);


ALTER TABLE camera.task OWNER TO postgres;

--
-- Name: task_event; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE task_event (
    description text,
    event_timestamp timestamp without time zone NOT NULL,
    event_type character varying(255) NOT NULL,
    task_id bigint NOT NULL,
    event_no integer NOT NULL
);


ALTER TABLE camera.task_event OWNER TO postgres;

--
-- Name: task_id_generated_sequence; Type: SEQUENCE; Schema: camera; Owner: postgres
--

CREATE SEQUENCE task_id_generated_sequence
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE camera.task_id_generated_sequence OWNER TO postgres;

--
-- Name: task_input_node; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE task_input_node (
    task_id bigint NOT NULL,
    node_id bigint NOT NULL
);


ALTER TABLE camera.task_input_node OWNER TO postgres;

--
-- Name: task_monitor; Type: VIEW; Schema: camera; Owner: postgres
--

CREATE VIEW task_monitor AS
    SELECT t.task_id, t.task_owner, t.job_name, te.event_type AS status, (now() - (te.event_timestamp)::timestamp with time zone) AS runtime, t.start_timestamp, CASE WHEN (upper((te.event_type)::text) = 'ERROR'::text) THEN (te.description)::character varying ELSE ''::character varying END AS error_message FROM (SELECT t1.task_id, t1.task_owner, t1.job_name, max(t2.event_no) AS last_event_no, min(t2.event_timestamp) AS start_timestamp FROM task t1, task_event t2 WHERE (t2.task_id = t1.task_id) GROUP BY t1.task_id, t1.task_owner, t1.job_name) t, task_event te WHERE (((((te.task_id = t.task_id) AND (te.event_no = t.last_event_no)) AND (upper((te.event_type)::text) <> 'ERROR'::text)) AND (upper((te.event_type)::text) <> 'COMPLETED'::text)) AND ((('now'::text)::date - (t.start_timestamp)::date) < 2)) ORDER BY t.task_id;


ALTER TABLE camera.task_monitor OWNER TO postgres;

--
-- Name: task_settings; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE task_settings (
    oid bigint NOT NULL,
    name character varying(255)
);


ALTER TABLE camera.task_settings OWNER TO postgres;

--
-- Name: task_status; Type: VIEW; Schema: camera; Owner: postgres
--

CREATE VIEW task_status AS
    SELECT t.task_id, t.task_owner, t.job_name, te.event_type AS status, t.start_timestamp, CASE WHEN ((upper((te.event_type)::text) = 'ERROR'::text) OR (upper((te.event_type)::text) = 'COMPLETED'::text)) THEN (t.last_timestamp - t.start_timestamp) ELSE (now() - (t.start_timestamp)::timestamp with time zone) END AS runtime, t.last_event_no, CASE WHEN (upper((te.event_type)::text) = 'ERROR'::text) THEN (te.description)::character varying ELSE ''::character varying END AS error_message FROM (SELECT t1.task_id, t1.task_owner, t1.job_name, max(t2.event_no) AS last_event_no, min(t2.event_timestamp) AS start_timestamp, max(t2.event_timestamp) AS last_timestamp FROM task t1, task_event t2 WHERE (t2.task_id = t1.task_id) GROUP BY t1.task_id, t1.task_owner, t1.job_name) t, task_event te WHERE ((te.task_id = t.task_id) AND (te.event_no = t.last_event_no));


ALTER TABLE camera.task_status OWNER TO postgres;

--
-- Name: template_parameters; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE template_parameters (
    param_id bigint NOT NULL,
    param_value character varying(255),
    param_name character varying(255) NOT NULL
);


ALTER TABLE camera.template_parameters OWNER TO postgres;

--
-- Name: uid_generated_sequence; Type: SEQUENCE; Schema: camera; Owner: postgres
--

CREATE SEQUENCE uid_generated_sequence
    START WITH 1
    INCREMENT BY 2
    NO MAXVALUE
    NO MINVALUE
    CACHE 1;


ALTER TABLE camera.uid_generated_sequence OWNER TO postgres;

--
-- Name: user_aa_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE user_aa_sequence (CONSTRAINT user_aa_sequence_chk CHECK ((partition_flag = 0))
)
INHERITS (aa_sequence);


ALTER TABLE camera.user_aa_sequence OWNER TO postgres;

--
-- Name: user_entity; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE user_entity (
    partition_flag integer DEFAULT 0,
    CONSTRAINT user_entity_chk CHECK ((partition_flag = 0)),
    CONSTRAINT user_entity_chk_not_system CHECK ((owner_id <> 60))
)
INHERITS (seq_entity);


ALTER TABLE camera.user_entity OWNER TO postgres;

--
-- Name: user_na_sequence; Type: TABLE; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE TABLE user_na_sequence (CONSTRAINT user_na_sequence_chk CHECK ((partition_flag = 0))
)
INHERITS (na_sequence);


ALTER TABLE camera.user_na_sequence OWNER TO postgres;

--
-- Name: alignment_key_result; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY alignment
    ADD CONSTRAINT alignment_key_result UNIQUE (result_node_id, result_rank);


--
-- Name: alignment_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY alignment
    ADD CONSTRAINT alignment_pkey PRIMARY KEY (alignment_id);


--
-- Name: alignment_type_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY alignment_type
    ADD CONSTRAINT alignment_type_pkey PRIMARY KEY (code);


--
-- Name: assembly_name_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY assembly
    ADD CONSTRAINT assembly_name_key UNIQUE (name);


--
-- Name: assembly_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY assembly
    ADD CONSTRAINT assembly_pkey PRIMARY KEY (assembly_id);


--
-- Name: author_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY author
    ADD CONSTRAINT author_pkey PRIMARY KEY (name);


--
-- Name: bio_material_key_material_acc; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bio_material
    ADD CONSTRAINT bio_material_key_material_acc UNIQUE (material_acc);


--
-- Name: bio_material_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bio_material
    ADD CONSTRAINT bio_material_pkey PRIMARY KEY (material_id);


--
-- Name: bio_sample_key_sample_acc; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bio_sample
    ADD CONSTRAINT bio_sample_key_sample_acc UNIQUE (sample_acc);


--
-- Name: bio_sample_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bio_sample
    ADD CONSTRAINT bio_sample_pkey PRIMARY KEY (sample_id);


--
-- Name: blast_hit_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY blast_hit
    ADD CONSTRAINT blast_hit_pkey PRIMARY KEY (alignment_id);


--
-- Name: blast_result_node_defline_map_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY blast_result_node_defline_map
    ADD CONSTRAINT blast_result_node_defline_map_pkey PRIMARY KEY (node_id, camera_acc);


--
-- Name: camera_user_key_user_login; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY camera_user
    ADD CONSTRAINT camera_user_key_user_login UNIQUE (user_login);


--
-- Name: camera_user_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY camera_user
    ADD CONSTRAINT camera_user_pkey PRIMARY KEY (user_id);


--
-- Name: chromosome_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY chromosome
    ADD CONSTRAINT chromosome_pkey PRIMARY KEY (entity_id);


--
-- Name: collection_observation_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY collection_observation
    ADD CONSTRAINT collection_observation_pkey PRIMARY KEY (material_id, observation_no);


--
-- Name: collection_sample_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bio_material_sample
    ADD CONSTRAINT collection_sample_pkey PRIMARY KEY (material_id, sample_id);


--
-- Name: constant_type_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY constant_type
    ADD CONSTRAINT constant_type_pkey PRIMARY KEY (oid);


--
-- Name: core_cluster_core_cluster_acc_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY core_cluster
    ADD CONSTRAINT core_cluster_core_cluster_acc_key UNIQUE (core_cluster_acc);


--
-- Name: core_cluster_peptide_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY core_cluster_peptide
    ADD CONSTRAINT core_cluster_peptide_pkey PRIMARY KEY (core_cluster_id, peptide_id);


--
-- Name: core_cluster_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY core_cluster
    ADD CONSTRAINT core_cluster_pkey PRIMARY KEY (core_cluster_id);


--
-- Name: data_file_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY data_file
    ADD CONSTRAINT data_file_pkey PRIMARY KEY (oid);


--
-- Name: data_file_sample_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY data_file_sample_link
    ADD CONSTRAINT data_file_sample_link_pkey PRIMARY KEY (data_file_id, sample_id);


--
-- Name: data_node_feature_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY data_node_feature
    ADD CONSTRAINT data_node_feature_pkey PRIMARY KEY (node_id, feature_id);


--
-- Name: data_source_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY data_source
    ADD CONSTRAINT data_source_pkey PRIMARY KEY (source_id);


--
-- Name: dataset_key_dataset_name; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY annotation_set
    ADD CONSTRAINT dataset_key_dataset_name UNIQUE (annotation_name);


--
-- Name: dataset_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY annotation_set
    ADD CONSTRAINT dataset_pkey PRIMARY KEY (annotation_id);


--
-- Name: entity_type_abbrev_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY entity_type
    ADD CONSTRAINT entity_type_abbrev_key UNIQUE (abbrev);


--
-- Name: entity_type_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY entity_type
    ADD CONSTRAINT entity_type_pkey PRIMARY KEY (code);


--
-- Name: experiment_experiment_acc_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY experiment
    ADD CONSTRAINT experiment_experiment_acc_key UNIQUE (experiment_acc);


--
-- Name: experiment_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY experiment
    ADD CONSTRAINT experiment_pkey PRIMARY KEY (experiment_id);


--
-- Name: external_entity_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY external_entity
    ADD CONSTRAINT external_entity_pkey PRIMARY KEY (entity_acc);


--
-- Name: final_cluster_final_cluster_acc_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY final_cluster
    ADD CONSTRAINT final_cluster_final_cluster_acc_key UNIQUE (final_cluster_acc);


--
-- Name: final_cluster_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY final_cluster
    ADD CONSTRAINT final_cluster_pkey PRIMARY KEY (final_cluster_id);


--
-- Name: gene_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY gene
    ADD CONSTRAINT gene_pkey PRIMARY KEY (entity_id);


--
-- Name: geo_location_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY collection_location
    ADD CONSTRAINT geo_location_pkey PRIMARY KEY (location_id);


--
-- Name: go_term_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY go_term
    ADD CONSTRAINT go_term_pkey PRIMARY KEY (go_term_acc);


--
-- Name: hierarchy_node_data_file_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY hierarchy_node_data_file_link
    ADD CONSTRAINT hierarchy_node_data_file_link_pkey PRIMARY KEY (hierarchy_node_id, "position");


--
-- Name: hierarchy_node_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY hierarchy_node
    ADD CONSTRAINT hierarchy_node_pkey PRIMARY KEY (oid);


--
-- Name: hierarchy_node_to_children_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY hierarchy_node_to_children_link
    ADD CONSTRAINT hierarchy_node_to_children_link_pkey PRIMARY KEY (parent_id, "position");


--
-- Name: internal_entity_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY internal_entity
    ADD CONSTRAINT internal_entity_pkey PRIMARY KEY (entity_acc);


--
-- Name: library_key_library_acc; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY library
    ADD CONSTRAINT library_key_library_acc UNIQUE (library_acc);


--
-- Name: library_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY library
    ADD CONSTRAINT library_pkey PRIMARY KEY (library_id);


--
-- Name: library_sample_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY library_sample
    ADD CONSTRAINT library_sample_pkey PRIMARY KEY (library_id, sample_id);


--
-- Name: location_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "location"
    ADD CONSTRAINT location_pkey PRIMARY KEY (location_id);


--
-- Name: mate_category_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY mate_category
    ADD CONSTRAINT mate_category_pkey PRIMARY KEY (code);


--
-- Name: measurement_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY measurement
    ADD CONSTRAINT measurement_pkey PRIMARY KEY (experiment_id, measurement_no);


--
-- Name: multi_select_choices_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY multi_select_choices
    ADD CONSTRAINT multi_select_choices_pkey PRIMARY KEY (choice_id, choice_position);


--
-- Name: multi_select_values_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY multi_select_values
    ADD CONSTRAINT multi_select_values_pkey PRIMARY KEY (value_id, value_position);


--
-- Name: new_bio_sequence_pkey_20070321; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bio_sequence
    ADD CONSTRAINT new_bio_sequence_pkey_20070321 PRIMARY KEY (sequence_id);


--
-- Name: new_bse_key_entity_20070321; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bse
    ADD CONSTRAINT new_bse_key_entity_20070321 UNIQUE (entity_id);


--
-- Name: new_bse_pkey_20070321; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY bse
    ADD CONSTRAINT new_bse_pkey_20070321 PRIMARY KEY (camera_acc);


--
-- Name: node_data_type_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY node_data_type
    ADD CONSTRAINT node_data_type_pkey PRIMARY KEY (data_type);


--
-- Name: node_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY node
    ADD CONSTRAINT node_pkey PRIMARY KEY (node_id);


--
-- Name: observation_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY observation
    ADD CONSTRAINT observation_pkey PRIMARY KEY (experiment_id, observation_no);


--
-- Name: orf_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY orf
    ADD CONSTRAINT orf_pkey PRIMARY KEY (entity_id);


--
-- Name: organism_name_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY organism
    ADD CONSTRAINT organism_name_key UNIQUE (name);


--
-- Name: organism_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY organism
    ADD CONSTRAINT organism_pkey PRIMARY KEY (organism_id);


--
-- Name: parameter_vo_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY parameter_vo
    ADD CONSTRAINT parameter_vo_pkey PRIMARY KEY (oid);


--
-- Name: path_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY path
    ADD CONSTRAINT path_pkey PRIMARY KEY (oid);


--
-- Name: path_point_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY path_point
    ADD CONSTRAINT path_point_pkey PRIMARY KEY (oid);


--
-- Name: peptide_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY peptide
    ADD CONSTRAINT peptide_pkey PRIMARY KEY (entity_id);


--
-- Name: project_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY project
    ADD CONSTRAINT project_pkey PRIMARY KEY (symbol);


--
-- Name: project_publication_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY project_publication_link
    ADD CONSTRAINT project_publication_link_pkey PRIMARY KEY (project_id, "position");


--
-- Name: publication_author_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY publication_author_link
    ADD CONSTRAINT publication_author_link_pkey PRIMARY KEY (publication_id, "position");


--
-- Name: publication_combined_archives_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY publication_combined_archives_link
    ADD CONSTRAINT publication_combined_archives_link_pkey PRIMARY KEY (publication_id, "position");


--
-- Name: publication_hierarchy_node_link_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY publication_hierarchy_node_link
    ADD CONSTRAINT publication_hierarchy_node_link_pkey PRIMARY KEY (publication_id, "position");


--
-- Name: publication_key_publication_acc; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY publication
    ADD CONSTRAINT publication_key_publication_acc UNIQUE (publication_acc);


--
-- Name: publication_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY publication
    ADD CONSTRAINT publication_pkey PRIMARY KEY (oid);


--
-- Name: read_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY "read"
    ADD CONSTRAINT read_pkey PRIMARY KEY (entity_id);


--
-- Name: sample_comment_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY sample_comment
    ADD CONSTRAINT sample_comment_pkey PRIMARY KEY (sample_id, comment_no);


--
-- Name: sample_component_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY sample_component
    ADD CONSTRAINT sample_component_pkey PRIMARY KEY (combined_sample_id, component_sample_id);


--
-- Name: sample_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY sample
    ADD CONSTRAINT sample_pkey PRIMARY KEY (sample_id);


--
-- Name: sample_sample_acc_key; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY sample
    ADD CONSTRAINT sample_sample_acc_key UNIQUE (sample_acc);


--
-- Name: scaffold_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY scaffold
    ADD CONSTRAINT scaffold_pkey PRIMARY KEY (entity_id);


--
-- Name: sequence_type_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY sequence_type
    ADD CONSTRAINT sequence_type_pkey PRIMARY KEY (code);


--
-- Name: single_select_values_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY single_select_values
    ADD CONSTRAINT single_select_values_pkey PRIMARY KEY (value_id, "position");


--
-- Name: site_blast_node_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY site_blast_node
    ADD CONSTRAINT site_blast_node_pkey PRIMARY KEY (site_acc, node_id);


--
-- Name: site_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY site
    ADD CONSTRAINT site_pkey PRIMARY KEY (site_acc);


--
-- Name: site_sample_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY site_sample
    ADD CONSTRAINT site_sample_pkey PRIMARY KEY (site_acc, sample_id);


--
-- Name: task_event_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY task_event
    ADD CONSTRAINT task_event_pkey PRIMARY KEY (task_id, event_no);


--
-- Name: task_input_node_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY task_input_node
    ADD CONSTRAINT task_input_node_pkey PRIMARY KEY (task_id, node_id);


--
-- Name: task_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY task
    ADD CONSTRAINT task_pkey PRIMARY KEY (task_id);


--
-- Name: task_settings_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY task_settings
    ADD CONSTRAINT task_settings_pkey PRIMARY KEY (oid);


--
-- Name: template_parameters_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY template_parameters
    ADD CONSTRAINT template_parameters_pkey PRIMARY KEY (param_id, param_name);


--
-- Name: user_entity_pkey; Type: CONSTRAINT; Schema: camera; Owner: postgres; Tablespace: 
--

ALTER TABLE ONLY user_entity
    ADD CONSTRAINT user_entity_pkey PRIMARY KEY (entity_acc);


--
-- Name: alignment_fk_query; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX alignment_fk_query ON alignment USING btree (query_id);


--
-- Name: alignment_fk_subject; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX alignment_fk_subject ON alignment USING btree (subject_id);


--
-- Name: bio_material_fk_location; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX bio_material_fk_location ON bio_material USING btree (collection_location_id);


--
-- Name: bio_material_sample_fk_sample; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX bio_material_sample_fk_sample ON bio_material_sample USING btree (sample_id);


--
-- Name: chromosome_fk_organism; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX chromosome_fk_organism ON chromosome USING btree (organism_id);


--
-- Name: collection_location_fk_path; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX collection_location_fk_path ON collection_location USING btree (path_id);


--
-- Name: constant_type_ix_name; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX constant_type_ix_name ON constant_type USING btree (name);


--
-- Name: data_node_feature_fk_feature_entity; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX data_node_feature_fk_feature_entity ON data_node_feature USING btree (feature_id);


--
-- Name: experiment_fk_experiment_type; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX experiment_fk_experiment_type ON experiment USING btree (experiment_type_id);


--
-- Name: experiment_ix_path_id; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX experiment_ix_path_id ON experiment USING btree (path_id);


--
-- Name: gene_fk_annotation_set; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX gene_fk_annotation_set ON gene USING btree (annotation_id);


--
-- Name: library_sample_fk_sample; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX library_sample_fk_sample ON library_sample USING btree (sample_id);


--
-- Name: measurement_fk_instrument_type; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX measurement_fk_instrument_type ON measurement USING btree (instrument_type_id);


--
-- Name: measurement_fk_measurement_type; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX measurement_fk_measurement_type ON measurement USING btree (measurement_type_id);


--
-- Name: measurement_fk_unit_type; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX measurement_fk_unit_type ON measurement USING btree (unit_type_id);


--
-- Name: measurement_ix_value; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX measurement_ix_value ON measurement USING btree (value);


--
-- Name: new_bse_ix_owner_20070321; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX new_bse_ix_owner_20070321 ON bse USING btree (owner_id) WHERE (owner_id > 60);


--
-- Name: node_fk_task; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX node_fk_task ON node USING btree (task_id);


--
-- Name: node_fk_user; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX node_fk_user ON node USING btree (user_id);


--
-- Name: observation_ix_observation_date; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX observation_ix_observation_date ON observation USING btree (observation_date);


--
-- Name: observation_ix_observation_type_id; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX observation_ix_observation_type_id ON observation USING btree (observation_type_id);


--
-- Name: orf_fk_peptide; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX orf_fk_peptide ON orf USING btree (peptide_id);


--
-- Name: path_point_ix_location_id; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX path_point_ix_location_id ON path_point USING btree (location_id);


--
-- Name: path_point_ix_path; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX path_point_ix_path ON path_point USING btree (path_id);


--
-- Name: peptide_fk_annotation_set; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX peptide_fk_annotation_set ON peptide USING btree (annotation_id);


--
-- Name: read_fk_library; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX read_fk_library ON "read" USING btree (library_id);


--
-- Name: read_ix_template_mate; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX read_ix_template_mate ON "read" USING btree (template_acc, sequencing_direction);


--
-- Name: sample_component_fk_component_sample; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX sample_component_fk_component_sample ON sample_component USING btree (component_sample_id);


--
-- Name: sample_ix_experiment; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX sample_ix_experiment ON sample USING btree (experiment_acc);


--
-- Name: scaffold_fk_assembly; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX scaffold_fk_assembly ON scaffold USING btree (assembly_id);


--
-- Name: site_blast_node_fk_node; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX site_blast_node_fk_node ON site_blast_node USING btree (node_id);


--
-- Name: site_sample_ix_ss; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX site_sample_ix_ss ON site_sample USING btree (sample_id, site_acc);


--
-- Name: task_event_fk_task; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX task_event_fk_task ON task_event USING btree (task_id);


--
-- Name: task_fk_user; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX task_fk_user ON task USING btree (user_id);


--
-- Name: task_input_node_fk_node; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX task_input_node_fk_node ON task_input_node USING btree (node_id);


--
-- Name: user_entity_ix_owner; Type: INDEX; Schema: camera; Owner: postgres; Tablespace: 
--

CREATE INDEX user_entity_ix_owner ON user_entity USING btree (owner_id);


--
-- Name: trig_new_bio_sequence_iu_20070321; Type: TRIGGER; Schema: camera; Owner: postgres
--

CREATE TRIGGER trig_new_bio_sequence_iu_20070321
    BEFORE INSERT OR UPDATE ON bio_sequence
    FOR EACH ROW
    EXECUTE PROCEDURE fn_trig_bio_sequence_iu();


--
-- Name: alignment_fk_alignment_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY alignment
    ADD CONSTRAINT alignment_fk_alignment_type FOREIGN KEY (alignment_type_code) REFERENCES alignment_type(code);


--
-- Name: alignment_fk_mate_category; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY alignment
    ADD CONSTRAINT alignment_fk_mate_category FOREIGN KEY (mate_category_code) REFERENCES mate_category(code);


--
-- Name: alignment_fk_result_node; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY alignment
    ADD CONSTRAINT alignment_fk_result_node FOREIGN KEY (result_node_id) REFERENCES node(node_id);


--
-- Name: bio_material_fk_location; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY bio_material
    ADD CONSTRAINT bio_material_fk_location FOREIGN KEY (collection_location_id) REFERENCES collection_location(location_id);


--
-- Name: bio_material_sample_fk_material; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY bio_material_sample
    ADD CONSTRAINT bio_material_sample_fk_material FOREIGN KEY (material_id) REFERENCES bio_material(material_id);


--
-- Name: bio_material_sample_fk_sample; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY bio_material_sample
    ADD CONSTRAINT bio_material_sample_fk_sample FOREIGN KEY (sample_id) REFERENCES bio_sample(sample_id);


--
-- Name: chromosome_fk_organism; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY chromosome
    ADD CONSTRAINT chromosome_fk_organism FOREIGN KEY (organism_id) REFERENCES organism(organism_id);


--
-- Name: collection_location_fk_path; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY collection_location
    ADD CONSTRAINT collection_location_fk_path FOREIGN KEY (path_id) REFERENCES collection_location(location_id);


--
-- Name: collection_observation_fk_bio_material; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY collection_observation
    ADD CONSTRAINT collection_observation_fk_bio_material FOREIGN KEY (material_id) REFERENCES bio_material(material_id);


--
-- Name: data_node_feature_fk_node; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY data_node_feature
    ADD CONSTRAINT data_node_feature_fk_node FOREIGN KEY (node_id) REFERENCES node(node_id);


--
-- Name: dataset_fk_data_source; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY annotation_set
    ADD CONSTRAINT dataset_fk_data_source FOREIGN KEY (data_source_id) REFERENCES data_source(source_id);


--
-- Name: dataset_fk_entity_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY annotation_set
    ADD CONSTRAINT dataset_fk_entity_type FOREIGN KEY (entity_type) REFERENCES entity_type(code);


--
-- Name: entity_type_fk_sequence_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY entity_type
    ADD CONSTRAINT entity_type_fk_sequence_type FOREIGN KEY (sequence_type) REFERENCES sequence_type(code);


--
-- Name: experiment_fk_experiment_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY experiment
    ADD CONSTRAINT experiment_fk_experiment_type FOREIGN KEY (experiment_type_id) REFERENCES constant_type(oid);


--
-- Name: experiment_fk_path; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY experiment
    ADD CONSTRAINT experiment_fk_path FOREIGN KEY (path_id) REFERENCES path(oid);


--
-- Name: experiment_id_fk; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY observation
    ADD CONSTRAINT experiment_id_fk FOREIGN KEY (experiment_id) REFERENCES experiment(experiment_id);


--
-- Name: final_cluster_go_term_fk_final_cluster; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY final_cluster_go_term
    ADD CONSTRAINT final_cluster_go_term_fk_final_cluster FOREIGN KEY (final_cluster_id) REFERENCES final_cluster(final_cluster_id) ON DELETE CASCADE;


--
-- Name: final_cluster_go_term_fk_go_term; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY final_cluster_go_term
    ADD CONSTRAINT final_cluster_go_term_fk_go_term FOREIGN KEY (go_acc) REFERENCES go_term(go_term_acc);


--
-- Name: final_cluster_id_fk; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY core_cluster
    ADD CONSTRAINT final_cluster_id_fk FOREIGN KEY (final_cluster_id) REFERENCES final_cluster(final_cluster_id);


--
-- Name: fk12cd9bfa1d1c2e4a; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY publication_hierarchy_node_link
    ADD CONSTRAINT fk12cd9bfa1d1c2e4a FOREIGN KEY (publication_id) REFERENCES publication(oid);


--
-- Name: fk12cd9bfa2a5fea55; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY publication_hierarchy_node_link
    ADD CONSTRAINT fk12cd9bfa2a5fea55 FOREIGN KEY (hierarchy_node_id) REFERENCES hierarchy_node(oid);


--
-- Name: fk4a0ba5897d26e105; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY hierarchy_node_to_children_link
    ADD CONSTRAINT fk4a0ba5897d26e105 FOREIGN KEY (child_id) REFERENCES hierarchy_node(oid);


--
-- Name: fk4a0ba58995a37ab7; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY hierarchy_node_to_children_link
    ADD CONSTRAINT fk4a0ba58995a37ab7 FOREIGN KEY (parent_id) REFERENCES hierarchy_node(oid);


--
-- Name: fk4e0163214638a6e3; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY data_file_sample_link
    ADD CONSTRAINT fk4e0163214638a6e3 FOREIGN KEY (sample_id) REFERENCES sample(sample_id);


--
-- Name: fk4e0163215a85795f; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY data_file_sample_link
    ADD CONSTRAINT fk4e0163215a85795f FOREIGN KEY (data_file_id) REFERENCES data_file(oid);


--
-- Name: fk61b3ce931d1c2e4a; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY project_publication_link
    ADD CONSTRAINT fk61b3ce931d1c2e4a FOREIGN KEY (publication_id) REFERENCES publication(oid);


--
-- Name: fk8a82a13f88e05cb8; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY multi_select_values
    ADD CONSTRAINT fk8a82a13f88e05cb8 FOREIGN KEY (value_id) REFERENCES parameter_vo(oid);


--
-- Name: fk94c5036c2250492e; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY core_cluster_peptide
    ADD CONSTRAINT fk94c5036c2250492e FOREIGN KEY (core_cluster_id) REFERENCES core_cluster(core_cluster_id);


--
-- Name: fk95ed5b411d1c2e4a; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY publication_combined_archives_link
    ADD CONSTRAINT fk95ed5b411d1c2e4a FOREIGN KEY (publication_id) REFERENCES publication(oid);


--
-- Name: fk95ed5b415a85795f; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY publication_combined_archives_link
    ADD CONSTRAINT fk95ed5b415a85795f FOREIGN KEY (data_file_id) REFERENCES data_file(oid);


--
-- Name: fkdbd10d5b2a5fea55; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY hierarchy_node_data_file_link
    ADD CONSTRAINT fkdbd10d5b2a5fea55 FOREIGN KEY (hierarchy_node_id) REFERENCES hierarchy_node(oid);


--
-- Name: fkdbd10d5b5a85795f; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY hierarchy_node_data_file_link
    ADD CONSTRAINT fkdbd10d5b5a85795f FOREIGN KEY (data_file_id) REFERENCES data_file(oid);


--
-- Name: fke4d1d675f81e288; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY multi_select_choices
    ADD CONSTRAINT fke4d1d675f81e288 FOREIGN KEY (choice_id) REFERENCES parameter_vo(oid);


--
-- Name: fked48a49b1d1c2e4a; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY publication_author_link
    ADD CONSTRAINT fked48a49b1d1c2e4a FOREIGN KEY (publication_id) REFERENCES publication(oid);


--
-- Name: fked48a49b5175bbca; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY publication_author_link
    ADD CONSTRAINT fked48a49b5175bbca FOREIGN KEY (author_id) REFERENCES author(name);


--
-- Name: fkfa414a0e9f2372e5; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY single_select_values
    ADD CONSTRAINT fkfa414a0e9f2372e5 FOREIGN KEY (value_id) REFERENCES parameter_vo(oid);


--
-- Name: gene_fk_annotation_set; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY gene
    ADD CONSTRAINT gene_fk_annotation_set FOREIGN KEY (annotation_id) REFERENCES annotation_set(annotation_id);


--
-- Name: library_sample_fk_library; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY library_sample
    ADD CONSTRAINT library_sample_fk_library FOREIGN KEY (library_id) REFERENCES library(library_id);


--
-- Name: library_sample_fk_sample; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY library_sample
    ADD CONSTRAINT library_sample_fk_sample FOREIGN KEY (sample_id) REFERENCES bio_sample(sample_id);


--
-- Name: location_id_fk; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY path_point
    ADD CONSTRAINT location_id_fk FOREIGN KEY (location_id) REFERENCES "location"(location_id);


--
-- Name: measurement_fk_experiment; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY measurement
    ADD CONSTRAINT measurement_fk_experiment FOREIGN KEY (experiment_id) REFERENCES experiment(experiment_id);


--
-- Name: measurement_fk_instrument_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY measurement
    ADD CONSTRAINT measurement_fk_instrument_type FOREIGN KEY (instrument_type_id) REFERENCES constant_type(oid);


--
-- Name: measurement_fk_measurement_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY measurement
    ADD CONSTRAINT measurement_fk_measurement_type FOREIGN KEY (measurement_type_id) REFERENCES constant_type(oid);


--
-- Name: measurement_fk_unit_type; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY measurement
    ADD CONSTRAINT measurement_fk_unit_type FOREIGN KEY (unit_type_id) REFERENCES constant_type(oid);


--
-- Name: node_fk_data_source; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY node
    ADD CONSTRAINT node_fk_data_source FOREIGN KEY (data_source_id) REFERENCES data_source(source_id);


--
-- Name: node_fk_task; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY node
    ADD CONSTRAINT node_fk_task FOREIGN KEY (task_id) REFERENCES task(task_id);


--
-- Name: node_fk_user; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY node
    ADD CONSTRAINT node_fk_user FOREIGN KEY (user_id) REFERENCES camera_user(user_id) ON DELETE CASCADE;


--
-- Name: observation_type_id_fk; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY observation
    ADD CONSTRAINT observation_type_id_fk FOREIGN KEY (observation_type_id) REFERENCES constant_type(oid);


--
-- Name: orf_fk_annotation_set; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY orf
    ADD CONSTRAINT orf_fk_annotation_set FOREIGN KEY (annotation_id) REFERENCES annotation_set(annotation_id);


--
-- Name: path_id_fk; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY path_point
    ADD CONSTRAINT path_id_fk FOREIGN KEY (path_id) REFERENCES path(oid);


--
-- Name: peptide_fk_annotation_set; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY peptide
    ADD CONSTRAINT peptide_fk_annotation_set FOREIGN KEY (annotation_id) REFERENCES annotation_set(annotation_id);


--
-- Name: project_publication_link_fk_project; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY project_publication_link
    ADD CONSTRAINT project_publication_link_fk_project FOREIGN KEY (project_id) REFERENCES project(symbol);


--
-- Name: read_fk_library; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY "read"
    ADD CONSTRAINT read_fk_library FOREIGN KEY (library_id) REFERENCES library(library_id);


--
-- Name: sample_comment_fk_sample; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY sample_comment
    ADD CONSTRAINT sample_comment_fk_sample FOREIGN KEY (sample_id) REFERENCES sample(sample_id);


--
-- Name: sample_component_fk_combined_sample; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY sample_component
    ADD CONSTRAINT sample_component_fk_combined_sample FOREIGN KEY (combined_sample_id) REFERENCES sample(sample_id);


--
-- Name: sample_component_fk_component_sample; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY sample_component
    ADD CONSTRAINT sample_component_fk_component_sample FOREIGN KEY (component_sample_id) REFERENCES sample(sample_id);


--
-- Name: scaffold_fk_assembly; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY scaffold
    ADD CONSTRAINT scaffold_fk_assembly FOREIGN KEY (assembly_id) REFERENCES assembly(assembly_id);


--
-- Name: site_blast_node_fk_node; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY site_blast_node
    ADD CONSTRAINT site_blast_node_fk_node FOREIGN KEY (node_id) REFERENCES node(node_id);


--
-- Name: site_blast_node_fk_site; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY site_blast_node
    ADD CONSTRAINT site_blast_node_fk_site FOREIGN KEY (site_acc) REFERENCES site(site_acc);


--
-- Name: site_sample_fk_sample; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY site_sample
    ADD CONSTRAINT site_sample_fk_sample FOREIGN KEY (sample_id) REFERENCES sample(sample_id);


--
-- Name: site_sample_fk_site; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY site_sample
    ADD CONSTRAINT site_sample_fk_site FOREIGN KEY (site_acc) REFERENCES site(site_acc);


--
-- Name: task_event_fk_task; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY task_event
    ADD CONSTRAINT task_event_fk_task FOREIGN KEY (task_id) REFERENCES task(task_id);


--
-- Name: task_fk_user; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY task
    ADD CONSTRAINT task_fk_user FOREIGN KEY (user_id) REFERENCES camera_user(user_id);


--
-- Name: task_input_node_fk_node; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY task_input_node
    ADD CONSTRAINT task_input_node_fk_node FOREIGN KEY (node_id) REFERENCES node(node_id);


--
-- Name: task_input_node_fk_task; Type: FK CONSTRAINT; Schema: camera; Owner: postgres
--

ALTER TABLE ONLY task_input_node
    ADD CONSTRAINT task_input_node_fk_task FOREIGN KEY (task_id) REFERENCES task(task_id);


--
-- Name: camera; Type: ACL; Schema: -; Owner: camapp
--

REVOKE ALL ON SCHEMA camera FROM PUBLIC;
REVOKE ALL ON SCHEMA camera FROM camapp;
GRANT ALL ON SCHEMA camera TO camapp;


--
-- Name: degree_to_num(character varying); Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON FUNCTION degree_to_num(p_string character varying) FROM PUBLIC;
REVOKE ALL ON FUNCTION degree_to_num(p_string character varying) FROM postgres;
GRANT ALL ON FUNCTION degree_to_num(p_string character varying) TO postgres;
GRANT ALL ON FUNCTION degree_to_num(p_string character varying) TO PUBLIC;
GRANT ALL ON FUNCTION degree_to_num(p_string character varying) TO camapp;


--
-- Name: getsubjectnode(character varying); Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON FUNCTION getsubjectnode(p_parameters character varying) FROM PUBLIC;
REVOKE ALL ON FUNCTION getsubjectnode(p_parameters character varying) FROM postgres;
GRANT ALL ON FUNCTION getsubjectnode(p_parameters character varying) TO postgres;
GRANT ALL ON FUNCTION getsubjectnode(p_parameters character varying) TO PUBLIC;
GRANT ALL ON FUNCTION getsubjectnode(p_parameters character varying) TO camapp;


--
-- Name: timeouttasks(); Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON FUNCTION timeouttasks() FROM PUBLIC;
REVOKE ALL ON FUNCTION timeouttasks() FROM postgres;
GRANT ALL ON FUNCTION timeouttasks() TO postgres;
GRANT ALL ON FUNCTION timeouttasks() TO PUBLIC;


--
-- Name: aa_sequence; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE aa_sequence FROM PUBLIC;
REVOKE ALL ON TABLE aa_sequence FROM postgres;
GRANT ALL ON TABLE aa_sequence TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE aa_sequence TO camapp;


--
-- Name: alignment; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE alignment FROM PUBLIC;
REVOKE ALL ON TABLE alignment FROM postgres;
GRANT ALL ON TABLE alignment TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE alignment TO camapp;


--
-- Name: alignment_type; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE alignment_type FROM PUBLIC;
REVOKE ALL ON TABLE alignment_type FROM postgres;
GRANT ALL ON TABLE alignment_type TO postgres;
GRANT SELECT ON TABLE alignment_type TO camapp;


--
-- Name: annotation_set; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE annotation_set FROM PUBLIC;
REVOKE ALL ON TABLE annotation_set FROM postgres;
GRANT ALL ON TABLE annotation_set TO postgres;
GRANT SELECT ON TABLE annotation_set TO camapp;


--
-- Name: assembly; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE assembly FROM PUBLIC;
REVOKE ALL ON TABLE assembly FROM postgres;
GRANT ALL ON TABLE assembly TO postgres;
GRANT SELECT ON TABLE assembly TO camapp;


--
-- Name: author; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE author FROM PUBLIC;
REVOKE ALL ON TABLE author FROM postgres;
GRANT ALL ON TABLE author TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE author TO camapp;


--
-- Name: bio_material; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE bio_material FROM PUBLIC;
REVOKE ALL ON TABLE bio_material FROM postgres;
GRANT ALL ON TABLE bio_material TO postgres;


--
-- Name: bio_material_sample; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE bio_material_sample FROM PUBLIC;
REVOKE ALL ON TABLE bio_material_sample FROM postgres;
GRANT ALL ON TABLE bio_material_sample TO postgres;
GRANT SELECT ON TABLE bio_material_sample TO camapp;


--
-- Name: bio_sequence; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE bio_sequence FROM PUBLIC;
REVOKE ALL ON TABLE bio_sequence FROM postgres;
GRANT ALL ON TABLE bio_sequence TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE bio_sequence TO camapp;


--
-- Name: blast_hit; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE blast_hit FROM PUBLIC;
REVOKE ALL ON TABLE blast_hit FROM postgres;
GRANT ALL ON TABLE blast_hit TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE blast_hit TO camapp;


--
-- Name: blast_result_node_defline_map; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE blast_result_node_defline_map FROM PUBLIC;
REVOKE ALL ON TABLE blast_result_node_defline_map FROM postgres;
GRANT ALL ON TABLE blast_result_node_defline_map TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE blast_result_node_defline_map TO camapp;


--
-- Name: bse; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE bse FROM PUBLIC;
REVOKE ALL ON TABLE bse FROM postgres;
GRANT ALL ON TABLE bse TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE bse TO camapp;


--
-- Name: camera_user; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE camera_user FROM PUBLIC;
REVOKE ALL ON TABLE camera_user FROM postgres;
GRANT ALL ON TABLE camera_user TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE camera_user TO camapp;


--
-- Name: chromosome; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE chromosome FROM PUBLIC;
REVOKE ALL ON TABLE chromosome FROM postgres;
GRANT ALL ON TABLE chromosome TO postgres;
GRANT SELECT ON TABLE chromosome TO camapp;


--
-- Name: collection_location; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE collection_location FROM PUBLIC;
REVOKE ALL ON TABLE collection_location FROM postgres;
GRANT ALL ON TABLE collection_location TO postgres;


--
-- Name: collection_observation; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE collection_observation FROM PUBLIC;
REVOKE ALL ON TABLE collection_observation FROM postgres;
GRANT ALL ON TABLE collection_observation TO postgres;


--
-- Name: constant_type; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE constant_type FROM PUBLIC;
REVOKE ALL ON TABLE constant_type FROM postgres;
GRANT ALL ON TABLE constant_type TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE constant_type TO camapp;


--
-- Name: core_cluster; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE core_cluster FROM PUBLIC;
REVOKE ALL ON TABLE core_cluster FROM postgres;
GRANT ALL ON TABLE core_cluster TO postgres;
GRANT SELECT ON TABLE core_cluster TO camapp;


--
-- Name: core_cluster_peptide; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE core_cluster_peptide FROM PUBLIC;
REVOKE ALL ON TABLE core_cluster_peptide FROM postgres;
GRANT ALL ON TABLE core_cluster_peptide TO postgres;
GRANT SELECT ON TABLE core_cluster_peptide TO camapp;


--
-- Name: data_file; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE data_file FROM PUBLIC;
REVOKE ALL ON TABLE data_file FROM postgres;
GRANT ALL ON TABLE data_file TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE data_file TO camapp;


--
-- Name: data_file_sample_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE data_file_sample_link FROM PUBLIC;
REVOKE ALL ON TABLE data_file_sample_link FROM postgres;
GRANT ALL ON TABLE data_file_sample_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE data_file_sample_link TO camapp;


--
-- Name: data_node_feature; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE data_node_feature FROM PUBLIC;
REVOKE ALL ON TABLE data_node_feature FROM postgres;
GRANT ALL ON TABLE data_node_feature TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE data_node_feature TO camapp;


--
-- Name: data_source; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE data_source FROM PUBLIC;
REVOKE ALL ON TABLE data_source FROM postgres;
GRANT ALL ON TABLE data_source TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE data_source TO camapp;


--
-- Name: entity_type; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE entity_type FROM PUBLIC;
REVOKE ALL ON TABLE entity_type FROM postgres;
GRANT ALL ON TABLE entity_type TO postgres;
GRANT SELECT ON TABLE entity_type TO camapp;


--
-- Name: experiment; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE experiment FROM PUBLIC;
REVOKE ALL ON TABLE experiment FROM postgres;
GRANT ALL ON TABLE experiment TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE experiment TO camapp;


--
-- Name: seq_entity; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE seq_entity FROM PUBLIC;
REVOKE ALL ON TABLE seq_entity FROM postgres;
GRANT ALL ON TABLE seq_entity TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE seq_entity TO camapp;


--
-- Name: external_entity; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE external_entity FROM PUBLIC;
REVOKE ALL ON TABLE external_entity FROM postgres;
GRANT ALL ON TABLE external_entity TO postgres;
GRANT SELECT ON TABLE external_entity TO camapp;


--
-- Name: final_cluster; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE final_cluster FROM PUBLIC;
REVOKE ALL ON TABLE final_cluster FROM postgres;
GRANT ALL ON TABLE final_cluster TO postgres;
GRANT SELECT ON TABLE final_cluster TO camapp;


--
-- Name: final_cluster_go_term; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE final_cluster_go_term FROM PUBLIC;
REVOKE ALL ON TABLE final_cluster_go_term FROM postgres;
GRANT ALL ON TABLE final_cluster_go_term TO postgres;
GRANT SELECT ON TABLE final_cluster_go_term TO camapp;


--
-- Name: gene; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE gene FROM PUBLIC;
REVOKE ALL ON TABLE gene FROM postgres;
GRANT ALL ON TABLE gene TO postgres;
GRANT SELECT ON TABLE gene TO camapp;


--
-- Name: go_term; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE go_term FROM PUBLIC;
REVOKE ALL ON TABLE go_term FROM postgres;
GRANT ALL ON TABLE go_term TO postgres;
GRANT SELECT ON TABLE go_term TO camapp;


--
-- Name: hierarchy_node; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE hierarchy_node FROM PUBLIC;
REVOKE ALL ON TABLE hierarchy_node FROM postgres;
GRANT ALL ON TABLE hierarchy_node TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE hierarchy_node TO camapp;


--
-- Name: hierarchy_node_data_file_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE hierarchy_node_data_file_link FROM PUBLIC;
REVOKE ALL ON TABLE hierarchy_node_data_file_link FROM postgres;
GRANT ALL ON TABLE hierarchy_node_data_file_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE hierarchy_node_data_file_link TO camapp;


--
-- Name: hierarchy_node_to_children_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE hierarchy_node_to_children_link FROM PUBLIC;
REVOKE ALL ON TABLE hierarchy_node_to_children_link FROM postgres;
GRANT ALL ON TABLE hierarchy_node_to_children_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE hierarchy_node_to_children_link TO camapp;


--
-- Name: internal_entity; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE internal_entity FROM PUBLIC;
REVOKE ALL ON TABLE internal_entity FROM postgres;
GRANT ALL ON TABLE internal_entity TO postgres;
GRANT SELECT ON TABLE internal_entity TO camapp;


--
-- Name: library; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE library FROM PUBLIC;
REVOKE ALL ON TABLE library FROM postgres;
GRANT ALL ON TABLE library TO postgres;
GRANT SELECT ON TABLE library TO camapp;


--
-- Name: library_sample; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE library_sample FROM PUBLIC;
REVOKE ALL ON TABLE library_sample FROM postgres;
GRANT ALL ON TABLE library_sample TO postgres;
GRANT SELECT ON TABLE library_sample TO camapp;


--
-- Name: location; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE "location" FROM PUBLIC;
REVOKE ALL ON TABLE "location" FROM postgres;
GRANT ALL ON TABLE "location" TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE "location" TO camapp;


--
-- Name: mate_category; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE mate_category FROM PUBLIC;
REVOKE ALL ON TABLE mate_category FROM postgres;
GRANT ALL ON TABLE mate_category TO postgres;
GRANT SELECT ON TABLE mate_category TO camapp;


--
-- Name: measurement; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE measurement FROM PUBLIC;
REVOKE ALL ON TABLE measurement FROM postgres;
GRANT ALL ON TABLE measurement TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE measurement TO camapp;


--
-- Name: multi_select_choices; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE multi_select_choices FROM PUBLIC;
REVOKE ALL ON TABLE multi_select_choices FROM postgres;
GRANT ALL ON TABLE multi_select_choices TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE multi_select_choices TO camapp;


--
-- Name: multi_select_values; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE multi_select_values FROM PUBLIC;
REVOKE ALL ON TABLE multi_select_values FROM postgres;
GRANT ALL ON TABLE multi_select_values TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE multi_select_values TO camapp;


--
-- Name: node; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE node FROM PUBLIC;
REVOKE ALL ON TABLE node FROM postgres;
GRANT ALL ON TABLE node TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE node TO camapp;


--
-- Name: node_data_type; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE node_data_type FROM PUBLIC;
REVOKE ALL ON TABLE node_data_type FROM postgres;
GRANT ALL ON TABLE node_data_type TO postgres;
GRANT SELECT ON TABLE node_data_type TO camapp;


--
-- Name: observation; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE observation FROM PUBLIC;
REVOKE ALL ON TABLE observation FROM postgres;
GRANT ALL ON TABLE observation TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE observation TO camapp;


--
-- Name: orf; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE orf FROM PUBLIC;
REVOKE ALL ON TABLE orf FROM postgres;
GRANT ALL ON TABLE orf TO postgres;
GRANT SELECT ON TABLE orf TO camapp;


--
-- Name: organism; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE organism FROM PUBLIC;
REVOKE ALL ON TABLE organism FROM postgres;
GRANT ALL ON TABLE organism TO postgres;
GRANT SELECT ON TABLE organism TO camapp;


--
-- Name: parameter_vo; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE parameter_vo FROM PUBLIC;
REVOKE ALL ON TABLE parameter_vo FROM postgres;
GRANT ALL ON TABLE parameter_vo TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE parameter_vo TO camapp;


--
-- Name: path; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE path FROM PUBLIC;
REVOKE ALL ON TABLE path FROM postgres;
GRANT ALL ON TABLE path TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE path TO camapp;


--
-- Name: path_point; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE path_point FROM PUBLIC;
REVOKE ALL ON TABLE path_point FROM postgres;
GRANT ALL ON TABLE path_point TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE path_point TO camapp;


--
-- Name: peptide; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE peptide FROM PUBLIC;
REVOKE ALL ON TABLE peptide FROM postgres;
GRANT ALL ON TABLE peptide TO postgres;
GRANT SELECT ON TABLE peptide TO camapp;


--
-- Name: project; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE project FROM PUBLIC;
REVOKE ALL ON TABLE project FROM postgres;
GRANT ALL ON TABLE project TO postgres;
GRANT ALL ON TABLE project TO camapp;


--
-- Name: project_publication_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE project_publication_link FROM PUBLIC;
REVOKE ALL ON TABLE project_publication_link FROM postgres;
GRANT ALL ON TABLE project_publication_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE project_publication_link TO camapp;


--
-- Name: publication; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE publication FROM PUBLIC;
REVOKE ALL ON TABLE publication FROM postgres;
GRANT ALL ON TABLE publication TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE publication TO camapp;


--
-- Name: publication_author_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE publication_author_link FROM PUBLIC;
REVOKE ALL ON TABLE publication_author_link FROM postgres;
GRANT ALL ON TABLE publication_author_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE publication_author_link TO camapp;


--
-- Name: publication_combined_archives_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE publication_combined_archives_link FROM PUBLIC;
REVOKE ALL ON TABLE publication_combined_archives_link FROM postgres;
GRANT ALL ON TABLE publication_combined_archives_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE publication_combined_archives_link TO camapp;


--
-- Name: publication_hierarchy_node_link; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE publication_hierarchy_node_link FROM PUBLIC;
REVOKE ALL ON TABLE publication_hierarchy_node_link FROM postgres;
GRANT ALL ON TABLE publication_hierarchy_node_link TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE publication_hierarchy_node_link TO camapp;


--
-- Name: read; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE "read" FROM PUBLIC;
REVOKE ALL ON TABLE "read" FROM postgres;
GRANT ALL ON TABLE "read" TO postgres;
GRANT SELECT ON TABLE "read" TO camapp;


--
-- Name: read_entity; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE read_entity FROM PUBLIC;
REVOKE ALL ON TABLE read_entity FROM postgres;
GRANT ALL ON TABLE read_entity TO postgres;
GRANT SELECT ON TABLE read_entity TO camapp;


--
-- Name: sample; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE sample FROM PUBLIC;
REVOKE ALL ON TABLE sample FROM postgres;
GRANT ALL ON TABLE sample TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE sample TO camapp;


--
-- Name: sample_comment; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE sample_comment FROM PUBLIC;
REVOKE ALL ON TABLE sample_comment FROM postgres;
GRANT ALL ON TABLE sample_comment TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE sample_comment TO camapp;


--
-- Name: sample_component; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE sample_component FROM PUBLIC;
REVOKE ALL ON TABLE sample_component FROM postgres;
GRANT ALL ON TABLE sample_component TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE sample_component TO camapp;


--
-- Name: scaffold; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE scaffold FROM PUBLIC;
REVOKE ALL ON TABLE scaffold FROM postgres;
GRANT ALL ON TABLE scaffold TO postgres;
GRANT SELECT ON TABLE scaffold TO camapp;


--
-- Name: sequence_type; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE sequence_type FROM PUBLIC;
REVOKE ALL ON TABLE sequence_type FROM postgres;
GRANT ALL ON TABLE sequence_type TO postgres;
GRANT SELECT ON TABLE sequence_type TO camapp;


--
-- Name: single_select_values; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE single_select_values FROM PUBLIC;
REVOKE ALL ON TABLE single_select_values FROM postgres;
GRANT ALL ON TABLE single_select_values TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE single_select_values TO camapp;


--
-- Name: site; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE site FROM PUBLIC;
REVOKE ALL ON TABLE site FROM postgres;
GRANT ALL ON TABLE site TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE site TO camapp;


--
-- Name: site_blast_node; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE site_blast_node FROM PUBLIC;
REVOKE ALL ON TABLE site_blast_node FROM postgres;
GRANT ALL ON TABLE site_blast_node TO postgres;
GRANT INSERT,SELECT ON TABLE site_blast_node TO camapp;


--
-- Name: site_sample; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE site_sample FROM PUBLIC;
REVOKE ALL ON TABLE site_sample FROM postgres;
GRANT ALL ON TABLE site_sample TO postgres;
GRANT SELECT ON TABLE site_sample TO camapp;


--
-- Name: task; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE task FROM PUBLIC;
REVOKE ALL ON TABLE task FROM postgres;
GRANT ALL ON TABLE task TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE task TO camapp;


--
-- Name: task_event; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE task_event FROM PUBLIC;
REVOKE ALL ON TABLE task_event FROM postgres;
GRANT ALL ON TABLE task_event TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE task_event TO camapp;


--
-- Name: task_input_node; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE task_input_node FROM PUBLIC;
REVOKE ALL ON TABLE task_input_node FROM postgres;
GRANT ALL ON TABLE task_input_node TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE task_input_node TO camapp;


--
-- Name: task_monitor; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE task_monitor FROM PUBLIC;
REVOKE ALL ON TABLE task_monitor FROM postgres;
GRANT ALL ON TABLE task_monitor TO postgres;
GRANT SELECT ON TABLE task_monitor TO camapp;


--
-- Name: task_settings; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE task_settings FROM PUBLIC;
REVOKE ALL ON TABLE task_settings FROM postgres;
GRANT ALL ON TABLE task_settings TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE task_settings TO camapp;


--
-- Name: task_status; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE task_status FROM PUBLIC;
REVOKE ALL ON TABLE task_status FROM postgres;
GRANT ALL ON TABLE task_status TO postgres;
GRANT SELECT ON TABLE task_status TO camapp;


--
-- Name: template_parameters; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE template_parameters FROM PUBLIC;
REVOKE ALL ON TABLE template_parameters FROM postgres;
GRANT ALL ON TABLE template_parameters TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE template_parameters TO camapp;


--
-- Name: user_entity; Type: ACL; Schema: camera; Owner: postgres
--

REVOKE ALL ON TABLE user_entity FROM PUBLIC;
REVOKE ALL ON TABLE user_entity FROM postgres;
GRANT ALL ON TABLE user_entity TO postgres;
GRANT INSERT,SELECT,UPDATE,DELETE ON TABLE user_entity TO camapp;


--
-- PostgreSQL database dump complete
--

