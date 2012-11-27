function  handles = Java_tools_init_v1(handles,call_options)
% Function to initiate MIJ and Bio-formats


%- Specify location of FISH-QUANT
if isempty(call_options)
        
    %- Get file-name and path-name of calling function
    %p = mfilename('fullpath');        
    %pathstr = fileparts(p); 
    
    %- MIJ and Bio-formats is in the sub-folder
    file_mij  = fullfile(handles.FQ_path,'java','mij.jar');
    file_ij   = fullfile(handles.FQ_path,'java','ij.jar');   
    file_loci = fullfile(handles.FQ_path,'java','loci_tools.jar'); 

else
    %- MIJ is in a user specified folder 
    if strcmpi( call_options{1},'dir')
        FQ_folder_full = char(call_options{2});
        file_mij  = fullfile(FQ_folder_full,'mij.jar');
        file_ij   = fullfile(FQ_folder_full,'ij.jar');
        file_loci = fullfile(FQ_folder_full,'loci_tools.jar');
    
    %- FISH-Quant is in user folder in folder FISH_Quant_version
    elseif strcmpi( call_options{1},'root')        
        
        %- MIJ is installed in Matlab root-directory in the sub-folder java.    
        MIJ_path = matlabroot;
        MIJ_path = regexprep(MIJ_path, ';', '', 'ignorecase');  % Had some problem with user path having a ; at the end
        file_mij = fullfile(MIJ_path,'java','mij.jar');
        file_ij  = fullfile(MIJ_path,'java','ij.jar');  
        file_loci  = fullfile(MIJ_path,'java','loci_tools.jar');  
        
    else  
        warndlg('Call option not specified. Will start with default settings.','FISH-QUANT; init_v2');
         %- Get file-name and path-name of calling function
        p = mfilename('fullpath');        
        pathstr = fileparts(p); 
    
        %- MIJ is in the sub-folder
        file_mij  = fullfile(pathstr,'java','mij.jar');
        file_ij   = fullfile(pathstr,'java','ij.jar'); 
        file_loci = fullfile(pathstr,'java','loci_tools.jar');
    end
end

disp(' ')
disp('== MIJ initialization')
disp(['mij.jar: ', file_mij])
disp(['ij.jar: ', file_ij])
disp(['loci_tools.jar: ', file_loci])
disp(' ')


%- Try to set path definition for MIJ    
if exist(file_mij,'file')
    javaaddpath(file_mij)  % Extend the java classpath to mij.jar
    javaaddpath(file_ij)   % Extend the java classpath to ij.jar   
    javaaddpath(file_loci)   % Extend the java classpath to loci_tools.jar  
    handles.flag_JAVA_init = 1;
else
    choice = 'Yes';
    while strcmp(choice,'Yes')
  
        choice = questdlg('Java-files of MIJ and Bio-formats not found. Specify other location?', 'MIJ', 'Yes','No','Yes');
        
        switch choice
        
            case 'Yes'
                folder_name = uigetdir(user_path);            
                if folder_name~= 0
                    file_mij = fullfile(folder_name,'mij.jar');
                    file_ij = fullfile(folder_name,'ij.jar');
                    if exist(file_mij,'file')
                        javaaddpath(file_mij)  % Extend the java classpath to mij.jar
                        javaaddpath(file_ij)   % Extend the java classpath to ij.jar  
                        javaaddpath(file_loci)   % Extend the java classpath to ij.jar  
                        
                        handles.flag_JAVA_init = 1;
                        choice = 'No';
                    else
                       choice = 'Yes';
                    end
                else
                    choice = 'Yes';
                end            
            case 'No'
                handles.flag_JAVA_init = 0;
                warndlg('Java-pathdef not set!!!!!', 'FISH QUANT')
        end
    end
end
  