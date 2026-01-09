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

import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class DeserializerTest {

    @Test
    public void testXmlStringToObject() throws Exception {
        String xml = "<person><name>Linus</name><age>28</age></person>";

        Object obj = Deserializer.xml2Obj(Person.class, xml);
        Person person = (Person) obj;

        assertEquals("Linus", person.getName());
        assertEquals(28, person.getAge());
    }

    @Test
    public void testXmlReaderToObject() throws Exception {
        String xml = "<person><name>Grace</name><age>35</age></person>";

        Object obj = Deserializer.xml2Obj(Person.class, new StringReader(xml));
        Person person = (Person) obj;

        assertEquals("Grace", person.getName());
        assertEquals(35, person.getAge());
    }

    @Test
    public void testXmlNodeToObject() throws Exception {
        Person person = new Person("Joan", 31);
        Document document = Serializer.obj2Doc(Person.class, person);

        Object obj = Deserializer.xml2Obj(Person.class, document.getDocumentElement());
        Person result = (Person) obj;

        assertEquals("Joan", result.getName());
        assertEquals(31, result.getAge());
    }
}
