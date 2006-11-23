/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * A CompareEditor takes a ICompareEditorInput as input.
 * Most functionality is delegated to the ICompareEditorInput.
 */
public class CompareEditor extends EditorPart implements IReusableEditor, ISaveablesSource, ICompareContainer, IPropertyChangeListener {

	public final static String CONFIRM_SAVE_PROPERTY= "org.eclipse.compare.internal.CONFIRM_SAVE_PROPERTY"; //$NON-NLS-1$

	private static final int UNINITIALIZED = 0;
	private static final int INITIALIZING = 1;
	private static final int NO_DIFF = 2;
	private static final int CANCELED = 3;
	private static final int INITIALIZED = 4;
	private static final int ERROR = 5;
	
	private IActionBars fActionBars;
	
	private PageBook fPageBook;
	
	/** the SWT control from the compare editor input*/
	private Control fControl;
	/** the outline page */
	private CompareOutlinePage fOutlinePage;

	private CompareSaveable fSaveable;

	private Control initializingPage;
	private Control noDiffFoundPage;
	private Control canceledPage;
	
	private FormToolkit forms;
	private int state = UNINITIALIZED;

	private Composite errorPage;

	/**
	 * No-argument constructor required for extension points.
	 */
	public CompareEditor() {
		// empty default implementation
	}
	
	/* (non-Javadoc)
	 * Method declared on IAdaptable
	 */
	public Object getAdapter(Class key) {
		
		if (key.equals(IContentOutlinePage.class)) {
			Object object= getCompareConfiguration().getProperty(CompareConfiguration.USE_OUTLINE_VIEW);
			if (object instanceof Boolean && ((Boolean)object).booleanValue()) {
				IEditorInput input= getEditorInput();
				if (input instanceof CompareEditorInput) {
					fOutlinePage= new CompareOutlinePage((CompareEditorInput) input);
					return fOutlinePage;
				}
			}
		}
		return super.getAdapter(key);
	}
	
