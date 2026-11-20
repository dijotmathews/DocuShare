package com.dijo.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByLocationAndDocumentIdentifier(String location, String documentIdentifier);

    List<Document> findByPanNumber(String panNumber);

    /**
     * All documents with client name and uploader, most recent first
     * (UC-007). Null parameters don't filter. {@code search} must be a
     * lower-case LIKE pattern using '\' as the escape character.
     */
    @Query("""
            select new com.dijo.document.AdminDocumentRow(d.id, d.panNumber, c.clientName, d.documentType,
                d.assessmentYear, d.financialYear, d.uploadedAt, u.username, d.location, d.documentIdentifier)
            from Document d
            join d.uploadedBy u
            left join Client c on c.panNumber = d.panNumber
            where (:search is null
                   or lower(d.panNumber) like :search escape '\\'
                   or lower(coalesce(c.clientName, '')) like :search escape '\\')
              and (:type is null or d.documentType = :type)
              and (:from is null or d.uploadedAt >= :from)
              and (:until is null or d.uploadedAt < :until)
            order by d.uploadedAt desc, d.id desc
            """)
    List<AdminDocumentRow> findAllForAdmin(@Param("search") String search, @Param("type") DocumentType type,
                                           @Param("from") Instant from, @Param("until") Instant until);

    /** A client's documents, most recently uploaded first. */
    List<Document> findByPanNumberOrderByUploadedAtDescIdDesc(String panNumber);
}
