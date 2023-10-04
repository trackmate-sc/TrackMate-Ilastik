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

import java.io.IOException;

public class TrackMateIlastikTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
//		final ImageJ ij = new ImageJ();
//		ij.launch( args );
//		final String path = "D:\\Projects\\NVerttiQuintero\\Data\\Series014b.tif";
//		final String path = "D:\\Projects\\LLeBlanc\\Data\\201125_Proliferation_2C43-PilQmCherry-HupmRhubarb-II4_100X_timestep5min_Stage3_reg.tif";
//		final String path = "/Users/tinevez/Development/TrackMateWS/TrackMate-Ilastik/samples/NeisseriaMeningitidisGrowth.tif";
		final String path = "/Users/tinevez/Development/TrackMateWS/TrackMate-Ilastik/samples/NeisseriaMeningitidisGrowth-mini.tif";
//		"D:/Projects/NVerttiQuintero/Data/Series014b.tif"
//		final Dataset dataset = ( Dataset ) ij.io().open( path );
//		ij.ui().show( dataset );

		ij.ImageJ.main( args );
		new TrackMatePlugIn().run( path );
	}

}
