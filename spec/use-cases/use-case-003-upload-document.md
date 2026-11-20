# UC-003: Upload Document

> Admin uploads a tax document and associates it with a client's PAN card.

---

**Goal:** As an Admin, I want to upload a document and tag it with client information so that clients can access their documents securely.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Admin user
- **Secondary actors:** None

---

## Preconditions

- Admin is logged in
- Document file exists and is ready to upload
- Client with the target PAN number exists in the system

---

## Trigger

Admin clicks "Upload Document" on the admin panel.

---

## Main Flow

1. [System] displays the document upload form.
2. [Actor] selects a file from their local system.
3. [Actor] selects the client's PAN from the dropdown of existing clients (searchable by PAN or client name).
4. [Actor] enters the Assessment Year.
5. [Actor] enters the Financial Year.
6. [Actor] selects a Document Type from a dropdown (IT Acknowledgement, Balance Sheet, etc.).
7. [Actor] enters the document storage location/path.
8. [Actor] clicks "Upload" button.
9. [System] validates all required fields are filled.
10. [System] validates that a Client with the entered PAN exists.
11. [System] stores the file at the specified location.
12. [System] creates a Document record in the database with all metadata.
13. [System] displays a success message: "Document uploaded successfully."
14. [System] redirects to the admin dashboard or document list.

---

## Alternative Flows

### AF-1: Invalid PAN Number

**Branches from:** Main Flow step 10
**Condition:** No Client exists with the entered PAN number.

> The form only offers the PANs of existing clients, so this happens only if the client is removed while the form is open; the server still checks.

1. [System] displays an error message: "Client with PAN {panNumber} not found. Please verify the PAN number."
2. [Actor] selects another PAN and resubmits.
3. Returns to Main Flow step 10 or use case ends.

### AF-2: Missing Required Field

**Branches from:** Main Flow step 9
**Condition:** One or more required fields (PAN, Assessment Year, Financial Year, Document Type, Location) are empty.

1. [System] displays a validation error listing all missing fields.
2. [System] highlights the empty field(s) in the form.
3. [Actor] fills in the missing information.
4. Returns to Main Flow step 9.

### AF-3: File Upload Fails

**Branches from:** Main Flow step 11
**Condition:** File cannot be stored at the specified location (disk full, permission denied, etc.).

1. [System] displays an error message: "Failed to upload file. Please check the storage location and try again."
2. [Actor] may verify the location or select a different path.
3. Returns to Main Flow step 7 or use case ends.

### AF-4: File Already Exists

**Branches from:** Main Flow step 11
**Condition:** A file with the same name already exists at the storage location.

1. [System] displays a warning: "A file with this name already exists at the location. Overwrite or choose a different location?"
2. [Actor] chooses to overwrite or change the location.
3. Returns to Main Flow step 7 or Main Flow step 11.

---

## Postconditions

- **On success:** Document is stored on disk, Document record is persisted in the database, and Admin receives confirmation.
- **On failure:** No file is written; no Document record is created; Admin sees error message and can correct the issue.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | All document metadata fields (PAN, Assessment Year, Financial Year, Document Type, Location) are mandatory |
| BR-02 | PAN must correspond to an existing Client in the system |
| BR-03 | Document Type must be one of: IT_ACKNOWLEDGEMENT, BALANCE_SHEET, PROFIT_LOSS, AUDIT_REPORT, TAX_AUDIT_REPORT, TAX_COMPUTATION, MISC_REPORT |
| BR-04 | Only Admin users can upload documents |
| BR-05 | Assessment Year and Financial Year must be valid 4-digit years |

---

## Tests

- [x] Main Flow covered (steps 1–14)
- [x] AF-1 (invalid PAN) covered
- [x] AF-2 (missing fields) covered
- [x] AF-3 (file upload fails) covered
- [x] AF-4 (file already exists) covered
- [x] BR-01, BR-02, BR-03, BR-04, BR-05 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Document Upload Form (`/admin/documents/upload`; "Upload Document" on the dashboard and in the admin header) | Authenticated Admin only |
| All Documents (`/admin/documents`, UC-007), shown with the success message after an upload | Authenticated Admin only |
| Overwrite confirmation (dialog: "Overwrite" or "Change location") | Authenticated Admin only |

**Implementation notes:**
- Location is a folder **relative to the document store** (`app.documents.root`, env `DOCUMENTS_ROOT`, default `documents`). Absolute paths and `..` are rejected so files can't be written elsewhere on the server. The form suggests `{PAN}/{Assessment Year}` until the admin types their own location.
- The file is stored under its original name; that name is the Document's `documentIdentifier`.
- A file is required as well as the metadata fields; empty files are rejected. Maximum size 25 MB.
- Assessment and Financial Year accept 4-digit years from 1900 up to next year.
- PAN is a dropdown (combo box) sourced from the client list. Each option shows the PAN with the client name below it; typing filters by PAN or client name, ignoring case; values that aren't an existing client can't be entered. The selected client's name is shown under the field so the admin can check it.
