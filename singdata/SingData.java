/**
 * 
 */
package wta.singdata;

import gnwd.*;
import gnwd.avlist.*;
import gnwd.awt.*;
import gnwd.examples.util.*;
import gnwd.layers.*;
import gnwd.render.Renderable;
import gnwd.util.*;
import gnwd.util.tree.*;
import gnwd.geom.*;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.util.List;
/**
 * @author wangtao
 *
 */
public class SingData 
{
	protected static class AppPanel extends JPanel
	{
		protected WorldWindowGLCanvas m_wwdcanvas;
        protected HotSpotController m_hotspotCtrl;
        protected Tree m_trvCtrl;
        protected RenderableLayer m_trvLayer;
        private static final String LAYER_MANAGER_ICON_PATH = "gov/nasa/worldwindow/images/layer-manager-64x64.png";
		protected StatusBar statusBar;
		public AppPanel(Dimension canvasSize)
		{
			super(new BorderLayout());
			m_wwdcanvas = new WorldWindowGLCanvas();
			m_wwdcanvas.setPreferredSize(canvasSize);
			
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

            StatusBar statusBar = new StatusBar();
            statusBar.SetWwd(m_wwdcanvas);
            add(statusBar, BorderLayout.PAGE_END);
            statusBar.setEventSource(m_wwdcanvas);            
			add(m_wwdcanvas, BorderLayout.CENTER);

			LatLonGraticuleLayer lyrGrid = new LatLonGraticuleLayer();
            lyrGrid.setName("Earth Grid");
            lyrall.add(lyrGrid);            
            
            PointLayer pl = InsertTrajectoryPoints();
            lyrall.add(pl);
            
            m_trvLayer = new RenderableLayer();
            m_trvCtrl = createTreeLayer();
            m_trvLayer.addRenderable(m_trvCtrl);
            m_hotspotCtrl = new HotSpotController(m_wwdcanvas);
            buildTreeModel();
            lyrall.add(m_trvLayer);
            //insertAfterWorldMapLayer(m_wwdcanvas, m_trvLayer);
						
		}

		protected PointLayer InsertTrajectoryPoints()
		{
            PointLayer ptraj = null;
            java.io.File dir = new java.io.File("F:\\Data\\lbs\\Data\\011\\Trajectory\\");
			//long nlens = file.length();            			
			ptraj = new PointLayer(Color.red, 10000);//(int)(nlens / 68));
            for(java.io.File file : dir.listFiles())
            {
            	if(file.isDirectory())
            	{
            		
            	}
            	else
            	{
            		try
            		{
            			int i = 0;
            			String[] strattri;
            			gnwd.geom.LatLon ptlatlon;
            			
            			FileReader fr = new FileReader(file); 
            			BufferedReader br = new BufferedReader(fr);
            			String strline = br.readLine();            			
            			while(strline != null)
            			{
            				i = i + 1;
            				if(i == 6)
            					break;
            				strline = br.readLine();
            			}
            			strline = br.readLine();
            			while(strline != null)
            			{
            				//System.out.println(strline);
            				strattri = strline.split(",");
            				ptlatlon = LatLon.fromDegrees(
            						Double.parseDouble(strattri[0]),
            						Double.parseDouble(strattri[1]));
            				ptraj.add(ptlatlon);
            				strline = br.readLine();
            			}
            			br.close();
            			fr.close();            			
            		}
            		catch(FileNotFoundException e)
            		{
            			e.printStackTrace();
            		}
            		catch(IOException e)
            		{
            			e.printStackTrace();
            		}
            		
            		//break;
            	}
            }
			return ptraj;
		}
		
		protected void buildTreeModel()
        {
            m_trvCtrl.getModel().getRoot().removeAllChildren();

            for (Layer layer : m_wwdcanvas.getModel().getLayers())
            {
                if (layer != m_trvLayer)
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
                	
                    TreeNode layerNode = new LayerTreeNode((BasicTreeModel) m_trvCtrl.getModel(), layer);
                    //layerNode.setDescription(layer.getName());
                    m_trvCtrl.getModel().getRoot().addChild(layerNode);

                    if (layer instanceof RenderableLayer)
                        addRenderables(layerNode, (RenderableLayer) layer);
                }
            }
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

                BasicTreeNode node = new BasicTreeNode((BasicTreeModel) m_trvCtrl.getModel(), name);
                node.setDescription(description);
                root.addChild(node);
            }
        }

        protected Tree createTreeLayer()
        {
            String title = "SingData Layers";

            BasicTree trvlayer = new BasicTree();
            BasicTreeModel model = new BasicTreeModel(trvlayer);

            TreeNode root = new BasicTreeNode(model, title, LAYER_MANAGER_ICON_PATH);
            trvlayer.setModel(model);
            model.setRoot(root);

            BasicTreeLayout layout = new BasicTreeLayout(trvlayer, 20, 130);
            trvlayer.setLayout(layout);
            layout.getFrame().setIconImageSource(LAYER_MANAGER_ICON_PATH);

            BasicTreeAttributes attributes = new BasicTreeAttributes();
            attributes.setRootVisible(false);
            attributes.setOpacity(0.7);
            layout.setAttributes(attributes);
            
            attributes = new BasicTreeAttributes();
            attributes.setRootVisible(false);
            attributes.setOpacity(1.0);
            layout.setHighlightAttributes(attributes);

            BasicFrameAttributes frameAttributes = new BasicFrameAttributes();
            frameAttributes.setBackgroundOpacity(0.7);
            layout.getFrame().setAttributes(frameAttributes);
            
            frameAttributes = new BasicFrameAttributes();
            frameAttributes.setForegroundOpacity(1.0);
            frameAttributes.setBackgroundOpacity(1.0);
            layout.getFrame().setHighlightAttributes(frameAttributes);

            layout.getFrame().setFrameTitle(title);

            trvlayer.expandPath(root.getPath());

            return trvlayer;
        }				
	}
    
	protected static class AppFrame extends JFrame
	{
		private Dimension m_szcanvas = new Dimension(800, 600);
		private AppPanel m_panelwwd;
		
		protected void initialize()
		{
			m_panelwwd = new AppPanel(m_szcanvas);
			m_panelwwd.setPreferredSize(m_szcanvas);
			getContentPane().add(m_panelwwd, BorderLayout.CENTER);
			
			//ViewControlsLayer viewctrl = new ViewControlsLayer();
			pack();
			WWUtil.alignComponent(null, this, AVKey.CENTER);
			setResizable(true);

		}
		public AppFrame()
		{			
			initialize();			
		}
		
	}
    static
    {
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
        else if (Configuration.isWindowsOS())
        {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

    public static AppFrame start(String appName, Class appFrameClass)
    {
        if (Configuration.isMacOS() && appName != null)
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try
        {
            final AppFrame frame = (AppFrame) appFrameClass.newInstance();
            frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setVisible(true);
                }
            });
            return frame;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void main(String[] args)
    {
    	//WorldWind.getNetworkStatus().setOfflineMode(true);
        SingData.start("Singapore Data Layers", AppFrame.class);
        javax.swing.ImageIcon ico = new javax.swing.ImageIcon("images/singdat.png");        
        Frame[] frms = java.awt.Frame.getFrames();
        frms[0].setIconImage((Image)ico.getImage());        
        
    }
}
