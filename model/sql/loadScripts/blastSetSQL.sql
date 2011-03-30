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

create or replace function blastSetSQL(pNodeId bigint) returns text as $$
  declare
    basesql text := '';
    joinsql text := 'select entity_id from (<basesql>) t0 where true';
    seqent_sql text := '';
    seqent_count bigint := 999999999999;
    numjoins integer := 0;
    rec record;
    prevent_joins boolean := false;
  begin
    for rec in select seq_count,entity_sql
               from camera.dma_tag
               where tag_id in (select tag_id from dma_tag_node where node_id=pNodeId)
               and entity_sql like 'select entity_id from sequence_entity where %'               
    loop
      seqent_count := least(seqent_count,rec.seq_count);
      if seqent_sql>'' then seqent_sql := seqent_sql || ' and '; end if;
      seqent_sql := seqent_sql || substring(rec.entity_sql from position(' where ' in rec.entity_sql)+7);
    end loop;
    if seqent_sql>'' then seqent_sql := 'select entity_id from sequence_entity where ' || seqent_sql; end if;

    for rec in (select seq_count, entity_sql, can_join
                from camera.dma_tag
                where tag_id in (select tag_id from dma_tag_node where node_id=pNodeId)
                and entity_sql not like 'select entity_id from sequence_entity where %'
                union all
                select seqent_count, seqent_sql, true where seqent_sql>'')
               order by 1
    loop
      if basesql='' then
        basesql := '('||rec.entity_sql||')';
        prevent_joins := rec.seq_count>50000000;
      elsif rec.can_join and not prevent_joins then
        numjoins := numjoins+1;
        joinsql := joinsql||'\nand exists(select 1 from ('||rec.entity_sql||') t'||numjoins||' where t0.entity_id=t'||numjoins||'.entity_id)';
      else
        basesql := basesql||'\nintersect\n('||rec.entity_sql||')';
      end if;
    end loop;
    if numjoins=0 then
      return basesql;
    else
      return replace(joinsql,'<basesql>',basesql);
    end if;
  end
$$ language plpgsql;
