package org.eclipse.ant.core;

/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp.  All rights reserved.
This file is made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html
**********************************************************************/

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.ant.internal.core.AntClassLoader;
import org.eclipse.ant.internal.core.AntCorePreferences;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.core.InternalCoreAntMessages;
import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Entry point for running Ant scripts inside Eclipse.
 */
public class AntRunner implements IPlatformRunnable, IAntCoreConstants {

	protected String fBuildFileLocation = DEFAULT_BUILD_FILENAME;
	protected List fBuildListeners;
	protected Vector fTargets;
	protected Map fUserProperties;
	protected int fMessageOutputLevel = 2; // Project.MSG_INFO
	protected String fBuildLoggerClassName;
	protected String[] fArguments;

	/** 
	 * Constructs an instance of this class.
	 */
	public AntRunner() {
	}

	protected ClassLoader getClassLoader() {
		AntCorePreferences preferences = AntCorePlugin.getPlugin().getPreferences();
		URL[] urls = preferences.getURLs();
		ClassLoader[] pluginLoaders = preferences.getPluginClassLoaders();
		return new AntClassLoader(urls, pluginLoaders, null);
	}

	/**
	 * Sets the build file location on the file system.
	 * 
	 * @param buildFileLocation the file system location of the build file
	 */
	public void setBuildFileLocation(String buildFileLocation) {
		if (buildFileLocation == null) {
			fBuildFileLocation = DEFAULT_BUILD_FILENAME;
		} else {
			fBuildFileLocation = buildFileLocation;
		}
	}

	/**
	 * Set the message output level.
	 * <p>
	 * Valid values are:
	 * <ul>
	 * <li><code>org.apache.tools.ant.Project.ERR</code>, 
	 * <li><code>org.apache.tools.ant.Project.WARN</code>,
	 * <li><code>org.apache.tools.ant.Project.INFO</code>,
	 * <li><code>org.apache.tools.ant.Project.VERBOSE</code> or
	 * <li><code>org.apache.tools.ant.Project.DEBUG</code>
	 * </ul>
	 * 
	 * @param level the message output level
	 */
	public void setMessageOutputLevel(int level) {
		fMessageOutputLevel = level;
	}

	/**
	 * Sets the arguments to be passed to the script (e.g. -Dos=win32 -Dws=win32 -verbose).
	 * 
	 * @param arguments the arguments to be passed to the script
	 */
	public void setArguments(String arguments) {
		fArguments = getArray(arguments);
	}

