package com.vaadin.securitystatus.ui.customer;

import org.springframework.beans.factory.ObjectProvider;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.securitystatus.customer.LookupState;
import com.vaadin.securitystatus.customer.ScanState;
import com.vaadin.securitystatus.data.DataRefreshSignal;
import com.vaadin.securitystatus.data.SecurityDataService;

/** Gives the customer shell its lookup, over the session's state. */
@SpringComponent
class LookupProvider implements LookupFactory {

    private final ObjectProvider<LookupState> state;
    private final SecurityDataService data;
    private final DataRefreshSignal refresh;
    private final ObjectProvider<ScanState> scan;

    LookupProvider(ObjectProvider<LookupState> state, SecurityDataService data,
            DataRefreshSignal refresh, ObjectProvider<ScanState> scan) {
        this.state = state;
        this.scan = scan;
        this.data = data;
        this.refresh = refresh;
    }

    @Override
    public Component create() {
        return new Lookup(state.getObject(), data, refresh, scan.getObject());
    }
}
