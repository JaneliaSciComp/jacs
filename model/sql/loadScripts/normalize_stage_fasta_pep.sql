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

create or replace function normalize_stage_fasta_pep() returns void as $$
  declare
    rec1 record; rec2 record;
    i integer := 0;
  begin
    for rec1 in select * from stage_fasta_pep where position('^|^' in defline)>0
    loop
      for rec2 in select * from public.splitpep(rec1.defline)
      loop
        if i>0 then
          begin
            insert into stage_fasta_pep(camera_acc,defline,orig_acc,sequence_id,sequence_length)
              values(split_part(rec2.splitpep,' ',1),rec2.splitpep,rec1.camera_acc,rec1.sequence_id,rec1.sequence_length);
          exception when unique_violation then
            null;
          end;
        else
          update stage_fasta_pep set defline=rec2.splitpep where camera_acc=rec1.camera_acc;
        end if;
        i := 1;
      end loop;
      i := 0;
    end loop;
  end
$$ language plpgsql;
