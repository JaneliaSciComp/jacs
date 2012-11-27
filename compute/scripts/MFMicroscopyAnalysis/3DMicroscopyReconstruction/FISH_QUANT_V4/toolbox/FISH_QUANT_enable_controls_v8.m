function FISH_QUANT_enable_controls_v8(handles)

%- Change name of GUI
if not(isempty(handles.file_name_image))
    set(handles.h_fishquant,'Name', ['FISH-QUANT ', handles.version, ': main interface - ', handles.file_name_image ]);
else
    set(handles.h_fishquant,'Name', ['FISH-QUANT ', handles.version, ': main interface']);
end

% ==== OUTLINE SELECTION AND IMAGEJ visualization
% Only if image was loaded

if handles.status_image

    set(handles.pop_up_outline_sel_cell,'Enable','on')
    %set(handles.button_correct_bgd,'Enable','on')
    
    %- Enable filtering
    set(handles.button_outline_define,'Enable','on')
    set(handles.button_filter,'Enable','on')
    set(handles.text_kernel_factor_bgd,'Enable','on')
    set(handles.text_kernel_factor_filter,'Enable','on')

    %- Enable plots in image
    set(handles.pop_up_image_select,'Enable','on')
    set(handles.button_plot_image,'Enable','on')
    set(handles.checkbox_plot_outline,'Enable','on')

else

    set(handles.pop_up_outline_sel_cell,'Enable','off')
%    set(handles.button_correct_bgd,'Enable','off')
    
    %- Enable filtering
    set(handles.button_outline_define,'Enable','off')
    set(handles.button_filter,'Enable','off')
    set(handles.text_kernel_factor_bgd,'Enable','off')
    set(handles.text_kernel_factor_filter,'Enable','off')

    %- Enable ImageJ plots and plots in image
 %   if handles.flag_JAVA_init == 1
 %       set(handles.button_visualize_imagej,'Enable','off')
%        set(handles.pop_up_imagej_style,'Enable','off')
%        set(handles.pop_up_imagej_1st,'Enable','off')
 %   end

    %- Enable plots in image
    set(handles.pop_up_image_select,'Enable','off')
    set(handles.button_plot_image,'Enable','off')
    set(handles.checkbox_plot_outline,'Enable','off') 
end 

% ==== Outline definition
if not(isempty(handles.cell_prop))
    set(handles.menu_save_outline,'Enable','on')
else
    set(handles.menu_save_outline,'Enable','off')
end


% ==== Filtered image
if handles.status_filtered
    set(handles.menu_save_filtered_img,'Enable','on')
else
    set(handles.menu_save_filtered_img,'Enable','off')
end


% ==== PSF values
if isempty(handles.PSF_exp)
    set(handles.pop_up_select_psf,'Enable','off')
else
    set(handles.pop_up_select_psf,'Enable','on')
end


% ==== PRE-DETECTION
% Only if image was filtered
if handles.status_filtered 
    set(handles.button_predetect,'Enable','on')
   % set(handles.text_detection_threshold,'Enable','on')
  %  set(handles.pop_up_detect_quality,'Enable','on')
    
   % set(handles.text_detect_region_xy,'Enable','on')
   % set(handles.text_detect_region_z,'Enable','on')

else
    set(handles.button_predetect,'Enable','off')
%    set(handles.text_detection_threshold,'Enable','off')
%    set(handles.pop_up_detect_quality,'Enable','off')
  %  
   % set(handles.text_detect_region_xy,'Enable','off')
   % set(handles.text_detect_region_z,'Enable','off')    
end


if not(isempty(handles.cell_prop))
    
    ind_cell  = get(handles.pop_up_outline_sel_cell,'Value');
    cell_prop = handles.cell_prop(ind_cell);
    

    % === Enable text region also if fitted results are present 
    %     > useful for fitting averaged spot
