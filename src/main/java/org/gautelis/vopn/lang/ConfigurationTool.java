/*
 * Copyright (C) 2011-2025 Frode Randers
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
package  org.gautelis.vopn.lang;

import org.gautelis.vopn.db.Database;
import org.gautelis.vopn.io.Closer;
import org.gautelis.vopn.lang.configuration.GeneralizedConfigurationInvocationHandler;
import org.gautelis.vopn.lang.configuration.PropertiesConfigurationInvocationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.*;

/**
 * Implements an abstraction over properties-files (or other configuration schemas) and
 * allows <strong>type safe</strong> access to configuration parameters - even though the
 * Properties-class only deals with Strings.
 * <p>
 * Use as follows:
 * <pre>
 * public class MyClass {
 *     private interface MyConfiguration {
 *         &#64;Configurable(property = "key", value = "default value, if needed")
 *         String aString();
 *
 *         &#64;Configurable(property = "some other key")
 *         int anInt();
 *     }
 *
 *     public MyClass() {
 *         Properties props = ConfigurationTool.loadFromResource(MyClass.class, "configuration.xml");
 *         MyConfiguration config = ConfigurationTool.bindProperties(MyConfiguration.class, props);
 *
 *         // Now you may access the configuration in a type safe manner
 *         String myString = config.aString();
 *         int myInt = config.anInt();
 *
 *         // Also, if no value was provided in the properties file, the default "value" will be
 *         // returned. This works even for integers, booleans etc.
 *     }
 * }
 * </pre>
 * <p>
 * Other means to load the configuration is provided by ConfigurationTool, such as:
 * <pre>
 *     Properties props1 = ConfigurationTool.load("/path/to/my/configuration.properties");
 *     Properties props2 = ConfigurationTool.load("/path/to/my/configuration.xml");
 *
 *     File file = new File("/some/other/path/to/a/configuration.xml");
 *     Properties props3 = ConfigurationTool.load(file);
 * </pre>
 * <p>
 * Note that ConfigurationTool will handle both classic .properties files as well as XML-files using the
 * properties DTD:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd"&gt;
 *
 * &lt;properties&gt;
 *     &lt;comment&gt;
 *         This file is used for testing ConfigurationTool and @Configurable
 *     &lt;/comment&gt;
 *     &lt;entry key="key"&gt;
 *         just some string value
 *     &lt;/entry&gt;
 *     &lt;entry key="some other key"&gt;
 *         42
 *     &lt;/entry&gt;
 * &lt;/properties&gt;
 * </pre>
 * <p>
 * Created by Frode Randers at 2011-11-04 14:14
 */
