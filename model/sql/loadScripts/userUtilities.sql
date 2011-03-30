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

create or replace function camera.clearUser(pUserLogin text, dropUser boolean) returns boolean as $$
declare
  userId bigint;
begin
  select user_id into userId from camera.camera_user where user_login=pUserLogin;

-- blast hits
  delete from camera.blast_result_node_defline_map where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.blast_hit where result_node_id in (select node_id from camera.node where user_id=userId);
-- nodes
  delete from camera.task_input_node where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.data_node_feature where node_id in (select node_id from camera.node where user_id=userId);
--  begin
--    delete from camera.node_ts_result where node_id in (select node_id from camera.node where user_id=userId);
--  exception when others then
--    null;
--  end;
  delete from camera.accession_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.website_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.project_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.publication_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.sample_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.final_cluster_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.protein_ts_result where node_id in (select node_id from camera.node where user_id=userId);
  delete from camera.node where node_id in (select node_id from camera.node where user_id=userId);
-- tasks
  delete from camera.accounting where task_id in (select task_id from camera.task where task_owner=pUserLogin);
  delete from camera.task_input_node where task_id in (select task_id from camera.task where task_owner=pUserLogin);
  delete from camera.task_parameter where task_id in (select task_id from camera.task where task_owner=pUserLogin);
  delete from camera.task_event where task_id in (select task_id from camera.task where task_owner=pUserLogin);
  delete from camera.task_message where task_id in (select task_id from camera.task where task_owner=pUserLogin);
  delete from camera.task where task_owner=pUserLogin;
-- sequences
  delete from camera.bio_sequence where sequence_id in (select sequence_id from camera.sequence_entity where owner_id=userId and owner_id<>60);
  delete from camera.sequence_entity where owner_id=userId and owner_id<>60;
-- account/preferences
  if dropUser then
    begin
      delete from camera.user_preference_map where user_id=userId;
    exception when others then
      null; 
    end;
    delete from camera.camera_user where user_id=userId;
  end if;
  return true;
-- error handling
exception
  when others then
    raise notice 'sql error';
    return false;
end $$ language plpgsql;

create or replace function camera.clearAllUsers(dropUser boolean) returns integer as $$
declare
  n integer := 0;
  userRec record;
  clearOK boolean;
begin
  for userRec in select * from camera.camera_user where user_id<>60
  loop
    select clearUser(userRec.user_login,dropUser) into clearOK;
    if clearOK then
      n := n + 1;
    else
      raise notice 'clear failed';
      return 0;
    end if;
  end loop;
  return n;
exception when others then
  raise notice 'sql error!';
  return 0;
end $$ language plpgsql;

create or replace function camera.stageUserData() returns boolean as $$
declare
begin
  begin
    execute 'drop schema stage_user_data cascade';
  exception when others then
    null;
  end;
  execute 'create schema stage_user_data';
-- users
  execute 'create table stage_user_data.camera_user as select * from camera.camera_user where user_id<>60';
  begin
    execute 'create table stage_user_data.user_preference_map as select * from camera.user_preference_map where user_id<>60';
  exception when others then
    null;
  end;
-- sequences
  execute 'create table stage_user_data.sequence_entity as select * from camera.sequence_entity where owner_id<>60';
  execute 'create table stage_user_data.bio_sequence as select * from camera.bio_sequence where sequence_id in (select sequence_id from stage_user_data.sequence_entity)';
-- tasks
  execute 'create table stage_user_data.task as select * from camera.task where task_owner<>''system''';
  execute 'create table stage_user_data.accounting as select * from camera.accounting where task_id in (select task_id from stage_user_data.task)';
  execute 'create table stage_user_data.task_input_node as select * from camera.task_input_node where task_id in (select task_id from stage_user_data.task)';
  execute 'create table stage_user_data.task_event as select * from camera.task_event where task_id in (select task_id from stage_user_data.task)';
  execute 'create table stage_user_data.task_parameter as select * from camera.task_parameter where task_id in (select task_id from stage_user_data.task)';
  execute 'create table stage_user_data.task_message as select * from camera.task_message where task_id in (select task_id from stage_user_data.task)';
-- nodes
  execute 'create table stage_user_data.node as select * from camera.node where user_id<>60';
  execute 'create table stage_user_data.data_node_feature as select * from camera.data_node_feature where node_id in (select node_id from stage_user_data.node)';
