function  TxSite_reconstruct_Output_v1(TxSite_quant, REC_prop, parameters)


%- Get parameters
flags       = parameters.flags;
factor_Q_ok = parameters.factor_Q_ok;

%- Images
img_res         = REC_prop.img_res;
img_fit         = REC_prop.img_fit;
img_TS_crop_xyz = REC_prop.img_TS;
img_bgd         = REC_prop.img_bgd;
coord           = REC_prop.coord;

%- Save properties of reconstruction
summary_Q_run_N_MRNA = REC_prop.summary_Q_run_N_MRNA;
summary_Q_run        = REC_prop.summary_Q_run;
mean_Q               = REC_prop.mean_Q;
Q_norm               = REC_prop.Q_norm;


if flags.output

    %=== Display results
    disp(' .... ')
    disp(['Range of Q: number of mRNA at TS (mean)    : ' , num2str( TxSite_quant.N_mRNA_Q_limit.mean)]);
    disp(['Range of Q: number of mRNA at TS (LB)      : ' , num2str( TxSite_quant.N_mRNA_Q_limit.lb)]);
    disp(['Range of Q: number of mRNA at TS (UB)      : ' , num2str( TxSite_quant.N_mRNA_Q_limit.ub)]);
    disp(['Range of Q: number of mRNA at TS (stdev)   : ' , num2str( TxSite_quant.N_mRNA_Q_limit.stdev)]);
    disp(' .... ')
    disp(['Best 10%: number of mRNA at TS (mean)      : ' , num2str(TxSite_quant.N_mRNA_TS_mean_10per)]);
    disp(['Best 10%: number of mRNA at TS (STDEV)     : ' , num2str(TxSite_quant.N_mRNA_TS_std_10per)]);
    disp(['Quality score (best 10%)                   : ' , num2str(TxSite_quant.Q_avg_10per)]);    
    disp(' .... ')
    disp(['Number of mRNA at TS (trad. method)        : ' , num2str(TxSite_quant.N_mRNA_trad)]);   
    disp(['Number of mRNA at TS (ratio fitted AMP)    : ' , num2str(TxSite_quant.N_mRNA_fitted_amp)]);     
    disp(['Number of mRNA at TS (ratio integrated INT): ' , num2str(TxSite_quant.N_mRNA_integrated_int)]); 
end 
    
