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
package fiji.plugin.trackmate;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fiji.plugin.trackmate.detection.DetectorKeys;
import fiji.plugin.trackmate.ilastik.IlastikDetectorFactory;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;

public class TrackMateIlastikTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final File root = new File( "/Users/tinevez/Google Drive/Cours/Gothenburg-CCI-course-Sept2023/GothenburgCCICourse-Day4-datasets/IlastikTutorialMaterial" );

		// 2D+T
		final String imageName = "2DFocalAdhesion/MDA231 Paxillin DMSO 01.tif";
		final String classifierName = "2DFocalAdhesion/2DFocalAdhesion.ilp";

		// 3D.
//		final String imageName = "3DEmbryo/CelegansEmbryo.tif";
//		final String classifierName = "3DEmbryo/3DEmbryo.ilp";

		final String imagePath = new File(root, imageName).getAbsolutePath();
		final String classifierPath = new File(root, classifierName).getAbsolutePath();

		ImageJ.main( args );
		final ImagePlus imp = IJ.openImage( imagePath );
		imp.show();

		// Inject ilastik settings.
		final TrackMatePlugIn plugin = new TrackMatePlugIn()
		{
			@Override
			protected Settings createSettings( final ImagePlus imp )
			{
				final Settings settings = super.createSettings( imp );

				final Map< String, Object > detectorSettings = new HashMap<>();
				detectorSettings.put( DetectorKeys.KEY_TARGET_CHANNEL, 1 );
				detectorSettings.put( IlastikDetectorFactory.KEY_CLASSIFIER_FILEPATH, classifierPath );
				detectorSettings.put( IlastikDetectorFactory.KEY_CLASS_INDEX, 0 );
				detectorSettings.put( IlastikDetectorFactory.KEY_PROBA_THRESHOLD, 0.5 );

				return settings;
			}
		};
		plugin.run( null );
	}
}