	/*
	 * Helper method used by ComapreEditorConfiguration to get at the compare configuration of the editor
	 */
	/* package */ CompareConfiguration getCompareConfiguration() {
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput)
			return ((CompareEditorInput)input).getCompareConfiguration();
		return null;
	}
				
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		
		if (!(input instanceof CompareEditorInput))
			throw new PartInitException(Utilities.getString("CompareEditor.invalidInput")); //$NON-NLS-1$
				
		setSite(site);
		setInput(input);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	public void setInput(IEditorInput input) {
		try {
	        doSetInput(input);
	        // Need to refresh the contributor (see #67888)
	        IEditorSite editorSite= getEditorSite();
	        if (editorSite != null) {
		        IEditorActionBarContributor actionBarContributor= editorSite.getActionBarContributor();
		        if (actionBarContributor != null) {
		        		actionBarContributor.setActiveEditor(null);
		        		actionBarContributor.setActiveEditor(this);
		        }
	        }
		} catch (CoreException x) {
			String title= Utilities.getString("CompareEditor.error.setinput.title"); //$NON-NLS-1$
			String msg= Utilities.getString("CompareEditor.error.setinput.message"); //$NON-NLS-1$
			ErrorDialog.openError(getSite().getShell(), title, msg, x.getStatus());
		}				
	}
	
	private void doSetInput(IEditorInput input) throws CoreException {
		if (!(input instanceof CompareEditorInput)) {
			IStatus s= new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, Utilities.getString("CompareEditor.invalidInput"), null); //$NON-NLS-1$
			throw new CoreException(s);
		}

		IEditorInput oldInput= getEditorInput();
		if (oldInput instanceof IPropertyChangeNotifier)
			((IPropertyChangeNotifier)input).removePropertyChangeListener(this);

		ISaveablesLifecycleListener lifecycleListener= null;
		if (oldInput != null) {
			lifecycleListener= (ISaveablesLifecycleListener) getSite().getService(ISaveablesLifecycleListener.class);
			lifecycleListener.handleLifecycleEvent(
				new SaveablesLifecycleEvent(this, SaveablesLifecycleEvent.POST_CLOSE, getSaveables(), false));
		}
			
		super.setInput(input);
		
		final CompareEditorInput cei= (CompareEditorInput) input;
		cei.setContainer(this);

		setTitleImage(cei.getTitleImage());
		setPartName(cei.getTitle());
		setTitleToolTip(cei.getToolTipText());
				
		if (input instanceof IPropertyChangeNotifier)
			((IPropertyChangeNotifier)input).addPropertyChangeListener(this);
			
		state = cei.getCompareResult() == null ? INITIALIZING : INITIALIZED;
		Point oldSize = null;
		if (oldInput != null) {
			if (fControl != null && !fControl.isDisposed()) {
				oldSize= fControl.getSize();
				fControl.dispose();
				fControl = null;
			}
		}
		if (fPageBook != null)
			createCompareControl();
		if (fControl != null && oldSize != null)
			fControl.setSize(oldSize);
		
		if (cei.getCompareResult() == null) {
			initializeInBackground(cei);
		}
        
        firePropertyChange(IWorkbenchPartConstants.PROP_INPUT);
        
        if (lifecycleListener != null) {
        	lifecycleListener.handleLifecycleEvent(
        		new SaveablesLifecycleEvent(this, SaveablesLifecycleEvent.POST_OPEN, getSaveables(), false));
        }
	}
	
	protected void initializeInBackground(final CompareEditorInput cei) {
		// Need to cancel any running jobs associated with the oldInput
		Job.getJobManager().cancel(this);
		Job job = new Job(CompareMessages.CompareEditor_0) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status;
				try {
					status = CompareUIPlugin.getDefault().prepareInput(cei, monitor);
					if (status.isOK()) {
						// We need to update the saveables list
						state = INITIALIZED;
						Saveable[] saveables = getSaveables();
						if (saveables.length > 0) {
							ISaveablesLifecycleListener listener= (ISaveablesLifecycleListener) getSite().getService(ISaveablesLifecycleListener.class);
							if (listener != null) {
								listener.handleLifecycleEvent(
										new SaveablesLifecycleEvent(CompareEditor.this, SaveablesLifecycleEvent.POST_OPEN, saveables, false));
							}
						}
						return Status.OK_STATUS;
					}
					if (status.getCode() == CompareUIPlugin.NO_DIFFERENCE) {
						state = NO_DIFF;
						return Status.OK_STATUS;
					}
					state = ERROR;
				} catch (OperationCanceledException e) {
					state= CANCELED;
					status = Status.CANCEL_STATUS;
				} finally {
					if (monitor.isCanceled())
						state= CANCELED;
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							createCompareControl();
						}
					});
				}
				return status;
			}
			public boolean belongsTo(Object family) {
				if (family == CompareEditor.this || family == cei)
					return true;
				return cei.belongsTo(family);
			}
		};
		job.setUser(true);
		Utilities.schedule(job, getSite());
	}

	/*
	 * Helper method used to find an action bars using the Utilities#findActionsBars(Control)
	 */
	public IActionBars getActionBars() {
		return fActionBars;
	}
	
	/*
	 * Set the action bars so the Utilities class can access it.
	 */
	/* package */ void setActionBars(IActionBars actionBars) {
		fActionBars= actionBars;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		parent.setData(this);
		fPageBook = new PageBook(parent, SWT.NONE);
		createCompareControl();
	}

	private void createCompareControl() {
		if (fPageBook.isDisposed())
			return;
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput) {
			CompareEditorInput ci = (CompareEditorInput) input;
			if (ci.getCompareResult() == null) {
				if (state == INITIALIZING) {
					if (initializingPage == null) {
						initializingPage = getInitializingMessagePane(fPageBook);
					}
					fPageBook.showPage(initializingPage);
				} else if (state == CANCELED) {
					if (canceledPage == null) {
						canceledPage = getCanceledMessagePane(fPageBook);
					}
					fPageBook.showPage(canceledPage);
				} else if (state == NO_DIFF) {
					if (noDiffFoundPage == null) {
						noDiffFoundPage = getNoDifferenceMessagePane(fPageBook);
					}
					fPageBook.showPage(noDiffFoundPage);
				} else if (state == ERROR) {
					if (errorPage == null) {
						errorPage = getErrorMessagePane(fPageBook);
					}
					fPageBook.showPage(errorPage);
				}
			} else {
				fControl= (ci).createContents(fPageBook);
				fPageBook.showPage(fControl);
				PlatformUI.getWorkbench().getHelpSystem().setHelp(fControl, ICompareContextIds.COMPARE_EDITOR);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	public void dispose() {
	
		IEditorInput input= getEditorInput();
		if (input instanceof IPropertyChangeNotifier)
			((IPropertyChangeNotifier)input).removePropertyChangeListener(this);
								
		super.dispose();
	}
			
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput)
			((CompareEditorInput)input).setFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/* (non-Javadoc)
	 * Always throws an AssertionFailedException.
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		Assert.isTrue(false); // Save As not supported for CompareEditor
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		final IEditorInput input= getEditorInput();
		
		WorkspaceModifyOperation operation= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor pm) throws CoreException {
				if (input instanceof CompareEditorInput)
					((CompareEditorInput)input).saveChanges(pm);
			}
		};

		Shell shell= getSite().getShell();
		
		try {
			
			operation.run(progressMonitor);
									
			firePropertyChange(PROP_DIRTY);
			
		} catch (InterruptedException x) {
			// NeedWork
		} catch (OperationCanceledException x) {
			// NeedWork
		} catch (InvocationTargetException x) {
			String title= Utilities.getString("CompareEditor.saveError.title"); //$NON-NLS-1$
			String reason= x.getTargetException().getMessage();
			MessageDialog.openError(shell, title, Utilities.getFormattedString("CompareEditor.cantSaveError", reason));	//$NON-NLS-1$
		}
	}	
		
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	public boolean isDirty() {
		IEditorInput input= getEditorInput();
		if (input instanceof ISaveablesSource) {
			ISaveablesSource sms= (ISaveablesSource) input;
			Saveable[] models= sms.getSaveables();
			for (int i= 0; i < models.length; i++) {
				Saveable model= models[i];
				if (model.isDirty())
					return true;
			}
		}
		if (input instanceof CompareEditorInput)
			return ((CompareEditorInput)input).isSaveNeeded();
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(CompareEditorInput.DIRTY_STATE)) {
			Object old_value= event.getOldValue();
			Object new_value= event.getNewValue();
			if (old_value == null || new_value == null || !old_value.equals(new_value))
				firePropertyChange(PROP_DIRTY);
		} else if (event.getProperty().equals(CompareEditorInput.PROP_TITLE)) {
			setPartName(((CompareEditorInput)getEditorInput()).getTitle());
			setTitleToolTip(((CompareEditorInput)getEditorInput()).getToolTipText());
		} else if (event.getProperty().equals(CompareEditorInput.PROP_TITLE_IMAGE)) {
			setTitleImage(((CompareEditorInput)getEditorInput()).getTitleImage());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getModels()
	 */
	public Saveable[] getSaveables() {
		IEditorInput input= getEditorInput();
		if (input instanceof ISaveablesSource) {
			ISaveablesSource source= (ISaveablesSource) input;
			return source.getSaveables();
		}
		return new Saveable[] { getSaveable() };
	}

	private Saveable getSaveable() {
		if (fSaveable == null) {
			fSaveable= new CompareSaveable();
		}
		return fSaveable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getActiveModels()
	 */
	public Saveable[] getActiveSaveables() {
		IEditorInput input= getEditorInput();
		if (input instanceof ISaveablesSource) {
			ISaveablesSource source= (ISaveablesSource) input;
			return source.getActiveSaveables();
		}
		return new Saveable[] { getSaveable() };
	}
	
	private class CompareSaveable extends Saveable {

		public String getName() {
			return CompareEditor.this.getPartName();
		}

		public String getToolTipText() {
			return CompareEditor.this.getTitleToolTip();
		}

		public ImageDescriptor getImageDescriptor() {
			return ImageDescriptor.createFromImage(CompareEditor.this.getTitleImage());
		}

		public void doSave(IProgressMonitor monitor) throws CoreException {
			CompareEditor.this.doSave(monitor);
		}

		public boolean isDirty() {
			return CompareEditor.this.isDirty();
		}

		public boolean equals(Object object) {
			return object == this;
		}

		public int hashCode() {
			return CompareEditor.this.hashCode();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#removeCompareInputChangeListener(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener)
	 */
	public void removeCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		input.removeCompareInputChangeListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#addCompareInputChangeListener(org.eclipse.compare.structuremergeviewer.ICompareInput, org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener)
	 */
	public void addCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		input.addCompareInputChangeListener(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#registerContextMenu(org.eclipse.jface.action.MenuManager, org.eclipse.jface.viewers.ISelectionProvider)
	 */
	public void registerContextMenu(MenuManager menu, ISelectionProvider provider) {
		getSite().registerContextMenu(menu, provider);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#setStatusMessage(java.lang.String)
	 */
	public void setStatusMessage(String message) {
		if (fActionBars != null) {
			IStatusLineManager slm= fActionBars.getStatusLineManager();
			if (slm != null) {
				slm.setMessage(message);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#getServiceLocator()
	 */
	public IServiceLocator getServiceLocator() {
		return getSite();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.operation.IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {
		PlatformUI.getWorkbench().getProgressService().run(fork, cancelable, runnable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.ICompareContainer#getNavigator()
	 */
	public ICompareNavigator getNavigator() {
		return null;
	}
	
	private Composite getInitializingMessagePane(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getBackgroundColor(parent));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		
		createDescriptionLabel(composite, CompareMessages.CompareEditor_1);
		createSpacer(composite);
		
		final Button cancelButton = getForms().createButton(composite, CompareMessages.CompareEditor_2, SWT.PUSH);
		cancelButton.setToolTipText(CompareMessages.CompareEditor_3);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 2;
		data.horizontalIndent=5;
		data.verticalIndent=5;
		cancelButton.setLayoutData(data);
		cancelButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				Job.getJobManager().cancel(CompareEditor.this);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing
			}
		});
		return composite;
	}
	
	private Composite getCanceledMessagePane(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getBackgroundColor(parent));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		
		createDescriptionLabel(composite, CompareMessages.CompareEditor_4);
		createSpacer(composite);
		
		final Button initializeButton = getForms().createButton(composite, CompareMessages.CompareEditor_5, SWT.PUSH);
		initializeButton.setToolTipText(CompareMessages.CompareEditor_6);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 1;
		data.horizontalIndent=5;
		data.verticalIndent=5;
		initializeButton.setLayoutData(data);
		initializeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				CompareEditor.this.initializeInBackground((CompareEditorInput)getEditorInput());
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing
			}
		});
		
		data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.horizontalSpan = 1;
		data.horizontalIndent=5;
		data.verticalIndent=5;
		createCloseButton(composite, data);
		return composite;
	}

	private void createSpacer(Composite composite) {
		Label l = new Label(composite, SWT.NONE); // spacer
		GridData data = new GridData(GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 1;
		l.setLayoutData(data);
	}

	private Composite getNoDifferenceMessagePane(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getBackgroundColor(parent));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		createDescriptionLabel(composite, CompareMessages.CompareEditor_7);
		createSpacer(composite);
		createCloseButton(composite);
		return composite;
	}
	
	private Composite getErrorMessagePane(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getBackgroundColor(parent));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		createDescriptionLabel(composite, getErrorMessage());
		createSpacer(composite);
		createCloseButton(composite);
		return composite;
	}

	private void createCloseButton(Composite composite) {
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		data.horizontalSpan = 2;
		data.horizontalIndent=5;
		data.verticalIndent=5;
		createCloseButton(composite, data);
	}
	private void createCloseButton(Composite composite, GridData data) {
		final Button closeButton = getForms().createButton(composite, CompareMessages.CompareEditor_8, SWT.PUSH);
		closeButton.setToolTipText(CompareMessages.CompareEditor_9);
		closeButton.setLayoutData(data);
		closeButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				getSite().getPage().closeEditor(CompareEditor.this, false);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				// Do nothing
			}
		});
	}
	
	private String getErrorMessage() {
		CompareEditorInput input = (CompareEditorInput)getEditorInput();
		String message = input.getMessage();
		if (message == null)
			return CompareMessages.CompareEditor_10;
		return message;
	}

	private Color getBackgroundColor(Composite parent) {
		return parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}
	
	private Label createDescriptionLabel(Composite parent, String text) {
		Label description = new Label(parent, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		description.setLayoutData(data);
		description.setText(text);
		description.setBackground(getBackgroundColor(parent));
		return description;
	}
	
	private FormToolkit getForms() {
		if (forms == null) {
			forms = new FormToolkit(fPageBook.getDisplay());
			forms.setBackground(getBackgroundColor(fPageBook));
			HyperlinkGroup group = forms.getHyperlinkGroup();
			group.setBackground(getBackgroundColor(fPageBook));
		}
		return forms;
	}
	
}

