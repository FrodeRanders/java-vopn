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

import org.gautelis.vopn.xml.fixtures.Person;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SerializerTest {

    @Test
    public void testObj2XmlUtf8() throws Exception {
        Person person = new Person("Ada", 42);

        String xml = Serializer.obj2Xml(Person.class, person);

        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("<name>Ada</name>"));
        assertTrue(xml.contains("<age>42</age>"));
    }

    @Test
    public void testObj2Doc() throws Exception {
        Person person = new Person("Alan", 33);

        Document document = Serializer.obj2Doc(Person.class, person);

        assertEquals("person", document.getDocumentElement().getNodeName());
        assertEquals("Alan", document.getElementsByTagName("name").item(0).getTextContent());
        assertEquals("33", document.getElementsByTagName("age").item(0).getTextContent());
    }

    @Test
    public void testObj2XmlUtf16() throws Exception {
        Person person = new Person("Edsger", 37);

        String xml = Serializer.obj2XmlUtf16(Person.class, person);

        assertTrue(xml.contains("UTF-16"));
        assertTrue(xml.contains("<person>"));
        assertTrue(xml.contains("<name>Edsger</name>"));
    }
}
