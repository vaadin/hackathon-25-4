




package com.vaadin.example.sightseeing.views.somethingelse;

import com.vaadin.flow.component.html.Span;

import com.vaadin.flow.component.html.Paragraph;

import com.vaadin.flow.theme.lumo.LumoUtility;

import com.vaadin.flow.component.formlayout.FormLayout;

import com.vaadin.example.sightseeing.data.Role;
import com.vaadin.example.sightseeing.security.AuthenticatedUser;
import com.vaadin.example.sightseeing.ui.AdminNav;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

@PageTitle("SomethingElseView")
@PermitAll
@Route(value = "something-else")
public class SomethingElseView extends Main {

    public SomethingElseView(AuthenticatedUser authenticatedUser) {
        authenticatedUser.get().ifPresent(u -> {
            if (u.getRoles().contains(Role.ADMIN)) {
                add(new AdminNav("something-else"));
            }
        });
        add(new H1("This is SomethingElseView"));
Paragraph loremIpsumDolorSitAm = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm.addClassNames(LumoUtility.FontSize.SMALL);
Paragraph loremIpsumDolorSitAm2 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm2.addClassNames(LumoUtility.FontSize.SMALL);
FormLayout formlayout = new FormLayout();
formlayout.setAutoResponsive(true);
formlayout.getStyle().set("minWidth", "500px").set("minHeight", "50px");
formlayout.addFormRow(loremIpsumDolorSitAm, loremIpsumDolorSitAm2);
Paragraph loremIpsumDolorSitAm3 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm3.addClassNames(LumoUtility.FontSize.SMALL);
Paragraph loremIpsumDolorSitAm4 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm4.addClassNames(LumoUtility.FontSize.SMALL);
formlayout.addFormRow(loremIpsumDolorSitAm4);
formlayout.addFormRow(loremIpsumDolorSitAm3);
add(formlayout);
Paragraph loremIpsumDolorSitAm5 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm5.addClassNames(LumoUtility.FontSize.SMALL);
Paragraph loremIpsumDolorSitAm7 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm7.addClassNames(LumoUtility.FontSize.SMALL);
Paragraph loremIpsumDolorSitAm6 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm6.addClassNames(LumoUtility.FontSize.SMALL);

FormLayout formlayout2 = new FormLayout();
formlayout2.setAutoResponsive(true);
formlayout2.getStyle().set("minWidth", "500px").set("minHeight", "50px");
formlayout2.addFormItem(loremIpsumDolorSitAm5);
Paragraph loremIpsumDolorSitAm9 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
//<theme-editor-local-classname>
loremIpsumDolorSitAm9.addClassName("something-else-view-p-3");
Paragraph loremIpsumDolorSitAm8 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
loremIpsumDolorSitAm8.addClassNames(LumoUtility.FontSize.SMALL);
loremIpsumDolorSitAm8.add(loremIpsumDolorSitAm9);

Paragraph loremIpsumDolorSitAm10 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");

Paragraph loremIpsumDolorSitAm11 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
Paragraph loremIpsumDolorSitAm12 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
formlayout2.addFormRow(loremIpsumDolorSitAm8, loremIpsumDolorSitAm10, loremIpsumDolorSitAm11, loremIpsumDolorSitAm12);

Paragraph loremIpsumDolorSitAm13 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
//<theme-editor-local-classname>
loremIpsumDolorSitAm13.addClassName("something-else-view-p-2");


Paragraph loremIpsumDolorSitAm14 = new Paragraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");
//<theme-editor-local-classname>
loremIpsumDolorSitAm14.addClassName("something-else-view-p-1");
loremIpsumDolorSitAm14.addClassNames(LumoUtility.FontSize.LARGE);
Span loremIpsumDolorSitAm15 = new Span("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
formlayout2.addFormRow(loremIpsumDolorSitAm13, loremIpsumDolorSitAm14, loremIpsumDolorSitAm15);


formlayout2.addFormRow(loremIpsumDolorSitAm7, loremIpsumDolorSitAm6);

add(formlayout2);
    }
}