public class ConfigurationTool {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationTool.class);

    private static final Class<?>[] C = {};

    /**
     * [Default] path to the environment location.
     * <p>
     * The value is
     * <I>"java:comp/env"</I>
     */
    public static final String JNDI_ENVIRONMENT = "java:comp/env";


    public interface ConfigurationResolver {
        /**
         * Resolves a configuration value by name.
         *
         * @param name property key
         * @return resolved value or {@code null} if not found
         */
        Object resolve(String name);
    }

    private ConfigurationTool() {} // may not be instantiated by user

    private static final Object lock = new Object();
    private static Properties globalProperties = null; // if provided

    private static void accumulateIfcs(Class<?> clazz, Collection<Class<?>> acc) {
        if (clazz != null && clazz.isInterface()) {
            acc.add(clazz);

            Class<?>[] superIfc = clazz.getInterfaces();
            for (Class<?> ifc : superIfc) {
                accumulateIfcs(ifc, acc);
            }
        }
    }

    /**
     * Binds a configuration interface to default values.
     *
     * @param clazz configuration interface
     * @param defaultValues default values map
     * @return proxy implementation of the configuration interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T bind(Class<T> clazz, Map<String, Object> defaultValues) {
        ArrayList<Class<?>> interfaces = new ArrayList<>();
        accumulateIfcs(clazz, interfaces);

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                interfaces.toArray(C),
                new GeneralizedConfigurationInvocationHandler(defaultValues)
        );
    }

    /**
     * Binds a configuration interface to default values and resolvers.
     *
     * @param clazz configuration interface
     * @param defaultValues default values map
     * @param resolvers configuration resolvers to consult
     * @return proxy implementation of the configuration interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T bind(Class<T> clazz, Map<String, Object> defaultValues, Collection<ConfigurationResolver> resolvers) {
        ArrayList<Class<?>> interfaces = new ArrayList<>();
        accumulateIfcs(clazz, interfaces);

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                interfaces.toArray(C),
                new GeneralizedConfigurationInvocationHandler(defaultValues, resolvers)
        );
    }


    /**
     * Binds a configuration interface to a properties object.
     *
     * @param clazz configuration interface
     * @param properties configuration properties
     * @return proxy implementation of the configuration interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T bindProperties(Class<T> clazz, Properties properties) {
        ArrayList<Class<?>> interfaces = new ArrayList<>();
        accumulateIfcs(clazz, interfaces);

        /*
        if (log.isTraceEnabled()) {
            log.trace("Binding properties to " + clazz.getCanonicalName());
            for (Class<?> c : interfaces.toArray(C)) {
                log.trace("ifc: " + c.getCanonicalName());
            }
        }
        */

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                interfaces.toArray(C),
                new PropertiesConfigurationInvocationHandler(properties)
        );
    }

    /**
     * Binds a configuration interface to properties and resolvers.
     *
     * @param clazz configuration interface
     * @param properties configuration properties
     * @param resolvers configuration resolvers to consult
     * @return proxy implementation of the configuration interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T bindProperties(Class<T> clazz, Properties properties, Collection<ConfigurationResolver> resolvers) {
        ArrayList<Class<?>> interfaces = new ArrayList<>();
        accumulateIfcs(clazz, interfaces);

        return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                interfaces.toArray(C),
                new PropertiesConfigurationInvocationHandler(properties, resolvers)
        );
    }

    /**
     * Binds a configuration interface to the globally registered properties.
     *
     * @param clazz configuration interface
     * @return proxy implementation of the configuration interface
     */
    @SuppressWarnings("unchecked")
    public static <T> T bindProperties(Class<T> clazz) {
        synchronized(lock) {
            if (null == globalProperties) {
                String info = "Tried to bind the configuration of " + clazz.getName();
                info += " to a non-existing *global* configuration!";
                throw new RuntimeException(info);
            }

            ArrayList<Class<?>> interfaces = new ArrayList<>();
            accumulateIfcs(clazz, interfaces);

            return (T) Proxy.newProxyInstance(
                clazz.getClassLoader(),
                interfaces.toArray(C),
                new PropertiesConfigurationInvocationHandler(globalProperties)
            );
        }
    }

    private static boolean isXML(File file) {
        return file.getName().toLowerCase().endsWith(".xml");
    }

    private static boolean isXML(String path) {
        return path.toLowerCase().endsWith(".xml");
    }

    public static Properties load(InputStream is, boolean isXML) throws IOException {
        Properties properties = new Properties();
        if (isXML) {
            properties.loadFromXML(is);
        } else {
            properties.load(is);
        }
        return properties;
    }

    /**
     * Loads properties from a file, supporting both .properties and .xml.
     *
     * @param propertiesFile properties file
     * @return loaded properties
     * @throws IOException if the file cannot be read
     */
    public static Properties load(File propertiesFile) throws IOException {
        InputStream is = null;
        try {
            is = Files.newInputStream(propertiesFile.toPath());
            return load(is, isXML(propertiesFile));
        } finally {
            Closer.close(is);
        }
    }

    /**
     * Loads properties from a file path.
     *
     * @param path properties file path
     * @return loaded properties
     * @throws IOException if the file cannot be read
     */
    public static Properties load(String path) throws IOException {
        return load(new File(path));
    }

    /**
     * Loads properties from a classpath resource.
     *
     * @param clazz class used to locate the resource
     * @param resourceName resource path
     * @return loaded properties
     * @throws IOException if the resource cannot be read
     */
    public static Properties loadFromResource(Class<?> clazz, String resourceName) throws IOException {
        InputStream is = null;
        try {
            is = clazz.getResourceAsStream(resourceName);
            return load(is, isXML(resourceName));
        } finally {
            Closer.close(is);
        }
    }

    /**
     * Registers a global properties object used by {@link #bindProperties(Class)}.
     *
     * @param properties properties to register
     */
    public static void useGlobalConfiguration(Properties properties) {
        synchronized(lock) {
            globalProperties = properties;
        }
    }

    /**
     * Looks up (configuration) resources in JNDI or system environment.
     *
     * @param resourceName property name to look up
     * @return resolved value or {@code null} if not found
     */
    public static String lookup(String resourceName) {
        String resource = null;

        // 1. Check in the JNDI tree
        Context ctx = null;
        try {
            ctx = (Context) new InitialContext().lookup(JNDI_ENVIRONMENT);
            resource = (String) ctx.lookup(resourceName);
        }
        catch (NamingException ignore) {}

        // 2. Check among system properties
        if (null == resource || resource.isEmpty()) {
            System.out.println("Resource \"" + resourceName + "\" not published through JNDI...");
            try {
                resource = System.getProperty(resourceName);
            }
            catch (Exception ignore) {}
        }

        if (null == resource || resource.isEmpty()) {
            System.out.println("Resource \"" + resourceName + "\" not published through system properties...");
        }
        return resource;
    }
}
