package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

/**
 * Created with IntelliJ IDEA.
 * User: fosterl
 * Date: 4/11/13
 * Time: 10:20 AM
 *
 * This is an exchange bean for settings, so that load-workers do not have to know about the GUI, and so that
 * multiple small settings can be kept together and cut down parameter list sizes.
 */
public class AlignmentBoardSettings {
    public static final double DEFAULT_GAMMA = 1.0;

    private double chosenDownSampleRate;
    private double gammaFactor =  DEFAULT_GAMMA;
    private boolean showChannelData;
    private double downSampleGuess;

    public AlignmentBoardSettings() {
        super();
    }

    public AlignmentBoardSettings( double downSampleRate, double downSampleGuess, double gammaFactor, boolean showChannelData ) {
        this();
        this.chosenDownSampleRate = downSampleRate;
        this.gammaFactor = gammaFactor;
        this.showChannelData = showChannelData;
        this.downSampleGuess = downSampleGuess;
    }

    public double getChosenDownSampleRate() {
        return chosenDownSampleRate;
    }

    public void setChosenDownSampleRate(double downSampleRate) {
        this.chosenDownSampleRate = downSampleRate;
    }

    /**
     * This "guess" is determined from graphics card, but never serialized.  If the user picks something
     * in particular, that will be used.  But if not, their guess will be used instead.
     *
     * @param downSampleRate
     */
    public void setDownSampleGuess(double downSampleRate) {
        this.downSampleGuess = downSampleRate;
    }

    public double getDownSampleGuess() {
        return downSampleGuess;
    }

    /** Call this to get the downsample rate that is actually used onscreen. */
    public double getAcceptedDownsampleRate() {
        if ( chosenDownSampleRate == 0.0 ) {
            return getDownSampleGuess();
        }
        else {
            return getChosenDownSampleRate();
        }
    }

    public double getGammaFactor() {
        return gammaFactor;
    }

    public void setGammaFactor(double gammaFactor) {
        this.gammaFactor = gammaFactor;
    }

    public boolean isShowChannelData() {
        return showChannelData;
    }

    public void setShowChannelData(boolean showChannelData) {
        this.showChannelData = showChannelData;
    }

    public AlignmentBoardSettings clone() throws CloneNotSupportedException {
        //super.clone();
        return new AlignmentBoardSettings( getChosenDownSampleRate(), getDownSampleGuess(), getGammaFactor(), isShowChannelData() );
    }
}
