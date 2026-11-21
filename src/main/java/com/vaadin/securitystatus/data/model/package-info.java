/**
 * The read models: one record per JSON object shape the cve-status pipeline
 * publishes, with its field names. A field the JSON does not carry, carries as
 * null, or carries as an empty text is absent here: an empty {@code Optional},
 * a null number or flag, an empty list or map. Nothing is defaulted.
 */
@NullMarked
package com.vaadin.securitystatus.data.model;

import org.jspecify.annotations.NullMarked;
