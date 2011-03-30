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

create or replace
function get_def_tag(p_defline text,p_tag text,p_eod text) returns text as $$
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
$$ language plpgsql;
