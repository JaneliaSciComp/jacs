function parameters_quant = FQ_TS_settings_detect_modify_v1(parameters_quant)
% Function to modify the settings of TS quantification

%- User-dialog
dlgTitle = 'Options for TxSite quantification';
prompt_avg(1) = {'[CROP] in XY +/- nm'};
prompt_avg(2) = {'[CROP] in Z +/- nm'};
prompt_avg(3) = {'[AUTO-DETECT] Number of connected comp in 3D (6,18,26)'};
prompt_avg(4) = {'[AUTO-DETECT] Minimum distance between detected sites [pix]'};


defaultValue_avg{1} = num2str(parameters_quant.crop_image.xy_nm);
defaultValue_avg{2} = num2str(parameters_quant.crop_image.z_nm);
defaultValue_avg{3} = num2str(parameters_quant.conn);
defaultValue_avg{4} = num2str(parameters_quant.min_dist);


options.Resize='on';
%options.WindowStyle='normal';
userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg,options);

%- Return results if specified
if( ~ isempty(userValue))
    parameters_quant.crop_image.xy_nm      = str2double(userValue{1});
    parameters_quant.crop_image.z_nm       = str2double(userValue{2});
    parameters_quant.conn                  = str2double(userValue{3});
    parameters_quant.min_dist              = str2double(userValue{4});
end
