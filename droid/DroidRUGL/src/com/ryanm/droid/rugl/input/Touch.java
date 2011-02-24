
package com.ryanm.droid.rugl.input;

import java.util.ArrayList;

import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;

import com.ryanm.droid.rugl.Game;

/**
 * Provides a polling-style interface to multitouch pointers. Note
 * that this flips the y-axis so the the origin is at the bottom-left
 * of the screen
 * 
 * @author ryanm
 */
public class Touch
{
	private static final boolean multitouch = Integer.parseInt( Build.VERSION.SDK ) >= 5;

	private static ArrayList<Pointer> pointerList = new ArrayList<Pointer>();

	private static ArrayList<TouchListener> listeners =
			new ArrayList<Touch.TouchListener>();

	private static float xScale = 1, yScale = 1;

	/**
	 * An array of the active pointers
	 */
	public static Pointer[] pointers = new Pointer[ 0 ];

	/**
	 * @param me
	 */
	public static void onTouchEvent( MotionEvent me )
	{
		if( multitouch )
		{
			switch( me.getAction() & MEWrap.ACTION_MASK )
			{
				case MotionEvent.ACTION_DOWN:
					addPrimary( me );
					break;
				case MEWrap.ACTION_POINTER_DOWN:
					addSecondary( me );
					break;
				case MotionEvent.ACTION_UP:
					removePrimary( me );
					break;
				case MEWrap.ACTION_POINTER_UP:
					removeSecondary( me );
					break;
				default:
					updatePointers( me );
			}
		}
		else
		{
			switch( me.getAction() )
			{
				case MotionEvent.ACTION_DOWN:
					addPrimary( me );
					break;
				case MotionEvent.ACTION_UP:
					removePrimary( me );
					break;
				default:
					updatePointer( me );
			}
		}
	}

	/**
	 * Sets scaling factors for translation between physical and
	 * desired coordinate systems
	 * 
	 * @param desiredWidth
	 * @param desiredHeight
	 * @param actualWidth
	 * @param actualHeight
	 */
	public static void setScreenSize( float desiredWidth, float desiredHeight,
			int actualWidth, int actualHeight )
	{
		xScale = desiredWidth / actualWidth;
		yScale = desiredHeight / actualHeight;
	}

	private static void addPrimary( MotionEvent me )
	{
		assert pointerList.isEmpty();

		Pointer p = new Pointer( me.getPointerId( 0 ) );
		p.x = me.getX() * xScale;
		p.y = ( Game.height - me.getY() ) * yScale;
		p.size = me.getSize();

		pointerList.add( p );

		pointers = pointerList.toArray( new Pointer[ pointerList.size() ] );

		boolean eaten = false;
		for( int j = 0; j < listeners.size() && !eaten; j++ )
		{
			eaten |= listeners.get( j ).pointerAdded( p );
		}
	}

	private static void addSecondary( MotionEvent me )
	{
		assert !pointerList.isEmpty();

		final int pointerIndex =
				( me.getAction() & MotionEvent.ACTION_POINTER_ID_MASK ) >> MotionEvent.ACTION_POINTER_ID_SHIFT;

		Pointer p = new Pointer( me.getPointerId( pointerIndex ) );
		p.x = me.getX( pointerIndex ) * xScale;
		p.y = ( Game.height - me.getY( pointerIndex ) ) * yScale;
		p.size = me.getSize( pointerIndex );

		pointerList.add( pointerIndex, p );

		pointers = pointerList.toArray( new Pointer[ pointerList.size() ] );

		boolean eaten = false;
		for( int j = 0; j < listeners.size() && !eaten; j++ )
		{
			eaten |= listeners.get( j ).pointerAdded( p );
		}
	}

	private static void removePrimary( MotionEvent me )
	{
		if( pointerList.size() == 1 )
		{
			Pointer p = pointerList.remove( 0 );

			pointers = pointerList.toArray( new Pointer[ pointerList.size() ] );

			for( int j = 0; j < listeners.size(); j++ )
			{
				listeners.get( j ).pointerRemoved( p );
			}
		}
		else
		{
			Log.e( Game.RUGL_TAG, "Touch.removePrimary() no primary to remove!" );
		}
	}

