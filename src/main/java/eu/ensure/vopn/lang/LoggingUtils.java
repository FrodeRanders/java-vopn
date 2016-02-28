/*
 * Copyright (C) 2012-2016 Frode Randers
 * All rights reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The research leading to the implementation of this software package
 * has received funding from the European Community´s Seventh Framework
 * Programme (FP7/2007-2013) under grant agreement n° 270000.
 *
 * Frode Randers was at the time of creation of this software module
 * employed as a doctoral student by Luleå University of Technology
 * and remains the copyright holder of this material due to the
 * Teachers Exemption expressed in Swedish law (LAU 1949:345)
 */
package  eu.ensure.vopn.lang;

import eu.ensure.vopn.io.FileIO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

/*---------------------------------------------------------------------------------------------
 * According to http://stackoverflow.com/a/18690783
 *
 *  Log4j2 can autoconfigure itself if you do not provide a config file.
 *
 *  Configuration of Log4j 2 can be accomplished in 1 of 4 ways:
 *
 *  1) Through a configuration file written in XML or JSON.
 *  2) Programmatically, by creating a ConfigurationFactory and Configuration implementation.
 *  3) Programmatically, by calling the APIs exposed in the Configuration interface to add
 *     components to the default configuration.
 *  4) Programmatically, by calling methods on the internal Logger class.
 *
 * See http://logging.apache.org/log4j/2.x/manual/configuration.html#AutomaticConfiguration
 *--------------------------------------------------------------------------------------------*/
public class LoggingUtils {
    public static final String DEFAULT_CONFIGURATION_FILE_NAME = "log4j2.xml";

    // Shared among different class loaders
    private static final Object lock = new Object();

    public interface Configuration {
        @Configurable(property = "log-configuration-file", value = DEFAULT_CONFIGURATION_FILE_NAME)
        String file();

        @Configurable(property = "log-configuration-check-interval-in-seconds", value = /* 2 minutes */ "" + 2 * 60)
        int delay();
    }

    private enum ConfigurationOption {
        PULL_FROM_FILE_ON_DISK,
        PULL_FROM_RESOURCES,
        CREATE_FROM_TEMPLATE
    }

    private static ConfigurationSource getConfigurationSource(Class clazz, String resourceName, PrintWriter pw) {

        // --------- Determine name of log configuration file ---------
        Collection<ConfigurationTool.ConfigurationResolver> resolvers =
                new ArrayList<ConfigurationTool.ConfigurationResolver>();

        // <<<"Check in the JNDI tree">>>-resolver
        resolvers.add(new JndiConfigurationResolver());

        // <<<"Check among system properties">>>-resolver
        resolvers.add(new SystemPropertiesConfigurationResolver());

        // Wire up the configuration
        Properties defaults = new Properties();
        Configuration configuration = ConfigurationTool.bindProperties(Configuration.class, defaults, resolvers);

        // --------- Determine location of configuration file and strategy ---------
        String fileName = configuration.file();
        File configFile = new File(fileName);

        ConfigurationOption strategy;

        if (configFile.exists() && configFile.canRead()) {
            // Configuration file exists in file system - use it.
            strategy = ConfigurationOption.PULL_FROM_FILE_ON_DISK;
        } else {
            // Configuration file does not exist in file system (yet?)
            if (configFile.isAbsolute()) {
                strategy = ConfigurationOption.CREATE_FROM_TEMPLATE;
            } else {
                // If configuration.file() returns a single name without File.separator ('/')
                // then we will just try to pull it from clazz' resources.
                // If configuration.file() contains separators (even if relative) but does not exist
                // on disk, then we will try to write to it using the template from clazz' resources.
                if (fileName.contains(File.separator)) {
                    strategy = ConfigurationOption.CREATE_FROM_TEMPLATE;
                } else {
                    strategy = ConfigurationOption.PULL_FROM_RESOURCES;
                }
            }
        }

        // Changes as per http://stackoverflow.com/questions/21083834/load-log4j2-configuration-file-programmatically
        ConfigurationSource source = null;
        try {
            InputStream is = null;
            try {
                switch (strategy) {
                    case CREATE_FROM_TEMPLATE:
                        try (InputStream sourceConfig = clazz.getResourceAsStream(resourceName)) {
                            configFile = FileIO.writeToFile(sourceConfig, configFile);
                            is = new FileInputStream(configFile);

                            // We need to consume the whole stream right away, before we
                            // close the configuration input stream
                            source = new ConfigurationSource(new ByteArrayInputStream(consumeInputStream(is)), configFile);

                            pw.println("Pulling log configuration from file: " + configFile.getAbsolutePath());
                            break;

                        } catch (Exception e) {
                            String info = "Failed to create log configuration file from internal template: ";
                            info += e.getMessage();
                            info += " - [falling back on default configuration]";
                            pw.println(info);

                            //----------------------------------------------------------------------
                            // OBSERVE!
                            //   Upon failure, we are doing a fall-through to the next clause...
                            //----------------------------------------------------------------------
                        }

                    case PULL_FROM_RESOURCES:
                        is = clazz.getResourceAsStream(resourceName);

                        // We need to consume the whole stream right away, before we
                        // close the configuration input stream
                        source = new ConfigurationSource(new ByteArrayInputStream(consumeInputStream(is)));

                        pw.println("Pulling log configuration from resources (default): " + clazz.getName() + "#" + fileName);
                        break;

                    case PULL_FROM_FILE_ON_DISK:
                        is = new FileInputStream(configFile);

                        // We need to consume the whole stream right away, before we
                        // close the configuration input stream
                        source = new ConfigurationSource(new ByteArrayInputStream(consumeInputStream(is)), configFile);

                        pw.println("Pulling log configuration from file: " + configFile.getAbsolutePath());
                        break;
                }
            } catch (IOException ioe) {
                String info = "Could not load logging configuration from resource \"" + resourceName + "\" ";
                info += "for class " + clazz.getName() + ": ";
                Throwable t = Stacktrace.getBaseCause(ioe);
                info += t.getMessage();

                pw.println(info);
                throw new RuntimeException(info, t);

            } finally {
                if (null != is) is.close();
            }
        } catch (Exception e) {
            String info = "Could not setup xml-reader for logging configuration: ";
            Throwable t = Stacktrace.getBaseCause(e);
            info += t.getMessage();

            pw.println(info);
            throw new RuntimeException(info, t);
        }
        return source;
    }

