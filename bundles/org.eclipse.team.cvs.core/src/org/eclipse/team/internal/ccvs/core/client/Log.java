/*******************************************************************************
 * Copyright (c) 2000, 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 * IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command.GlobalOption;

public class Log extends AbstractMessageCommand {
	/*** Local options: specific to log ***/
	
	public static LocalOption makeRevisionOption(String revision) {
		return new LocalOption("-r" + revision, null); //$NON-NLS-1$
	}
	public static final LocalOption RCS_FILE_NAMES_ONLY = new LocalOption("-R"); //$NON-NLS-1$
	
	protected Log() { }
	protected String getRequestId() {
		return "log"; //$NON-NLS-1$
	}
	
	protected ICVSResource[] sendLocalResourceState(Session session, GlobalOption[] globalOptions,
		LocalOption[] localOptions, ICVSResource[] resources, IProgressMonitor monitor)
		throws CVSException {			
		
		// Send all folders that are already managed to the server
		boolean sendEmptyFolders = Command.findOption(localOptions, RCS_FILE_NAMES_ONLY.getOption()) != null;
		new FileStructureVisitor(session, sendEmptyFolders, false /* send modified contents */, monitor).visit(session, resources);
		return resources;
	}
}

