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
import org.apache.logging.log4j.status.StatusLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    /**
     * Initializes (and sets up) logging.
     * <p>
     * @param clazz
     * @param resourceName
     * @return
     */
    public static Logger setupLoggingFor(Class clazz, String resourceName) {

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
        }
        else {
            // Configuration file does not exist in file system (yet?)
            if (configFile.isAbsolute()) {
                strategy = ConfigurationOption.CREATE_FROM_TEMPLATE;
            }
            else {
                // If configuration.file() returns a single name without File.separator ('/')
                // then we will just try to pull it from clazz' resources.
                // If configuration.file() contains separators (even if relative) but does not exist
                // on disk, then we will try to write to it using the template from clazz' resources.
                if (fileName.contains(File.separator)) {
                    strategy = ConfigurationOption.CREATE_FROM_TEMPLATE;
                }
                else {
                    strategy = ConfigurationOption.PULL_FROM_RESOURCES;
                }
            }
        }

        // Changes as per http://stackoverflow.com/questions/21083834/load-log4j2-configuration-file-programmatically
        Logger log = null;
        try {
            ConfigurationSource source = null;
            InputStream config = null;
            try {
                switch (strategy) {
                    case CREATE_FROM_TEMPLATE:
                        try (InputStream sourceConfig = clazz.getResourceAsStream(resourceName)) {
                            configFile = FileIO.writeToFile(sourceConfig, configFile);
                            config = new FileInputStream(configFile);
                            source = new ConfigurationSource(config, configFile);

                            System.out.println("Pulling log configuration from file: " + configFile.getAbsolutePath());
                            break;

                        } catch (Exception e) {
                            String info = "Failed to create log configuration file from internal template: ";
                            info += e.getMessage();
                            info += " - [falling back on default configuration]";
                            System.out.println(info);

                            //----------------------------------------------------------------------
                            // OBSERVE!
                            //   Upon failure, we are doing a fall-through to the next clause...
                            //----------------------------------------------------------------------
                        }

                    case PULL_FROM_RESOURCES:
                        config = clazz.getResourceAsStream(resourceName);
                        source = new ConfigurationSource(config);

                        System.out.println("Pulling log configuration from resources (default): " + clazz.getName() + "#" + fileName);
                        break;

                    case PULL_FROM_FILE_ON_DISK:
                        config = new FileInputStream(configFile);
                        source = new ConfigurationSource(config, configFile);

                        System.out.println("Pulling log configuration from file: " + configFile.getAbsolutePath());
                        break;
                }

                synchronized (lock) {
                    // Load new and restart
                    Logger rootLogger = LogManager.getRootLogger();
                    if (null != rootLogger) {
                        // Need the core Logger in order to extract a LoggerContext
                        org.apache.logging.log4j.core.LoggerContext context =
                                ((org.apache.logging.log4j.core.Logger) rootLogger).getContext();

                        if (context.getState() == LifeCycle.State.STARTED) {
                            org.apache.logging.log4j.core.config.Configuration _configuration = context.getConfiguration();
                            System.out.print("Log4j2 is already running a configuration (");
                            System.out.print(_configuration.getName());
                            System.out.print(") with these appenders:");

                            Map<String, Appender> appenders = _configuration.getAppenders();
                            if (null != appenders) {
                                for (Appender appender : appenders.values()) {
                                    System.out.print(" " + appender.getName());
                                }
                            }
                            System.out.println();

                            context.stop();
                        }

                        context = Configurator.initialize(/* class loader */ null, source, context);
                        log = context.getLogger(clazz.getName());
                        String info = "Logging initiated (by " + clazz.getCanonicalName() + ")...";
                        System.out.println(info);
                    }
                }
            } catch (IOException ioe) {
                String info = "Could not load logging configuration from resource \"" + resourceName + "\" ";
                info += "for class " + clazz.getName() + ": ";
                Throwable t = Stacktrace.getBaseCause(ioe);
                info += t.getMessage();

                System.out.println(info);
                throw new RuntimeException(info, t);
            } finally {
                if (null != config) config.close();
            }
        } catch (Exception e) {
            String info = "Could not setup xml-reader for logging configuration: ";
            Throwable t = Stacktrace.getBaseCause(e);
            info += t.getMessage();

            System.out.println(info);
            e.printStackTrace();
            throw new RuntimeException(info, t);
        }
        return log;
    }
}