    /**
     * Initializes (and sets up) logging.
     * <p/>
     * These are class loader hierarchies as seen from different containers
     * --------------------------------------------------------------------------------------
     *     Servlet container
     * --------------------------------------------------------------------------------------
     *   1: org.apache.catalina.loader.WebappClassLoader@4465BAF
     *   2: java.net.URLClassLoader@38AF3868
     *   3: sun.misc.Launcher.AppClassLoader@764C12B6
     *   4: sun.misc.Launcher.ExtClassLoader@5F2050F6
     *
     * --------------------------------------------------------------------------------------
     *     Axis2 service container
     * --------------------------------------------------------------------------------------
     *   1: org.apache.axis2.deployment.DeploymentClassLoader@582FEEFE
     *   2: org.apache.axis2.classloader.JarFileClassLoader@557D9535
     *   3: sun.misc.Launcher.AppClassLoader@764C12B6
     *   4: sun.misc.Launcher.ExtClassLoader@5F2050F6
     *
     * Class loader 2 is parent of 1 and so forth.
     *
     * If we want to share a common logger context between these containers, we need to
     * find a common ground -- a common ancestor class loader. This is the reason we
     * take the third parameter, which basically is the class name of the shared
     * class loader.
     *
     * We will search up the class loader hierarchy untill we find a common ancestor, into
     * which we place our common LoggerContext.
     *
     * <p/>
     * @param clazz - the class requesting a Logger
     * @param resourceName - name of parameter holding name of Log4j2 configuration
     * @param classLoaderType - name of common class loader ancestor (or null if we don't care)
     * @return
     */
    public static Logger setupLoggingFor(Class clazz, String resourceName, String classLoaderType, Writer reporter) {
        Logger log = null;

        boolean hasSpecifiedClassLoader = null != classLoaderType && classLoaderType.length() > 0;

        ClassLoader classLoader;
        if (hasSpecifiedClassLoader) {
            classLoader = getTypedClassLoader(clazz.getClassLoader(), classLoaderType);
        } else {
            classLoader = clazz.getClassLoader();
        }

        PrintWriter pw;
        if (null != reporter) {
            pw = new PrintWriter(reporter);
        } else {
            pw = new PrintWriter(System.out);
        }

        pw.print("[Classloader: " + classLoader.getClass().getCanonicalName());
        pw.println("@" + String.format("%X", classLoader.hashCode()) + "]");
        pw.flush();

        synchronized (lock) {
            org.apache.logging.log4j.spi.LoggerContext targetContext =
                    LogManager.getContext(classLoader, !hasSpecifiedClassLoader);

            if (null != targetContext) {
                pw.print("[LoggerContext: " + targetContext.getClass().getCanonicalName());
                pw.println("@" + String.format("%X", targetContext.hashCode()) + "]");
                pw.flush();

                org.apache.logging.log4j.core.LoggerContext context =
                        (org.apache.logging.log4j.core.LoggerContext) targetContext;

                if (context.isStarted()) {
                    org.apache.logging.log4j.core.config.Configuration targetConfiguration =
                            context.getConfiguration();

                    pw.print("Log4j2 is running a configuration (");
                    pw.print(targetConfiguration.getName());
                    pw.print(") with appenders:");

                    Map<String, Appender> appenders = targetConfiguration.getAppenders();
                    if (null != appenders) {
                        for (Appender appender : appenders.values()) {
                            pw.print(" " + appender.getName());
                            appender.stop();
                        }
                    }
                    pw.println();
                    pw.flush();

                    targetConfiguration.stop();
                    context.stop();
                } else {
                    pw.println("No Log4j2 configuration yet running");
                }

                // Re-instantiate logging
                ConfigurationSource source = getConfigurationSource(clazz, resourceName, pw);
                context = Configurator.initialize(classLoader, source);
                context.start();

                log = context.getLogger(clazz.getName());

                String info = "Logging initiated (by " + clazz.getCanonicalName() + ")...";
                pw.println(info);
                pw.flush();

                log.info(info);
            }
        }

        return log;
    }

    public static Logger setupLoggingFor(Class clazz, String resourceName, String classLoaderType) {
        return setupLoggingFor(clazz, resourceName, classLoaderType, null);
    }

    public static Logger setupLoggingFor(Class clazz, String resourceName) {
        return setupLoggingFor(clazz, resourceName, null, null);
    }

    private static byte[] consumeInputStream(java.io.InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[0x4000];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    private static ClassLoader getTypedClassLoader(ClassLoader classLoader, String requestedClassName) {
        String className = classLoader.getClass().getCanonicalName();
        if (requestedClassName.equals(className)) {
            return classLoader;
        }

        ClassLoader parent = classLoader.getParent();
        if (null != parent) {
            return getTypedClassLoader(parent, requestedClassName);
        }

        return null;
    }
}