	private static void removeSecondary( MotionEvent me )
	{
		final int pointerIndex =
				( me.getAction() & MotionEvent.ACTION_POINTER_ID_MASK ) >> MotionEvent.ACTION_POINTER_ID_SHIFT;

		if( pointerList.size() > pointerIndex )
		{
			Pointer p = pointerList.remove( pointerIndex );

			pointers = pointerList.toArray( new Pointer[ pointerList.size() ] );

			assert p.id == me.getPointerId( pointerIndex );

			for( int j = 0; j < listeners.size(); j++ )
			{
				listeners.get( j ).pointerRemoved( p );
			}
		}
		else
		{
			Log.e( Game.RUGL_TAG,
					"Touch.removeSecondary() no pointer to remove! from index " + pointerIndex );
		}
	}

	private static void updatePointers( MotionEvent me )
	{
		int pointerCount = MEWrap.getPointerCount( me );

		for( int i = 0; i < pointerCount; i++ )
		{
			Pointer p = pointerList.get( i );

			assert p.id == me.getPointerId( i );

			p.x = me.getX( i ) * xScale;
			p.y = ( Game.height - me.getY( i ) ) * yScale;
			p.size = me.getSize( i );
		}
	}

	private static void updatePointer( MotionEvent me )
	{
		Pointer p = pointerList.get( 0 );

		p.x = me.getX() * xScale;
		p.y = ( Game.height - me.getY() ) * yScale;
		p.size = me.getSize();
	}

	/**
	 * @param l
	 *           The object to inform of pointer changes
	 */
	public static void addListener( TouchListener l )
	{
		listeners.add( l );
	}

	/**
	 * @param l
	 *           The object to stop informing of pointer changes
	 */
	public static void removeListener( TouchListener l )
	{
		listeners.remove( l );
	}

	/**
	 * Information on one pointer
	 * 
	 * @author ryanm
	 */
	public static class Pointer
	{
		/***/
		public final int id;

		/***/
		public float x;

		/***/
		public float y;

		/***/
		public float size;

		private Pointer( int id )
		{
			this.id = id;
		}

		@Override
		public String toString()
		{
			StringBuilder buff = new StringBuilder();
			buff.append( id ).append( " ( " ).append( x ).append( ", " );
			buff.append( y ).append( " ) " ).append( size );
			return buff.toString();
		}
	}

	/**
	 * @author ryanm
	 */
	public interface TouchListener
	{
		/**
		 * Called when a new pointer is added to the screen
		 * 
		 * @param p
		 *           This object's fields will be updated as the pointer
		 *           changes
		 * @return <code>true</code> if the touch should be consumed. No
		 *         other listeners will be notified
		 */
		public boolean pointerAdded( Pointer p );

		/**
		 * Called when a pointer is removed from the screen
		 * 
		 * @param p
		 *           This object will no longer be updated
		 */
		public void pointerRemoved( Pointer p );

		/**
		 * Called when the Touch system is initiated
		 */
		public void reset();
	}

	/**
	 * Wraps {@link MotionEvent} methods and variables that may not be
	 * present
	 * 
	 * @author ryanm
	 */
	private static class MEWrap
	{
		private static final int ACTION_MASK = MotionEvent.ACTION_MASK;

		private static final int ACTION_POINTER_DOWN = MotionEvent.ACTION_POINTER_DOWN;

		private static final int ACTION_POINTER_UP = MotionEvent.ACTION_POINTER_UP;

		private static final int getPointerCount( MotionEvent me )
		{
			return me.getPointerCount();
		}
	}

	/**
	 * Called at startup
	 */
	public static void reset()
	{
		pointerList.clear();
		pointers = new Pointer[ 0 ];

		for( TouchListener l : listeners )
		{
			l.reset();
		}
	}
}