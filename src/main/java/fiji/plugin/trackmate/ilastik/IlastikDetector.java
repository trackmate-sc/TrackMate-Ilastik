/*-
 * #%L
 * TrackMate: your buddy for everyday tracking.
 * %%
 * Copyright (C) 2020 - 2023 TrackMate developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package fiji.plugin.trackmate.ilastik;

import java.io.UncheckedIOException;

import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.detection.SpotGlobalDetector;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class IlastikDetector< T extends RealType< T > & NativeType< T > > implements SpotGlobalDetector< T >
{

	private final static String BASE_ERROR_MESSAGE = "IlastikDetector: ";

	protected final ImgPlus< T > img;

	protected final Interval interval;

	protected final String classifierPath;

	protected final int classIndex;

	protected final double probaThreshold;

	protected final double smoothingScale;

	private final int channel;

	protected String baseErrorMessage;

	protected String errorMessage;

	protected long processingTime;

	protected SpotCollection spots;



	/**
	 * Instantiate an ilastik detector.
	 * 
	 * @param img
	 *            source image, possibly multiple frames, possibly multiple Zs,
	 *            possibly multiple channels.
	 * @param interval
	 *            the interval on which to operate.
	 * @param channel
	 *            the channel in the source image on which to operate when a
	 *            model trained on a single channel is specified.
	 * @param classifierPath
	 *            the path to the ilastik project containing the classifier.
	 * @param classIndex
	 *            the index of the class to extract.
	 * @param probaThreshold
	 *            a threshold on the probability map to extract objects.
	 */
	public IlastikDetector(
			final ImgPlus< T > img,
			final Interval interval,
			final int channel,
			final String classifierPath,
			final int classIndex,
			final double probaThreshold,
			final double smoothingScale )
	{
		this.img = img;
		this.interval = interval;
		this.channel = channel;
		this.classifierPath = classifierPath;
		this.classIndex = classIndex;
		this.probaThreshold = probaThreshold;
		this.smoothingScale = smoothingScale;
		this.baseErrorMessage = BASE_ERROR_MESSAGE;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		/*
		 * Run Ilastik.
		 */
		
		try
		{
			final IlastikRunner< T > runner = new IlastikRunner<>( classifierPath );
			runner.computeProbabilities( img, channel, interval, classIndex );
			final boolean simplify = true;
			spots = runner.getSpotsFromLastProbabilities( probaThreshold, simplify, smoothingScale );
		}
		catch ( final UncheckedIOException e )
		{
			errorMessage = BASE_ERROR_MESSAGE + "Problem accessing the Ilastik executable or the project file:\n" + e.getMessage();
			e.printStackTrace();
			return false;
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;

		return true;
	}

	@Override
	public SpotCollection getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == img )
		{
			errorMessage = baseErrorMessage + "Image is null.";
			return false;
		}
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}
}
