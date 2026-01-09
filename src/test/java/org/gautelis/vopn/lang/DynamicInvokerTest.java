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
package org.gautelis.vopn.lang;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DynamicInvokerTest {

    @Test
    public void testCompileAndInvoke() throws Throwable {
        Path tempRoot = Files.createTempDirectory("dyninvoker");
        DynamicCompiler compiler = new DynamicCompiler(tempRoot.toFile());
        String className = "org.gautelis.vopn.lang.fixtures.DynamicInvokerSample";
        StringBuilder code = new StringBuilder();
        code.append("package org.gautelis.vopn.lang.fixtures;")
            .append("import java.nio.file.Files;")
            .append("import java.nio.file.Path;")
            .append("import java.nio.charset.StandardCharsets;")
            .append("public class DynamicInvokerSample {")
            .append("  public void writeMarker(String path, String content) throws Exception {")
            .append("    Files.writeString(Path.of(path), content, StandardCharsets.UTF_8);")
            .append("  }")
            .append("}");

        StringBuilder diagnose = new StringBuilder();
        boolean success = compiler.compile(className, code, diagnose);

        assertTrue("Compilation failed: " + diagnose, success);

        Path marker = tempRoot.resolve("marker.txt");
        DynamicInvoker invoker = new DynamicInvoker(compiler.getDirectory(), "sample");
        invoker.invoke(className, "writeMarker",
                new Object[]{marker.toString(), "ok"},
                new Class<?>[]{String.class, String.class});

        String content = Files.readString(marker, StandardCharsets.UTF_8);
        assertEquals("ok", content);
    }
}
