function settings = FISH_QUANT_load_settings_v3(file_name,struct_store)
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
    warnMessage=sprintf('Settings file cannot be opened FISH_QUANT_load_settings_v1 %s',file_name);
    disp(warnMessage); 
else
    tmpMsg = sprintf('Reading settings from file %s',file_name);
    disp(tmpMsg)
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

            case 'flag_parallel'
                settings.flag_parallel = str_val;            

            case 'Kernel_bgd'            
                settings.filter.factor_bgd = str2double(str_val);

            case 'Kernel_psf'
                settings.filter.factor_psf = str2double(str_val);

            case 'Detect_Region_XY'
                settings.detect.region.xy = str2double(str_val);             

            case 'Detect_Region_Z'            
                settings.detect.region.z = str2double(str_val);            

            case 'Detect_Thresh_int'
                settings.detect.thresh_int = str2double(str_val);            

            case 'Detect_Thresh_score'
                settings.detect.thresh_score = str2double(str_val); 
                
            case 'Detect_FLAG_reg_smaller'
                settings.detect.flag_region_smaller = str2double(str_val);                 

            case 'Detect_Score'    
                settings.detect.score = str_val;              

            case 'AVG_Region_XY'            
                settings.average.crop.xy = str2double(str_val);            

            case 'AVG_Region_Z'
                settings.average.crop.z = str2double(str_val);            

            case 'AVG_OS_XY'
                settings.average.fact_os.xy = str2double(str_val);   

            case 'AVG_OS_Z'    
                settings.average.fact_os.z = str2double(str_val);   
                
            case 'Fit_mode'    
                
                switch str_val
                    case 'sigma_free_xz'                    
                        settings.flag_fit = 0;
                        
                     case 'sigma_fixed'
                        settings.flag_fit = 1;   
                end
                
            case 'Sigma_XY_fixed'    
                settings.par_fit.sigma_XY_fixed = str2double(str_val);  
                
                
            case 'Sigma_Z_fixed'    
                settings.par_fit.sigma_Z_fixed = str2double(str_val);      
                
                
            case 'SPOTS_TH_sigmaXY_min'
                settings.thresh_all.sigmaxy.min_hist = str2double(str_val);
                settings.thresh_all.sigmaxy.lock      = 1;

            case 'SPOTS_TH_sigmaXY_max'    
                settings.thresh_all.sigmaxy.max_hist = str2double(str_val);
                settings.thresh_all.sigmaxy.lock      = 1;

             case 'SPOTS_TH_sigmaZ_min'
                settings.thresh_all.sigmaz.min_hist = str2double(str_val);
                settings.thresh_all.sigmaz.lock      = 1;

            case 'SPOTS_TH_sigmaZ_max'    
                settings.thresh_all.sigmaz.max_hist = str2double(str_val);
                settings.thresh_all.sigmaz.lock      = 1;    

           case 'SPOTS_TH_amp_min'
                settings.thresh_all.amp.min_hist = str2double(str_val);
                settings.thresh_all.amp.lock      = 1;

            case 'SPOTS_TH_amp_max'    
                settings.thresh_all.amp.max_hist = str2double(str_val);
                settings.thresh_all.amp.lock      = 1;    

           case 'SPOTS_TH_bgd_min'
                settings.thresh_all.bgd.min_hist = str2double(str_val);
                settings.thresh_all.bgd.lock      = 1;

            case 'SPOTS_TH_bgd_max'    
                settings.thresh_all.bgd.max_hist = str2double(str_val);
                settings.thresh_all.bgd.lock      = 1;              

            case 'SPOTS_TH_score_min'
                settings.thresh_all.score.min_hist = str2double(str_val);
                settings.thresh_all.score.lock     = 1;

            case 'SPOTS_TH_score_max'    
                settings.thresh_all.score.max_hist = str2double(str_val);
                settings.thresh_all.score.lock     = 1;                      

            case 'SPOTS_TH_iter_min'
                settings.thresh_all.iter.min_hist = str2double(str_val);
                settings.thresh_all.iter.lock     = 1;

            case 'SPOTS_TH_iter_max'    
                settings.thresh_all.iter.max_hist = str2double(str_val);
                settings.thresh_all.iter.lock      = 1;   

            case 'SPOTS_TH_resNorm_min'
                settings.thresh_all.resNorm.min_hist = str2double(str_val);
                settings.thresh_all.resNorm.lock      = 1;

            case 'SPOTS_TH_resNorm_max'    
                settings.thresh_all.resNorm.max_hist = str2double(str_val);
                settings.thresh_all.resNorm.lock      = 1;                  
                
            
            case 'SPOTS_TH_pos_z_min'
                settings.thresh_all.pos_z.min_hist = str2double(str_val);
                settings.thresh_all.pos_z.lock      = 1;

            case 'SPOTS_TH_pos_z_max'    
                settings.thresh_all.pos_z.max_hist = str2double(str_val);
                settings.thresh_all.pos_z.lock      = 1;     
                
                
        end      

    end
    
    fclose(fid);
end


