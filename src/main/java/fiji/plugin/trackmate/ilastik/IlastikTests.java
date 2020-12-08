package fiji.plugin.trackmate.ilastik;

import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.ilastik.ilastik4ij.executors.AbstractIlastikExecutor.PixelPredictionType;
import org.ilastik.ilastik4ij.executors.PixelClassification;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.display.imagej.ImgPlusViews;
import net.imglib2.type.BooleanType;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;

public class IlastikTests
{

	public static < T extends RealType< T > & NativeType< T > > void main( final String[] args ) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException
	{
		Locale.setDefault( Locale.ROOT );
		UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() );

		final ImageJ ij = new ImageJ();
		ij.launch( args );
		final LogService logService = ij.log();
		final StatusService statusService = ij.status();

		final File executableFilePath = new File( "C:/Program Files/ilastik-1.3.3post3/ilastik.exe" );
		final File projectFileName = new File( "D:/Projects/NVerttiQuintero/Ilastik/NVertti.ilp" );
		final int numThreads = Runtime.getRuntime().availableProcessors() / 2;
		final int maxRamMb = 16_000;
		final PixelClassification classifier = new PixelClassification(
				executableFilePath,
				projectFileName,
				logService,
				statusService,
				numThreads,
				maxRamMb );

		System.out.println( "Loading image." );
		final String imagePath = "D:/Projects/NVerttiQuintero/Data/Series063a-ch1.tif";
		final Dataset dataset = ( Dataset ) ij.io().open( imagePath );
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > input = ( ImgPlus< T > ) dataset.getImgPlus();

		System.out.println( "Running prediction." );
//		final PixelPredictionType predictionType = PixelPredictionType.Segmentation;
		final PixelPredictionType predictionType = PixelPredictionType.Probabilities;
		final ImgPlus< T > output = classifier.classifyPixels( input, predictionType );

		System.out.println( "Done." );
//		ij.ui().show( output );

		final ImagePlus imp = ImageJFunctions.show( output );

		final long targetChannel = 0;
		final ImgPlus< T > proba = ImgPlusViews.hyperSlice( output, output.dimensionIndex( Axes.CHANNEL ), targetChannel );

		final long nTimepoints = proba.dimension( proba.dimensionIndex( Axes.TIME ) );
		
		// Threshold on probabiliyt map.
		final double threshold = 0.5;
		// Smoothing interval for ROIs.
		final double smoothInterval = 2.;
		// Douglas-Peucker polygon simplification max distance.
		final double epsilon = 0.5;

		final Converter< T, BitType > thresholder = ( r, b) -> b.set( r.getRealDouble() > threshold  );
		
