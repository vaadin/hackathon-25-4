package com.vaadin.securitystatus.status;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.securitystatus.base.ui.ShellLookup;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.SecurityDataService;

/** Gives the customer shell its lookup, over the session's state. */
@SpringComponent
class LookupProvider implements ShellLookup {

    private final org.springframework.beans.factory.ObjectProvider<LookupState> state;
    private final SecurityDataService data;
    private final DataRefreshSignal refresh;

    LookupProvider(org.springframework.beans.factory.ObjectProvider<LookupState> state, SecurityDataService data,
            DataRefreshSignal refresh) {
        this.state = state;
        this.data = data;
        this.refresh = refresh;
    }

    @Override
    public Component create() {
        return new Lookup(state.getObject(), data, refresh);
    }
}
