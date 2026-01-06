/*
 * Copyright (C) 2012-2026 Frode Randers
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

import org.gautelis.vopn.lang.Stacktrace;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.zip.CRC32;

/**
 * InputStream that tracks multiple message digests and optional CRC32.
 */
public class MultiDigestInputStream extends FilterInputStream {

    private final static MessageDigest[] DIGEST_ARRAY_TEMPLATE = new MessageDigest[] {};

    private CRC32 crc32 = null;
    private MessageDigest[] digests = null;

    private long size = 0L;

    /**
     * Creates a stream that computes CRC32, MD5, SHA-1, and SHA-512 by default.
     *
     * @param is input stream to wrap
     */
    public MultiDigestInputStream(InputStream is) {
        super(is);

        // This is just a silly default, we will calculate the digest using several
        // algorithms
        crc32 = new CRC32();

        List<MessageDigest> digestList = new Vector<MessageDigest>();
        try {
            digestList.add(MessageDigest.getInstance("MD5"));
            digestList.add(MessageDigest.getInstance("SHA-1"));
            digestList.add(MessageDigest.getInstance("SHA-512"));

        } catch (NoSuchAlgorithmException ignore) {}

        digests = digestList.toArray(new MessageDigest[digestList.size()]); // may be empty if exception
    }

    /**
     * This is the preferred constructor
     * <p>
     * @param algorithms digest algorithm names (e.g. SHA-256, CRC32)
     * @param is input stream to wrap
     */
    public MultiDigestInputStream(String[] algorithms, InputStream is) {
        super(is);
        List<MessageDigest> digestList = new Vector<MessageDigest>();
        for (String algorithm : algorithms) {
            if (algorithm.equalsIgnoreCase("CRC32") || algorithm.equalsIgnoreCase("CRC-32")) {
                crc32 = new CRC32();
            } else {
                try {
                    digestList.add(MessageDigest.getInstance(algorithm));
                } catch (NoSuchAlgorithmException ignore) {
                }
            }
        }
        digests = digestList.toArray(new MessageDigest[digestList.size()]); // may be empty if exception
    }


    /**
     * Returns the computed digests keyed by algorithm name.
     *
     * @return map of algorithm name to digest bytes
     */
    public Map<String,byte[]> getDigests() {
        Map<String, byte[]> map = new HashMap<String, byte[]>();

        // Handle CRC32
        if (null != crc32) {
            byte[] b = new byte[8];
            ByteBuffer buf = ByteBuffer.wrap(b);
            buf.putLong(crc32.getValue());
            map.put("CRC32", b);
        }

        // Handle the rest
        for (MessageDigest digest : digests) {
            map.put(digest.getAlgorithm(), digest.digest());
        }

        return map;
    }

    /**
     * Returns the number of bytes read so far.
     *
     * @return byte count
     */
    public long getSize() {
        return size;
    }

    /**
     * Mark/reset is not supported to keep digest state consistent.
     *
     * @return {@code false}
     */
    @Override
    public boolean markSupported() {
        return false; // Very important!!!
    }

    /**
     * Reads a single byte and updates digest state.
     *
     * @return byte value or -1 on EOF
     * @throws IOException if reading fails
     */
    @Override
    public int read() throws IOException {
        int b = super.read();
        ++size;

        if (b >= 0) {
            // Update digests
            if (null != crc32) {
                try {
                    crc32.update(b);
                }
                catch (ArrayIndexOutOfBoundsException aioob) {
                    Throwable baseCause = Stacktrace.getBaseCause(aioob);
                    String info = "Failed to update CRC32 digest: " + baseCause.getMessage();
                    throw new IOException(info, aioob);
                }
            }
            for (MessageDigest digest : digests) {
                digest.update((byte) b);
            }
        }

        return b;
    }

    /**
     * Reads bytes into a buffer and updates digest state.
     *
     * @param bytes destination buffer
     * @return number of bytes read or -1 on EOF
     * @throws IOException if reading fails
     */
    @Override
    public int read(byte[] bytes) throws IOException {
        int actualLength = super.read(bytes);
        size += actualLength;

        if (actualLength > 0) {
            // Update digests
            if (null != crc32) {
                try {
                    crc32.update(bytes, /* offset */ 0, actualLength);
                }
                catch (ArrayIndexOutOfBoundsException aioob) {
                    Throwable baseCause = Stacktrace.getBaseCause(aioob);
                    String info = "Failed to update CRC32 digest: " + baseCause.getMessage();
                    throw new IOException(info, aioob);
                }
            }
            for (MessageDigest digest : digests) {
                digest.update(bytes, /* offset */ 0, actualLength);
            }
        }

        return actualLength;
    }

    /**
     * Reads bytes into a buffer range and updates digest state.
     *
     * @param bytes destination buffer
     * @param off offset in the buffer
     * @param len max number of bytes to read
     * @return number of bytes read or -1 on EOF
     * @throws IOException if reading fails
     */
    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        int actualLength = super.read(bytes, off, len);
        size += actualLength;

        if (actualLength > 0) {
            // Update digests
            if (null != crc32) {
                try {
                    crc32.update(bytes, off, actualLength);
                }
                catch (ArrayIndexOutOfBoundsException aioob) {
                    Throwable baseCause = Stacktrace.getBaseCause(aioob);
                    String info = "Failed to update CRC32 digest: " + baseCause.getMessage();
                    throw new IOException(info, aioob);
                }
            }
            for (MessageDigest digest : digests) {
                digest.update(bytes, off, actualLength);
            }
        }

        return actualLength;
    }
}
