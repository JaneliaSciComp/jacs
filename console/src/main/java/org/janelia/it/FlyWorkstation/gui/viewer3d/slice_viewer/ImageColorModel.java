package org.janelia.it.FlyWorkstation.gui.viewer3d.slice_viewer;

import java.awt.Color;
import java.util.Vector;
import org.janelia.it.FlyWorkstation.gui.viewer3d.interfaces.VolumeImage3d;

public class ImageColorModel 
{
	private static Color defaultChannelColors[] = {
		Color.red,
		Color.green,
		Color.blue,
		Color.lightGray,
		Color.orange,
		Color.yellow,
		Color.pink,
		Color.magenta,
	};
	
	private Vector<ChannelColorModel> channels = new Vector<ChannelColorModel>();
	private boolean blackSynchronized = true;
	private boolean gammaSynchronized = true;
	private boolean whiteSynchronized = true;

	private Signal colorModelChangedSignal = new Signal();
	private Signal colorModelInitializedSignal = new Signal();
	
	/*
	public ImageColorModel() {
		init(3, 8); // Default to 3 8-bit channels
	}
	**/
	
	public ImageColorModel(VolumeImage3d volumeImage) 
	{
		reset(volumeImage);
	}

	private void addChannel(Color color, int bitDepth) {
		int c = channels.size();
		ChannelColorModel channel = new ChannelColorModel(
				c, color, bitDepth);
		channels.add(channel);
		// connect signals
		channel.getBlackLevelChangedSignal().connect(colorModelChangedSignal);
		channel.getColorChangedSignal().connect(colorModelChangedSignal);
		channel.getDataMaxChangedSignal().connect(colorModelChangedSignal);
		channel.getGammaChangedSignal().connect(colorModelChangedSignal);
		channel.getWhiteLevelChangedSignal().connect(colorModelChangedSignal);
		channel.getVisibilityChangedSignal().connect(colorModelChangedSignal);
	}

	public Signal getColorModelInitializedSignal() {
		return colorModelInitializedSignal;
	}

	public boolean isBlackSynchronized() {
		return blackSynchronized;
	}

	public void setBlackSynchronized(boolean blackSynchronized) {
		this.blackSynchronized = blackSynchronized;
	}

	public boolean isGammaSynchronized() {
		return gammaSynchronized;
	}

	public void setGammaSynchronized(boolean gammaSynchronized) {
		this.gammaSynchronized = gammaSynchronized;
	}

	public boolean isWhiteSynchronized() {
		return whiteSynchronized;
	}

	public void setWhiteSynchronized(boolean whiteSynchronized) {
		this.whiteSynchronized = whiteSynchronized;
	}

	public Signal getColorModelChangedSignal() {
		return colorModelChangedSignal;
	}

	public ChannelColorModel getChannel(int channelIndex) {
		return channels.get(channelIndex);
	}
	
	public int getChannelCount() {
		return channels.size();
	}
	
	private void init(int channelCount, int bitDepth) {
		channels.clear();
		for (int c = 0; c < channelCount; ++c) {
			int ix = c % defaultChannelColors.length;
			addChannel(defaultChannelColors[ix], bitDepth);
		}
		// System.out.println("model channel count = "+channelCount);
		resetColors(); // in case 1 or 2 channels
		colorModelInitializedSignal.emit();
	}
	
	public void reset(VolumeImage3d volumeImage) 
	{
		int maxI = volumeImage.getMaximumIntensity();
		assert maxI <= 65535;
		int bitDepth = 8;
		if (maxI > 255)
			bitDepth = 16;
		int hardMax = (int)Math.pow(2.0, bitDepth) - 1;
		init(volumeImage.getNumberOfChannels(), bitDepth);
		if (hardMax != maxI) {
			for (ChannelColorModel ccm : channels)
				ccm.setWhiteLevel(maxI);
		}
	}

	public void resetColors() 
	{
		for (ChannelColorModel channel : channels) {
			channel.resetContrast();
			channel.setVisible(true);
		}
		// Set default colors
		// Show single channel as grayscale
		if (getChannelCount() == 1) {
			getChannel(0).setColor(Color.white);
		}
		else if (getChannelCount() == 2) {
			// magenta/green uses orthogonal primaries and balances brightness
			getChannel(0).setColor(Color.magenta);
			getChannel(1).setColor(Color.green);
		}
		else {
			for (ChannelColorModel channel : channels) {
				int ix = channel.getIndex();
				ix = ix % defaultChannelColors.length; // in case we have so many channels, repeat
				channel.setColor(defaultChannelColors[ix]);
			}
		}
	}

}
