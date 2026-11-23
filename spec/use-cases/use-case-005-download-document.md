# UC-005: Download Document

> Client downloads a document from their document list.

---

**Goal:** As a Client, I want to download a document so that I can save it to my device or review it offline.

**Status:** Implemented
**Date:** 2024-01-15

---

## Actors

- **Primary actor:** Client user
- **Secondary actors:** None

---

## Preconditions

- Client is logged in
- Client is viewing their document list (UC-004)
- At least one document is visible in the list

---

## Trigger

Client clicks the "Download" button next to a document in their document list.

---

## Main Flow

1. [Actor] clicks the "Download" button next to a document.
2. [System] verifies that the document belongs to the logged-in client's PAN number.
3. [System] retrieves the document from the storage location.
4. [System] initiates a file download with the original filename.
5. [System] logs the download action for audit purposes.

---

## Alternative Flows

### AF-1: Document Not Found

**Branches from:** Main Flow step 3
**Condition:** The document file no longer exists at the stored location.

1. [System] displays an error message: "Document could not be found. Please contact your consultant."
2. Use case ends.

### AF-2: Unauthorized Access Attempt

**Branches from:** Main Flow step 2
**Condition:** A user attempts to download a document that does not belong to their PAN number.

1. [System] denies the access and logs a security event.
2. [System] displays an error message: "You do not have permission to access this document."
3. Use case ends.

### AF-3: File Read Error

**Branches from:** Main Flow step 3
**Condition:** The file exists but cannot be read due to a permission or I/O error.

1. [System] displays an error message: "Unable to download document. Please try again or contact support."
2. [System] logs the error for technical review.
3. Use case ends.

### AF-4: Print Document

**Branches from:** Main Flow step 1
**Condition:** The client clicks "Print" (right next to "Download") instead of "Download". Shown only for files the browser can display: PDF, PNG, JPEG, GIF, WebP and plain text (BR-05).

1. [System] verifies ownership and retrieves the document as in steps 2–3 (AF-1 to AF-3 apply, with the same messages).
2. [System] sends the file for display rather than saving (`Content-Disposition: inline`) and logs the access as in step 5.
3. [System] opens the browser's print dialog for the document, staying on My Documents. On Safari and touch devices, or when a modifier key is held, the document opens in a new tab instead, to print from there.
4. Use case ends.

---

## Postconditions

- **On success:** Document is downloaded to the client's device; download is logged.
- **On failure:** No file is transferred; appropriate error message is displayed; security event may be logged.

---

## Business Rules

| ID | Rule |
|----|------|
| BR-01 | A client can only download documents associated with their own PAN number |
| BR-02 | All download attempts must be logged for audit and security purposes |
| BR-03 | Downloaded files must retain their original metadata and naming conventions |
| BR-04 | Only authenticated clients can initiate downloads |
| BR-05 | Printing is offered only for file types a browser can display; it goes through the same checks and audit as a download |

---

## Tests

- [x] Main Flow covered (steps 1–5)
- [x] AF-1 (document not found) covered
- [x] AF-2 (unauthorized access attempt) covered
- [x] AF-3 (file read error) covered
- [x] AF-4 (print) covered
- [x] BR-01, BR-02, BR-03, BR-04, BR-05 covered

---

## UI Surface

| Page | Access |
|------|--------|
| Client Dashboard / My Documents page (`/documents`) | Authenticated Client only |
| My Documents (side nav of groups, documents by financial year, UC-004) with Download and Print links on every document, Print right after Download (both icon only on phones) | Authenticated Client only |
| Error notification for failed downloads (top of the page) | Authenticated Client only |

**Implementation notes:**
- Each Download link is bound to the signed-in session's page; the server still checks the PAN on every download (BR-01), so a guessed or replayed link can't fetch another client's file.
- A request for a document id that doesn't exist is treated like AF-2 (same message), so ids can't be probed.
- Every attempt is stored in the `download_audit` table with time, user, document id, document PAN, file name and outcome (SUCCESS, NOT_FOUND, ACCESS_DENIED, READ_ERROR). Denied attempts are also written to the `com.dijo.security.audit` log at WARN; read errors are logged at ERROR with the cause (AF-3 step 2).
- Users who aren't signed-in clients (admins, anonymous) are rejected by method security before reaching the download and don't create audit rows; they can't see the Download links in the UI.
- Print fetches the file and shows it in a hidden frame from a blob URL, then calls the frame's `print()`. A blob URL is used because the app sends `X-Frame-Options: DENY`, which stops its own responses from being framed. A print is recorded in `download_audit` exactly like a download (there is no separate outcome for it).
- The file keeps its original name; the content type is detected from the file (unknown types are sent as `application/octet-stream`).
