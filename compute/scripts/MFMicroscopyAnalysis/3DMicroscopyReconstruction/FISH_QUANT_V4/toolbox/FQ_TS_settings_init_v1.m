function parameters_quant = FQ_TS_settings_init_v1

parameters_quant.flags.placement       = 2;    % 1 ...random placement
parameters_quant.flags.quality         = 1;    % asr = 2, ssr = 1 
parameters_quant.N_reconstruct         = 100;
parameters_quant.N_run_prelim          = 100;
parameters_quant.nBins                 = 50;
parameters_quant.per_avg_bgd           = 0.95;
parameters_quant.crop_image.xy_nm      = 500;
parameters_quant.crop_image.z_nm       = 1000;
parameters_quant.factor_Q_ok           = 1.5;

parameters_quant.flags.posWeight   = 1;   % 1 to recalc position weighting vector after placement of each PSF, 0 to use only image of TS
parameters_quant.flags.bgd_local   = 1;   % For local background measurement from actual image, 1 with defined threshold, 2 with defined threshold and adjustment possiblity
parameters_quant.flags.crop        = 1;   % [0] no crop, [1] specified size, [2] padding (for simulated sites)
parameters_quant.flags.psf         = 2;   % 1: model, 2: image
parameters_quant.flags.shift       = 1;   % Shift yes (1) - no (0)  


parameters_quant.conn = 6;
parameters_quant.min_dist = 10;         % Minimum distance between identified components