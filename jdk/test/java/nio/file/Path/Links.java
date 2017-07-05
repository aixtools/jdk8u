/*
 * Copyright 2008-2009 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/* @test
 * @bug 4313887 6838333
 * @summary Unit test for java.nio.file.Path createSymbolicLink,
 *     readSymbolicLink, and createLink methods
 * @library ..
 */

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

public class Links {

    static final boolean isWindows =
        System.getProperty("os.name").startsWith("Windows");

    static void assertTrue(boolean okay) {
        if (!okay)
            throw new RuntimeException("Assertion failed");
    }

    /**
     * Exercise createSymbolicLink and readLink methods
     */
    static void testSymLinks(Path dir) throws IOException {
        Path link = dir.resolve("link");

        // Check if sym links are supported
        try {
            link.createSymbolicLink(Paths.get("foo"));
            link.delete();
        } catch (UnsupportedOperationException x) {
            // sym links not supported
            return;
        } catch (IOException x) {
            // probably insufficient privileges to create sym links (Windows)
            return;
        }

        // Test links to various targets
        String[] windowsTargets =
            { "foo", "C:\\foo", "\\foo", "\\\\server\\share\\foo" };
        String[] otherTargets = { "relative", "/absolute" };

        String[] targets = (isWindows) ? windowsTargets : otherTargets;
        for (String s: targets) {
            Path target = Paths.get(s);
            link.createSymbolicLink(target);
            try {
                assertTrue(link.readSymbolicLink().equals(target));
            } finally {
                link.delete();
            }
        }
    }

    /**
     * Exercise createLink method
     */
    static void testHardLinks(Path dir) throws IOException {
        Path foo = dir.resolve("foo").createFile();
        try {
            Path bar;
            try {
                bar = dir.resolve("bar").createLink(foo);
            } catch (UnsupportedOperationException x) {
                return;
            } catch (IOException x) {
                // probably insufficient privileges (Windows)
                return;
            }
            try {
                Object key1 = Attributes
                    .readBasicFileAttributes(foo).fileKey();
                Object key2 = Attributes
                    .readBasicFileAttributes(bar).fileKey();
                assertTrue((key1 == null) || (key1.equals(key2)));
            } finally {
                bar.delete();
            }


        } finally {
            foo.delete();
        }
    }

    public static void main(String[] args) throws IOException {
        Path dir = TestUtil.createTemporaryDirectory();
        try {
            testSymLinks(dir);
            testHardLinks(dir);

            // repeat tests on Windows with long path
            if (isWindows) {
                Path dirWithLongPath = null;
                try {
                    dirWithLongPath = TestUtil.createDirectoryWithLongPath(dir);
                } catch (IOException x) {
                    System.out.println("Unable to create long path: " + x);
                }
                if (dirWithLongPath != null) {
                    System.out.println("");
                    System.out.println("** REPEAT TESTS WITH LONG PATH **");
                    testSymLinks(dirWithLongPath);
                    testHardLinks(dirWithLongPath);
                }
            }
        } finally {
            TestUtil.removeAll(dir);
        }
    }
}