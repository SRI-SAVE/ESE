/*
 * Copyright 2016 SRI International
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sri.pal.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Will Haines (haines@ai.sri.com)
 *
 *         Launches a JavaFX process, piping output appropriately and allowing
 *         listeners for when the process ends.
 */
public class JFXProcessLauncher extends Thread implements JFXProcessController {

	private static final Logger log = LoggerFactory
			.getLogger(JFXProcessLauncher.class);

	private Process process;

	private ProcessBuilder builder;

	private List<JFXProcessListener> listeners = new ArrayList<JFXProcessListener>();

	private InputStream inStream;

	private PrintStream outStream;

	private volatile Thread redirect = new Thread(new Runnable() {
		/*
                 * (non-Javadoc)
                 *
                 * @see java.lang.Runnable#run()
                 */
		@Override
		public void run() {
			try {
				byte[] buffer = new byte[1024];
				while (true) {
					if (inStream.available() <= 0) {
						if (redirect == null) {
							log.debug("Redirect thread terminated.");
							return;
						}
						try {
							Thread.sleep(250);
						} catch (final InterruptedException e) {
							// Intentionally left blank
						}
					} else {
						outStream.write(buffer, 0, inStream.read(buffer));
					}
				}
			} catch (final IOException e) {
				// Intentionally left blank
			}
		}
	});

	/**
         * Create a new {@link JFXProcessLauncher}
         *
         * @param outStream
         *            an output stream to which to redirect output
         * @param jarPath
     *            the path to the jar to execute
         * @param args
         *            the arguments to pass to JavaFX, starting with the jar to
         *            execute
         */
	public JFXProcessLauncher(final PrintStream outStream, final File jarPath, final String... args) {
		final String[] argsPlus = new String[args.length + 3];
		argsPlus[0] = "java";
		argsPlus[1] = "-jar";
		argsPlus[2] = jarPath.getAbsolutePath();
		for (int i = 0; i < args.length; i++) {
			argsPlus[i + 3] = args[i].replaceAll("\"", "");
		}
		log.debug("JavaFX args: {}", Arrays.toString(argsPlus));
		final ProcessBuilder builder = new ProcessBuilder(argsPlus);
		builder.redirectErrorStream(true);
		this.builder = builder;
		this.outStream = outStream;
	}

	/**
         * Get the process this launcher launched.
         *
         * @return the process that it is watched by this detector.
         */
	public Process getProcess() {
		return process;
	}

	/**
         * Start the process.
         */
	public void startProcess() {
		this.start();
	}

	/*
         * (non-Javadoc)
         *
         * @see com.sri.pal.common.JFXProcessController#endProcess()
         */
	@Override
	public void endProcess() {
		// TODO: Handle process ending more gracefully
		process.destroy();
	}

	/*
         * Launch the process, start piping the output, and wait for completion.
         *
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
	@Override
	public void run() {
		try {
			// Start process
			process = builder.start();

			// Redirect console
			inStream = process.getInputStream();
			redirect.start();

			// Wait for the process to end
			process.waitFor();

			// Turn of the redirect and notify listeners that the process ended
			redirect = null;
			for (final JFXProcessListener listener : listeners) {
				listener.processFinished(process);
			}

		} catch (final InterruptedException e) {
			// Intentionally left blank
		} catch (final Exception e) {
			log.error("Failed to launch JavaFX process:", e);
			throw new JFXProcessLauncherException(e);
		}
	}

	/**
         * Adds a process listener.
         *
         * @param listener
         *            the listener to be added
         */
	public void addProcessListener(final JFXProcessListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	/**
         * Removes a process listener.
         *
         * @param listener
         *            the listener to be removed
         */
	public void removeProcessListener(final JFXProcessListener listener) {
		listeners.remove(listener);
	}

    /**
     * Find a jar that is on the classpath. Recursively expand Class-Path
     * attributes of found jar files.
     *
     * TODO: Return a File instead of a String.
     *
     * @param jarName
     *            the jar to look up
     * @return the full path to the jar, or null if it can't be found
     */
    public static File findJarPath(final String jarName) {
        final File cwd = new File(System.getProperty("user.dir"));
        final String[] classpath = System.getProperty("java.class.path").split(
                File.pathSeparator);
        final List<File> fullClasspath = new ArrayList<File>();
        expandClasspath(fullClasspath, cwd, classpath);

        File result = null;
        for(final File cpElement : fullClasspath) {
            if(cpElement.getName().contains(jarName)) {
                result = cpElement;
                break;
            }
        }
        log.debug("Found jar: {}", result);
        if(result == null) {
            return null;
        } else {
        return new File(result.getAbsolutePath());
        }
    }

    /**
     * Expand classPath relative to parentDir. If a jar file contains a
     * Class-Path attribute, expand that relative to the jar's directory. Avoid
     * infinite loops if the jar refers to itself or two jars refer to each
     * other.
     *
     * @param expandedCP
     *            the destination list for the expanded classpath
     */
    private static void expandClasspath(List<File> expandedCP,
                                        final File parentDir,
                                        final String[] classPath) {
        for (final String cpElementStr : classPath) {
            // Figure out if it's an absolute path.
            File cpElement = new File(cpElementStr);
            if (!cpElement.isAbsolute()) {
                cpElement = new File(parentDir, cpElementStr);
            }

            // Add it to the list, if we don't already have it.
            if (!expandedCP.contains(cpElement)) {
                expandedCP.add(cpElement);

                // If it's a jar file, and it has a Class-Path attribute, expand
                // that. Ignore any errors.
                try (JarFile jf = new JarFile(cpElement)) {
                    final Manifest manifest = jf.getManifest();
                    final String subClasspath = manifest.getMainAttributes()
                            .getValue("Class-Path");
                    final File jarParentDir = cpElement.getParentFile();
                    expandClasspath(expandedCP, jarParentDir,
                            subClasspath.split(" "));
                } catch (Exception e) {
                    log.info("Couldn't read manifest of " + cpElement, e);
                }
            }
        }
    }

	/**
         * @author Will Haines (haines@ai.sri.com)
         *
         *         An exception class for {@link JFXProcessLauncher}.
         */
	public static class JFXProcessLauncherException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		/**
                 * Create a new {@link JFXProcessLauncherException}.
                 *
                 * @param e
                 *            the internal exception
                 */
		public JFXProcessLauncherException(final Throwable e) {
			super(e);
		}
	}
}
