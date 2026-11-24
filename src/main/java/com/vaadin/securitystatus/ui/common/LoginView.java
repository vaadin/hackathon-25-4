package com.vaadin.securitystatus.ui.common;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("login.title")
@AnonymousAllowed
public final class LoginView extends Composite<Div> implements BeforeEnterObserver, StandsAlone {

    private final LoginForm form = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        form.setAction("login");
        form.setForgotPasswordButtonVisible(false);
        form.setI18n(i18n());
        getContent().add(form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        form.setError(event.getLocation().getQueryParameters().getParameters().containsKey("error"));
    }

    private LoginI18n i18n() {
        var i18n = LoginI18n.createDefault();
        var texts = i18n.getForm();
        texts.setTitle(getTranslation("login.title"));
        texts.setUsername(getTranslation("login.username"));
        texts.setPassword(getTranslation("login.password"));
        texts.setSubmit(getTranslation("login.submit"));
        var error = i18n.getErrorMessage();
        error.setTitle(getTranslation("login.error.title"));
        error.setMessage(getTranslation("login.error.message"));
        error.setUsername(getTranslation("login.error.username"));
        error.setPassword(getTranslation("login.error.password"));
        return i18n;
    }
}
