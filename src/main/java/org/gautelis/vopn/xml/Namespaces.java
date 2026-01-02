/*
 * Copyright (C) 2012-2025 Frode Randers
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
package  org.gautelis.vopn.xml;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import java.util.*;

/**
 * Namespace registry for use with Axiom elements and XPath.
 */
public class Namespaces {
    private final Map<String, OMNamespace> namespaces = new HashMap<>();

    /**
     * Creates an empty namespace registry.
     */
    public Namespaces() {
        // No default namespaces defined yet...
    }

    /**
     * Creates a registry from an iterator of namespaces.
     *
     * @param nsit namespace iterator
     */
    public Namespaces(Iterator<OMNamespace> nsit) {
        while (nsit.hasNext()) {
            OMNamespace ns = nsit.next();
            defineNamespace(ns.getNamespaceURI(), ns.getPrefix());
        }
    }

    /**
     * Creates a registry from a prefix-to-URI map.
     *
     * @param namespaces prefix-to-URI mappings
     */
    public Namespaces(Map<String,String> namespaces) {
        for (String prefix : namespaces.keySet()) {
            defineNamespace(namespaces.get(prefix), prefix);
        }
    }

    /**
     * Defines and registers a namespace.
     *
     * @param uri namespace URI
     * @param prefix namespace prefix
     * @return created namespace
     */
    public OMNamespace defineNamespace(String uri, String prefix) {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        OMNamespace namespace = factory.createOMNamespace(uri, prefix);
        namespaces.put(prefix, namespace);
        return namespace;
    }

    /**
     * Declares all namespaces on an Axiom element.
     *
     * @param element element to update
     */
    public void applyNamespacesOn(OMElement element) {
        for (OMNamespace ns : namespaces.values()) {
            element.declareNamespace(ns);
        }
    }

    /**
     * Adds all namespaces to an XPath expression.
     *
     * @param xpath XPath expression to update
     * @throws JaxenException if namespace registration fails
     */
    public void applyNamespacesOn(AXIOMXPath xpath) throws JaxenException {
        for (OMNamespace ns : namespaces.values()) {
            xpath.addNamespace(ns.getPrefix(), ns.getNamespaceURI());
        }
    }

    /**
     * Returns the namespace registered for a prefix.
     *
     * @param prefix namespace prefix
     * @return namespace or {@code null}
     */
    public OMNamespace get(String prefix) {
        return namespaces.get(prefix);
    }

    /**
     * Returns an unmodifiable view of all registered namespaces.
     *
     * @return namespace map
     */
    public Map<String, OMNamespace> getNamespaces() {
        return Collections.unmodifiableMap(namespaces);
    }
}
