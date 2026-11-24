package com.vaadin.securitystatus.ui.common;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitleContext;
import com.vaadin.flow.router.PageTitleGenerator;
import com.vaadin.flow.router.RouteConfiguration;

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
        // The names come as a set; the template gives their order.
        String template = RouteConfiguration.forApplicationScope().getTemplate(context.navigationTarget())
                .orElse("");
        Object[] arguments = parameters.getParameterNames().stream()
                .sorted(Comparator.comparingInt((String name) -> {
                    int at = template.indexOf(":" + name);
                    return at < 0 ? Integer.MAX_VALUE : at;
                }).thenComparing(Comparator.naturalOrder()))
                .map(name -> parameters.get(name).orElse("")).toArray();
        return I18NProvider.translate(key, arguments);
    }
}
