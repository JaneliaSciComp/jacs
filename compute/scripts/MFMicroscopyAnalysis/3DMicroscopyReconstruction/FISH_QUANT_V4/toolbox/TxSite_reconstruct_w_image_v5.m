function [TxSite_quant REC_prop] = TxSite_reconstruct_w_image_v5(TS_analysis,img_PSF,parameters)

% Need to run TxSite_reconstruct_ANALYSIS_v1 first/

% v1 Aug 11, 2011
% - Initial implementation

%% Parameters
flags               = parameters.flags;
N_mRNA_analysis_MAX = parameters.N_mRNA_analysis_MAX;
N_reconstruct       = parameters.N_reconstruct;
mRNA_prop           = parameters.mRNA_prop;
pixel_size_os       = parameters.pixel_size_os;
factor_Q_ok         = parameters.factor_Q_ok;


%% Extract results from analysis with TxSite_reconstruct_ANALYSIS_v1

img_TS_crop_xyz  = TS_analysis.img_TS_crop_xyz;
coord            = TS_analysis.coord;
index_table      = TS_analysis.index_table;
%img_bgd          = TS_analysis.img_bgd;
bgd_amp          = TS_analysis.bgd_amp;

%% Determine rouhghly the number of mRNA's at the site


% 
% if isempty(N_mRNA_analysis_MAX)
%     
%     N_initial = parameters.N_run_prelim;
%     
%     %- Repeat estimation five times to reduce noise
%     for i=1:N_initial
%          [N_MAX_loop(i) res_all{i}] =  TxSite_reconstruct_w_image_FUN_est_N_v2(img_TS_crop_xyz,img_bgd,img_PSF,parameters_rec);    
%          min_res(i) = min(res_all{i});
%     end
%     
%     N_mRNA_analysis_MAX = round(mean(N_MAX_loop));
%     
%     if flags.output == 2
%         figure, hold on
%         for i=1:N_initial 
%             x_value = (1:1:length(res_all{i}))-1;
%             plot(x_value,res_all{i}/min(res_all{i}))
%         end       
%         xlabel('Number of mRNAs')
%         ylabel('Quality score [rel to min]')
%         title(['Maximum number of PSFs which will be placed: ', num2str(N_mRNA_analysis_MAX)])
%         box on 
%     end
% end


%% Determine number of iterations and background (if range is specified)
disp(' ')
disp('Determining background')
disp(' .... ')

parameters_rec.coord         = coord;
parameters_rec.mRNA_prop     = mRNA_prop;
parameters_rec.index_table   = index_table;
parameters_rec.index_table   = index_table;
parameters_rec.pixel_size_os = pixel_size_os;
parameters_rec.flags         = flags;
parameters_rec.flags.output = 0;


%if flags.bgd_local == 0 && length(parameters.BGD.amp) > 1 ;

N_run_prelim = parameters.N_run_prelim;

N_bgd = length(bgd_amp);

for i_bgd = 1: N_bgd
    bgd_loop = parameters.BGD.amp(i_bgd);

    img_bgd     = bgd_loop*ones(size(img_TS_crop_xyz));

    %- Repeat reconstruction to reduce noise
    if(flags.parallel)
        parfor i=1:N_run_prelim
            [N_MAX_loop(i) res_all{i}] =  TxSite_reconstruct_w_image_FUN_est_N_v2(img_TS_crop_xyz,img_bgd,img_PSF,parameters_rec);    
            min_res_loop(i) = min(res_all{i});
        end
    else
        for i=1:N_run_prelim
            [N_MAX_loop(i) res_all{i}] =  TxSite_reconstruct_w_image_FUN_est_N_v2(img_TS_crop_xyz,img_bgd,img_PSF,parameters_rec);    
            min_res_loop(i) = min(res_all{i});
        end    
    end
    
    N_mRNA_analysis_MAX(i_bgd) = round(mean(N_MAX_loop));
    min_res_all(i_bgd)         = round(mean(min_res_loop));
end

[res_min_val res_min_ind] = min(min_res_all);

bgd_rec = parameters.BGD.amp(res_min_ind);
N_mRNA_MAX = N_mRNA_analysis_MAX(res_min_ind);

disp(['Background value: ', num2str(bgd_rec)])


if flags.output == 2
    figure
    subplot(1,1,1)        
    plot(parameters.BGD.amp,min_res_all)
    xlabel('Background of TxSite')
    ylabel('AVGed minimum residuals')   
    box on 
end
    




%% Determine # nascent mRNA's
parameters_rec.N_mRNA_MAX = N_mRNA_MAX;
img_bgd                   = bgd_rec*ones(size(img_TS_crop_xyz));
disp(' ')
disp('Sampling different configurations')
disp(' .... ')

