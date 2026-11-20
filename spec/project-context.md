# Project Context

> High-level context for the project: the problem being solved, who it's for, what's in scope, and what constraints apply.

## 1. Vision

The Income Tax Consultancy File Storage and Distribution application enables secure, role-based access to tax documents. Clients can login with their PAN card number and instantly access documents prepared by the consultancy for their tax filings. This eliminates manual distribution, reduces errors, and provides clients with a centralized repository of all their tax-related documentation. The Admin maintains complete control over document uploads, organization, and client access.

## 2. Users

**Admin:**
- Uploads and manages tax documents (IT Acknowledgement, Financials, Tax Computations, Misc reports)
- Associates documents with specific clients via PAN card
- Maintains client information
- Has full visibility into all documents and clients

**Client:**
- Logs in using their PAN card number as username with a default password
- Accesses only documents associated with their PAN card
- Views documents uploaded by the Admin
- Cannot upload, delete, or modify documents

## 3. Constraints

- **Database:** SQLite with JPA for persistence
- **Authentication:** Simple username (PAN) + password authentication
- **Data access:** Clients see only their own documents; Admin sees all documents
- **No external integrations:** All functionality operates within the application
- **Document metadata:** Each document must be tagged with PAN, Assessment Year, Financial Year, Document Identifier, and Location

---

# Related Documents

- [Spec README](README.md) — process overview and workflow
- [Architecture](architecture.md) — technology stack and application structure
- [Design System](design-system.md) — theme, component usage, and visual standards
- [Use Case Template](use-cases/use-case-template.md) — template for feature specifications