%     if handles.status_filtered  || cell_prop.status_fit
% 
%         set(handles.text_detect_region_xy,'Enable','on')
%         set(handles.text_detect_region_z,'Enable','on')
%     else
%         set(handles.text_detect_region_xy,'Enable','off')
%         set(handles.text_detect_region_z,'Enable','off')
%     end


    % ==== Fit
    % Only afer pre-detection
    if cell_prop.status_detect
        set(handles.button_fit_3d,'Enable','on')
    else
        set(handles.button_fit_3d,'Enable','off')
    end
    
    %===== Fit with averaged width
    str_x = (get(handles.text_psf_fit_sigmaX,'String'));
    str_y = (get(handles.text_psf_fit_sigmaY,'String'));
    str_z = (get(handles.text_psf_fit_sigmaZ,'String'));

    if not(isempty(str_x)) && not(isempty(str_y)) && not(isempty(str_z)) 
        set(handles.checkbox_fit_fixed_width,'Enable','on')
    else
        set(handles.checkbox_fit_fixed_width,'Enable','off') 
    end
    

    % ==== After fit is done
    if cell_prop.status_fit
        
        %- Enable fit with fixed width
        %set(handles.checkbox_fit_fixed_width,'Enable','on')

        %- Enable thresholding
        set(handles.button_threshold,'Enable','on')
        set(handles.pop_up_threshold,'Enable','on')
        set(handles.slider_th_min,'Enable','on')
        set(handles.slider_th_max,'Enable','on')
        set(handles.text_th_min,'Enable','on')
        set(handles.text_th_max,'Enable','on')
        set(handles.checkbox_th_lock,'Enable','on')
        set(handles.button_th_unlock_all,'Enable','on')        
        set(handles.button_visualize_matlab,'Enable','on')  
        

        %- Enable plot of fitted spots
        set(handles.pop_up_image_spots,'Enable','on')

        %- Selection of different parameters of PSF
        set(handles.pop_up_select_psf,'Enable','on')
        
        %- Enable averaging of spots
        set(handles.menu_avg_calc,'Enable','on')
        
        
    else
        %- Enable fit with fixed width
       % set(handles.checkbox_fit_fixed_width,'Enable','off')

        %- Enable thresholding
        set(handles.button_threshold,'Enable','off')
        set(handles.pop_up_threshold,'Enable','off')
        set(handles.slider_th_min,'Enable','off')
        set(handles.slider_th_max,'Enable','off')
        set(handles.text_th_min,'Enable','off')
        set(handles.text_th_max,'Enable','off')
        set(handles.checkbox_th_lock,'Enable','off')
        set(handles.button_th_unlock_all,'Enable','off')
        set(handles.button_visualize_matlab,'Enable','off')
        
        %- Enable plot of fitted spots
        set(handles.pop_up_image_spots,'Enable','off')

        %- Selection of different parameters of PSF
        set(handles.pop_up_select_psf,'Enable','off')
        
        %- Enable averaging of spots
        set(handles.menu_avg_calc,'Enable','off')
        
    end
       


    
    %====== Averaged spot
    
    if cell_prop.status_avg
        set(handles.menu_spot_avg_construct,'Enable','on')
        set(handles.menu_avg_fit,'Enable','on')
        set(handles.menu_spot_avg_construct,'Enable','on')
        set(handles.menu_spot_avg_radial_avg,'Enable','on')
        set(handles.menu_spot_avg_imagej_ns,'Enable','on')
        set(handles.menu_spot_avg_imagej_os,'Enable','on')
    else
        set(handles.menu_spot_avg_construct,'Enable','off')
        set(handles.menu_avg_fit,'Enable','off')
        set(handles.menu_spot_avg_construct,'Enable','off')
        set(handles.menu_spot_avg_radial_avg,'Enable','off')
        set(handles.menu_spot_avg_imagej_ns,'Enable','off')
        set(handles.menu_spot_avg_imagej_os,'Enable','off')
    end
    
    
    %====== Radial averaged PSF
    if cell_prop.status_avg_rad
        set(handles.menu_spot_rad_avg_construct,'Enable','on')
        set(handles.menu_spot_avg_imagej_radial,'Enable','on')
    else
        set(handles.menu_spot_rad_avg_construct,'Enable','off')
        set(handles.menu_spot_avg_imagej_radial,'Enable','off')    
    end
    
    
    %====== Constructed spot
    if cell_prop.status_avg_con
        set(handles.menu_spot_avg_imagej_const_ns,'Enable','on')
        set(handles.menu_spot_contruct_fit,'Enable','on')
    else
        set(handles.menu_spot_avg_imagej_const_ns,'Enable','off')
        set(handles.menu_spot_contruct_fit,'Enable','off')
    end
    
    
    
%- No spot results defined    
else    
    set(handles.button_fit_3d,'Enable','off')
    set(handles.checkbox_fit_fixed_width,'Enable','off')
    set(handles.button_threshold,'Enable','off')
    set(handles.pop_up_threshold,'Enable','off')
    set(handles.slider_th_min,'Enable','off')
    set(handles.slider_th_max,'Enable','off')
    set(handles.text_th_min,'Enable','off')
    set(handles.text_th_max,'Enable','off')
    set(handles.checkbox_th_lock,'Enable','off')
    set(handles.button_th_unlock_all,'Enable','off')
    set(handles.button_visualize_matlab,'Enable','off')
    set(handles.pop_up_image_spots,'Enable','off')
    set(handles.pop_up_select_psf,'Enable','off')
    set(handles.menu_avg_calc,'Enable','off')
    set(handles.menu_spot_avg_construct,'Enable','off')
    set(handles.menu_avg_fit,'Enable','off')
    set(handles.menu_spot_avg_construct,'Enable','off')
    set(handles.menu_spot_avg_radial_avg,'Enable','off')
    set(handles.menu_spot_avg_imagej_ns,'Enable','off')
    set(handles.menu_spot_avg_imagej_os,'Enable','off')
    set(handles.menu_spot_rad_avg_construct,'Enable','off')
    set(handles.menu_spot_avg_imagej_radial,'Enable','off')  
    set(handles.menu_spot_avg_imagej_const_ns,'Enable','off')
    set(handles.menu_spot_contruct_fit,'Enable','off')
end



%==== PSF

%- Amplitudes
% if isfield(handles,'mRNA_prop')
%     if isempty(handles.mRNA_prop)
%         set(handles.menu_TS_quant,'Enable','off')
%     else
%         set(handles.menu_TS_quant,'Enable','on')
%         
%     end
% else
%     set(handles.menu_TS_quant,'Enable','off')    
% end
% 
%        
% %- Results of quantification
% if isfield(handles,'TS_summary')
%     if isempty(handles.TS_summary)
%         set(handles.menu_TS_save,'Enable','off')
%         set(handles.menu_TS_ImageJ,'Enable','off')        
%     else
%         set(handles.menu_TS_save,'Enable','on')
%         set(handles.menu_TS_ImageJ,'Enable','on')
%     end
% else
%     set(handles.menu_TS_save,'Enable','off') 
%     set(handles.menu_TS_ImageJ,'Enable','off') 
% end

