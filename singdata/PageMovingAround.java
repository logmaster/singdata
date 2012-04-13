package wta.singdata;

import java.awt.BorderLayout;
import java.util.List;

import gnwd.*;
import gnwd.avlist.AVKey;
import gnwd.avlist.AVList;
import gnwd.awt.WorldWindowGLCanvas;
import gnwd.examples.util.HotSpotController;
import gnwd.examples.util.LayerTreeNode;
import gnwd.geom.Angle;
import gnwd.geom.Position;
import gnwd.layers.CompassLayer;
import gnwd.layers.LatLonGraticuleLayer;
import gnwd.layers.Layer;
import gnwd.layers.LayerList;
import gnwd.layers.RenderableLayer;
import gnwd.layers.ScalebarLayer;
import gnwd.layers.SkyGradientLayer;
import gnwd.layers.StarsLayer;
import gnwd.layers.WorldMapLayer;
import gnwd.render.Renderable;
import gnwd.util.StatusBar;
import gnwd.util.tree.BasicFrameAttributes;
import gnwd.util.tree.BasicTree;
import gnwd.util.tree.BasicTreeAttributes;
import gnwd.util.tree.BasicTreeLayout;
import gnwd.util.tree.BasicTreeModel;
import gnwd.util.tree.BasicTreeNode;
import gnwd.util.tree.Tree;
import gnwd.util.tree.TreeNode;
import gnwd.view.orbit.BasicOrbitView;
import gnwd.view.orbit.OrbitView;

import javax.swing.*;

import netscape.javascript.JSObject;

public class PageMovingAround extends JApplet
{
    private WorldWindowGLCanvas m_wwdcanvas;

    protected HotSpotController controller;
    protected Tree selector;
    protected RenderableLayer layerSelectorLayer;
    private static final String LAYER_MANAGER_ICON_PATH = "gov/nasa/worldwindow/images/layer-manager-64x64.png";
    
    public PageMovingAround()
    {
    	
    }
    
    public void init()
    {
    	try
    	{
	    	m_wwdcanvas = new WorldWindowGLCanvas();
	    	getContentPane().add(m_wwdcanvas, BorderLayout.CENTER);
	    	Model m = (Model)WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
	    	m_wwdcanvas.setModel(m);
			int i;
			Layer lyr;
			LayerList lyrall = m_wwdcanvas.getModel().getLayers();
			List<Layer> lyrlist = lyrall.getLayersByClass(CompassLayer.class);
			if(lyrlist.size() > 0)
			{
				CompassLayer lyrcl = (CompassLayer)lyrlist.get(0);
				lyrcl.setIconFilePath("images/notched-compass.gif");
				CompassListener cpl = new CompassListener(m_wwdcanvas, CompassLayer.class);
				m_wwdcanvas.addSelectListener(cpl);
			}
			for(i = 0; i < lyrall.size(); i ++)
			{
				lyr = lyrall.get(i);				
			}
	    	
			ClickAndGoSelectListener cgsl = new ClickAndGoSelectListener(m_wwdcanvas, WorldMapLayer.class);
			m_wwdcanvas.addSelectListener(cgsl);

            LatLonGraticuleLayer lyrGrid = new LatLonGraticuleLayer();
            lyrGrid.setName("Earth Grid");
            insertAfterWorldMapLayer(m_wwdcanvas, lyrGrid);
            
            layerSelectorLayer = new RenderableLayer();
            selector = createLayerSelector();
            layerSelectorLayer.addRenderable(selector);
            controller = new HotSpotController(m_wwdcanvas);
            buildTreeModel();
            // Add the layer to the model.
            insertAfterWorldMapLayer(m_wwdcanvas, layerSelectorLayer);

            StatusBar statusBar = new StatusBar();
            getContentPane().add(statusBar, BorderLayout.PAGE_END);
            statusBar.setEventSource(m_wwdcanvas);

            // Call javascript appletInit()
            try
            {
                JSObject win = JSObject.getWindow(this);
                win.call("appletInit", null);
            }
            catch(Exception ignore) {}

            //WorldWind.getNetworkStatus().setOfflineMode(true);
    	}
    	catch(Throwable e)
    	{
    		e.printStackTrace();
    	}
    }

    protected void buildTreeModel()
    {
        selector.getModel().getRoot().removeAllChildren();

        for (Layer layer : m_wwdcanvas.getModel().getLayers())
        {
            if (layer != layerSelectorLayer)
            {
            	if(layer instanceof StarsLayer)
            		continue;
            	if(layer instanceof WorldMapLayer)
            		continue;
            	if(layer instanceof CompassLayer)
            		continue;
            	if(layer instanceof SkyGradientLayer)
            		continue;
            	if(layer instanceof ScalebarLayer)
            		continue;
            	
                TreeNode layerNode = new LayerTreeNode((BasicTreeModel) selector.getModel(), layer);
                selector.getModel().getRoot().addChild(layerNode);

                if (layer instanceof RenderableLayer)
                    addRenderables(layerNode, (RenderableLayer) layer);
            }
        }
    }

