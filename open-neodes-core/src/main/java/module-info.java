/**
 * Module principal pour la bibliothèque open-neodes.
 */
module com.jikenlabs.openneodes {
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires org.slf4j;

    exports com.jikenlabs.openneodes.facade;
    exports com.jikenlabs.openneodes.model;
    exports com.jikenlabs.openneodes.core;
    exports com.jikenlabs.openneodes.norm;
    exports com.jikenlabs.openneodes.exception;
    exports com.jikenlabs.openneodes.engine;
    exports com.jikenlabs.openneodes.domain;

    opens com.jikenlabs.openneodes.norm to com.fasterxml.jackson.databind;
    opens com.jikenlabs.openneodes.model to com.fasterxml.jackson.databind;
}
