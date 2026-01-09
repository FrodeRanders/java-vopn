/*
 * Copyright (C) 2026 Frode Randers
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
 */
package org.gautelis.vopn.xml;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.junit.Test;

import java.util.Map;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NamespacesAttributeXPathTest {

    @Test
    public void testAttributesWithNamespaces() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        Namespaces namespaces = new Namespaces();
        OMNamespace ns = namespaces.defineNamespace("urn:test", "t");

        OMElement root = factory.createOMElement("root", ns);
        OMElement child = factory.createOMElement("child", ns);
        child.addAttribute("plain", "value", null);
        child.addAttribute("id", "123", ns);
        root.addChild(child);

        Attribute attribute = new Attribute(namespaces);
        assertEquals("value", attribute.getValueFrom(child, "plain"));
        assertEquals("123", attribute.getValueFrom(child, "t", "id"));
        assertNull(attribute.getValueFrom(child, "missing", true));
    }

    @Test
    public void testXPathQueries() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        Namespaces namespaces = new Namespaces();
        OMNamespace ns = namespaces.defineNamespace("urn:test", "t");

        OMElement root = factory.createOMElement("root", ns);
        OMElement child = factory.createOMElement("child", ns);
        child.setText("value");
        root.addChild(child);

        XPath xpath = new XPath(namespaces);
        assertEquals("value", xpath.getTextFrom(child, "."));
        assertNull(xpath.getTextFrom(child, "missing", true));

        OMElement found = xpath.getElementFrom(root, "t:child");
        assertEquals("child", found.getLocalName());

        List<OMElement> elements = xpath.getElementsFrom(root, "t:child");
        assertEquals(1, elements.size());
        assertEquals("child", elements.get(0).getLocalName());

        List<OMElement> missing = xpath.getElementsFrom(root, "t:missing");
        assertTrue(missing.isEmpty());
    }

    @Test(expected = XmlException.class)
    public void testMissingNamespacedAttributeThrows() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        Namespaces namespaces = new Namespaces();
        OMElement element = factory.createOMElement("root", null);

        Attribute attribute = new Attribute(namespaces);
        attribute.getValueFrom(element, "p", "id");
    }

    @Test(expected = XmlException.class)
    public void testMissingAttributeThrows() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        Namespaces namespaces = new Namespaces();
        OMElement element = factory.createOMElement("root", null);

        Attribute attribute = new Attribute(namespaces);
        attribute.getValueFrom(element, "missing");
    }

    @Test(expected = XmlException.class)
    public void testMissingXPathElementThrows() throws Exception {
        OMFactory factory = OMAbstractFactory.getOMFactory();
        Namespaces namespaces = new Namespaces();
        OMNamespace ns = namespaces.defineNamespace("urn:test", "t");

        OMElement root = factory.createOMElement("root", ns);
        XPath xpath = new XPath(namespaces);

        xpath.getElementFrom(root, "t:missing");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNamespacesMapIsUnmodifiable() {
        Namespaces namespaces = new Namespaces(Map.of("t", "urn:test"));
        namespaces.getNamespaces().put("x", namespaces.defineNamespace("urn:other", "x"));
    }
}
