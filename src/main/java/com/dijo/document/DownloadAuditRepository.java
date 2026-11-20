package com.dijo.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DownloadAuditRepository extends JpaRepository<DownloadAuditEntry, Long> {

    List<DownloadAuditEntry> findByDocumentIdOrderByIdAsc(Long documentId);

    List<DownloadAuditEntry> findByUsernameOrderByIdAsc(String username);
}
