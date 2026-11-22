package com.vaadin.securitystatus.base.nav;

/**
 * Every query flag the application writes, in the order they appear in an
 * address. The order is declared here rather than left to
 * {@code QueryParameters}, which writes its parameters in hash order, draft 14.
 */
public enum QueryFlag {

    /** The version screen: which shape the Spring Boot section takes. */
    BOOT("boot"),
    /** The version screen: the findings ruled out are listed. */
    RULED("ruled"),
    /** The version screen: the findings only a discontinued product brings are listed. */
    DISC("disc"),
    /** The version screen: the advisory whose details are open. */
    OPEN("open"),
    /** Home and the version screen: the prerelease lines are listed too. */
    PRE("pre"),
    /** The scan screen: the secure findings are listed too. */
    SECURE("secure"),
    /** The advisories screen: the verdict the list is narrowed to. */
    VERDICT("verdict"),
    /** The explorer: the search. */
    Q("q"),
    /** The explorer: the products turned off, by name, comma separated. */
    OFF("off"),
    /** The workbench: the one series the queue is narrowed to. */
    SERIES("series");

    private final String key;

    QueryFlag(String key) {
        this.key = key;
    }

    /** The name of the flag in the address. */
    public String key() {
        return key;
    }
}
