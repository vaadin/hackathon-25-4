package com.vaadin.securitystatus.workbench;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.securitystatus.base.security.Roles;
import com.vaadin.securitystatus.workbench.VerdictYaml.Handover;

/**
 * The workbench's two actions and the drafts they keep. Every method checks
 * the caller's role itself, so the rule of {@code 04-security.md} holds
 * whatever calls it, a view, the assistant of epic 11 or a test, D-126.
 *
 * <p>
 * One draft per advisory and package pair: a save of a pair that is already
 * stored replaces it, so the newest save wins, and the draft remembers who
 * made it.
 */
@Service
public class VerdictDrafts {

    private static final String AUTHORITY = "ROLE_" + Roles.SECURITY;

    private final VerdictDraftRepository repository;
    private final Clock clock;

    VerdictDrafts(VerdictDraftRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Saves the draft, replacing any stored draft of the same pair, and answers
     * a detached copy of what is stored now.
     */
    @Transactional
    public VerdictDraft save(VerdictDraft draft) {
        String who = requireRole().getName();
        String pair = VerdictDraft.pairOf(draft.getPurl(), draft.advisoryIds());
        VerdictDraft stored = repository.findByPair(pair).orElseGet(VerdictDraft::new);
        stored.copyFrom(draft);
        stored.setPair(pair);
        stored.setSavedBy(who);
        stored.setUpdatedAt(clock.instant());
        return repository.saveAndFlush(stored).copy();
    }

    /** Every saved draft, newest first, as detached copies. */
    @Transactional(readOnly = true)
    public List<VerdictDraft> list() {
        requireRole();
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(VerdictDraft::copy).toList();
    }

    /**
     * The handover into GitHub's new file editor for a verdict that is ready:
     * its path, its text and the address. The text travels to github.com,
     * where GitHub's own sign in and review take over.
     */
    public Handover handover(VerdictDraft draft) {
        requireRole();
        if (draft.getVerdictId().isBlank()) {
            throw new IllegalArgumentException("A verdict without an id has no file to go into");
        }
        return VerdictYaml.handover(draft, today());
    }

    /** The day a verdict composed now is dated. */
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private static Authentication requireRole() {
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = caller != null && caller.isAuthenticated() && caller.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(AUTHORITY::equals);
        if (!allowed) {
            throw new AccessDeniedException("The verdict workbench needs the " + Roles.SECURITY + " role");
        }
        return caller;
    }
}
