package com.vaadin.securitystatus.base.i18n;

import org.springframework.stereotype.Component;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;

/**
 * The one writer of page titles. A route declares a bundle key in
 * {@code @PageTitle} and the route parameters are handed to the message as
 * {@code {0}}, {@code {1}}, in the order the route template declares them.
 */
@Component
public class TranslatedPageTitleGenerator implements PageTitleGenerator {

    @Override
    public String generatePageTitle(PageTitleContext context) {
        String key = context.value();
        if (key == null || key.isBlank()) {
            return "";
        }
        var parameters = context.routeParameters();
        Object[] arguments = parameters.getParameterNames().stream()
                .map(name -> parameters.get(name).orElse("")).toArray();
        return I18NProvider.translate(key, arguments);
    }
}
