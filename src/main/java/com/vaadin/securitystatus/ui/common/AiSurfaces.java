package com.vaadin.securitystatus.ui.common;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.function.SerializableConsumer;

/**
 * What the assistant offers the other screens, so that no feature depends on
 * another: the shell, the advisories list and the workbench ask for this
 * interface, and the {@code assistant} package implements it. When no bean
 * implements it, none of the surfaces is shown.
 */
public interface AiSurfaces {

    /** Whether the assistant is offered in this deployment; when not, every surface is an empty element. */
    default boolean offered() {
        return true;
    }

    /** The chat panel for the drawer of the internal shell, a new one per shell. */
    Component chatPanel();

    /**
     * A plain language ask that narrows the advisories list. The consumer
     * receives the advisory ids the ask matched, or empty when the list is to
     * show everything again. Only ever offered to a signed in team member.
     */
    Component advisoryAsk(SerializableConsumer<Optional<Set<String>>> narrow);

    /**
     * A paste box that fills the fields of the given form from prose. It never
     * saves: the fields change, the bean behind the binder changes, and the
     * person decides what happens next. The fields named as the person's own
     * are hidden from the model and never written.
     */
    Component formFill(FormLayout form, Binder<?> binder, Collection<HasValue<?, ?>> personOnly);
}