		final RoiManager roiManager = RoiManager.getRoiManager();
		for ( int t = 0; t < nTimepoints; t++ )
		{
			imp.setT( t + 1 );
			final ImgPlus< T > frame = ImgPlusViews.hyperSlice( proba, proba.dimensionIndex( Axes.TIME ), t );
			final RandomAccessibleInterval< BitType > mask = Converters.convertRAI( frame, thresholder, new BitType() );
			final List< Polygon > spots = maskToSpots( mask );
			for ( final Polygon polygon : spots )
			{
				final PolygonRoi roi = new PolygonRoi( polygon, PolygonRoi.POLYGON );
				final PolygonRoi fRoi = simplify( roi, smoothInterval, epsilon );
				fRoi.setPosition( ( int ) targetChannel, 0, t + 1 );
				roiManager.addRoi( fRoi );
			}
		}
		roiManager.setVisible( true );
	}

	public static final PolygonRoi simplify( final PolygonRoi roi, final double smoothInterval, final double epsilon )
	{
		final FloatPolygon fPoly = roi.getInterpolatedPolygon( smoothInterval, true );

		final List< double[] > points = new ArrayList<>( fPoly.npoints );
		for ( int i = 0; i < fPoly.npoints; i++ )
			points.add( new double[] { fPoly.xpoints[ i ], fPoly.ypoints[ i ] } );

		final List< double[] > simplifiedPoints = RamerDouglasPeucker.douglasPeucker( points, epsilon );

		final float[] sX = new float[ simplifiedPoints.size() ];
		final float[] sY = new float[ simplifiedPoints.size() ];
		for ( int i = 0; i < sX.length; i++ )
		{
			sX[ i ] = ( float ) simplifiedPoints.get( i )[ 0 ];
			sY[ i ] = ( float ) simplifiedPoints.get( i )[ 1 ];
		}
		final FloatPolygon simplifiedPolygon = new FloatPolygon( sX, sY );
		final PolygonRoi fRoi = new PolygonRoi( simplifiedPolygon, PolygonRoi.POLYGON );
		return fRoi;
	}

	public static final < B extends BooleanType< B > > List< Polygon > maskToSpots( final RandomAccessibleInterval< B > mask )
	{
		final int w = ( int ) mask.dimension( 0 );
		final int h = ( int ) mask.dimension( 1 );
		final RandomAccess< B > ra = mask.randomAccess( mask );

		final List< Polygon > polygons = new ArrayList<>();
		boolean[] prevRow = new boolean[ w + 2 ];
		boolean[] thisRow = new boolean[ w + 2 ];
		final Outline[] outline = new Outline[ w + 1 ];

		for ( int y = 0; y <= h; y++ )
		{
			ra.setPosition( y, 1 );

			final boolean[] b = prevRow;
			prevRow = thisRow;
			thisRow = b;
			int xAfterLowerRightCorner = -1;
			Outline oAfterLowerRightCorner = null;

			ra.setPosition( 0, 0 );
			thisRow[ 1 ] = y < h ? ra.get().get() : false;

			for ( int x = 0; x <= w; x++ )
			{
				// we need to read one pixel ahead
				ra.setPosition( x + 1, 0 );
				if ( y < h && x < w - 1 )
					thisRow[ x + 2 ] = ra.get().get();
				else if ( x < w - 1 )
					thisRow[ x + 2 ] = false;

				if ( thisRow[ x + 1 ] )
				{ // i.e., pixel (x,y) is selected
					if ( !prevRow[ x + 1 ] )
					{
						// Upper edge of selected area:
						// - left and right outlines are null: new outline
						// - left null: append (line to left)
						// - right null: prepend (line to right), or
						// prepend&append (after lower right corner, two borders
						// from one corner)
						// - left == right: close (end of hole above) unless we
						// can continue at the right
						// - left != right: merge (prepend) unless we can
						// continue at the right
						if ( outline[ x ] == null )
						{
							if ( outline[ x + 1 ] == null )
							{
								outline[ x + 1 ] = outline[ x ] = new Outline();
								outline[ x ].append( x + 1, y );
								outline[ x ].append( x, y );
							}
							else
							{
								outline[ x ] = outline[ x + 1 ];
								outline[ x + 1 ] = null;
								outline[ x ].append( x, y );
							}
						}
						else if ( outline[ x + 1 ] == null )
						{
							if ( x == xAfterLowerRightCorner )
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = oAfterLowerRightCorner;
								outline[ x ].append( x, y );
								outline[ x + 1 ].prepend( x + 1, y );
							}
							else
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = null;
								outline[ x + 1 ].prepend( x + 1, y );
							}
						}
						else if ( outline[ x + 1 ] == outline[ x ] )
						{
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& !thisRow[ x + 2 ] && prevRow[ x + 2 ] )
							{ // at lower right corner & next pxl deselected
								outline[ x ] = null;
								// outline[x+1] unchanged
								outline[ x + 1 ].prepend( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x + 1 ];
							}
							else
							{
								// MINUS (add inner hole)
								// We cannot handle holes in TrackMate.
//								polygons.add( outline[ x ].getPolygon() );
								outline[ x + 1 ] = null;
								outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
							}
						}
						else
						{
							outline[ x ].prepend( outline[ x + 1 ] );
							for ( int x1 = 0; x1 <= w; x1++ )
								if ( x1 != x + 1 && outline[ x1 ] == outline[ x + 1 ] )
								{
									outline[ x1 ] = outline[ x ];
									outline[ x + 1 ] = null;
									outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
									break;
								}
							if ( outline[ x + 1 ] != null )
								throw new RuntimeException( "assertion failed" );
						}
					}
					if ( !thisRow[ x ] )
					{
						// left edge
						if ( outline[ x ] == null )
							throw new RuntimeException( "assertion failed" );
						outline[ x ].append( x, y + 1 );
					}
				}
				else
				{ // !thisRow[x + 1], i.e., pixel (x,y) is deselected
					if ( prevRow[ x + 1 ] )
					{
						// Lower edge of selected area:
						// - left and right outlines are null: new outline
						// - left == null: prepend
						// - right == null: append, or append&prepend (after
						// lower right corner, two borders from one corner)
						// - right == left: close unless we can continue at the
						// right
						// - right != left: merge (append) unless we can
						// continue at the right
						if ( outline[ x ] == null )
						{
							if ( outline[ x + 1 ] == null )
							{
								outline[ x ] = outline[ x + 1 ] = new Outline();
								outline[ x ].append( x, y );
								outline[ x ].append( x + 1, y );
							}
							else
							{
								outline[ x ] = outline[ x + 1 ];
								outline[ x + 1 ] = null;
								outline[ x ].prepend( x, y );
							}
						}
						else if ( outline[ x + 1 ] == null )
						{
							if ( x == xAfterLowerRightCorner )
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = oAfterLowerRightCorner;
								outline[ x ].prepend( x, y );
								outline[ x + 1 ].append( x + 1, y );
							}
							else
							{
								outline[ x + 1 ] = outline[ x ];
								outline[ x ] = null;
								outline[ x + 1 ].append( x + 1, y );
							}
						}
						else if ( outline[ x + 1 ] == outline[ x ] )
						{
							// System.err.println("add " + outline[x]);
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& thisRow[ x + 2 ] && !prevRow[ x + 2 ] )
							{ // at lower right corner & next pxl selected
								outline[ x ] = null;
								// outline[x+1] unchanged
								outline[ x + 1 ].append( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x + 1 ];
							}
							else
							{
								polygons.add( outline[ x ].getPolygon() );
								outline[ x + 1 ] = null;
								outline[ x ] = x == xAfterLowerRightCorner ? oAfterLowerRightCorner : null;
							}
						}
						else
						{
							if ( x < w - 1 && y < h && x != xAfterLowerRightCorner
									&& thisRow[ x + 2 ] && !prevRow[ x + 2 ] )
							{ // at lower right corner && next pxl selected
								outline[ x ].append( x + 1, y );
								outline[ x + 1 ].prepend( x + 1, y );
								xAfterLowerRightCorner = x + 1;
								oAfterLowerRightCorner = outline[ x ];
								// outline[x + 1] unchanged (the one at the
								// right-hand side of (x, y-1) to the top)
								outline[ x ] = null;
							}
							else
							{
								outline[ x ].append( outline[ x + 1 ] ); // merge
								for ( int x1 = 0; x1 <= w; x1++ )
									if ( x1 != x + 1 && outline[ x1 ] == outline[ x + 1 ] )
									{
										outline[ x1 ] = outline[ x ];
										outline[ x + 1 ] = null;
										outline[ x ] = ( x == xAfterLowerRightCorner ) ? oAfterLowerRightCorner : null;
										break;
									}
								if ( outline[ x + 1 ] != null )
									throw new RuntimeException( "assertion failed" );
							}
						}
					}
					if ( thisRow[ x ] )
					{
						// right edge
						if ( outline[ x ] == null )
							throw new RuntimeException( "assertion failed" );
						outline[ x ].prepend( x, y + 1 );
					}
				}
			}
		}
		return polygons;
	}

	/*
	 * This class implements a Cartesian polygon in progress. The edges are
	 * supposed to be parallel to the x or y axis. It is implemented as a deque
	 * to be able to add points to both sides.
	 */
	private static class Outline
	{
		int[] x, y;

		int first, last, reserved;

		final int GROW = 10; // default extra (spare) space when enlarging
								// arrays (similar performance with 6-20)

		public Outline()
		{
			reserved = GROW;
			x = new int[ reserved ];
			y = new int[ reserved ];
			first = last = GROW / 2;
		}

		/**
		 * Makes sure that enough free space is available at the beginning and
		 * end of the list, by enlarging the arrays if required
		 */
		private void needs( final int neededAtBegin, final int neededAtEnd )
		{
			if ( neededAtBegin > first || neededAtEnd > reserved - last )
			{
				final int extraSpace = Math.max( GROW, Math.abs( x[ last - 1 ] - x[ first ] ) );
				final int newSize = reserved + neededAtBegin + neededAtEnd + extraSpace;
				final int newFirst = neededAtBegin + extraSpace / 2;
				final int[] newX = new int[ newSize ];
				final int[] newY = new int[ newSize ];
				System.arraycopy( x, first, newX, newFirst, last - first );
				System.arraycopy( y, first, newY, newFirst, last - first );
				x = newX;
				y = newY;
				last += newFirst - first;
				first = newFirst;
				reserved = newSize;
			}
		}

		/** Adds point x, y at the end of the list */
		public void append( final int x, final int y )
		{
			if ( last - first >= 2 && collinear( this.x[ last - 2 ], this.y[ last - 2 ], this.x[ last - 1 ], this.y[ last - 1 ], x, y ) )
			{
				this.x[ last - 1 ] = x; // replace previous point
				this.y[ last - 1 ] = y;
			}
			else
			{
				needs( 0, 1 ); // new point
				this.x[ last ] = x;
				this.y[ last ] = y;
				last++;
			}
		}

		/** Adds point x, y at the beginning of the list */
		public void prepend( final int x, final int y )
		{
			if ( last - first >= 2 && collinear( this.x[ first + 1 ], this.y[ first + 1 ], this.x[ first ], this.y[ first ], x, y ) )
			{
				this.x[ first ] = x; // replace previous point
				this.y[ first ] = y;
			}
			else
			{
				needs( 1, 0 ); // new point
				first--;
				this.x[ first ] = x;
				this.y[ first ] = y;
			}
		}

		/**
		 * Merge with another Outline by adding it at the end. Thereafter, the
		 * other outline must not be used any more.
		 */
		public void append( final Outline o )
		{
			final int size = last - first;
			final int oSize = o.last - o.first;
			if ( size <= o.first && oSize > reserved - last )
			{ // we don't have enough space in our own array but in that of 'o'
				System.arraycopy( x, first, o.x, o.first - size, size );
				System.arraycopy( y, first, o.y, o.first - size, size );
				x = o.x;
				y = o.y;
				first = o.first - size;
				last = o.last;
				reserved = o.reserved;
			}
			else
			{ // append to our own array
				needs( 0, oSize );
				System.arraycopy( o.x, o.first, x, last, oSize );
				System.arraycopy( o.y, o.first, y, last, oSize );
				last += oSize;
			}
		}

		/**
		 * Merge with another Outline by adding it at the beginning. Thereafter,
		 * the other outline must not be used any more.
		 */
		public void prepend( final Outline o )
		{
			final int size = last - first;
			final int oSize = o.last - o.first;
			if ( size <= o.reserved - o.last && oSize > first )
			{ // we don't have enough space in our own array but in that of 'o'
				System.arraycopy( x, first, o.x, o.last, size ); // so append
																	// our own
																	// data to
																	// that of
																	// 'o'
				System.arraycopy( y, first, o.y, o.last, size );
				x = o.x;
				y = o.y;
				first = o.first;
				last = o.last + size;
				reserved = o.reserved;
			}
			else
			{ // prepend to our own array
				needs( oSize, 0 );
				first -= oSize;
				System.arraycopy( o.x, o.first, x, first, oSize );
				System.arraycopy( o.y, o.first, y, first, oSize );
			}
		}

		public Polygon getPolygon()
		{
			// optimize out intermediate points of straight lines (created,
			// e.g., by merging outlines)
			int i, j = first + 1;
			for ( i = first + 1; i + 1 < last; j++ )
			{
				if ( collinear( x[ j - 1 ], y[ j - 1 ], x[ j ], y[ j ], x[ j + 1 ], y[ j + 1 ] ) )
				{
					// merge i + 1 into i
					last--;
					continue;
				}
				if ( i != j )
				{
					x[ i ] = x[ j ];
					y[ i ] = y[ j ];
				}
				i++;
			}
			// wraparound
			if ( collinear( x[ j - 1 ], y[ j - 1 ], x[ j ], y[ j ], x[ first ], y[ first ] ) )
				last--;
			else
			{
				x[ i ] = x[ j ];
				y[ i ] = y[ j ];
			}
			if ( last - first > 2 && collinear( x[ last - 1 ], y[ last - 1 ], x[ first ], y[ first ], x[ first + 1 ], y[ first + 1 ] ) )
				first++;

			final int count = last - first;
			final int[] xNew = new int[ count ];
			final int[] yNew = new int[ count ];
			System.arraycopy( x, first, xNew, 0, count );
			System.arraycopy( y, first, yNew, 0, count );
			return new Polygon( xNew, yNew, count );
		}

		/** Returns whether three points are on one straight line */
		boolean collinear( final int x1, final int y1, final int x2, final int y2, final int x3, final int y3 )
		{
			return ( x2 - x1 ) * ( y3 - y2 ) == ( y2 - y1 ) * ( x3 - x2 );
		}

		@Override
		public String toString()
		{
			String res = "[first:" + first + ",last:" + last +
					",reserved:" + reserved + ":";
			if ( last > x.length )
				System.err.println( "ERROR!" );
			int nmax = 10; // don't print more coordinates than this
			for ( int i = first; i < last && i < x.length; i++ )
			{
				if ( last - first > nmax && i - first > nmax / 2 )
				{
					i = last - nmax / 2;
					res += "...";
					nmax = last - first; // dont check again
				}
				else
					res += "(" + x[ i ] + "," + y[ i ] + ")";
			}
			return res + "]";
		}
	}
}