    public static void insertAfterWorldMapLayer(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the placenames.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer lyr : layers)
        {
            if (lyr instanceof WorldMapLayer)
                compassPosition = layers.indexOf(lyr);
        }
        layers.add(compassPosition + 1, layer);
    }
	
    protected void addRenderables(TreeNode root, RenderableLayer layer)
    {
        for (Renderable renderable : layer.getRenderables())
        {
            String name = null;
            String description = null;
            if (renderable instanceof AVList)
            {
                AVList list = (AVList) renderable;
                name = list.getStringValue(AVKey.DISPLAY_NAME);
                description = list.getStringValue(AVKey.DESCRIPTION);
            }

            if (name == null)
                name = renderable.getClass().getSimpleName();

            BasicTreeNode node = new BasicTreeNode((BasicTreeModel) selector.getModel(), name);
            node.setDescription(description);
            root.addChild(node);
        }
    }

    protected Tree createLayerSelector()
    {
    	String title = "SingData Layers";

        BasicTree selector = new BasicTree();
        BasicTreeModel model = new BasicTreeModel(selector);

        TreeNode root = new BasicTreeNode(model, title, LAYER_MANAGER_ICON_PATH);
        selector.setModel(model);
        model.setRoot(root);

        BasicTreeLayout layout = new BasicTreeLayout(selector, 20, 130);
        selector.setLayout(layout);
        layout.getFrame().setIconImageSource(LAYER_MANAGER_ICON_PATH);

        BasicTreeAttributes attributes = new BasicTreeAttributes();
        attributes.setRootVisible(false);
        attributes.setOpacity(0.7);
        layout.setAttributes(attributes);

        BasicFrameAttributes frameAttributes = new BasicFrameAttributes();
        frameAttributes.setBackgroundOpacity(0.7);
        layout.getFrame().setAttributes(frameAttributes);

        BasicTreeAttributes highlightAttributes = new BasicTreeAttributes(attributes);
        highlightAttributes.setOpacity(1.0);
        layout.setHighlightAttributes(highlightAttributes);

        BasicFrameAttributes highlightFrameAttributes = new BasicFrameAttributes(frameAttributes);
        highlightFrameAttributes.setForegroundOpacity(1.0);
        highlightFrameAttributes.setBackgroundOpacity(1.0);
        layout.getFrame().setHighlightAttributes(highlightFrameAttributes);
        layout.getFrame().setFrameTitle(title);

        selector.expandPath(root.getPath());

        return selector;
    }		
    
    public void start()
    {
        // Call javascript appletStart()
        try
        {
            JSObject win = JSObject.getWindow(this);
            win.call("appletStart", null);
        }
        catch(Exception ignore) {}
    }

    public void stop()
    {
        // Call javascript appletSop()
        try
        {
            JSObject win = JSObject.getWindow(this);
            win.call("appletStop", null);
        }
        catch(Exception ignore) {}
        
        // Shut down World Wind
        WorldWind.shutDown();
    }

    
    /**
     * Move the current view position
     * @param lat the target latitude in decimal degrees
     * @param lon the target longitude in decimal degrees
     */
    public void gotoLatLon(double lat, double lon)
    {
        gotoLatLon(lat, lon, Double.NaN, 0, 0);
    }

    /**
     * Move the current view position, zoom, heading and pitch
     * @param lat the target latitude in decimal degrees
     * @param lon the target longitude in decimal degrees
     * @param zoom the target eye distance in meters
     * @param heading the target heading in decimal degrees
     * @param pitch the target pitch in decimal degrees
     */
    public void gotoLatLon(double lat, double lon, double zoom, double heading, double pitch)
    {
        BasicOrbitView view = (BasicOrbitView)m_wwdcanvas.getView();
        if(!Double.isNaN(lat) || !Double.isNaN(lon) || !Double.isNaN(zoom))
        {
            lat = Double.isNaN(lat) ? view.getCenterPosition().getLatitude().degrees : lat;
            lon = Double.isNaN(lon) ? view.getCenterPosition().getLongitude().degrees : lon;
            zoom = Double.isNaN(zoom) ? view.getZoom() : zoom;
            heading = Double.isNaN(heading) ? view.getHeading().degrees : heading;
            pitch = Double.isNaN(pitch) ? view.getPitch().degrees : pitch;
            view.addPanToAnimator(Position.fromDegrees(lat, lon, 0),
                    Angle.fromDegrees(heading), Angle.fromDegrees(pitch), zoom, true);
        }
    }

    /**
     * Set the current view heading and pitch
     * @param heading the traget heading in decimal degrees
     * @param pitch the target pitch in decimal degrees
     */
    public void setHeadingAndPitch(double heading, double pitch)
    {
        BasicOrbitView view = (BasicOrbitView)m_wwdcanvas.getView();
        if(!Double.isNaN(heading) || !Double.isNaN(pitch))
        {
            heading = Double.isNaN(heading) ? view.getHeading().degrees : heading;
            pitch = Double.isNaN(pitch) ? view.getPitch().degrees : pitch;

            view.addHeadingPitchAnimator(
                view.getHeading(), Angle.fromDegrees(heading), view.getPitch(), Angle.fromDegrees(pitch));
        }
    }

    /**
     * Set the current view zoom
     * @param zoom the target eye distance in meters
     */
    public void setZoom(double zoom)
    {
        BasicOrbitView view = (BasicOrbitView)m_wwdcanvas.getView();
        if(!Double.isNaN(zoom))
        {
            view.addZoomAnimator(view.getZoom(), zoom);
        }
    }    

    /**
     * Get the WorldWindowGLCanvas
     * @return the current WorldWindowGLCanvas
     */
    public WorldWindowGLCanvas getWW()
    {
        return m_wwdcanvas;
    }

    /**
     * Get the current OrbitView
     * @return the current OrbitView
     */
    public OrbitView getOrbitView()
    {
        if(m_wwdcanvas.getView() instanceof OrbitView)
            return (OrbitView)m_wwdcanvas.getView();
        return null;
    }

    /**
     * Get a reference to a layer with part of its name
     * @param layerName part of the layer name to match.
     * @return the corresponding layer or null if not found.
     */
    public Layer getLayerByName(String layerName)
    {
        for (Layer layer : m_wwdcanvas.getModel().getLayers())
            if (layer.getName().indexOf(layerName) != -1)
                return layer;
        return null;
    }    
}