if flags.output == 2
   
      
    %=== Show residuals

    figure
    subplot(3,1,1)
    plot(summary_Q_run_N_MRNA,summary_Q_run)
    xlabel('Number of placed mRNA')
    ylabel('Quality score')    

    subplot(3,1,2)
    plot(summary_Q_run_N_MRNA,mean_Q)
    xlabel('Number of placed mRNA')
    ylabel('Quality score') 
    
    subplot(3,1,3), hold on
    plot(summary_Q_run_N_MRNA,Q_norm)
    v = axis;
    plot([0 max(summary_Q_run_N_MRNA)], [factor_Q_ok factor_Q_ok],'k')
    axis(v)
    box on
    xlabel('Number of placed mRNA')
    ylabel('Quality score [NORM]')    
    
    %=== Plot histogram of residuals
    img_diff_bgd = img_TS_crop_xyz-img_bgd;
    img_diff_fit = img_TS_crop_xyz-img_fit;
    figure, 
    subplot(2,1,1)
    hist(img_diff_bgd(:),200)
    title(['Resid with only bgd subtraction: ', num2str(round(sum((img_diff_bgd(:)))))])
    
    subplot(2,1,2)
    hist(img_diff_fit(:),200)
    title(['Resid with FIT: ', num2str(round(sum((img_diff_fit(:)))))])

    %=== Plot distribution of mRNA in 3D
    figure
    plot3(REC_prop.pos_best(:,2),REC_prop.pos_best(:,1),REC_prop.pos_best(:,3),'or')
    grid on
    axis square
    
    %=== Plot images
    
    img_proj_max_xy  = max(img_TS_crop_xyz,[],3);
    img_proj_max_yz  = squeeze(max(img_TS_crop_xyz,[],2))';  
    img_proj_max_xz  = squeeze(max(img_TS_crop_xyz,[],1))';
    
    
    img_fit_proj_max_xy  = max(img_fit,[],3);
    img_fit_proj_max_yz  = squeeze(max(img_fit,[],2))';  
    img_fit_proj_max_xz  = squeeze(max(img_fit,[],1))';
    
    res_proj_max_xy  = max(img_res,[],3);
    res_proj_max_yz  = squeeze(max(img_res,[],2))';  
    res_proj_max_xz  = squeeze(max(img_res,[],1))';

    img_res_neg = abs(img_res .* (img_res<0));
    
    res_neg_proj_max_xy  = max(img_res_neg,[],3);
    res_neg_proj_max_yz  = squeeze(max(img_res_neg,[],2))';  
    res_neg_proj_max_xz  = squeeze(max(img_res_neg,[],1))';
    
    % Min & max of image
    TS_max  = max(img_TS_crop_xyz(:));
    FIT_max = max(img_fit(:));
    
    if TS_max >= FIT_max
        img_max = TS_max;
    else
        img_max = FIT_max;
    end
    
    
    TS_min  = min(img_TS_crop_xyz(:));
    FIT_min = min(img_fit(:));
    
    if TS_min <= FIT_min
        img_min = TS_min;
    else
        img_min = FIT_min;
    end
       
    %% === Images of TxSite, reconstruction, and residuals
    figure    
    
    %- TxSite    
    subplot(3,5,1)
    imshow(img_proj_max_xy,[img_min img_max],'XData',coord.X_nm,'YData',coord.Y_nm)
    title('TS - XY')
    colorbar
    axis image

    subplot(3,5,6)
    imshow(img_proj_max_yz,[ img_min img_max],'XData',coord.Y_nm,'YData',coord.Z_nm);
    title('TS - YZ')
    colorbar
    axis image

    subplot(3,5,11)
    imshow(img_proj_max_xz,[ img_min img_max],'XData',coord.X_nm,'YData',coord.Z_nm);
    title('TS - XZ')
    colorbar
    axis image
    
    %- Fit
    subplot(3,5,2)
    hold on
    imshow(img_fit_proj_max_xy,[img_min img_max ],'XData',coord.X_nm,'YData',coord.Y_nm)
    plot(REC_prop.pos_best(:,2),REC_prop.pos_best(:,1),'g+')
    hold off
    title('Fit - XY')
    colorbar
    axis image

    subplot(3,5,7)
    hold on
    imshow(img_fit_proj_max_yz,[ img_min img_max],'XData',coord.X_nm,'YData',coord.Z_nm);
    plot(REC_prop.pos_best(:,1),REC_prop.pos_best(:,3),'g+')
    hold off
    title('Fit - YZ')
    colorbar
    axis image

    subplot(3,5,12)
    hold on
    imshow(img_fit_proj_max_xz,[ img_min img_max],'XData',coord.X_nm,'YData',coord.Z_nm);
    plot(REC_prop.pos_best(:,2),REC_prop.pos_best(:,3),'g+')
    hold off
    title('Fit - XZ')
    colorbar
    axis image
    
    %- Residuals
    subplot(3,5,3)
    imshow(res_proj_max_xy,[ 0 1*img_max],'XData',coord.X_nm,'YData',coord.Y_nm)
    title('RES - XY')
    colorbar
    axis image

    subplot(3,5,8)
    imshow(res_proj_max_yz,[ 0 1*img_max],'XData',coord.Y_nm,'YData',coord.Z_nm);
    title('RES - YZ')
    colorbar
    axis image

    subplot(3,5,13)
    imshow(res_proj_max_xz,[ 0 1*img_max],'XData',coord.X_nm,'YData',coord.Z_nm);
    title('RES - XZ')
    colorbar
    axis image
    
    %- Residuals [POS]
    subplot(3,5,4)
    imshow(res_proj_max_xy,[ ],'XData',coord.X_nm,'YData',coord.Y_nm)
    title('RES [pos] - XY')
    colorbar
    axis image

    subplot(3,5,9)
    imshow(res_proj_max_yz,[ ],'XData',coord.Y_nm,'YData',coord.Z_nm);
    title('RES [pos] - YZ')
    colorbar
    axis image

    subplot(3,5,14)
    imshow(res_proj_max_xz,[ ],'XData',coord.X_nm,'YData',coord.Z_nm);
    title('RES [pos] - XZ')
    colorbar
    axis image
    
    %- Residuals [NEG]
    subplot(3,5,5)
    imshow(res_neg_proj_max_xy,[ ],'XData',coord.X_nm,'YData',coord.Y_nm)
    title('RES [neg] - XY')
    colorbar
    axis image

    subplot(3,5,10)
    imshow(res_neg_proj_max_yz,[ ],'XData',coord.Y_nm,'YData',coord.Z_nm);
    title('RES [neg] - YZ')
    colorbar
    axis image

    subplot(3,5,15)
    imshow(res_neg_proj_max_xz,[ ],'XData',coord.X_nm,'YData',coord.Z_nm);
    title('RES [neg] - XZ')
    colorbar
    axis image
    
    colormap Hot
    
end