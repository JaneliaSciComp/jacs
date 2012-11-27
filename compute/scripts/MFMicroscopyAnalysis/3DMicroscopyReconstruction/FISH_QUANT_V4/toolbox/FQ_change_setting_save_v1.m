function settings_save = FQ_change_setting_save_v1(settings_save)
% Function to modify the settings of TS quantification

%- User-dialog
dlgTitle = 'Options to save results of quantification';
prompt_avg(1) = {'[SUMMARY] Number of characters at the end of file-name used as identifier'};
defaultValue_avg{1} = num2str(settings_save.N_ident);


options.Resize='on';
%options.WindowStyle='normal';

userValue = inputdlg(prompt_avg,dlgTitle,1,defaultValue_avg,options);

%- Return results if specified
if( ~ isempty(userValue))
    settings_save.N_ident = str2double(userValue{1});
end


