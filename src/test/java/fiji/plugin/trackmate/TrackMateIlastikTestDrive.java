/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2020 - 2021 The Institut Pasteur.
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

import net.imagej.Dataset;
import net.imagej.ImageJ;

public class TrackMateIlastikTestDrive
{

	public static void main( final String[] args ) throws IOException
	{
		final ImageJ ij = new ImageJ();
		ij.launch( args );
//		final String path = "D:\\Projects\\NVerttiQuintero\\Data\\Series014b.tif";
		final String path = "D:\\Projects\\LLeBlanc\\Data\\201125_Proliferation_2C43-PilQmCherry-HupmRhubarb-II4_100X_timestep5min_Stage3_reg.tif";
//		"D:/Projects/NVerttiQuintero/Data/Series014b.tif"
		final Dataset dataset = ( Dataset ) ij.io().open( path );
		ij.ui().show( dataset );

		new TrackMatePlugIn().run( null );
	}

}
