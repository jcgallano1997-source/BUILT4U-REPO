package com.built4u.pos.docsettings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Per-site document settings. Key = site_id. */
@Repository
public interface DocSettingsRepository extends JpaRepository<DocSettings, Long> {
}
