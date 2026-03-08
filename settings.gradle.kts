rootProject.name = "foculist"

include(
    "platform:tenancy-core",
    "services:gateway-bff",
    "services:identity-service",
    "services:project-service",
    "services:planning-service",
    "services:calendar-service",
    "services:meeting-service",
    "services:sync-service",
    "services:resource-service"
)
