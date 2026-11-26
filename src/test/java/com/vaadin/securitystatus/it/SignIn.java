package com.vaadin.securitystatus.it;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Signs a browser in through the demo login, because since D-258 every page
 * asks for a signed-in user. The demo accounts are team/team and guest/guest,
 * and the customer page tests take guest on purpose: what they prove must not
 * need the role.
 */
final class SignIn {

    private SignIn() {
    }

    static void as(ChromeDriver driver, String base, String user) {
        String root = base.endsWith("/") ? base : base + "/";
        driver.get(root + "login");
        var wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        wait.until(d -> !d.findElements(By.cssSelector("vaadin-login-form input[name=password]")).isEmpty());
        driver.executeScript("""
                document.querySelector('vaadin-login-form vaadin-text-field').value = arguments[0];
                document.querySelector('vaadin-login-form vaadin-password-field').value = arguments[0];
                document.querySelector('vaadin-login-form vaadin-button[slot=submit]').click();
                """, user);
        wait.until(d -> !d.getCurrentUrl().contains("/login"));
    }
}
