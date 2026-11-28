package com.vaadin.securitystatus.ui.team.workbench;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasHelper;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout.LabelTextAlign;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.binder.DefaultBinderValidationErrorHandler;
import com.vaadin.flow.data.binder.ErrorLevel;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.securitystatus.team.workbench.VerdictDraft.Draft;
import com.vaadin.securitystatus.team.workbench.VerdictDraft.Ready;
import com.vaadin.securitystatus.team.workbench.VerdictDraft;
import com.vaadin.securitystatus.team.workbench.VerdictYaml;

/**
 * The verdict form: a {@link FormLayout} with its labels beside the fields,
 * the 25.4 side labels aligned through {@link LabelTextAlign}, over a
 * {@link VerdictDraft} bound by a {@link BeanValidationBinder} that checks the
 * {@link Draft} group as the person types and the {@link Ready} group only
 * when asked to export.
 *
 * <p>
 * A plain component with its layout and binder reachable, so the form AI
 * controller of epic 11 can attach to it without this class knowing.
 */
public final class VerdictForm extends Composite<FormLayout> {

    private final BeanValidationBinder<VerdictDraft> binder = new BeanValidationBinder<>(VerdictDraft.class,
            Draft.class);

    private final TextField verdictId = text("verdictId");
    private final TextField purl = text("purl");
    private final TextArea advisories = area("advisories");
    private final Select<String> applicability = select("applicability", VerdictDraft.APPLICABILITY);
    private final Select<String> depth = select("depth", VerdictDraft.DEPTH);
    private final TextField affects = text("affects");
    private final TextArea impact = area("impact");
    private final TextArea workaround = area("workaround");
    private final TextArea commentary = area("commentary");
    private final TextArea fixExpected = area("fixExpected");
    private final TextField fixExpectedVersion = text("fixExpectedVersion");
    private final TextArea fixPrs = area("fixPrs");
    private final TextArea evidence = area("evidence");
    private final TextField supersedes = text("supersedes");
    private final TextField assessedBy = text("assessedBy");

    /** The one line fields, where Enter saves as the Save button does; in a text area Enter is a new line. */
    public List<TextField> lines() {
        return List.of(verdictId, purl, affects, fixExpectedVersion, supersedes, assessedBy);
    }

    public VerdictForm() {
        FormLayout layout = getContent();
        layout.addClassName("verdict-form");
        layout.setAutoResponsive(true);
        layout.setMaxColumns(1);
        layout.setColumnWidth("16em");
        layout.setExpandColumns(true);
        layout.setExpandFields(true);
        layout.setLabelsAside(true);
        layout.setLabelWidth("10em");
        layout.setLabelTextAlign(LabelTextAlign.END);
        layout.add(verdictId, purl, advisories, applicability, depth, affects, impact, workaround, commentary,
                fixExpected, fixExpectedVersion, fixPrs, evidence, supersedes, assessedBy);

        binder.setValidationErrorHandler(new TranslatedErrors());
        binder.forField(applicability).withConverter(blankAsNull()).bind("applicability");
        binder.forField(depth).withConverter(blankAsNull()).bind("depth");
        binder.forField(impact).withValidator((value, context) -> {
            boolean exporting = List.of(binder.getValidationGroups()).contains(Ready.class);
            boolean needed = VerdictDraft.NEED_IMPACT.contains(Optional.ofNullable(applicability.getValue()).orElse(""));
            return exporting && needed && value.isBlank() ? ValidationResult.error("workbench.invalid.impact")
                    : ValidationResult.ok();
        }).bind("impact");
        binder.forField(fixExpectedVersion).withValidator((value, context) -> VerdictYaml.promised(value).entrySet()
                .stream().allMatch(promise -> promise.getValue().startsWith(promise.getKey() + "."))
                        ? ValidationResult.ok()
                        : ValidationResult.error("workbench.invalid.promise.line"))
                .bind("fixExpectedVersion");
        // Every other field is bound by its name, after the ones above that convert or check.
        binder.bindInstanceFields(this);
        binder.setBean(new VerdictDraft());
    }

    /** The layout, for whatever attaches to the form later. */
    public FormLayout layout() {
        return getContent();
    }

    /** The binder over the draft being edited. */
    public BeanValidationBinder<VerdictDraft> binder() {
        return binder;
    }

