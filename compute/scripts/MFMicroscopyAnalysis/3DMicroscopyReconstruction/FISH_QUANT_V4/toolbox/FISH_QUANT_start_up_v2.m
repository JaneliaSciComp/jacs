function handles = FISH_QUANT_start_up_v2(handles)


file_name      = 'FISH-QUANT_def.txt';
file_name_full = fullfile(handles.FQ_path,file_name);


%- Open file
fid  =  fopen(file_name_full,'r');

% Read in each line and check if one of the known identifiers is present.
% If yes, assign the corresponding value

if fid == -1
    warndlg('Definition file cannot be opened','FISH_QUANT_start_up_v2'); 
else

    %- Loop through file until end of file
    while not(feof(fid))

        %- Extract string of entire line
        C   = textscan(fid,'%s',1,'delimiter','\n');
        str =  char(C{1});

        %- Is there and equal sign? Extract strings before and after
        k = strfind(str, '=');    
        str_tag = str(1:k-1);
        str_val = str(k+1:end);


        %- Compare identifier before the equal sign to known identifier
        switch str_tag

            case 'path_imagej'
                handles.path_imagej = str_val;
                
           case 'version'
                handles.version = str_val;              
                
           case 'ij_macro_name'
                handles.ij_macro_name = str_val;
                
        end

    end
    
    fclose(fid);
end


