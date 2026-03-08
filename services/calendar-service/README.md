# calendar-service

Architecture: `DDD + Hexagonal`

Responsibilities:
- event scheduling and retrieval
- agenda context for AI meeting flows

Port: `8084`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)

Baseline implementation:
- Hexagonal ports/adapters for `CalendarEvent` and `AgendaContext`
- Tenant-aware persistence tables: `calendar_event`, `calendar_agenda_context`
- Domain validation for date/time parsing and agenda-context upsert by tenant + meeting
