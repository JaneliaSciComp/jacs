function FISH_QUANT_save_outline_v5(handles,file_save)

current_dir = pwd;
cd(handles.path_name_image);


%- If not file-name to save was provided
if isempty(file_save)    
    %- Ask user for file-name for spot results
    [dum, name_file] = fileparts(handles.file_name_image); 
    file_name_default_spot = [name_file,'__outline.txt'];
    [file_save,path_save] = uiputfile(file_name_default_spot,'Save outline of cell and TxSite(s)'); 
    file_save_full = fullfile(path_save,file_save);
else
    file_save_full = file_save;
    file_save = 1;    
end


% Only write if file_save specified
if file_save ~= 0
    
    par_microscope = handles.par_microscope;
    cell_prop      = handles.cell_prop;          
    
    fid = fopen(file_save_full,'w');
    
    %- Header 
    fprintf(fid,'FISH-QUANT\t%s\n', handles.version);
    fprintf(fid,'OUTLINE DEFINITION, %s\n', date);
    fprintf(fid,'%s\t%s\n','COMMENT','Outline definition performed in FISH-QUANT (Main program)');   
  
    %- File Name
    fprintf(fid,'%s\t%s\n','FILE',handles.file_name_image);
    if isfield(handles,'file_name_image_filtered');
        fprintf(fid,'%s\t%s\n','FILTERED',handles.file_name_image_filtered); 
    else
        fprintf(fid,'%s\t%s\n','FILTERED',''); 
    end
    
    
    %- Experimental parameters
    fprintf(fid,'PARAMETERS\n');
    fprintf(fid,'Pix-XY\tPix-Z\tRI\tEx\tEm\tNA\tType\n');
    fprintf(fid,'%g\t%g\t%g\t%g\t%g\t%g\t%s\n', par_microscope.pixel_size.xy, par_microscope.pixel_size.z, par_microscope.RI, par_microscope.Ex, par_microscope.Em,par_microscope.NA, par_microscope.type );
    
    %- Analysis settings
    fprintf(fid,'ANALYSIS-SETTINGS \t%s\n', handles.file_name_settings);    
    
    
    for i_cell = 1:size(cell_prop,2)
    
        %- Outline of cell
        fprintf(fid,'%s\t%s\n', 'CELL', cell_prop(i_cell).label);
        fprintf(fid,'X_POS\t');
        fprintf(fid,'%g\t',cell_prop(i_cell).x);
        fprintf(fid,'END\n');
        fprintf(fid,'Y_POS\t');
        fprintf(fid,'%g\t',cell_prop(i_cell).y);
        fprintf(fid,'END\n');
    
        %- TS
        for i_TS = 1:size(cell_prop(i_cell).pos_TS,2)
            fprintf(fid,'%s\t%s\n', 'TxSite', cell_prop(i_cell).pos_TS(i_TS).label);
            fprintf(fid,'X_POS\t');
            fprintf(fid,'%g\t',cell_prop(i_cell).pos_TS(i_TS).x);
            fprintf(fid,'END\n');
            fprintf(fid,'Y_POS\t');
            fprintf(fid,'%g\t',cell_prop(i_cell).pos_TS(i_TS).y);
            fprintf(fid,'END\n');        
        end
    end
    
    fclose(fid);
end

cd(current_dir)