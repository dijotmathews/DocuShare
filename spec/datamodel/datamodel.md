# Data Model

> Entity definitions and relationships. Evolves as features are added.

## Entities

| Entity | Key Fields | Relationships |
|--------|-----------|------------------|
| **User** | id, username (PAN), password, role (ADMIN/CLIENT), createdAt, updatedAt | Has one Client profile (if role=CLIENT) |
| **Client** | id, clientName, panNumber, dateOfIncorporation, address, email, contactNumber, whatsAppNumber, userId | Has one User account; Has many Documents |
| **DownloadAuditEntry** | id, attemptedAt, username, documentId, documentPanNumber, fileName, outcome (SUCCESS/NOT_FOUND/ACCESS_DENIED/READ_ERROR) | Refers to a Document and User by value (no foreign keys), so the trail survives deletions |
| **Document** | id, panNumber, assessmentYear, financialYear, documentIdentifier, location, documentType, uploadedAt, uploadedBy | Belongs to Client (via PAN); Uploaded by Admin User (uploadedBy = User id) |

## Entity Relationships

### User → Client
- A User with role ADMIN has no linked Client profile
- A User with role CLIENT has exactly one linked Client profile (one-to-one)
- Each Client is linked to one User account

### Client → Document
- A Client can have many Documents (one-to-many)
- Each Document is associated with a Client through PAN number
- A Client can only view documents with their PAN number

### User → Document (Upload tracking)
- An Admin User uploads Documents
- Documents store uploadedBy to track which Admin uploaded it

## Field Notes

- **Client.clientName:** mandatory in the form (max 200 characters). The column is nullable so existing databases could add it; clients created before it existed show a blank name until edited.
- **Document.documentIdentifier:** the stored file's name inside `location`. `(location, documentIdentifier)` is unique.
- **Document.location:** folder relative to the document store root (`app.documents.root`), e.g. `ABCDE1234F/2025`.
- **Document.assessmentYear / financialYear:** 4-digit years; the year the period starts in (financialYear 2024 = FY 2024-25, assessmentYear 2025 = AY 2025-26).
- **Document.documentType → group:** each type belongs to one group shown to the client (Filings, Financials, Computations, Misc); the mapping is in code (`DocumentType.group()`), not stored.

## Document Types

Document Identifier can be one of:
- IT_ACKNOWLEDGEMENT
- BALANCE_SHEET
- PROFIT_LOSS
- AUDIT_REPORT
- TAX_AUDIT_REPORT
- TAX_COMPUTATION
- MISC_REPORT

## Storage

- **Database:** SQLite (configured via JPA)
- **File storage:** Files are written to `{app.documents.root}/{location}/{documentIdentifier}` (env `DOCUMENTS_ROOT`, default `documents`)
