# IgnitionAI Requirements Register

Based on IgnitionAI MVP Implementation Plan v0.2 (September 6, 2026) derived from Founder answers Q01–Q42.

| Req ID | Category | Summary | Verification / Evidence Gate |
|---|---|---|---|
| REQ-01 | Target Market | Pre-owned car dealerships throughout India. | Sales validation in Indian workshop environments. |
| REQ-02 | Primary Workflow | Workshop fault triage -> Post-repair verification -> Acquisition inspection. | End-to-end case flow with explicit pre/post-repair linkage. |
| REQ-03 | Vehicle Scope | BS4 12V passenger cars via classical CAN (11/29-bit, 250/500 kbit/s). | Tested against specific verified vehicle configuration cards. |
| REQ-04 | User Roles | Technician (capture, diagnostic, tests, repairs), Inspector (intake, report reader, comparison), Admin. | RBAC and separate mobile/web user flows. |
| REQ-05 | Hardware Kit | Custom ESP32-S3 12V Bluetooth BLE dongle + Android phone. | Bench peer testing -> Stationary vehicle testing -> Supervised drive. |
| REQ-06 | Drive Protocol | Standard 10-minute drive (idle, low-speed, cruise, acceleration, deceleration, return). | Time-aware window detection; minimum valid data gates. |
| REQ-07 | Command Safety | Firmware & app allowlist: Read-only (Services 01, 02, 03, 06, 07, 09, 0A). No code clears (04) or resets. | Byte fixtures reject prohibited commands at parser and firmware levels. |
| REQ-08 | Diagnostic Hierarchy | Strict L0 (Fact) -> L1 (Subsystem) -> L2 (Component Candidate) -> L3 (Reference Supported) -> L4 (Post-Repair). | Output schema enforces level requirements and alternatives. |
| REQ-09 | Health Score | Observed powertrain condition score only; real issuance disabled until calibrated. | Watermarked `SIMULATED / NOT VALIDATED` in development. |
| REQ-10 | Cloud Infrastructure | Monolith API + Worker; PostgreSQL + Object Store; Multi-region target (Mumbai/Chennai/Bangalore). | Docker Compose development -> Verified deployment runbooks. |
| REQ-11 | Offline Durability | Phone logs append-only encrypted chunks locally; resilient to screen lock, calls, and network loss. | Tested Android foreground service with simulated dropouts. |
| REQ-12 | Report Integrity | Tamper-evident advisory reports with cryptographic digests; explicit advisory disclaimers. | Immutable report manifests; no direct roadworthiness certification. |