%== Other variables

%= RUN LOOP
TS_rec = struct('Q_min', [], 'N_mRNA', [],'data', [],'pos', [],'amp', []);
Q_all  = struct('data', []);

%- Run loop and do reconstruction
if(flags.parallel)
     disp('Parallel computing ....')
      
     parfor iRun_p = 1 : N_reconstruct
        [TS_rec(iRun_p) Q_all(iRun_p).data]  = TxSite_reconstruct_w_image_FUN_v2(img_TS_crop_xyz,img_bgd,img_PSF,parameters_rec);
     end
 
 else
    for iRun = 1 : N_reconstruct    

        if (rem(iRun,10) == 0);
            disp(['Configurations tested: ', num2str(100*(iRun/N_reconstruct)),'%']) 
        end       

        
       [TS_rec(iRun) Q_all(iRun).data]  = TxSite_reconstruct_w_image_FUN_v2(img_TS_crop_xyz,img_bgd,img_PSF,parameters_rec);
        
        
    
    end
end

disp(' .... ')
disp('RECONSTRUCTION FINISHED')



%% Analyse results to get error bars
for iRun = 1 : N_reconstruct 
    
    data_loop = Q_all(iRun).data;
    
    N_mRNA = data_loop(:,1);
    Q_loop = data_loop(:,2);
    
    [Q_min_loop ind_min] = min(Q_loop);
    Q_loop_norm        = Q_loop/Q_min_loop;
    
    ind_ok = find(Q_loop_norm <= factor_Q_ok);
           
    N_best    = N_mRNA(ind_min);
    N_best_lb = N_mRNA(ind_ok(1));
    N_best_ub = N_mRNA(ind_ok(end));
     
    N_mRNA_all(iRun,:) = [N_best N_best_lb N_best_ub];
    Q_norm(:,iRun)     = Q_loop_norm;
    
    
end
clear Q_min_loop Q_loop_norm 

N_mRNA_all_avg = round(mean(N_mRNA_all,1));

N_mRNA_rec.mean      = N_mRNA_all_avg(1);
N_mRNA_rec.lb        = N_mRNA_all_avg(2);
N_mRNA_rec.ub        = N_mRNA_all_avg(3);
N_mRNA_rec.stdev     = mean([ (N_mRNA_all_avg(1)-N_mRNA_all_avg(2)), (N_mRNA_all_avg(3)-N_mRNA_all_avg(1))]);
N_mRNA_rec.factor_ok = factor_Q_ok;


%% Analyse results
Q_min_loop = zeros(N_reconstruct,2);

for iRun = 1 : N_reconstruct     
    Q_min_loop(iRun,:) = [TS_rec(iRun).N_mRNA TS_rec(iRun).Q_min];      
    summary_Q_run(:,iRun) = Q_all(iRun).data(:,2);    
end

summary_Q_run_N_MRNA = Q_all(iRun).data(:,1);
mean_Q = mean(summary_Q_run,2);

%% Traditional method to estimate # nascent transcripts


%== Analyze residuals
%- Extract best fit for each run and sort
[Q_min_run_sorted ind_Q_min_sorted] = sortrows(Q_min_loop,2);

%=== Extract best fit for image
ind_best          = ind_Q_min_sorted(1);
img_fit           = TS_rec(ind_best).data;
img_res           = img_TS_crop_xyz-img_fit;

%== Estimate number of transcripts with different metrics

%- Use over-all best fit 
N_mRNA_TS_global = Q_min_run_sorted(1,1);
Q_min_global     = Q_min_run_sorted(1,2);


%- Determine average of best 10%
N_avg                = round(0.1*size(Q_min_run_sorted,1));
N_mRNA_TS_mean_10per = round(mean(Q_min_run_sorted(1:N_avg,1)));
N_mRNA_TS_std_10per  = ceil(std(Q_min_run_sorted(1:N_avg,1)));
Q_avg_10per          = mean(Q_min_run_sorted(1:N_avg,2));


%- Determine average of all fits
N_mRNA_TS_mean = mean(Q_min_loop(:,1));
N_mRNA_TS_std  = std(Q_min_loop(:,1));
Q_avg          = mean(Q_min_loop(:,2));


% === QUANTIFY WIHT OTHER METHODS

%== Traditional method: division: intensity of transcription site by intensity of individual mRNA
max_TS      = max(img_TS_crop_xyz(:)) - mean(img_bgd(:));
N_mRNA_trad = round(max_TS/mRNA_prop.pix_brightest);


