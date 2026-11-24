package com.vaadin.securitystatus.team.workbench;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** The saved drafts. Reached only through {@link VerdictDrafts}, which checks the role. */
public interface VerdictDraftRepository extends JpaRepository<VerdictDraft, Long> {

    Optional<VerdictDraft> findByPair(String pair);

    List<VerdictDraft> findAllByOrderByUpdatedAtDesc();
}
