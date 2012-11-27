function mRNA_prop = FQ_AMP_analyze_v1(spots_fit,thresh,h_plot) 

%-- Analyze distribution of amplitudes
int_th  = logical(thresh.in);
amp_th = spots_fit(int_th,10);

%-- Mean value and standard deviation
mu3 = mean(amp_th); % Data mean
sigma3 = std(amp_th); % Data standard deviation

outliers = (amp_th - mu3) > 3*sigma3;
amp_th_all = amp_th; % Copy c3 to c3m
amp_th(outliers) = []; % Add NaN valu

%- Histogram
[count_amp bin_amp] = hist(amp_th,30);
count_amp_norm = count_amp/max(count_amp);

% Skewness
mRNA_prop.amp_skew = skewness(amp_th);
mRNA_prop.amp_kurt = kurtosis(amp_th);

%- Fit with normal distribution
[mRNA_prop.amp_mean,mRNA_prop.amp_sigma] = normfit(amp_th);
amp_fit          = normpdf(bin_amp,mRNA_prop.amp_mean,mRNA_prop.amp_sigma);
amp_fit_norm     = amp_fit/max(amp_fit);

%- Consider skewness of fit
[rand_n type] = pearsrnd(mRNA_prop.amp_mean,mRNA_prop.amp_sigma,mRNA_prop.amp_skew ,mRNA_prop.amp_kurt,100000,1);
[count_rand bin_rand] = hist(rand_n,30);
count_rand_norm       = count_rand/max(count_rand);

%- Plot results
axes(h_plot)
cla(h_plot,'reset')
hold on
bar(bin_amp,count_amp_norm,'FaceColor',[0.7 0.7 0.7])
plot(bin_amp,amp_fit_norm,'-b')
plot(bin_rand,count_rand_norm,'-r')
hold on

box on
legend('Experiment','Normal distribution','Skewed normal distribution')
xlabel('Amplitude')
ylabel('Normalized count')

disp(' '); disp('Fit with skewed normal distribution')
disp(['Mean:     ', num2str(mRNA_prop.amp_mean )])
disp(['Sigma:    ', num2str(mRNA_prop.amp_sigma)])
disp(['Skewness: ', num2str(mRNA_prop.amp_skew)])
disp(['Kurtosis: ', num2str(mRNA_prop.amp_kurt)])
disp(' ');
