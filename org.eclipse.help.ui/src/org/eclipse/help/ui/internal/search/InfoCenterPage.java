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

package org.eclipse.help.ui.internal.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.help.INode;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableTocsArray;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.ui.RootScopePage;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Local Help participant in the federated search.
 */
public class InfoCenterPage extends RootScopePage {
	private Text urlText;

	private Button searchAll;

	private Button searchSelected;

	private CheckboxTreeViewer tree;

	private ITreeContentProvider treeContentProvider;

	private ILabelProvider elementLabelProvider;

	private AdaptableTocsArray remoteTocs;

	//private boolean firstCheck;

	private RemoteWorkingSet workingSet;
	
	private boolean tocStale;

	class RemoteWorkingSet extends WorkingSet {
		public RemoteWorkingSet() {
			super("InfoCenter"); //$NON-NLS-1$
		}

		public void load(IPreferenceStore store) {
			String elements = store.getString(getKey(InfoCenterSearchScopeFactory.P_TOCS));
			StringTokenizer stok = new StringTokenizer(elements, InfoCenterSearchScopeFactory.TOC_SEPARATOR);
			ArrayList list = new ArrayList();
			while (stok.hasMoreTokens()) {
				final String url = stok.nextToken();
				AdaptableHelpResource res = find(url);
				if (res != null)
					list.add(res);
			}
			setElements((AdaptableHelpResource[]) list
					.toArray(new AdaptableHelpResource[list.size()]));
		}

		private AdaptableHelpResource find(String url) {
			if (remoteTocs == null)
				return null;
			IAdaptable[] children = remoteTocs.getChildren();
			for (int i = 0; i < children.length; i++) {
				IAdaptable child = children[i];
				if (child instanceof AdaptableHelpResource) {
					AdaptableHelpResource res = (AdaptableHelpResource) child;
					if (res.getHref().equals(url))
						return res;
				}
			}
			return null;
		}

		public void store(IPreferenceStore store) {
			StringBuffer buf = new StringBuffer();
			AdaptableHelpResource[] elements = getElements();

			for (int i = 0; i < elements.length; i++) {
				if (i > 0)
					buf.append(InfoCenterSearchScopeFactory.TOC_SEPARATOR);
				buf.append(elements[i].getHref());
			}
			store.setValue(getKey(InfoCenterSearchScopeFactory.P_TOCS), buf.toString());
		}
	}