--  begin
--    execute 'create table stage_user_data.node_ts_result as select * from camera.node_ts_result where node_id in (select node_id from --stage_user_data.node)';
--  exception when others then
--    null;
--  end;
-- blast hits
  execute 'create table stage_user_data.blast_hit as select * from camera.blast_hit where result_node_id in (select node_id from stage_user_data.node)';
  execute 'create table stage_user_data.blast_result_node_defline_map as select * from camera.blast_result_node_defline_map where node_id in (select node_id from stage_user_data.node)';
  return true;
exception when others then
  raise notice 'sql error!';
  return false;
end $$ language plpgsql;

create or replace function camera.loadStagedUserData() returns setOf text as $$
declare
  alterRec record;
  columnList text;
  clearCount integer;
  tableList text := 'camera_user,user_preference_map,sequence_entity,bio_sequence,task,accounting,task_event,task_parameter,task_message,node,task_input_node,data_node_feature,blast_hit,blast_result_node_defline_map';
  tableNo integer := 1;
  tableName varchar(255);
begin
--
-- remove old data
  begin
    tableName := 'blast_result_node_defline_map';
    delete from camera.blast_result_node_defline_map where node_id in (select node_id from camera.node where user_id<>60);
    tableName := 'blast_hit';
    delete from camera.blast_hit where result_node_id in (select node_id from camera.node where user_id<>60);
--    tableName := 'node_ts_result';
--    begin
--      delete from camera.node_ts_result where node_id in (select node_id from camera.node where user_id<>60);
--    exception when others then
--      null;
--    end;
    tableName := 'data_node_feature';
    delete from camera.data_node_feature where node_id in (select node_id from camera.node where user_id<>60);
    tableName := 'task_input_node (node)';
    delete from camera.task_input_node where node_id in (select node_id from camera.node where user_id<>60);
    tableName := 'text search results (node)';
    delete from camera.accession_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    delete from camera.website_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    delete from camera.project_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    delete from camera.publication_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    delete from camera.sample_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    delete from camera.final_cluster_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    delete from camera.protein_ts_result where node_id in (select node_id from camera.node where user_id<>60);
    tableName := 'node';
    delete from camera.node where user_id<>60;
    tableName := 'accounting (task)';
    delete from camera.accounting where task_id in (select task_id from camera.task where task_owner<>'system');
    tableName := 'task_input_node';
    delete from camera.task_input_node where task_id in (select task_id from camera.task where task_owner<>'system');
    tableName := 'task_message';
    delete from camera.task_message where task_id in (select task_id from camera.task where task_owner<>'system');
    tableName := 'task_parameter';
    delete from camera.task_parameter where task_id in (select task_id from camera.task where task_owner<>'system');
    tableName := 'task_event';
    delete from camera.task_event where task_id in (select task_id from camera.task where task_owner<>'system');
    tableName := 'task';
    delete from camera.task where task_owner<>'system';
    tableName := 'bio_sequence';
    delete from camera.bio_sequence where source_id=0;
    tableName := 'sequence_entity';
    delete from camera.sequence_entity where source_id=0;
    tableName := 'user_preference_map';
    begin
      delete from camera.user_preference_map where user_id<>60;
    exception when others then
      null;
    end;
    tableName := 'camera_user';
    delete from camera.camera_user where user_id<>60;
    return next 'cleared '||clearCount||' users';
  exception when others then
    return next 'clear '||tableName||' failed';
    return;
  end;
--
-- blast_result_node_defline_map
  tableName := split_part(tableList,',',1);
  while tableName>'' loop
    select appendrows('select u.column_name '||
                    'from information_schema.columns c, information_schema.columns u '||
                    'where c.table_name=u.table_name and c.column_name=u.column_name '||
                    'and c.table_schema=''camera'' and u.table_schema=''stage_user_data'' '||
                    'and c.table_name='''||lower(tableName)||''' order by 1',',')
      into columnList;
    if columnList>'' then
      begin
--        return next 'insert into camera.'||tableName||'('||columnList||') select '||columnList||' from stage_user_data.'||tableName;
        execute 'insert into camera.'||tableName||'('||columnList||') select '||columnList||' from stage_user_data.'||tableName;
--        execute 'vacuum analyze camera.'||tableName;
        return next 'loaded '||tableName;
      exception when others then
        return next tableName||' failed';
        return;
      end;
    else
      return next 'skipped '||tableName;
    end if;
    tableNo := tableNo + 1;
    tableName := split_part(tableList,',',tableNo);
  end loop;
--
-- SUCCESS!
  execute 'drop schema stage_user_data cascade';
  return next 'completed';
  return;
exception when others then
  return next 'sql error';
  return;
end $$ language plpgsql;
