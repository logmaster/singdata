
package wta.singdata;

import gnwd.*;
import gnwd.event.*;
import gnwd.view.orbit.*;
import gnwd.geom.Angle;

public class CompassListener implements SelectListener
{
	private final WorldWindow m_wwd;
	private final Class m_clsCompass;
	
	Angle m_startHeading = null;

	public CompassListener(WorldWindow wwd, Class clsclicked)
	{
		m_wwd = wwd;
		m_clsCompass = clsclicked;
	}
	
	@Override
	public void selected(SelectEvent event) 
	{
		if(event.hasObjects())
		{
			Object obj = event.getTopObject().getClass(); 
			if(obj.equals(m_clsCompass))
			{
				Angle heading = (Angle)event.getTopPickedObject().getValue("Heading");
				View view = this.m_wwd.getView();
				view.stopAnimations();				
				String strevent = event.getEventAction();
				if(strevent.equals(SelectEvent.LEFT_CLICK))				
				{					
					view.setPitch(Angle.fromDegrees(0.0));
					//OrbitViewInputHandler ovih = (OrbitViewInputHandler)view.getViewInputHandler();
					//ovih.addPitchAnimator(view.getPitch(), Angle.ZERO);
				}
				else if(strevent.equals(SelectEvent.RIGHT_CLICK))
				{
					view.setHeading(Angle.fromDegrees(0.0));
					//OrbitViewInputHandler ovih = (OrbitViewInputHandler)view.getViewInputHandler();
					//ovih.addHeadingAnimator(view.getHeading(), Angle.ZERO);
					
				}
				else if(strevent.equals(SelectEvent.DRAG) && m_startHeading == null)
				{
					m_startHeading = heading;
				}
				else if(strevent.equals(SelectEvent.ROLLOVER) && m_startHeading != null)
				{
					double move = heading.degrees - m_startHeading.degrees;
					double newheading = m_startHeading.degrees - move;
					newheading = newheading >= 0 ? newheading : newheading + 360;					
					view.setHeading(Angle.fromDegrees(newheading));
				}
				else if(strevent.equals(SelectEvent.DRAG_END) && m_startHeading != null)
				{
					m_startHeading = null;
				}
				//event.consume();
			}
		}
	}		
}
