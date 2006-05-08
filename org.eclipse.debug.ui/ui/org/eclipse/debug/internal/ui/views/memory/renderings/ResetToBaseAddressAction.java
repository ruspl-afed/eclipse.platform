/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.lang.reflect.Method;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.memory.AbstractTableRendering;
import org.eclipse.jface.action.Action;

/**
 * Reest MemoryViewTab to the base address of a memory block
 * 
 * @since 3.0
 */
public class ResetToBaseAddressAction extends Action {

    private AbstractBaseTableRendering fRendering;

    public ResetToBaseAddressAction(AbstractBaseTableRendering rendering) {
        fRendering = rendering;
        setText(DebugUIMessages.ResetMemoryBlockAction_title);
        setToolTipText(DebugUIMessages.ResetMemoryBlockAction_tootip);

        setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_RESET_MEMORY));
        setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_RESET_MEMORY));
        setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_RESET_MEMORY));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.IAction#run()
     */
    public void run() {
    	
    	// check if client has overrode the #reset method
    	// if client has overrode #reset method, call old method
    	// otherwise, call new #resetRendering method
    	// This is done to ensure that client's code will continue to be executed until
    	// they have migrated to the new #resetRendering API
    	Class renderingClass = fRendering.getClass();
    	try {
			Method method = renderingClass.getMethod("reset", new Class[]{}); //$NON-NLS-1$
			if (method.getDeclaringClass().equals(AbstractTableRendering.class))
			{
				// client has not overrode, call new method
				try {
					fRendering.resetRendering();
				} catch (DebugException e) {
					MemoryViewUtil.openError(DebugUIMessages.AbstractTableRendering_12, DebugUIMessages.AbstractTableRendering_13, e); //
				}
				return;
			}
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
			try {
				// if no such method, then it must be AbstractAsycTableRendering
				fRendering.resetRendering();
			} catch (DebugException e1) {
				MemoryViewUtil.openError(DebugUIMessages.AbstractTableRendering_12, DebugUIMessages.AbstractTableRendering_13, e); //
			}
		}
		
		if(fRendering instanceof AbstractTableRendering)
		{
			// call old method
			((AbstractTableRendering)fRendering).reset();
		}
    }
}
