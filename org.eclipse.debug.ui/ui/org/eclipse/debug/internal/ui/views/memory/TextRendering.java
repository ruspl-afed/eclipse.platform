/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.core.model.IMemoryBlock;

/**
 * Represents a text rendering
 */
public class TextRendering extends AbstractMemoryRendering {

	/**
	 * @param memoryBlock
	 * @param renderingId
	 */
	public TextRendering(IMemoryBlock memoryBlock, String renderingId) {
		super(memoryBlock, renderingId);
	}
}
