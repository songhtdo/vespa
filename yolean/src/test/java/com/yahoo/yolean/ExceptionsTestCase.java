// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.yolean;

import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.NoSuchFileException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author bratseth
 */
public class ExceptionsTestCase {

    @Test
    public void testToMessageStrings() {
        assertEquals("Blah",Exceptions.toMessageString(new Exception("Blah")));
        assertEquals("Blah", Exceptions.toMessageString(new Exception(new Exception("Blah"))));
        assertEquals("Exception",Exceptions.toMessageString(new Exception()));
        assertEquals("Foo: Blah",Exceptions.toMessageString(new Exception("Foo",new Exception(new IllegalArgumentException("Blah")))));
        assertEquals("Foo",Exceptions.toMessageString(new Exception("Foo",new Exception("Foo"))));
        assertEquals("Foo: Exception",Exceptions.toMessageString(new Exception("Foo",new Exception())));
        assertEquals("Foo",Exceptions.toMessageString(new Exception(new Exception("Foo"))));
    }

    @Test
    public void testUnchecks() {
        try {
            Exceptions.uncheck(this::throwNoSuchFileException);
        } catch (UncheckedIOException e) {
            assertEquals("filename", e.getCause().getMessage());
        }

        try {
            Exceptions.uncheck(this::throwNoSuchFileException, "additional %s", "info");
        } catch (UncheckedIOException e) {
            assertEquals("additional info", e.getMessage());
        }

        try {
            int i = Exceptions.uncheck(this::throwNoSuchFileExceptionSupplier);
        } catch (UncheckedIOException e) {
            assertEquals("filename", e.getCause().getMessage());
        }

        try {
            int i = Exceptions.uncheck(this::throwNoSuchFileExceptionSupplier, "additional %s", "info");
        } catch (UncheckedIOException e) {
            assertEquals("additional info", e.getMessage());
        }

        Exceptions.uncheckAndIgnore(this::throwNoSuchFileException, NoSuchFileException.class);
        assertNull(Exceptions.uncheckAndIgnore(this::throwNoSuchFileExceptionSupplier, NoSuchFileException.class));
    }

    private void throwNoSuchFileException() throws IOException {
        throw new NoSuchFileException("filename");
    }

    private int throwNoSuchFileExceptionSupplier() throws IOException {
        throw new NoSuchFileException("filename");
    }
}
