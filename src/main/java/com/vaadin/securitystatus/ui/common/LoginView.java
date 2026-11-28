package com.vaadin.securitystatus.ui.common;

import static com.vaadin.securitystatus.ui.common.Parts.css;
import static com.vaadin.securitystatus.ui.common.Parts.paragraph;

import java.util.Arrays;

import org.springframework.core.env.Environment;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

// Drawn alone, without the customer shell and its app bar, D-276.
@Route(value = "login", autoLayout = false)
@PageTitle("login.title")
@AnonymousAllowed
public final class LoginView extends Composite<Div> implements BeforeEnterObserver {

    private final LoginForm form = new LoginForm();
    private final boolean singleSignOn;

    public LoginView(Environment environment) {
        // Under vaadin.com's single sign on nobody signs in here; the home page sends them there, D-258.
        singleSignOn = Arrays.asList(environment.getActiveProfiles()).contains("cas");
        addClassName("login-view");
        form.setAction("login");
        form.setForgotPasswordButtonVisible(false);
        form.setI18n(i18n());
        // The prototype's card: the brand and the theme switch, its own heading and lead, the form, a footer.
        var brand = css(new Div(AppBar.icon(),
                css(new Span(getTranslation("shell.customer.title")), "login-brand-name"), new ThemeSwitch()),
                "login-brand");
        getContent().add(css(new Div(brand, new H1(getTranslation("login.title")),
                paragraph("login-lead", getTranslation("login.lead")), form,
                paragraph("login-foot", getTranslation("login.trouble"))), "login-card"));
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
