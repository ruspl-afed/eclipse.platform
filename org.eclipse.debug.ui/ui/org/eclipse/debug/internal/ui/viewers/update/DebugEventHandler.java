/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.debug.internal.ui.viewers.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.ModelDelta;

/**
 * Handles debug events for an event update policy in a viewer.
 * 
 * @since 3.2
 */
public abstract class DebugEventHandler {
	
	private AbstractModelProxy fModelProxy;
	
	// debug flags
	public static boolean DEBUG_DELTAS = false;
	
	static {
		DEBUG_DELTAS = DebugUIPlugin.DEBUG && "true".equals( //$NON-NLS-1$
		 Platform.getDebugOption("org.eclipse.debug.ui/debug/viewers/deltas")); //$NON-NLS-1$
	} 	

	/**
	 * Constructs an event handler for the given model proxy.
	 * 
	 * @param policy
	 */
	public DebugEventHandler(AbstractModelProxy proxy) {
		fModelProxy = proxy;
	}
	
	/**
	 * Disposes this event handler
	 */
	public synchronized void dispose() {
		fModelProxy = null;
	}
		
	/**
	 * Returns the model proxy this event handler working for,
	 * or <code>null</code> if disposed.
	 * 
	 * @return
	 */
	protected synchronized AbstractModelProxy getModelProxy() {
		return fModelProxy;
	}

	/**
	 * Returns whether this event handler handles the given event
	 * 
	 * @param event event to handle
	 * @return whether this event handler handles the given event
	 */
	protected abstract boolean handlesEvent(DebugEvent event);
	
	/**
	 * Handles a create event. 
	 * 
	 * @param event
	 */
	protected void handleCreate(DebugEvent event) {
		refreshRoot(event);
	}
		
	/**
	 * Handles a terminate event.
	 * 
	 * @param event
	 */
	protected void handleTerminate(DebugEvent event) {
		refreshRoot(event);
	}
	
	/**
	 * Handles a suspend event.
	 * 
	 * @param event
	 */	
	protected void handleSuspend(DebugEvent event) {
		refreshRoot(event);
	}
	
	/**
	 * Handles a resume event for which a suspend is expected shortly (<500ms).
	 * 
	 * @param event
	 */
	protected void handleResumeExpectingSuspend(DebugEvent event) {
		// do nothing unless the suspend times out
	}
	
	/**
	 * Handles a resume event that is not expecting an immediate suspend event
	 * 
	 * @param event
	 */
	protected void handleResume(DebugEvent event) {
		refreshRoot(event);
	}
	
	/**
	 * Handles a change event. 
	 * 
	 * @param event
	 */
	protected void handleChange(DebugEvent event) {
		refreshRoot(event);
	}	

	/**
	 * Handles an unknown event.
	 * 
	 * @param event
	 */
	protected void handleOther(DebugEvent event) {
		refreshRoot(event);
	}
	
	/**
	 * Notification that a pending suspend event was not received for the given
	 * resume event within the timeout period.
	 * 
	 * @param resume resume event with missing suspend event
	 */
	protected void handleSuspendTimeout(DebugEvent event) {
		refreshRoot(event);
	}
	
	/**
	 * Handles the given suspend event which caused a timeout. It is
	 * parired with its original resume event.
	 * 
	 * @param suspend suspend event
	 * @param resume resume event
	 */
	protected void handleLateSuspend(DebugEvent suspend, DebugEvent resume) {
		refreshRoot(suspend);
	}

	/**
	 * Fires a model delta to indicate that the launch manager should be refreshed.
	 * Subclasses should override individual handle events to provide deltas that
	 * better reflect the actual change in the model.
	 */
	protected void refreshRoot(DebugEvent event) {
		ModelDelta delta = new ModelDelta(DebugPlugin.getDefault().getLaunchManager(), IModelDelta.CONTENT);
		fireDelta(delta);
	}
	
	/**
	 * Fires the given delta, unless this handler has been disposed.
	 * 
	 * @param delta
	 */
	protected void fireDelta(IModelDelta delta) {
		AbstractModelProxy modelProxy = getModelProxy();
		if (modelProxy != null) {
			if (DEBUG_DELTAS) {
				DebugUIPlugin.debug("FIRE DELTA: " + delta.toString()); //$NON-NLS-1$
			}
			modelProxy.fireModelChanged(delta);
		}
	}
	
	/**
	 * Returns whether this handler has been disposed.
	 * 
	 * @return whether this handler has been disposed
	 */
	protected synchronized boolean isDisposed() {
		return fModelProxy == null;
	}
}
