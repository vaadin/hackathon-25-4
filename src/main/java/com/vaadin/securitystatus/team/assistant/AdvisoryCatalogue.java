package com.vaadin.securitystatus.team.assistant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.ai.provider.ToolException;
import com.vaadin.securitystatus.data.SecurityDataService;
import com.vaadin.securitystatus.data.model.CasesIndex.AdvisoryCatalogEntry;
import com.vaadin.securitystatus.data.model.CasesIndex;

/**
 * The advisories catalogue of {@code cases/index.json} as a small SQL
 * database, so the Grid AI controller, which only speaks SQL, can narrow the
 * advisories list. In memory, built again whenever the index's
 * {@code generated_at} moves, and nothing else in the application reads it:
 * it is a way of asking the published data, not a store.
 *
 * <p>
 * The model's queries run as a database user that may only read, as the
 * controller documentation asks; the tables are written once by the owner when
 * the copy is built.
 */
@Component
public class AdvisoryCatalogue {

    /** How many ids a narrowing may name; the catalogue holds a few hundred. */
    static final int MOST_ROWS = 5000;

    private static final String OWNER = "OWNER";
    private static final String READER = "READER";

    private final SecurityDataService data;
    private @Nullable Instant builtFor;
    private @Nullable String url;
    private final String readerPassword = UUID.randomUUID().toString();
    private final String ownerPassword = UUID.randomUUID().toString();

    AdvisoryCatalogue(SecurityDataService data) {
        this.data = data;
    }

    /** The schema the model is given. */
    public String schema() {
        return Prompts.text(Prompts.CATALOGUE);
    }

