// Copyright 2017 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.config.model.application.provider;

import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.yahoo.config.application.api.DeployLogger;
import com.yahoo.io.reader.NamedReader;
import com.yahoo.yolean.Exceptions;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;

/**
 * Validates xml files against a schema.
 *
 * @author Tony Vaagenes
 */
public class SchemaValidator {

    private final CustomErrorHandler errorHandler = new CustomErrorHandler();
    private final ValidationDriver driver;
    private final DeployLogger deployLogger;

    /**
     * Initializes the validator by using the given file as schema file
     *
     * @param schemaFile schema file
     * @throws IOException if it is not possible to read schema files
     */
    SchemaValidator(File schemaFile, DeployLogger deployLogger) throws IOException, SAXException {
        this.deployLogger = deployLogger;
        this.driver = new ValidationDriver(PropertyMap.EMPTY, instanceProperties(), CompactSchemaReader.getInstance());
        driver.loadSchema(ValidationDriver.fileInputSource(schemaFile));
    }

    public void validate(File file) throws IOException {
        validate(file, file.getName());
    }

    public void validate(File file, String fileName) throws IOException {
        validate(ValidationDriver.fileInputSource(file), fileName);
    }

    public void validate(Reader reader) throws IOException {
        validate(new InputSource(reader), null);
    }

    public void validate(NamedReader reader) throws IOException {
        validate(new InputSource(reader), reader.getName());
    }

    public void validate(InputSource inputSource, String fileName)  throws IOException {
        errorHandler.fileName = (fileName == null ? " input" : fileName);
        try {
            if ( ! driver.validate(inputSource)) {
                // Shouldn't happen, error handler should have thrown
                throw new RuntimeException("Aborting due to earlier XML errors.");
            }
        } catch (SAXException e) {
            // This should never happen, as it is handled by the ErrorHandler
            // installed for the driver.
            throw new IllegalArgumentException(
                    "XML error in " + (fileName == null ? " input" : fileName) + ": " + Exceptions.toMessageString(e));
        }
    }

    private PropertyMap instanceProperties() {
        PropertyMapBuilder builder = new PropertyMapBuilder();
        builder.put(ValidateProperty.ERROR_HANDLER, errorHandler);
        return builder.toPropertyMap();
    }

    private class CustomErrorHandler implements ErrorHandler {
        volatile String fileName;

        public void warning(SAXParseException e) {
            deployLogger.log(Level.WARNING, message(e));
        }

        public void error(SAXParseException e) {
            throw new IllegalArgumentException(message(e));
        }

        public void fatalError(SAXParseException e) {
            throw new IllegalArgumentException(message(e));
        }

        private String message(SAXParseException e) {
            return "XML error in " + fileName + ": " +
                    Exceptions.toMessageString(e)
                    + " [" + e.getLineNumber() + ":" + e.getColumnNumber() + "]";
        }
    }

}
