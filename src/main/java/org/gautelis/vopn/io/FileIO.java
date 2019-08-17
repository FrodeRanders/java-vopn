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
package  org.gautelis.vopn.io;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Various handy file IO related functions.
 * <p>
 * Created by Frode Randers at 2012-12-18 01:09
 */
public class FileIO {

    /**
     * A nice one: http://thomaswabner.wordpress.com/2007/10/09/fast-stream-copy-using-javanio-channels/
     */
    public static void fastChannelCopy(final ReadableByteChannel src, final WritableByteChannel dest) throws IOException {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

        /*
         * If built with JDK 9 (and later) with target JDK 8, invoking flip() on a ByteBuffer
         * yields a weird error:
         *     java.lang.NoSuchMethodError: java.nio.ByteBuffer.flip()Ljava/nio/ByteBuffer
         *
         * Apparently, in JDK 9 (and later) ByteBuffer.flip() returns a ByteBuffer but it
         * used to return a Buffer in JDK 8. If run on JDK 8 you get this error.
         *
         * How can this even be :(
         *
         * The workaround is to cast ByteBuffer to Buffer before calling flip(),
         * whereafter you can compile the project with JDK 9 and have it running on JDK 8.
         */
        Buffer yuck = buffer;

        while (src.read(buffer) != -1) {
            yuck.flip();
            dest.write(buffer);
            buffer.compact();
        }
        yuck.flip();
        while (buffer.hasRemaining()) {
            dest.write(buffer);
        }
    }

    /**
     * Copies a file or a directory (including subdirectories)
     */
    public static void copy(final File src, final File dest) throws IOException {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                dest.mkdir();
            }

            // Copy everything in directory
            String[] children = src.list();
            for (String child : children) {
                copy(new File(src, child), new File(dest, child));
            }
        } else {
            try (ReadableByteChannel in = Channels.newChannel(new FileInputStream(src))) {
                try (WritableByteChannel out = Channels.newChannel(new FileOutputStream(dest))) {
                    fastChannelCopy(in, out);
                }
            }
        }
    }

    /**
     * Writes from an InputStream to a file
     */
    public static File writeToFile(InputStream inputStream, File file) throws IOException {

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            ReadableByteChannel inputChannel = Channels.newChannel(inputStream);
            FileChannel fileChannel = raf.getChannel();
            fastChannelCopy(inputChannel, fileChannel);
        }

        return file;
    }


    /**
     * Writes from an InputStream to a temporary file
     */
    public static File writeToTempFile(InputStream inputStream, String prefix, String suffix) throws IOException {

        File file = File.createTempFile(prefix, "." + suffix);
        writeToFile(inputStream, file);
        return file;
    }

    /**
     * Writes from a String to a temporary file
     */
    public static File writeToTempFile(String buf, String prefix, String suffix) throws IOException {

        InputStream is = new ByteArrayInputStream(buf.getBytes("UTF-8"));
        return writeToTempFile(is, prefix, suffix);
    }

    /**
     * Writes a ByteBuffer (internally a series of byte[]) to a temporary file
     */
    public static File writeToTempFile(ByteBuffer byteBuffer, String prefix, String suffix) throws IOException {
        File tmpOutputFile = null;

        try (RandomAccessFile outputRaf = new RandomAccessFile(tmpOutputFile, "rw")) {
            // Create temporary file
            tmpOutputFile = File.createTempFile(prefix, "." + suffix);
            FileChannel outputChannel = outputRaf.getChannel();

            outputChannel.write(byteBuffer);

        } catch (IOException ioe) {
            String info = "Failed to write to temporary file: " + ioe.getMessage();
            throw new IOException(info, ioe);
        }
        return tmpOutputFile;
    }

    /**
     * Writes a ByteBuffer (internally a series of byte[]) to a temporary file
     */
    public static File writeToTempFile(ByteBuffer byteBuffer, File directory, String prefix, String suffix) throws IOException {
        File tmpOutputFile = null;

        try (RandomAccessFile outputRaf = new RandomAccessFile(tmpOutputFile, "rw")) {
            // Create temporary file
            tmpOutputFile = File.createTempFile(prefix, "." + suffix, directory);
            FileChannel outputChannel = outputRaf.getChannel();

            outputChannel.write(byteBuffer);

        } catch (IOException ioe) {
            String info = "Failed to write to temporary file: " + ioe.getMessage();
            throw new IOException(info, ioe);
        }
        return tmpOutputFile;
    }

    /**
     * Writes a list of byte[] to a temporary file
     */
    public static File writeToTempFile(List<byte[]> bytesList, String prefix, String suffix) throws IOException {
        File tmpOutputFile = null;

        try (RandomAccessFile outputRaf = new RandomAccessFile(tmpOutputFile, "rw")) {
            // Create temporary file
            tmpOutputFile = File.createTempFile(prefix, "." + suffix);
            FileChannel outputChannel = outputRaf.getChannel();

            for (byte[] bytes : bytesList) {
                ByteArrayInputStream is = null;
                try {
                    is = new ByteArrayInputStream(bytes);
                    ReadableByteChannel inputChannel = Channels.newChannel(is);
                    fastChannelCopy(inputChannel, outputChannel);
                } catch (Exception e) {
                    String info = "Failed to write to temporary file: " + e.getMessage();
                    throw new IOException(info, e);
                } finally {
                    if (null != is) is.close();
                }
            }
        } catch (IOException ioe) {
            String info = "Failed to write to temporary file: " + ioe.getMessage();
            throw new IOException(info, ioe);
        }
        return tmpOutputFile;
    }

    /**
     * Removes a file or, if a directory, a directory substructure...
     * <p>
     * @param d a file or a directory
     */
    public static boolean delete(File d) {
        if (null == d || !d.exists())
            return true; // by definition

        if (d.isDirectory()) {
            File[] files = d.listFiles(); // and directories
            if (null != files) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        delete(f);
                    } else {
                        f.delete();
                    }
                }
            }
        }
        return d.delete();
    }

    /**
     * Retrieves file from a remote location identified by a URL.
     * <p>
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static File getRemoteFile(URL url, boolean keepAlive) throws IOException {
        File downloadedFile = File.createTempFile("downloaded-", ".bytes");

        URLConnection conn = url.openConnection();
        if (keepAlive) {
            conn.setRequestProperty("connection", "Keep-Alive");
        }
        conn.setUseCaches(false);

        try (ReadableByteChannel inputChannel = Channels.newChannel(conn.getInputStream())) {
            try (WritableByteChannel outputChannel = Channels.newChannel(new FileOutputStream(downloadedFile))) {
                fastChannelCopy(inputChannel, outputChannel);
            }
        }
        return downloadedFile;
    }
}