    /**
     * Runs a query of the model as the reading user. A database error is
     * relayed as a {@link ToolException}: H2's message names only the model's
     * own query and the schema the model was already given, never a row.
     */
    public List<Map<String, Object>> query(String sql) {
        refuseAnythingButOneSelect(sql);
        try (Connection connection = reader(); Statement statement = connection.createStatement()) {
            statement.setMaxRows(MOST_ROWS);
            statement.setQueryTimeout(5);
            try (ResultSet rows = statement.executeQuery(sql)) {
                var out = new ArrayList<Map<String, Object>>();
                var meta = rows.getMetaData();
                while (rows.next()) {
                    var row = new LinkedHashMap<String, Object>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i), rows.getObject(i));
                    }
                    out.add(row);
                }
                return out;
            }
        } catch (SQLException failure) {
            throw new ToolException(Prompts.refusal("gridQueryFailed", firstLine(failure.getMessage())), failure);
        }
    }

    /**
     * Refuses a query whose first column is not {@code advisories.cve}: the
     * grid narrows the list by id, so a query without it could narrow
     * nothing. This is the query validation the controller runs before it
     * queues a query.
     */
    public void requireAdvisoryFirst(String sql) {
        refuseAnythingButOneSelect(sql);
        try (Connection connection = reader(); Statement statement = connection.createStatement()) {
            statement.setMaxRows(1);
            statement.setQueryTimeout(5);
            try (ResultSet rows = statement.executeQuery(sql)) {
                var meta = rows.getMetaData();
                boolean cveFirst = meta.getColumnCount() > 0
                        && "CVE".equalsIgnoreCase(meta.getColumnName(1))
                        && "ADVISORIES".equalsIgnoreCase(meta.getTableName(1));
                if (!cveFirst) {
                    throw new ToolException(Prompts.refusal("gridFirstColumn"));
                }
            }
        } catch (SQLException failure) {
            throw new ToolException(Prompts.refusal("gridQueryFailed", firstLine(failure.getMessage())), failure);
        }
    }

    /** The advisory ids a validated query names, in the order it returns them. */
    public Set<String> ids(String sql) {
        requireAdvisoryFirst(sql);
        var ids = new LinkedHashSet<String>();
        for (Map<String, Object> row : query(sql)) {
            Object first = row.values().iterator().next();
            if (first != null) {
                ids.add(first.toString());
            }
        }
        return ids;
    }

    private static void refuseAnythingButOneSelect(String sql) {
        String bare = sql.strip().replaceAll(";\\s*$", "");
        String head = bare.toLowerCase(Locale.ROOT);
        if (!(head.startsWith("select") || head.startsWith("with")) || bare.contains(";")) {
            throw new ToolException(Prompts.refusal("gridOnlySelect"));
        }
    }

    private static String firstLine(@Nullable String message) {
        return message == null ? "" : message.lines().findFirst().orElse("");
    }

    private synchronized Connection reader() throws SQLException {
        CasesIndex index = data.index();
        if (url == null || !index.generatedAt().equals(builtFor)) {
            build(index);
        }
        Connection connection = DriverManager.getConnection(url, READER, readerPassword);
        connection.setReadOnly(true);
        return connection;
    }

    private void build(CasesIndex index) throws SQLException {
        if (url != null) {
            try (Connection old = DriverManager.getConnection(url, OWNER, ownerPassword);
                    Statement statement = old.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
        // The setting that keeps the database after its last connection closes is
        // the owner's to give; the reader connects by the bare name, since a
        // setting in its address would need admin rights.
        String fresh = "jdbc:h2:mem:catalogue-" + UUID.randomUUID();
        try (Connection owner = DriverManager.getConnection(fresh + ";DB_CLOSE_DELAY=-1", OWNER, ownerPassword);
                Statement statement = owner.createStatement()) {
            statement.execute("CREATE TABLE advisories (cve VARCHAR PRIMARY KEY, severity VARCHAR, verdict VARCHAR, fix_owner VARCHAR, vaadin_advisory BOOLEAN, in_current_release BOOLEAN)");
            statement.execute("CREATE TABLE packages (cve VARCHAR, package VARCHAR)");
            statement.execute("CREATE TABLE modules (cve VARCHAR, module VARCHAR)");
            statement.execute("CREATE TABLE fixes (cve VARCHAR, line VARCHAR, fixed_in VARCHAR, fixed_in_prerelease VARCHAR)");
            statement.execute("CREATE TABLE lines (line VARCHAR PRIMARY KEY, newest_release VARCHAR)");
            fill(owner, index);
            statement.execute("CREATE USER " + READER + " PASSWORD '" + readerPassword + "'");
            for (String table : List.of("advisories", "packages", "modules", "fixes", "lines")) {
                statement.execute("GRANT SELECT ON " + table + " TO " + READER);
            }
        }
        url = fresh;
        builtFor = index.generatedAt();
    }

    private static void fill(Connection owner, CasesIndex index) throws SQLException {
        try (PreparedStatement advisory = owner.prepareStatement("INSERT INTO advisories VALUES (?, ?, ?, ?, ?, ?)");
                PreparedStatement pkg = owner.prepareStatement("INSERT INTO packages VALUES (?, ?)");
                PreparedStatement module = owner.prepareStatement("INSERT INTO modules VALUES (?, ?)");
                PreparedStatement fix = owner.prepareStatement("INSERT INTO fixes VALUES (?, ?, ?, ?)");
                PreparedStatement line = owner.prepareStatement("INSERT INTO lines VALUES (?, ?)")) {
            for (AdvisoryCatalogEntry entry : index.advisories().values()) {
                advisory.setString(1, entry.cve());
                advisory.setString(2, entry.severity().map(word -> word.toLowerCase(Locale.ROOT)).orElse(null));
                advisory.setString(3, entry.applicability().orElse(null));
                advisory.setString(4, entry.fixOwner().orElse(null));
                advisory.setObject(5, entry.vaadinAdvisory());
                advisory.setObject(6, entry.current());
                advisory.addBatch();
                for (String name : entry.packages()) {
                    pkg.setString(1, entry.cve());
                    pkg.setString(2, name);
                    pkg.addBatch();
                }
                for (String name : entry.modules()) {
                    module.setString(1, entry.cve());
                    module.setString(2, name);
                    module.addBatch();
                }
            }
            for (var perAdvisory : index.fixedIn().entrySet()) {
                for (var perLine : perAdvisory.getValue().entrySet()) {
                    fix.setString(1, perAdvisory.getKey());
                    fix.setString(2, perLine.getKey());
                    fix.setString(3, perLine.getValue().fixedIn().map(Object::toString).orElse(null));
                    fix.setString(4, perLine.getValue().fixedInPrerelease().map(Object::toString).orElse(null));
                    fix.addBatch();
                }
            }
            for (CasesIndex.Line entry : index.lines()) {
                line.setString(1, entry.line());
                line.setString(2, entry.version().toString());
                line.addBatch();
            }
            advisory.executeBatch();
            pkg.executeBatch();
            module.executeBatch();
            fix.executeBatch();
            line.executeBatch();
        }
    }
}