	/**
	 * Default constructor.
	 */
	public InfoCenterPage() {
		//firstCheck = true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.help.ui.RootScopePage#createScopeContents(org.eclipse.swt.widgets.Composite)
	 */
	protected int createScopeContents(Composite parent) {
		Font font = parent.getFont();
		initializeDialogUnits(parent);

		Label label = new Label(parent, SWT.NULL);
		label.setText(Messages.InfoCenterPage_url); 

		urlText = new Text(parent, SWT.BORDER);
		urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		urlText.setEditable(getEngineDescriptor().isUserDefined());

		searchAll = new Button(parent, SWT.RADIO);
		searchAll.setText(Messages.selectAll); 
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		searchAll.setLayoutData(gd);
		searchAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (searchAll.getSelection())
					tree.getTree().setEnabled(false);
			}
		});

		searchSelected = new Button(parent, SWT.RADIO);
		searchSelected.setText(Messages.selectWorkingSet); 
		gd = new GridData();
		gd.horizontalSpan = 2;
		searchSelected.setLayoutData(gd);
		searchSelected.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (searchSelected.getSelection()) {
					tree.getTree().setEnabled(true);
					if (tocStale)
						updateTocs();
				}
			}
		});

		label = new Label(parent, SWT.WRAP);
		label.setFont(font);
		label.setText(Messages.WorkingSetContent); 
		gd = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		tree = new CheckboxTreeViewer(parent, SWT.BORDER | SWT.H_SCROLL
				| SWT.V_SCROLL);
		gd = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		gd.heightHint = convertHeightInCharsToPixels(15);
		gd.horizontalSpan = 2;
		tree.getControl().setLayoutData(gd);
		tree.getControl().setFont(font);

		treeContentProvider = new HelpWorkingSetTreeContentProvider();
		tree.setContentProvider(treeContentProvider);

		elementLabelProvider = new HelpWorkingSetElementLabelProvider();
		tree.setLabelProvider(elementLabelProvider);

		tree.setUseHashlookup(true);

		initializeControls();

		tree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				handleCheckStateChange(event);
			}
		});

		tree.addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			public void treeExpanded(TreeExpansionEvent event) {
				final Object element = event.getElement();
				if (tree.getGrayed(element) == false)
					BusyIndicator.showWhile(getShell().getDisplay(),
							new Runnable() {
								public void run() {
									setSubtreeChecked(element, tree
											.getChecked(element), false);
								}
							});
			}
		});
		
		applyDialogFont(parent);

		// Set help for the page
		// WorkbenchHelp.setHelp(tree, "help_workingset_page");
		return 2;
	}

	private void loadTocs(String urlName) {
		InputStream is = null;
		if (urlName==null || urlName.length()==0) {
			resetRemoteTocs();
			return;
		}
		try {
			URL url = new URL(urlName);
			url = new URL(url, "toc/"); //$NON-NLS-1$
			URLConnection connection = url.openConnection();
			is = connection.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "utf-8"));//$NON-NLS-1$
			load(reader);
			reader.close();
		} catch (MalformedURLException e) {
			HelpUIPlugin.logError(Messages.InfoCenterPage_invalidURL, e, false, true);
			resetRemoteTocs();
		} catch (IOException e) {
			HelpUIPlugin.logError(Messages.InfoCenterPage_tocError, e, false, true);
			resetRemoteTocs();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	private void resetRemoteTocs() {
		remoteTocs = new AdaptableTocsArray(new IToc[0]);
	}

	private void load(Reader r) {
		Document document = null;
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
			// parser.setProcessNamespace(true);
			document = parser.parse(new InputSource(r));

			// Strip out any comments first
			Node root = document.getFirstChild();
			while (root.getNodeType() == Node.COMMENT_NODE) {
				document.removeChild(root);
				root = document.getFirstChild();
			}
			load(document, (Element) root);
		} catch (ParserConfigurationException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		} catch (SAXException e) {
			// ignore
		}
	}

	private void load(Document doc, Element root) {
		ArrayList list = new ArrayList();
		NodeList engines = root.getElementsByTagName("toc"); //$NON-NLS-1$
		for (int i = 0; i < engines.getLength(); i++) {
			final Node node = engines.item(i);
			IToc toc = new IToc() {
				public ITocContribution getTocContribution() {
					return null;
				}
				public ITopic[] getTopics() {
					return new ITopic[0];
				}
				public ITopic getTopic(String href) {
					return null;
				}
				public String getHref() {
					return node.getAttributes().getNamedItem("href") //$NON-NLS-1$
							.getNodeValue();
				}
				public String getLabel() {
					return node.getAttributes().getNamedItem("label") //$NON-NLS-1$
							.getNodeValue();
				}
				public INode[] getChildren() {
					return new INode[0];
				}
				public INode getParent() {
					return null;
				}
			};
			list.add(toc);
		}
		IToc[] tocs = (IToc[]) list.toArray(new IToc[list.size()]);
		remoteTocs = new AdaptableTocsArray(tocs);
	}

	private void initializeControls() {
		IPreferenceStore store = getPreferenceStore();
		String url = store
				.getString(getKey(InfoCenterSearchScopeFactory.P_URL));
		if (url.length() == 0) {
			url = (String) getEngineDescriptor().getParameters().get(
					InfoCenterSearchScopeFactory.P_URL);
			if (url == null)
				url = ""; //$NON-NLS-1$
		}
		urlText.setText(url);
		busyLoadTocs(url);
		workingSet = new RemoteWorkingSet();
		workingSet.load(store);
		urlText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				tocStale=true;
			}
		});
		tree.setInput(remoteTocs);
		boolean selected = store
				.getBoolean(getKey(InfoCenterSearchScopeFactory.P_SEARCH_SELECTED));
		searchAll.setSelection(!selected);
		searchSelected.setSelection(selected);
		tree.getTree().setEnabled(selected);
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				Object[] elements = workingSet.getElements();
				tree.setCheckedElements(elements);
				for (int i = 0; i < elements.length; i++) {
					Object element = elements[i];
					if (isExpandable(element))
						setSubtreeChecked(element, true, true);
					updateParentState(element, true);
				}
			}
		});
	}
	
	private void updateTocs() {
		String url = urlText.getText();
		busyLoadTocs(url);
		tocStale=false;
		tree.setInput(remoteTocs);
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				Object[] elements = workingSet.getElements();
				tree.setCheckedElements(elements);
				for (int i = 0; i < elements.length; i++) {
					Object element = elements[i];
					if (isExpandable(element))
						setSubtreeChecked(element, true, true);
					updateParentState(element, true);
				}
			}
		});
	}
	
	private void busyLoadTocs(final String url) {
		BusyIndicator.showWhile(urlText.getDisplay(), new Runnable() {
			public void run() {
				loadTocs(url);
			}
		});		
	}

	boolean isExpandable(Object element) {
		return treeContentProvider.hasChildren(element);
	}

	void updateParentState(Object child, boolean baseChildState) {
		if (child == null)
			return;

		Object parent = treeContentProvider.getParent(child);
		if (parent == null)
			return;

		boolean allSameState = true;
		Object[] children = null;
		children = treeContentProvider.getChildren(parent);

		for (int i = children.length - 1; i >= 0; i--) {
			if (tree.getChecked(children[i]) != baseChildState
					|| tree.getGrayed(children[i])) {
				allSameState = false;
				break;
			}
		}

		tree.setGrayed(parent, !allSameState);
		tree.setChecked(parent, !allSameState || baseChildState);

		updateParentState(parent, baseChildState);
	}

	void setSubtreeChecked(Object parent, boolean state,
			boolean checkExpandedState) {

		Object[] children = treeContentProvider.getChildren(parent);
		for (int i = children.length - 1; i >= 0; i--) {
			Object element = children[i];
			if (state) {
				tree.setChecked(element, true);
				tree.setGrayed(element, false);
			} else
				tree.setGrayChecked(element, false);
			if (isExpandable(element))
				setSubtreeChecked(element, state, checkExpandedState);
		}
	}

	private void findCheckedElements(java.util.List checkedResources,
			Object parent) {
		Object[] children = treeContentProvider.getChildren(parent);
		for (int i = 0; i < children.length; i++) {
			if (tree.getGrayed(children[i]))
				findCheckedElements(checkedResources, children[i]);
			else if (tree.getChecked(children[i]))
				checkedResources.add(children[i]);
		}
	}

	void handleCheckStateChange(final CheckStateChangedEvent event) {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				Object element = event.getElement();
				boolean state = event.getChecked();
				tree.setGrayed(element, false);
				if (isExpandable(element))
					setSubtreeChecked(element, state, state);
				// only check subtree if state is set to true

				updateParentState(element, state);
				// validateInput();
			}
		});
	}

	public void updateWorkingSet() {
		ArrayList elements = new ArrayList(10);
		findCheckedElements(elements, tree.getInput());
		workingSet.setElements((AdaptableHelpResource[]) elements
				.toArray(new AdaptableHelpResource[elements.size()]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		IPreferenceStore store = getPreferenceStore();
		if (getEngineDescriptor().isUserDefined())
			store.setValue(getKey(InfoCenterSearchScopeFactory.P_URL), urlText
					.getText());
		updateWorkingSet();
		workingSet.store(store);
		store.setValue(getKey(InfoCenterSearchScopeFactory.P_SEARCH_SELECTED),
				searchSelected.getSelection());
		return super.performOk();
	}

	private String getKey(String key) {
		return getEngineDescriptor().getId() + "." + key; //$NON-NLS-1$
	}
}
