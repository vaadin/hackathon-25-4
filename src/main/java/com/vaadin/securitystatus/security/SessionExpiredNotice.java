package com.vaadin.securitystatus.security;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

/**
 * An expired session reloads the view it was on, behind one notice.
 */
@Component
class SessionExpiredNotice implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().setSystemMessagesProvider(info -> {
            var i18n = info.getService().getInstantiator().getI18NProvider();
            var messages = new CustomizedSystemMessages();
            messages.setSessionExpiredNotificationEnabled(true);
            messages.setSessionExpiredURL(null);
            messages.setSessionExpiredCaption(i18n.getTranslation("session.expired.caption", info.getLocale()));
            messages.setSessionExpiredMessage(i18n.getTranslation("session.expired.message", info.getLocale()));
            return messages;
        });
    }
}
