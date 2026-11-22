package com.vaadin.securitystatus.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.ai.provider.ToolException;
import com.vaadin.securitystatus.data.FixturePages;

/**
 * The query validation the Grid AI controller runs through the catalogue: the
 * advisory id first or nothing, a single read only SELECT, and a database
 * error relayed in words the model can correct from.
 */
class AdvisoryCatalogueTest {

    private final AdvisoryCatalogue catalogue = new AdvisoryCatalogue(FixturePages.offline());

    @Test
    void a_query_with_the_advisory_first_names_its_advisories() {
        var ids = catalogue.ids("SELECT a.cve AS \"Advisory\", a.severity AS \"Severity\" FROM advisories a "
                + "WHERE a.severity = 'critical'");
        assertThat(ids).hasSize(46).contains("CVE-2016-1000027");
    }

    @Test
    void a_query_without_the_advisory_first_is_refused() {
        assertThatThrownBy(() -> catalogue.requireAdvisoryFirst("SELECT a.severity AS \"Severity\", a.cve FROM advisories a"))
                .isInstanceOf(ToolException.class).hasMessageStartingWith("The grid narrows the advisories list");
        assertThatThrownBy(() -> catalogue.requireAdvisoryFirst("SELECT p.cve AS \"Advisory\" FROM packages p"))
                .isInstanceOf(ToolException.class).hasMessageStartingWith("The grid narrows the advisories list");
    }

    @Test
    void anything_but_one_select_is_refused_and_nothing_can_be_written() {
        assertThatThrownBy(() -> catalogue.query("DELETE FROM advisories")).isInstanceOf(ToolException.class)
                .hasMessage("Only a single SELECT query is accepted.");
        assertThatThrownBy(() -> catalogue.query("SELECT 1; DROP TABLE advisories")).isInstanceOf(ToolException.class);
        assertThatThrownBy(() -> catalogue.query("SELECT FILE_READ('/etc/hosts')")).isInstanceOf(ToolException.class);
        assertThat(catalogue.query("SELECT COUNT(*) AS n FROM advisories").getFirst().values()).containsExactly(535L);
    }

    @Test
    void an_unknown_column_is_relayed_so_the_model_can_correct_the_query() {
        assertThatThrownBy(() -> catalogue.requireAdvisoryFirst(
                "SELECT a.cve AS \"Advisory\" FROM advisories a WHERE a.exploit_available = TRUE"))
                .isInstanceOf(ToolException.class)
                .hasMessageStartingWith("The query was refused: Column \"A.EXPLOIT_AVAILABLE\" not found");
    }
}
