function settings = FQ_TS_settings_load_v1(file_name,struct_store)
% Function to read in settings for analysis of FISH data
% Settings are stored in a simple format. Each property starts with the
% name followed by a '='and the actual value. There is NO space inbetween
% the equal sign and the identifier and the actual value!
%
% struct_store is the structure where the settings will be save. Can either
% be empty or a user defined structure. In FISH_QUANT the handles structure
% of the GUI will be the input. This way all the saved settings will be
% over-written while other will be untouched.


if isempty(struct_store)
    settings = {};
else
    settings = struct_store;
end


%- Open file
fid  =  fopen(file_name,'r');


% Read in each line and check if one of the known identifiers is present.
% If yes, assign the corresponding value

if fid == -1
    warndlg('Settings file cannot be opened','FISH_QUANT_load_settings_v1'); 
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

            case 'lambda_EM'
                settings.par_microscope.Em = str2double(str_val);

            case 'lambda_Ex'
                settings.par_microscope.Ex = str2double(str_val);             

            case 'NA'            
                settings.par_microscope.NA = str2double(str_val);            

            case 'RI'
                settings.par_microscope.RI = str2double(str_val);            

            case 'Microscope'
                settings.par_microscope.type = str_val;   

            case 'Pixel_XY'    
                settings.par_microscope.pixel_size.xy = str2double(str_val);                 

            case 'Pixel_Z' 
                settings.par_microscope.pixel_size.z = str2double(str_val);                
                
            case 'Fit_mode'    
                
                switch str_val
                    case 'sigma_free_xz'                    
                        settings.flag_fit = 0;
                        
                     case 'sigma_fixed'
                        settings.flag_fit = 1;   
                end                
     
              case 'PSF_path_name'                  
                settings.PSF_path_name = (str_val); 
                
              case 'PSF_file_name'
                settings.PSF_file_name = (str_val);               
                
               case 'BGD_path_name'                  
                settings.BGD_path_name = (str_val); 
            
            case 'PSF_BGD_value'    
                settings.bgd_value = str2double(str_val); 
                
                
              case 'BGD_file_name'
                settings.BGD_file_name = (str_val);                
                
                  case 'AMP_path_name'                  
                settings.AMP_path_name = (str_val); 
                
              case 'AMP_file_name'
                settings.AMP_file_name = (str_val);     
                
              case 'fact_os_xy'
                settings.fact_os.xy = str2double(str_val);                
                
               case 'fact_os_z'
                settings.fact_os.z = str2double(str_val);                   
                
 
            case 'FLAG_placement'
                settings.parameters_quant.flags.placement = str2double(str_val);    

            case 'FLAG_quality'
                settings.parameters_quant.flags.quality = str2double(str_val); 

            case 'FLAG_posWeight'
                settings.parameters_quant.flags.posWeight = str2double(str_val);                 
                
            case 'FLAG_bgd_local'
                settings.parameters_quant.flags.bgd_local = str2double(str_val);    

            case 'FLAG_crop'
                settings.parameters_quant.flags.crop = str2double(str_val); 

            case 'FLAG_psf'
                settings.parameters_quant.flags.psf = str2double(str_val); 
                
            case 'FLAG_shift'
                settings.parameters_quant.flags.shift = str2double(str_val);    

            case 'FLAG_parallel'
          %      settings.handles.checkbox_parallel_computing = str2double(str_val); 
                

            case 'N_reconstruct'
                settings.parameters_quant.N_reconstruct = str2double(str_val);      
          
            case 'N_run_prelim'
                settings.parameters_quant.N_run_prelim = str2double(str_val);
                
            case 'nBins'
                settings.parameters_quant.nBins = str2double(str_val);      
          
            case 'per_avg_bgd'
                settings.parameters_quant.per_avg_bgd = str2double(str_val);            
            
            case 'crop_image_xy_nm'
                settings.parameters_quant.crop_image.xy_nm= str2double(str_val);      
          
            case 'crop_image_z_nm'
                settings.parameters_quant.crop_image.z_nm = str2double(str_val);            
     
          
            case 'factor_Q_ok'
                settings.parameters_quant.factor_Q_ok = str2double(str_val);  
                
                
            case 'TS_BGD_value'
                if isfield(settings.parameters_quant,'BGD')
                    if isfield(settings.parameters_quant.BGD,'amp') 
                        settings.parameters_quant.BGD.amp = [settings.parameters_quant.BGD.amp,str2double(str_val)];
                    end
                else
                    settings.parameters_quant.BGD.amp = str2double(str_val);
                end
                
                
           case 'auto_detect_th_int'    
                settings.parameters_auto_detect.int_th = str2double(str_val);
                
          
           case 'auto_detect_conn'    
                settings.parameters_auto_detect.conn = str2double(str_val);  
                
           case 'FLAG_auto_detect'
                settings.FLAG_auto_detect = str2double(str_val);       
                
        end      

    end
    
    fclose(fid);
end


