package com.genai.java.spring.triage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TriageRunRepository extends JpaRepository<TriageRun, Long> {
    // No custom queries needed yet: lookup by id (findById, inherited)
    // and creation (save, inherited) are enough for Phase 3/4.
}