    /** The draft being edited, holding every value that passed the format checks. */
    public VerdictDraft draft() {
        VerdictDraft bean = binder.getBean();
        return bean == null ? new VerdictDraft() : bean;
    }

    /** Edits a copy of the given draft. */
    public void edit(VerdictDraft draft) {
        binder.setBean(draft.copy());
    }

    /** The labels of the fields whose format the {@link Draft} group refuses, in the form's order. */
    public List<String> invalidForDraft() {
        return refusedBy(binder.validate(Draft.class));
    }

    /**
     * The labels of the fields the {@link Ready} group refuses, in the form's
     * order; empty when the draft can be exported. The refusals stay on the
     * fields until they are edited.
     */
    public List<String> missingForReady() {
        return refusedBy(binder.validate(Draft.class, Ready.class));
    }

    private List<String> refusedBy(BinderValidationStatus<VerdictDraft> status) {
        var refused = new ArrayList<HasValue<?, ?>>();
        status.getFieldValidationErrors().forEach(error -> refused.add(error.getField()));
        var ordered = new ArrayList<String>();
        for (HasValue<?, ?> field : fields().values()) {
            if (refused.contains(field) && field instanceof HasLabel labelled) {
                ordered.add(Optional.ofNullable(labelled.getLabel()).orElse(""));
            }
        }
        return ordered;
    }

    /**
     * The fields only a person fills: the verdict's id, who assessed it and
     * what it supersedes. The form AI controller never sees them.
     */
    public List<HasValue<?, ?>> personOnly() {
        return List.of(verdictId, assessedBy, supersedes);
    }

    /** Every field by its property name, in the form's order. */
    public Map<String, HasValue<?, ?>> fields() {
        var out = new LinkedHashMap<String, HasValue<?, ?>>();
        out.put("verdictId", verdictId);
        out.put("purl", purl);
        out.put("advisories", advisories);
        out.put("applicability", applicability);
        out.put("depth", depth);
        out.put("affects", affects);
        out.put("impact", impact);
        out.put("workaround", workaround);
        out.put("commentary", commentary);
        out.put("fixExpected", fixExpected);
        out.put("fixExpectedVersion", fixExpectedVersion);
        out.put("fixPrs", fixPrs);
        out.put("evidence", evidence);
        out.put("supersedes", supersedes);
        out.put("assessedBy", assessedBy);
        return out;
    }

    private TextField text(String property) {
        var field = new TextField(getTranslation("workbench.field." + property));
        field.setId("verdict-" + property);
        help(field, property);
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.setWidthFull();
        field.addClassName("verdict-" + property);
        return field;
    }

    private TextArea area(String property) {
        var field = new TextArea(getTranslation("workbench.field." + property));
        field.setId("verdict-" + property);
        help(field, property);
        field.setValueChangeMode(ValueChangeMode.EAGER);
        field.setWidthFull();
        field.addClassName("verdict-" + property);
        return field;
    }

    private Select<String> select(String property, List<String> words) {
        var field = new Select<String>();
        field.setLabel(getTranslation("workbench.field." + property));
        field.setId("verdict-" + property);
        help(field, property);
        field.setItems(words);
        field.setEmptySelectionAllowed(true);
        field.setEmptySelectionCaption(getTranslation("workbench.select.none"));
        field.setItemLabelGenerator(word -> word == null ? getTranslation("workbench.select.none")
                : getTranslation("workbench." + property + "." + word));
        field.setWidthFull();
        field.addClassName("verdict-" + property);
        return field;
    }

    private void help(HasHelper field, String property) {
        String help = getTranslation("workbench.help." + property);
        if (!help.isBlank()) {
            field.setHelperText(help);
        }
    }

    /** A select's empty choice is no value; the draft keeps an empty text for it. */
    private static Converter<@Nullable String, String> blankAsNull() {
        return Converter.from(value -> Result.ok(value == null ? "" : value),
                value -> value.isEmpty() ? null : value);
    }

    /** The constraint messages are bundle keys, turned into words where they are shown. */
    private final class TranslatedErrors extends DefaultBinderValidationErrorHandler {

        @Override
        public void handleError(HasValue<?, ?> field, ValidationResult result) {
            String message = result.getErrorMessage();
            super.handleError(field, ValidationResult.create(
                    message.startsWith("workbench.") ? getTranslation(message) : message,
                    result.getErrorLevel().orElse(ErrorLevel.ERROR)));
        }
    }
}
