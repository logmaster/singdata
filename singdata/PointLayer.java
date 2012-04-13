
package wta.singdata;

import gnwd.geom.LatLon;
import gnwd.geom.Vec4;
import gnwd.layers.AbstractLayer;
import gnwd.render.DrawContext;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.media.opengl.GL;

/**
 * Displays colored {@code GL_POINTS} on the surface of the Earth. This class
 * is thread-safe. Some caching is performmed based on the given {@code LatLon}
 * points. If {@code LatLon} points are given to this {@code PointLayer} and 
 * subsequently they are cleared using the {@code clear} method <i>and</i> hard 
 * references to these {@code LatLon} instances remain, then periodic calls to 
 * {@code clearCache} are advised avoid a memory leak.
 * <p>
 * Note: caching using {@link java.util.WeakHashMap} was preferred over the
 * {@link java.lang.ref.SoftReference} technique because, (1) Soft references
 * are cleared entirely and then must be refilled from scratch and, (2) It 
 * would've made the code a bit uglier!
 * <p>
 * Possible future update: add a wrapper around this layer that either uses
 * a quad-tree or otherwise discretezies and selectively redraws invalidated
 * layers.
 */
public class PointLayer extends AbstractLayer
{

    private final Queue<LatLon> pointsToDraw;
    private final Map<LatLon, Color> pointColor;
    private final Map<LatLon, Vec4> pointVec4;
    private final Color defaultColor;

    /**
     * Creates a new {@code PointLayer} that, by default, will draw all 
     * points in the given color
     * @param color the default drawing color for all points, can be
     * overridden on a point-by-point basis (see {@code add(LatLon, Color)})
     * @param numberOfPointsExpected the number of points expected to be added
     * to this layer, the better the guess the more efficient this layer.
     */
    public PointLayer(Color color, int numberOfPointsExpected)
    {
        this.defaultColor = color;
        this.pointsToDraw = new ConcurrentLinkedQueue<LatLon>();
        this.pointColor = new WeakHashMap<LatLon, Color>(numberOfPointsExpected);
        this.pointVec4 = new WeakHashMap<LatLon, Vec4>(numberOfPointsExpected);
    }

    /**
     * Creates a new {@code PointLayer} that, by default, will draw all 
     * the specified points in the given color
     * @param color the default drawing color for all {@code points}, can be
     * overridden on a point-by-point basis (see {@code add(LatLon, Color)})
     * @param points the initial set of points to draw
     */
    public PointLayer(Color color, LatLon... points)
    {
        this.defaultColor = color;
        this.pointsToDraw = new ConcurrentLinkedQueue<LatLon>(Arrays.asList(
                points));
        this.pointColor = new WeakHashMap<LatLon, Color>(points.length);
        this.pointVec4 = new WeakHashMap<LatLon, Vec4>(points.length);
    }

    /**
     * Creates a new {@code PointLayer} that, by default, will draw all 
     * the specified points in the given color
     * @param color the default drawing color for all {@code points}, can be
     * overridden on a point-by-point basis (see {@code add(LatLon, Color)})
     * @param points the initial set of points to draw
     */
    public PointLayer(Color color, Collection<LatLon> points)
    {
        this.defaultColor = color;
        this.pointsToDraw = new ConcurrentLinkedQueue<LatLon>(points);
        this.pointColor = new WeakHashMap<LatLon, Color>(points.size());
        this.pointVec4 = new WeakHashMap<LatLon, Vec4>(points.size());
    }

    /**
     * Adds a new point to draw
     * 
     * @param point the point to draw
     */
    public void add(LatLon point)
    {
        pointsToDraw.add(point);
    }

    /**
     * Adds a new point to draw, with the specified color.
     * 
     * @param point the point to draw
     * @param color the color to draw the point
     */
    public void add(LatLon point, Color color)
    {
        pointsToDraw.add(point);
        pointColor.put(point, color);
    }

    /**
     * Deletes the specified point to draw
     * @param point the point to delete
     * @param deleteCache true if this {@code LatLon} will probably not
     * be drawn again in this {@code PointLayer}. See class comments for
     * notes on caching.
     */
    public void delete(LatLon point, boolean deleteCache)
    {
        pointsToDraw.remove(point);
        if (deleteCache)
        {
            pointColor.remove(point);
            pointVec4.remove(point);
        }
    }

    /**
     * Clears all the points to draw
     */
    public void clear()
    {
        pointsToDraw.clear();
    }

    /**
     * Manually clears the cache. If no references are maintained to the 
     * {@code LatLon} instances provided in the {@code add} method, then this
     * call isn't necessary. However, if hard references are maintained to the
     * {@code LatLon} objects then clearing the cache periodically is required
     * to avoid a memory leak.
     */
    public void clearCache()
    {
        //clearList();
        pointColor.clear();
        pointVec4.clear();
    }

    /**
     * Renders all the points each call. 
     * 
     *
     * @param dc the GL drawing context
     */
    @Override
    protected void doRender(DrawContext dc)
    {
        final GL gl = dc.getGL();
        boolean attribsPushed = false;
        long ndrawed = 0;
        long nstart = System.currentTimeMillis(), ndelta;
        try
        {
            gl.glBegin(GL.GL_POINTS);
            gl.glPushAttrib(
                    GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT | GL.GL_POLYGON_BIT);
            attribsPushed = true;

            gl.glDisable(GL.GL_TEXTURE_2D);        // no textures
            gl.glDisable(GL.GL_DEPTH_TEST);        // no depth testing

            for (LatLon toDraw : pointsToDraw)
            {
                // clipping points
                if (!dc.getVisibleSector().contains(toDraw))
                {
                    continue;
                }

                Vec4 vec = pointVec4.get(toDraw);
                if (vec == null)
                {
                    vec = dc.getGlobe().computePointFromPosition(
                            toDraw.getLatitude(), toDraw.getLongitude(),
                            dc.getGlobe().getElevation(
                            toDraw.getLatitude(), toDraw.getLongitude()));
                    //put this sucker in the cache
                    pointVec4.put(toDraw, vec);
                }

                gl.glVertex3d(vec.x, vec.y, vec.z);
                final Color color = pointColor.containsKey(toDraw) ? pointColor.get(
                        toDraw) : defaultColor;
                gl.glColor3f(color.getRed(), color.getGreen(),
                        color.getBlue());
                ndrawed = ndrawed + 1;
            }
            gl.glEnd();
        }
        finally
        {
            if (attribsPushed)
            {
                gl.glPopAttrib();
            }
        }
        ndelta = System.currentTimeMillis() - nstart;
        System.out.println(ndelta + ", " + ndrawed);
    }
}