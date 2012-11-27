function TxSite_SIZE  = TxSite_size_v1(REC_prop,TS_analysis_results,parameters)

flags               = parameters.flags;

%% Analyze size of transcription site


%- Center of coordinate grid describing transcription site
X_center = TS_analysis_results.coord.X_nm(ceil(length(TS_analysis_results.coord.X_nm)/2));
Y_center = TS_analysis_results.coord.Y_nm(ceil(length(TS_analysis_results.coord.Y_nm)/2));
Z_center = TS_analysis_results.coord.Z_nm(ceil(length(TS_analysis_results.coord.Z_nm)/2));

%- Correct positions with respect to center of coordinate grid
pos_TS_best_shift = [];
pos_TS_best_shift(:,1) = REC_prop.pos_best(:,1) - Y_center;
pos_TS_best_shift(:,2) = REC_prop.pos_best(:,2) - X_center;
pos_TS_best_shift(:,3) = REC_prop.pos_best(:,3) - Z_center;


TxSite_SIZE.pos            = pos_TS_best_shift;
TxSite_SIZE.pos_avg        = mean(TxSite_SIZE.pos ,1);
TxSite_SIZE.pos_shift      = (TxSite_SIZE.pos  - repmat(TxSite_SIZE.pos_avg,size(TxSite_SIZE.pos ,1),1));
TxSite_SIZE.dist_3D        = sqrt((TxSite_SIZE.pos(:,1).^2 + TxSite_SIZE.pos(:,2).^2  + TxSite_SIZE.pos(:,3).^2));
TxSite_SIZE.dist_avg       = round(mean(TxSite_SIZE.dist_3D));
TxSite_SIZE.dist_3D_shift  = sqrt((TxSite_SIZE.pos_shift(:,1).^2 + TxSite_SIZE.pos_shift(:,2).^2  + TxSite_SIZE.pos_shift(:,3).^2));
TxSite_SIZE.dist_avg_shift = round(mean(TxSite_SIZE.dist_3D_shift));

%- Calc histogram of positions
[TxSite_SIZE.dist_counts       TxSite_SIZE.dist_bins]       = hist(TxSite_SIZE.dist_3D);
[TxSite_SIZE.dist_counts_shift TxSite_SIZE.dist_bins_shift] = hist(TxSite_SIZE.dist_3D_shift);

%% OUTPUT

if flags.output
    disp(' ')
    disp(['Dist AVG [CENTERED DATA]     : ', num2str(TxSite_SIZE.dist_avg_shift,'%10.0f'),' nm'])
    disp(['DIST AVG [NOT CENTERED DATA] : ', num2str(TxSite_SIZE.dist_avg,'%10.0f'),' nm'])
end

if flags.output == 2
        
    %- Plot positions in 3D
    figure, hold on
    plot3(TxSite_SIZE.pos_shift(:,1),TxSite_SIZE.pos_shift(:,2),TxSite_SIZE.pos_shift(:,3), 'og')
    hold off
    
    figure 
    subplot(2,1,1)
    bar(TxSite_SIZE.dist_bins,TxSite_SIZE.dist_counts)
    legend(['Dist AVG=', num2str(TxSite_SIZE.dist_avg,'%10.0f'),' nm'])
    xlabel('Distance from center in nm')
    ylabel('# of mRNA')
    title('Not centered data')

    subplot(2,1,2)
    bar(TxSite_SIZE.dist_bins_shift,TxSite_SIZE.dist_counts_shift)
    legend(['Dist AVG=', num2str(TxSite_SIZE.dist_avg_shift,'%10.0f'),' nm'])
    title('Centered data')
    xlabel('Distance from center in nm')
    ylabel('# of mRNA')
    
end





