package org.eclipse.update.tests.regularInstall;

import java.io.File;
import java.net.URL;

import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestInstall extends UpdateManagerTestCase {
	/**
	 * Constructor for Test1
	 */
	public TestInstall(String arg0) {
		super(arg0);
	}
	
	
	private IFeature getFeature1(ISite site){
		URL id = UpdateManagerUtils.getURL(site.getURL(),"features/org.eclipse.update.core.tests.feature1_1.0.4.jar",null);
		DefaultPackagedFeature remoteFeature = new DefaultPackagedFeature(id,site);
		return remoteFeature;
	}	
	

	public void testFileSite() throws Exception{
		
		ISite remoteSite = new URLSite(SOURCE_FILE_SITE);
		IFeature remoteFeature = getFeature1(remoteSite);
		ISite localSite = new FileSite(TARGET_FILE_SITE);
		localSite.install(remoteFeature,null);
		
		// verify
		String site = localSite.getURL().getFile();
		IPluginEntry[] entries = remoteFeature.getPluginEntries();
		assertTrue("no plugins entry",(entries!=null && entries.length!=0));
		String pluginName= entries[0].getIdentifier().toString();
		File pluginFile = new File(site,AbstractSite.DEFAULT_PLUGIN_PATH+pluginName);
		assertTrue("plugin files not installed locally",pluginFile.exists());

		File featureFile = new File(site,FileSite.INSTALL_FEATURE_PATH+remoteFeature.getIdentifier().toString());
		assertTrue("feature info not installed locally",featureFile.exists());
		//cleanup
		removeFromFileSystem(pluginFile);

	}


	private IFeature getFeature2(ISite site){
		URL id = UpdateManagerUtils.getURL(site.getURL(),"features/features2.jar",null);
		DefaultPackagedFeature remoteFeature = new DefaultPackagedFeature(id,site);
		return remoteFeature;
	}	
	

	public void testHTTPSite() throws Exception{
		
		ISite remoteSite = new URLSite(SOURCE_HTTP_SITE);
		IFeature remoteFeature = getFeature2(remoteSite);
		ISite localSite = new FileSite(TARGET_FILE_SITE);
		localSite.install(remoteFeature,null);

		String site = localSite.getURL().getFile();
		IPluginEntry[] entries = remoteFeature.getPluginEntries();
		assertTrue("no plugins entry",(entries!=null && entries.length!=0));
		
		String pluginName= entries[0].getIdentifier().toString();
		File pluginFile = new File(site,AbstractSite.DEFAULT_PLUGIN_PATH+pluginName);
		assertTrue("plugin info not installed locally",pluginFile.exists());

		File featureFile = new File(site,FileSite.INSTALL_FEATURE_PATH+remoteFeature.getIdentifier().toString());
		assertTrue("feature info not installed locally",featureFile.exists());

		//cleanup
		removeFromFileSystem(pluginFile);
	}
}

