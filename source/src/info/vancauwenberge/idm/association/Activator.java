/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package info.vancauwenberge.idm.association;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "info.vancauwenberge.associationeditor"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	public static void log(String msg) {
		Activator.log(msg, null);
	   }
	
	public static void log(String msg, Throwable e) {
		if (e != null)
			plugin.getLog().log(new Status(Status.ERROR, plugin.getBundle().getSymbolicName(), Status.OK, msg, e));
		else
			plugin.getLog().log(new Status(Status.OK, plugin.getBundle().getSymbolicName(), Status.OK, msg, e));
	}
	   
	public static Properties getProperties(){
		IPath path = plugin.getStateLocation();
		path = path.append("associationEditor.properties");
		Properties props = new Properties();
		File f = path.toFile();
		if (f.exists())
			try {
				props.load(new FileReader(f));
			} catch (Exception e) {
				e.printStackTrace();
			}
		return props;
	}

	public static void storeProperties(Properties props){
		IPath path = plugin.getStateLocation();
		path = path.append("associationEditor.properties");
		File f = path.toFile();
		f.delete();
		try {
			props.store(new FileWriter(f),"");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Shell getActiveShell(){
		return plugin.getWorkbench().getActiveWorkbenchWindow().getShell();
	}
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}
}
