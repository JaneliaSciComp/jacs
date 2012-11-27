function FISH_QUANT_batch_save_summary_TS_v3(file_name_full,TS_summary,parameters)

current_dir = pwd;

%% Parameters
path_name_image    = parameters.path_name_image;
file_name_settings = parameters.file_name_settings;
version            = parameters.version;


%% Ask for file-name if it's not specified
if isempty(file_name_full)
    cd(path_name_image);

    %- Ask user for file-name for spot results
    if isempty(parameters.file_name_default)
        file_name_default = ['FISH-QUANT__batch_summary_', datestr(date,'yyyy-mm-dd'), '.txt'];
    else
        file_name_default = parameters.file_name_default;
    end
    
    
    [file_save,path_save] = uiputfile(file_name_default,'Save results of batch processing');
    file_name_full = fullfile(path_save,file_save);
    
    if file_save ~= 0
        %- Ask user to specify comment
        prompt = {'Comment (cancel for no comment):'};
        dlg_title = 'User comment for file';
        num_lines = 1;
        def = {''};
        answer = inputdlg(prompt,dlg_title,num_lines,def);
    end
else   

    file_save = 1;
    answer = 'Batch detection';
end


%% Only write if FileName specified
if file_save ~= 0 & not(isempty(answer))
    
    fid = fopen(file_name_full,'w');
    
    %- Header    
    fprintf(fid,'FISH-QUANT\t%s\n', version );
    fprintf(fid,'RESULTS TxSite quantification performed ON %s \n', date);
    fprintf(fid,'%s\t%s\n','COMMENT',char(answer)); 
    fprintf(fid,'ANALYSIS-SETTINGS \t%s\n', file_name_settings);   
    fprintf(fid,'FILE\tCELL\tTS\tN_mean\tN_err\tSize[nm]\tN_integ_int\tN_Amp\tN_ratio_int\n');    
    
    %- Summary for each cell
    for i_TS = 1:length(TS_summary)  
        
        res_quant = [ TS_summary(i_TS).TxSite_quant.N_mRNA_Q_limit.mean, ...
              TS_summary(i_TS).TxSite_quant.N_mRNA_Q_limit.stdev, ...
              TS_summary(i_TS).TxSite_SIZE.dist_avg_shift, ...
              TS_summary(i_TS).TxSite_quant.N_mRNA_integrated_int, ...
              TS_summary(i_TS).TxSite_quant.N_mRNA_fitted_amp, ...
              TS_summary(i_TS).TxSite_quant.N_mRNA_trad];
        
        fprintf(fid,'%s\t%s\t%s\t%g\t%g\t%g\t%g\t%g\t%g\n',TS_summary(i_TS).file_name_image, TS_summary(i_TS).cell_label, TS_summary(i_TS).TS_label, res_quant);        
    end
end
fclose(fid);
cd(current_dir)

       
        
       