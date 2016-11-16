package org.lamport.tla.toolbox.util;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import tla2unicode.Unicode;

public class TLATreeViewer extends TreeViewer {
	private EventHandler eventHandler;
	
	public TLATreeViewer(Composite parent, int style) {
		super(parent, style);
	}

	public TLATreeViewer(Composite parent) {
		super(parent);
	}

	public TLATreeViewer(Tree tree) {
		super(tree);
	}

	private synchronized EventHandler getEventHandler() {
		if (eventHandler == null) {
			this.eventHandler = new EventHandler() {
				@Override
				public void handleEvent(Event event) {
					if (event == null)
						return;
					final Object value = event.getProperty(IEventBroker.DATA);
					switch (event.getTopic()) {
					case TLAUnicodeReplacer.EVENTID_TOGGLE_UNICODE:
						unicodeToggleHandler((Boolean)value);
						break;
					default:		
					}
				}
			};
		}
		return eventHandler;
	}

	@Override
	protected void hookControl(Control control) {
		super.hookControl(control);
		setLabelProvider(new LabelProvider() {
            public String getText(Object element) {
                return Unicode.convert(TLAUnicodeReplacer.isUnicode(), super.getText(element));
            }
        });
		UIHelper.getEventBroker().subscribe(TLAUnicodeReplacer.EVENTID_TOGGLE_UNICODE, getEventHandler());
	}
	
	@Override
	protected void handleDispose(DisposeEvent event) {
		UIHelper.getEventBroker().unsubscribe(eventHandler);
		super.handleDispose(event);
	}
	
	private void unicodeToggleHandler(boolean unicode) {
		refresh(); // getControl().redraw();
	}
}
