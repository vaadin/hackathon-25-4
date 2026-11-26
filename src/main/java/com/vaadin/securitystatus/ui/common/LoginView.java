package com.vaadin.securitystatus.ui.common;

import java.util.Arrays;

import org.springframework.core.env.Environment;

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
    private final boolean singleSignOn;

    public LoginView(Environment environment) {
        // Under vaadin.com's single sign on nobody signs in here; the home page sends them there, D-258.
        singleSignOn = Arrays.asList(environment.getActiveProfiles()).contains("cas");
        addClassName("login-view");
        form.setAction("login");
        form.setForgotPasswordButtonVisible(false);
        form.setI18n(i18n());
        getContent().add(form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (singleSignOn) {
            event.getUI().getPage().setLocation(".");
            return;
        }
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
