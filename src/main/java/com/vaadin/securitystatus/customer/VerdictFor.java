package com.vaadin.securitystatus.customer;

import java.util.List;

import com.vaadin.securitystatus.data.model.Assessment;

/** One verdict that answers an advisory, with the packages it answers for. */
public record VerdictFor(String id, Assessment assessment, List<String> packages) {
}
