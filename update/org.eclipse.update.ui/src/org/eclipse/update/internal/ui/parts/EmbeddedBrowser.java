package org.eclipse.update.internal.ui.parts;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.SWT;
import org.eclipse.update.ui.forms.IFormPage;
import org.eclipse.swt.events.*;
import org.eclipse.swt.ole.win32.*;
import org.eclipse.ui.texteditor.IUpdate;
import org.eclipse.update.internal.ui.*;
import java.util.*;


public class EmbeddedBrowser implements IUpdateFormPage {
	private int ADDRESS_SIZE = 10;
	private WebBrowser browser;
	private Control control;
	private Combo addressCombo;
	private Object input;
	
	public void setInput(Object input) {
		this.input = input;
	}
	
	public Object getInput() {
		return input;
	}
	
	class UninstallURLAction implements IURLAction {
		public void run(Hashtable params) {
			System.out.println("Uninstall: "+params.toString());
			System.out.println("Input: "+input.toString());
		}
	}
	
	/**
	 * @see IUpdateFormPage#contextMenuAboutToShow(IMenuManager)
	 */
	public boolean contextMenuAboutToShow(IMenuManager manager) {
		return false;
	}

	/**
	 * @see IUpdateFormPage#getAction(String)
	 */
	public IAction getAction(String id) {
		return null;
	}

	/**
	 * @see IUpdateFormPage#openTo(Object)
	 */
	public void openTo(Object object) {
		if (object instanceof String) {
			String url = object.toString();
			addressCombo.setText(url);
			navigate(url);
		}
	}

	/**
	 * @see IUpdateFormPage#performGlobalAction(String)
	 */
	public void performGlobalAction(String id) {
	}

	/**
	 * @see IUpdateFormPage#init(Object)
	 */
	public void init(Object model) {
		UninstallURLAction uninstallURLAction = new UninstallURLAction();
		UpdateUIPlugin.getDefault().registerURLAction("uninstall", uninstallURLAction);
	}

	/**
	 * @see IUpdateFormPage#update()
	 */
	public void update() {
	}

	/**
	 * @see IFormPage#becomesInvisible(IFormPage)
	 */
	public boolean becomesInvisible(IFormPage newPage) {
		return true;
	}

	/**
	 * @see IFormPage#becomesVisible(IFormPage)
	 */
	public void becomesVisible(IFormPage previousPage) {
	}

	/**
	 * @see IFormPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		//browser = new WebBrowser(parent);
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		Composite navContainer = new Composite(container, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 1;
		navContainer.setLayout(layout);
		createNavBar(navContainer);
		navContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		browser = new WebBrowser(container);
		Control c = browser.getControl();
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		Composite statusContainer = new Composite(container, SWT.NONE);
		statusContainer.setLayoutData(
				new GridData(GridData.FILL_HORIZONTAL));
		final BrowserControlSite site = browser.getControlSite();
		site.setStatusContainer(statusContainer);
		site.addEventListener(WebBrowser.DownloadComplete, new OleListener() {
			public void handleEvent(OleEvent event) {
				String url = site.getPresentationURL();
				if (url!=null)
			   		addressCombo.setText(url);
			}
		});
		control = container;
	}
	
	public void addUpdate(IUpdate update) {
		browser.addUpdate(update);
	}
	
	public void removeUpdate(IUpdate update) {
		browser.removeUpdate(update);
	}
	
	private void createNavBar(Composite parent) {
		Label addressLabel = new Label(parent, SWT.NONE);
		addressLabel.setText("Address");

		addressCombo = new Combo(parent, SWT.DROP_DOWN | SWT.BORDER);
		addressCombo.addSelectionListener(new SelectionListener () {
			public void widgetSelected(SelectionEvent e) {
				String text = addressCombo.getItem(addressCombo.getSelectionIndex());
				navigate(text);
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				navigate(addressCombo.getText());
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);	
		addressCombo.setLayoutData(gd);
	}
	
	private void navigate(String url) {
		browser.navigate(url);
		String [] items = addressCombo.getItems();
		int loc = -1;
		String normURL = getNormalizedURL(url);
		for (int i=0; i<items.length; i++) {
			String normItem = getNormalizedURL(items[i]);
			if (normURL.equals(normItem)) {
				// match 
				loc = i;
				break;
			}
		}
		if (loc != -1) {
			addressCombo.remove(loc);
		}
		addressCombo.add(url, 0);
		if (addressCombo.getItemCount()>ADDRESS_SIZE) {
			addressCombo.remove(addressCombo.getItemCount()-1);
		}
	}
	
	public boolean canPerformBackward() {
		return browser.isBackwardEnabled();
	}
	
	public void performBackward() {
		browser.back();
	}
	
	public void performForward() {
		browser.forward();
	}
	
	public boolean canPerformForward() {
		return browser.isForwardEnabled();
	}
	
	private String getNormalizedURL(String url) {
		url = url.toLowerCase();
		if (url.indexOf("://")== -1) {
			url = "http://"+url;
		}
		return url;
	}
	
	public void dispose() {
		UpdateUIPlugin.getDefault().unregisterURLAction("uninstall");
		if (browser!=null) browser.dispose();
	}
	
	/**
	 * @see IFormPage#getControl()
	 */
	public Control getControl() {
		return control;
	}

	/**
	 * @see IFormPage#getLabel()
	 */
	public String getLabel() {
		return "Web Browser";
	}

	/**
	 * @see IFormPage#getTitle()
	 */
	public String getTitle() {
		return getLabel();
	}

	/**
	 * @see IFormPage#isSource()
	 */
	public boolean isSource() {
		return false;
	}

	/**
	 * @see IFormPage#isVisible()
	 */
	public boolean isVisible() {
		return true;
	}

}

