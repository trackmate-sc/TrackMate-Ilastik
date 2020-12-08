package fiji.plugin.trackmate.ilastik;

import java.util.ArrayList;
import java.util.List;

/**
 * The Ramer–Douglas–Peucker algorithm (RDP) is an algorithm for reducing the
 * number of points in a curve that is approximated by a series of points.
 * <p>
 * 
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/Ramer%E2%80%93Douglas%E2%80%93Peucker_algorithm">Ramer–Douglas–Peucker
 *      Algorithm (Wikipedia)</a> <br>
 * @author Justin Wetherell <phishman3579@gmail.com>
 */
public class RamerDouglasPeucker
{

	private RamerDouglasPeucker()
	{}

	private static final double sqr( final double x )
	{
		return Math.pow( x, 2 );
	}

	private static final double distanceBetweenPoints( final double vx, final double vy, final double wx, final double wy )
	{
		return sqr( vx - wx ) + sqr( vy - wy );
	}

	private static final double distanceToSegmentSquared( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		final double l2 = distanceBetweenPoints( vx, vy, wx, wy );
		if ( l2 == 0 )
			return distanceBetweenPoints( px, py, vx, vy );
		final double t = ( ( px - vx ) * ( wx - vx ) + ( py - vy ) * ( wy - vy ) ) / l2;
		if ( t < 0 )
			return distanceBetweenPoints( px, py, vx, vy );
		if ( t > 1 )
			return distanceBetweenPoints( px, py, wx, wy );
		return distanceBetweenPoints( px, py, ( vx + t * ( wx - vx ) ), ( vy + t * ( wy - vy ) ) );
	}

	private static final double perpendicularDistance( final double px, final double py, final double vx, final double vy, final double wx, final double wy )
	{
		return Math.sqrt( distanceToSegmentSquared( px, py, vx, vy, wx, wy ) );
	}

	private static final void douglasPeucker( final List< double[] > list, final int s, final int e, final double epsilon, final List< double[] > resultList )
	{
		// Find the point with the maximum distance
		double dmax = 0;
		int index = 0;

		final int start = s;
		final int end = e - 1;
		for ( int i = start + 1; i < end; i++ )
		{
			// Point
			final double px = list.get( i )[ 0 ];
			final double py = list.get( i )[ 1 ];
			// Start
			final double vx = list.get( start )[ 0 ];
			final double vy = list.get( start )[ 1 ];
			// End
			final double wx = list.get( end )[ 0 ];
			final double wy = list.get( end )[ 1 ];
			final double d = perpendicularDistance( px, py, vx, vy, wx, wy );
			if ( d > dmax )
			{
				index = i;
				dmax = d;
			}
		}
		// If max distance is greater than epsilon, recursively simplify
		if ( dmax > epsilon )
		{
			// Recursive call
			douglasPeucker( list, s, index, epsilon, resultList );
			douglasPeucker( list, index, e, epsilon, resultList );
		}
		else
		{
			if ( ( end - start ) > 0 )
			{
				resultList.add( list.get( start ) );
				resultList.add( list.get( end ) );
			}
			else
			{
				resultList.add( list.get( start ) );
			}
		}
	}

	/**
	 * Given a curve composed of line segments find a similar curve with fewer
	 * points.
	 * 
	 * @param list
	 *            List of Double[] points (x,y)
	 * @param epsilon
	 *            Distance dimension
	 * @return Similar curve with fewer points
	 */
	public static final List< double[] > douglasPeucker( final List< double[] > list, final double epsilon )
	{
		final List< double[] > resultList = new ArrayList< double[] >();
		douglasPeucker( list, 0, list.size(), epsilon, resultList );
		return resultList;
	}
}
