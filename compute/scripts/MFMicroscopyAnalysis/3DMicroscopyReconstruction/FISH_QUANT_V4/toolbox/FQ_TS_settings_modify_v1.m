function parameters_quant = FQ_TS_settings_modify_v1(parameters_quant)
% Function to modify the settings of TS quantification

%- User-dialog
dlgTitle = 'Options for TxSite quantification';
prompt_avg(1) = {'Factor for Quality score to determine error'};
prompt_avg(2) = {'[Placement] 1-random; 2-max Int'};
prompt_avg(3) = {'[Residuals] 1-ssr;    2-asr'};
prompt_avg(4) = {'Number of runs'};
prompt_avg(5) = {'Number of runs for preliminary analysis'};
prompt_avg(6) = {'[BGD] Number of bins for histogram'};
prompt_avg(7) = {'[BGD] Percentage of pixel for calculation'};
prompt_avg(8) = {'[CROP] 0-no, 1-yes (with region)'};
prompt_avg(9) = {'[CROP] in XY +/- nm'};
prompt_avg(10) = {'[CROP] in Z +/- nm'};
prompt_avg(11) = {'[AUTO-DETECT] Number of connected comp in 3D (6,18,26)'};
prompt_avg(12) = {'[AUTO-DETECT] Minimum distance between detected sites [pix]'};


defaultValue_avg{1} = num2str(parameters_quant.factor_Q_ok);
defaultValue_avg{2} = num2str(parameters_quant.flags.placement);
defaultValue_avg{3} = num2str(parameters_quant.flags.quality);
defaultValue_avg{4} = num2str(parameters_quant.N_reconstruct);
defaultValue_avg{5} = num2str(parameters_quant.N_run_prelim);
defaultValue_avg{6} = num2str(parameters_quant.nBins);
defaultValue_avg{7} = num2str(parameters_quant.per_avg_bgd);
defaultValue_avg{8} = num2str(parameters_quant.flags.crop);
defaultValue_avg{9} = num2str(parameters_quant.crop_image.xy_nm);
defaultValue_avg{10} = num2str(parameters_quant.crop_image.z_nm);
defaultValue_avg{11} = num2str(parameters_quant.conn);
defaultValue_avg{12} = num2str(parameters_quant.min_dist);

options.Resize='on';
%options.WindowStyle='normal';
userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg,options);

%- Return results if specified
if( ~ isempty(userValue))
    parameters_quant.factor_Q_ok           = str2double(userValue{1});
    parameters_quant.flags.placement       = str2double(userValue{2});
    parameters_quant.flags.quality         = str2double(userValue{3});
    parameters_quant.N_reconstruct         = str2double(userValue{4});
    parameters_quant.N_run_prelim          = str2double(userValue{5}); 
    parameters_quant.nBins                 = str2double(userValue{6});
    parameters_quant.per_avg_bgd           = str2double(userValue{7});
    parameters_quant.flags.crop            = str2double(userValue{8});
    parameters_quant.crop_image.xy_nm      = str2double(userValue{9});
    parameters_quant.crop_image.z_nm       = str2double(userValue{10});
    parameters_quant.conn                  = str2double(userValue{11});
    parameters_quant.min_dist              = str2double(userValue{12});
    
end