	/**
	 * Helper method to ensure an array is converted into an ArrayList.
	 */
	private String[] getArray(String args) {
		StringBuffer sb = new StringBuffer();
		boolean waitingForQuote = false;
		ArrayList result = new ArrayList();
		for (StringTokenizer tokens = new StringTokenizer(args, ", \"", true); tokens.hasMoreTokens();) { //$NON-NLS-1$
			String token = tokens.nextToken();
			if (waitingForQuote) {
				if (token.equals("\"")) { //$NON-NLS-1$
					result.add(sb.toString());
					sb.setLength(0);
					waitingForQuote = false;
				} else {
					sb.append(token);
				}
			} else {
				if (token.equals("\"")) { //$NON-NLS-1$
					// test if we have something like -Dproperty="value"
					if (result.size() > 0) {
						int index = result.size() - 1;
						String last = (String) result.get(index);
						if (last.charAt(last.length() - 1) == '=') {
							result.remove(index);
							sb.append(last);
						}
					}
					waitingForQuote = true;
				} else {
					if (!(token.equals(",") || token.equals(" "))) //$NON-NLS-1$ //$NON-NLS-2$
						result.add(token);
				}
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/**
	 * Sets the arguments to be passed to the script (e.g. -Dos=win32 -Dws=win32 -verbose).
	 * 
	 * @param arguments the arguments to be passed to the script
	 * @since 2.1
	 */
	public void setArguments(String[] arguments) {
		fArguments = arguments;
	}

	/**
	 * Sets the targets and execution order.
	 * 
	 * @param executionTargets which targets should be run and in which order
	 */
	public void setExecutionTargets(String[] executionTargets) {
		fTargets = new Vector(executionTargets.length);
		for (int i = 0; i < executionTargets.length; i++) {
			fTargets.add(executionTargets[i]);
		}
	}

	/**
	 * Adds a build listener. The parameter <code>className</code>
	 * is the class name of a <code>org.apache.tools.ant.BuildListener</code>
	 * implementation. The class will be instantiated at runtime and the
	 * listener will be called on build events
	 * (<code>org.apache.tools.ant.BuildEvent</code>).
	 *
	 * @param className a build listener class name
	 */
	public void addBuildListener(String className) {
		if (className == null) {
			return;
		}
		if (fBuildListeners == null) {
			fBuildListeners = new ArrayList(5);
		}
		fBuildListeners.add(className);
	}

	/**
	 * Adds a build logger. The parameter <code>className</code>
	 * is the class name of a <code>org.apache.tools.ant.BuildLogger</code>
	 * implementation. The class will be instantiated at runtime and the
	 * logger will be called on build events
	 * (<code>org.apache.tools.ant.BuildEvent</code>).
	 *
	 * @param className a build logger class name
	 */
	public void addBuildLogger(String className) {
		fBuildLoggerClassName = className;
	}

	/**
	 * Adds user-defined properties. Keys and values must be String objects.
	 * 
	 * @param properties a Map of user-defined properties
	 */
	public void addUserProperties(Map properties) {
		fUserProperties = properties;
	}

	/**
	 * Returns the build file target information.
	 * 
	 * @return an array containing the target information
	 * 
	 * @see TargetInfo
	 * @since 2.1
	 */
	public TargetInfo[] getAvailableTargets() throws CoreException {
		try {
			ClassLoader loader = getClassLoader();
			Class classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
			Object runner = classInternalAntRunner.newInstance();
			// set build file
			Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] { String.class }); //$NON-NLS-1$
			setBuildFileLocation.invoke(runner, new Object[] { fBuildFileLocation });
			// get the info for each targets
			Method getTargets = classInternalAntRunner.getMethod("getTargets", null); //$NON-NLS-1$
			Object results = getTargets.invoke(runner, null);
			// collect the info into target objects
			String[][] infos = (String[][]) results;
			if (infos.length < 2) {
				return new TargetInfo[0];
			}
			// The last info is the name of the default target or null if none
			int count = infos.length - 1;
			String defaultName = infos[count][0];
			TargetInfo[] targets = new TargetInfo[count];
			for (int i = 0; i < count; i++) {
				String[] info = infos[i];
				boolean isDefault = info[0].equals(defaultName);
				targets[i] = new TargetInfo(info[0], info[1], isDefault);
			}
			return targets;
		} catch (NoClassDefFoundError e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._1"), e)); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._1"), e)); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			String message = (realException.getMessage() == null) ? InternalCoreAntMessages.getString("AntRunner.Build_Failed._3") : realException.getMessage(); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, message, realException));
		} catch (Exception e) {
			String message = (e.getMessage() == null) ? InternalCoreAntMessages.getString("AntRunner.Build_Failed._3") : e.getMessage(); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, message, e));
		}
	}

	/**
	 * Runs the build script. If a progress monitor is specified it will
	 * be available during the script execution as a reference in the
	 * Ant Project (<code>org.apache.tools.ant.Project.getReferences()</code>).
	 * A long-running task could, for example, get the monitor during its
	 * execution and check for cancellation. The key value to retrieve the
	 * progress monitor instance is <code>AntCorePlugin.ECLIPSE_PROGRESS_MONITOR</code>.
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		long startTime = 0;
		if (DEBUG_BUILDFILE_TIMING) {
			startTime = System.currentTimeMillis();
		}
		try {
			ClassLoader loader = getClassLoader();
			Class classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
			Object runner = classInternalAntRunner.newInstance();
			// set build file
			Method setBuildFileLocation = classInternalAntRunner.getMethod("setBuildFileLocation", new Class[] { String.class }); //$NON-NLS-1$
			setBuildFileLocation.invoke(runner, new Object[] { fBuildFileLocation });
			// add listeners
			Method addBuildListeners = classInternalAntRunner.getMethod("addBuildListeners", new Class[] { List.class }); //$NON-NLS-1$
			addBuildListeners.invoke(runner, new Object[] { fBuildListeners });
			// add build logger
			if (fBuildLoggerClassName != null) {
				Method addBuildLogger = classInternalAntRunner.getMethod("addBuildLogger", new Class[] { String.class }); //$NON-NLS-1$
				addBuildLogger.invoke(runner, new Object[] { fBuildLoggerClassName });
			}
			// add progress monitor
			if (monitor != null) {
				Method setProgressMonitor = classInternalAntRunner.getMethod("setProgressMonitor", new Class[] { IProgressMonitor.class }); //$NON-NLS-1$
				setProgressMonitor.invoke(runner, new Object[] { monitor });
			}
			// add properties
			Method addUserProperties = classInternalAntRunner.getMethod("addUserProperties", new Class[] { Map.class }); //$NON-NLS-1$
			addUserProperties.invoke(runner, new Object[] { fUserProperties });
			// set message output level
			Method setMessageOutputLevel = classInternalAntRunner.getMethod("setMessageOutputLevel", new Class[] { int.class }); //$NON-NLS-1$
			setMessageOutputLevel.invoke(runner, new Object[] { new Integer(fMessageOutputLevel)});
			// set execution targets
			if (fTargets != null) {
				Method setExecutionTargets = classInternalAntRunner.getMethod("setExecutionTargets", new Class[] { Vector.class }); //$NON-NLS-1$
				setExecutionTargets.invoke(runner, new Object[] { fTargets });
			}
			// set extra arguments
			if (fArguments != null && fArguments.length > 0) {
				Method setArguments = classInternalAntRunner.getMethod("setArguments", new Class[] { String[].class }); //$NON-NLS-1$
				setArguments.invoke(runner, new Object[] { fArguments });
			}
			// run
			Method run = classInternalAntRunner.getMethod("run", null); //$NON-NLS-1$
			run.invoke(runner, null);
		} catch (NoClassDefFoundError e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._1"), e)); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._1"), e)); //$NON-NLS-1$
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			// J9 throws NoClassDefFoundError nested in a InvocationTargetException
			if ((realException instanceof NoClassDefFoundError) || (realException instanceof ClassNotFoundException)) {
				throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, InternalCoreAntMessages.getString("AntRunner.Could_not_find_one_or_more_classes._Please_check_the_Ant_classpath._1"), realException)); //$NON-NLS-1$
			}
			String message = (realException.getMessage() == null) ? InternalCoreAntMessages.getString("AntRunner.Build_Failed._3") : realException.getMessage(); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, message, realException));
		} catch (Exception e) {
			String message = (e.getMessage() == null) ? "Build Failed." : e.getMessage(); //$NON-NLS-1$
			throw new CoreException(new Status(IStatus.ERROR, PI_ANTCORE, ERROR_RUNNING_SCRIPT, message, e));
		} finally {
			if (DEBUG_BUILDFILE_TIMING) {
				long finishTime = System.currentTimeMillis();
				System.out.println(InternalCoreAntMessages.getString("AntRunner.Buildfile_run_took___9") + (finishTime - startTime) + InternalCoreAntMessages.getString("AntRunner._milliseconds._10")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * Runs the build script.
	 */
	public void run() throws CoreException {
		run((IProgressMonitor) null);
	}

	/**
	 * Invokes the building of a project object and executes a build using either a given
	 * target or the default target. This method is called when running Eclipse headless
	 * and specifying <code>org.eclipse.ant.core.antRunner</code> as the application.
	 *
	 * @param argArray the command line arguments
	 * @exception Exception if a problem occurred during the script execution
	 */
	public Object run(Object argArray) throws Exception {
		// Add debug information if necessary - fix for bug 5672.
		// Since the platform parses the -debug command line arg
		// and removes it from the args passed to the applications,
		// we have to check if Eclipse is in debug mode in order to
		// forward the -debug argument to Ant.
		if (BootLoader.inDebugMode()) {
			String[] args = (String[]) argArray;
			String[] newArgs = new String[args.length + 1];
			for (int i = 0; i < args.length; i++) {
				newArgs[i] = args[i];
			}
			newArgs[args.length] = "-debug"; //$NON-NLS-1$
			argArray = newArgs;
		}
		ClassLoader loader = getClassLoader();
		Class classInternalAntRunner = loader.loadClass("org.eclipse.ant.internal.core.ant.InternalAntRunner"); //$NON-NLS-1$
		Object runner = classInternalAntRunner.newInstance();
		Method run = classInternalAntRunner.getMethod("run", new Class[] { Object.class }); //$NON-NLS-1$
		run.invoke(runner, new Object[] { argArray });
		return null;
	}
}