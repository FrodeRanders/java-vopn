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
package  org.gautelis.vopn.io;

import javax.activation.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/*
 * Implements a data source that caches (possibly large) buffers of data in memory.
 * <p>
 * Is assumed to be used together with a javax.activation.DataHandler (like this)
 * <pre>
 *     StringBuffer buf = new StringBuffer();
 *     buf.append("123456...");
 *     StringBufferDataSource ds = new StringBufferDataSource("data", "binary/octet-stream", buf);
 *     DataHandler dh = new javax.activation.DataHandler(ds);
 * </pre>
 * <p>
 * Created by Frode Randers at 2013-01-23 00:33
 */
public class StringBufferDataSource implements DataSource {

    private String name;
    private String mimeType;
    private StringBuffer buffer;

    public StringBufferDataSource(String name, String mimeType, StringBuffer buffer) {
        this.name = name;
        this.mimeType = mimeType;
        this.buffer = buffer;
    }

    public String getName() {
            return name;
    }

    public String getContentType() {
        return mimeType;
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(buffer.toString().getBytes("UTF-8"));
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
}