%== Semi-Traditional method: fitting
N_mRNA_fitted_amp = round(TS_analysis.TS_Fit_Result.amp / mRNA_prop.amp_mean);

           
%=== Integrated intensity of spot
par_mod_int(1)  = mRNA_prop.sigma_xy;
par_mod_int(2)  = mRNA_prop.sigma_xy;
par_mod_int(3)  = mRNA_prop.sigma_z;

par_mod_int(4)  = 0;
par_mod_int(5)  = 0;
par_mod_int(6)  = 0;

par_mod_int(7)  = mRNA_prop.amp_mean;
par_mod_int(8)  = 0 ;


x_int = TS_analysis.TS_Fit_Result.x_int;
y_int = TS_analysis.TS_Fit_Result.y_int;
z_int = TS_analysis.TS_Fit_Result.z_int;

% x_int.min = 0 - 10*mRNA_prop.sigma_xy;
% x_int.max = 0 + 10*mRNA_prop.sigma_xy;
% 
% y_int.min = 0 - 10*mRNA_prop.sigma_xy;
% y_int.max = 0 + 10*mRNA_prop.sigma_xy;
% 
% z_int.min = 0 - 10*mRNA_prop.sigma_z;
% z_int.max = 0 + 10*mRNA_prop.sigma_z;

integrated_int = fun_Gaussian_3D_triple_integral_v1(x_int,y_int,z_int,par_mod_int);

N_mRNA_integrated_int = round(TS_analysis.TS_Fit_Result.Integrated_int / integrated_int);


%== Analyse histogram of amplitudes

%- All
amp_all = [];
for iRun = 1 : N_reconstruct
    amp_all = [amp_all,TS_rec(iRun).amp];
end

[hist_all_counts hist_bin] = hist(amp_all,30);
hist_all_counts_norm = hist_all_counts/max(hist_all_counts);

%- Best 10%
amp_best_10 = [];
for iRun = 1 : N_avg
    i_loop = ind_Q_min_sorted(iRun);
    amp_best_10 = [amp_best_10,TS_rec(i_loop).amp];
end
[hist_top10_counts] = hist(amp_best_10,hist_bin);
hist_top10_counts_norm = hist_top10_counts/max(hist_top10_counts);

%- Best reconstruction
ind_best = ind_Q_min_sorted(1);
[hist_best_counts] = hist(TS_rec(ind_best).amp,hist_bin);
hist_best_counts_norm = hist_best_counts/max(hist_best_counts);



%% Save output

%- Save properties of reconstruction
REC_prop.img_TS   = img_TS_crop_xyz;
REC_prop.img_res  = img_res;
REC_prop.img_fit  = img_fit;
REC_prop.img_bgd  = img_bgd;
REC_prop.coord    = coord;
REC_prop.bgd_amp  = bgd_rec;


REC_prop.pos_best = TS_rec(ind_Q_min_sorted(1)).pos;

[REC_prop.pos_all(1:N_reconstruct).coord] = deal(TS_rec(ind_Q_min_sorted).pos);

REC_prop.summary_Q_run_N_MRNA = summary_Q_run_N_MRNA;
REC_prop.summary_Q_run        = summary_Q_run;
REC_prop.mean_Q               = mean_Q;
REC_prop.Q_norm               = Q_norm;

%- TxSite quantification
TxSite_quant.N_mRNA_Q_limit        = N_mRNA_rec;

TxSite_quant.N_mRNA_TS_global      = N_mRNA_TS_global;
TxSite_quant.Q_min_global          = Q_min_global;

TxSite_quant.N_mRNA_TS_mean_10per  = N_mRNA_TS_mean_10per;
TxSite_quant.N_mRNA_TS_std_10per   = N_mRNA_TS_std_10per;
TxSite_quant.Q_avg_10per           = Q_avg_10per;

TxSite_quant.N_mRNA_TS_mean_all    = N_mRNA_TS_mean;
TxSite_quant.N_mRNA_TS_std_all     = N_mRNA_TS_std;
TxSite_quant.Q_avg_all             = Q_avg;

TxSite_quant.N_mRNA_trad           = N_mRNA_trad;
TxSite_quant.N_mRNA_fitted_amp     = N_mRNA_fitted_amp;
TxSite_quant.N_mRNA_integrated_int = N_mRNA_integrated_int;

    

%% PLOTS
if flags.output == 2    
    %= Histogram of used amplitudes
    figure, hold on
    plot(hist_bin,hist_all_counts_norm,'r')
    plot(hist_bin,hist_top10_counts_norm,'b')
    plot(hist_bin,hist_best_counts_norm,'g')
    hold off
    box on 
    legend('All','Top 10','Best reconstruction')
    title('Histogram of used amplitudes in reconstructions')
end

 TxSite_reconstruct_Output_v1(TxSite_quant, REC_prop, parameters);




