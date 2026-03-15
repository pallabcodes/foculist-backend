package com.yourorg.platform.foculist.identity.clean.infrastructure.bootstrap;

import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.MembershipRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.OrganizationRepository;
import com.yourorg.platform.foculist.identity.clean.adapter.out.persistence.UserRepository;
import com.yourorg.platform.foculist.identity.clean.domain.model.Membership;
import com.yourorg.platform.foculist.identity.clean.domain.model.MembershipRole;
import com.yourorg.platform.foculist.identity.clean.domain.model.Organization;
import com.yourorg.platform.foculist.identity.clean.domain.model.User;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.sql.Timestamp;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final MongoTemplate mongoTemplate;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() > 0) {
            log.info("Database already initialized. Skipping bootstrap seeding.");
            return;
        }

        log.info("🚀 Initializing V1 Beta — Sovereign Data Scaffolding...");

        Instant now = Instant.now();
        Timestamp tsNow = Timestamp.from(now);

        // ============================================================
        // PHASE 1: ORGANIZATIONS
        // ============================================================
        Organization systemOrg = createOrg("Foculist Global", "foculist", "public", "ENTERPRISE",
                Map.of("features", Map.of("sso_enabled", true, "custom_domains", true), "region", "global-system"));

        Organization apolloOrg = createOrg("Project Apollo", "apollo-team", "apollo-team", "PRO",
                Map.of("context", "space-exploration-unit", "max_members", 50));

        // ============================================================
        // PHASE 2: USERS (6 users covering all L5 archetypes)
        // ============================================================
        User admin = createUser("admin@foculist.com", "Platform Administrator", "admin123", "public", "ADMIN");
        User lead = createUser("lead@apollo.team", "Commander Rover", "apollo123", "apollo-team", "USER");
        User dev1 = createUser("dev1@apollo.team", "Elena Vasquez", "apollo123", "apollo-team", "USER");
        User dev2 = createUser("dev2@apollo.team", "Kai Nakamura", "apollo123", "apollo-team", "USER");
        User qa = createUser("qa@apollo.team", "Priya Sharma", "apollo123", "apollo-team", "USER");
        User viewer = createUser("viewer@apollo.team", "Jordan Chen", "apollo123", "apollo-team", "USER");

        // ============================================================
        // PHASE 3: MEMBERSHIPS
        // ============================================================
        createMembership(systemOrg, admin, "public", MembershipRole.OWNER);
        createMembership(apolloOrg, lead, "apollo-team", MembershipRole.ADMIN);
        createMembership(apolloOrg, dev1, "apollo-team", MembershipRole.MEMBER);
        createMembership(apolloOrg, dev2, "apollo-team", MembershipRole.MEMBER);
        createMembership(apolloOrg, qa, "apollo-team", MembershipRole.MEMBER);
        createMembership(apolloOrg, viewer, "apollo-team", MembershipRole.GUEST);

        // Flush JPA entities to DB so JDBC FK references resolve correctly
        entityManager.flush();

        // ============================================================
        // PHASE 4: PERMISSIONS & ROLES (Identity V9)
        // ============================================================
        log.info("🔐 Seeding IAM Permissions & Roles...");

        String tenant = "apollo-team";

        // Permissions
        Map<String, UUID> perms = new HashMap<>();
        String[][] permDefs = {
            {"project:read",    "View Projects",        "PROJECT"},
            {"project:write",   "Edit Projects",        "PROJECT"},
            {"project:admin",   "Manage Projects",      "PROJECT"},
            {"sprint:read",     "View Sprints",         "PLANNING"},
            {"sprint:write",    "Edit Sprints",         "PLANNING"},
            {"sprint:admin",    "Manage Sprints",       "PLANNING"},
            {"task:read",       "View Tasks",           "PLANNING"},
            {"task:write",      "Edit Tasks",           "PLANNING"},
            {"task:assign",     "Assign Tasks",         "PLANNING"},
            {"task:admin",      "Manage Tasks",         "PLANNING"},
            {"comment:write",   "Write Comments",       "PLANNING"},
            {"comment:delete",  "Delete Comments",      "PLANNING"},
            {"member:read",     "View Members",         "IDENTITY"},
            {"member:invite",   "Invite Members",       "IDENTITY"},
            {"member:admin",    "Manage Members",       "IDENTITY"},
            {"label:write",     "Manage Labels",        "PLANNING"},
            {"settings:read",   "View Settings",        "SYSTEM"},
            {"settings:admin",  "Manage Settings",      "SYSTEM"},
        };
        for (String[] p : permDefs) {
            UUID pid = UUID.randomUUID();
            perms.put(p[0], pid);
            jdbcTemplate.update("""
                INSERT INTO identity.permissions (id, tenant_id, code, name, category, created_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """, pid, tenant, p[0], p[1], p[2], "system-bootstrap");
        }

        // Roles
        UUID ownerRoleId = seedRole(tenant, "OWNER", "Owner", "Full platform control", true);
        UUID adminRoleId = seedRole(tenant, "ADMIN", "Administrator", "Team and project management", true);
        UUID memberRoleId = seedRole(tenant, "MEMBER", "Member", "Standard contributor", true);
        UUID viewerRoleId = seedRole(tenant, "VIEWER", "Viewer", "Read-only access", true);

        // Role-Permission mappings
        // Owner gets everything
        for (UUID pid : perms.values()) {
            seedRolePerm(ownerRoleId, pid);
        }
        // Admin gets everything except settings:admin
        for (Map.Entry<String, UUID> e : perms.entrySet()) {
            if (!e.getKey().equals("settings:admin")) {
                seedRolePerm(adminRoleId, e.getValue());
            }
        }
        // Member gets read + write + comment + assign
        for (Map.Entry<String, UUID> e : perms.entrySet()) {
            String code = e.getKey();
            if (code.endsWith(":read") || code.endsWith(":write") || code.equals("comment:write") || code.equals("task:assign")) {
                seedRolePerm(memberRoleId, e.getValue());
            }
        }
        // Viewer gets all reads
        for (Map.Entry<String, UUID> e : perms.entrySet()) {
            if (e.getKey().endsWith(":read")) {
                seedRolePerm(viewerRoleId, e.getValue());
            }
        }

        // Assign roles to users
        seedUserRole(lead.getId(), adminRoleId, tenant);
        seedUserRole(dev1.getId(), memberRoleId, tenant);
        seedUserRole(dev2.getId(), memberRoleId, tenant);
        seedUserRole(qa.getId(), memberRoleId, tenant);
        seedUserRole(viewer.getId(), viewerRoleId, tenant);

        // User Groups
        UUID engGroup = seedUserGroup(tenant, "Engineering", "Core engineering team");
        UUID qaGroup = seedUserGroup(tenant, "Quality Assurance", "QA and testing team");
        seedGroupMember(engGroup, dev1.getId());
        seedGroupMember(engGroup, dev2.getId());
        seedGroupMember(engGroup, lead.getId());
        seedGroupMember(qaGroup, qa.getId());

        // ============================================================
        // PHASE 5: PROJECTS (Project Service)
        // ============================================================
        log.info("📡 Seeding Projects...");

        UUID marsProjectId = UUID.randomUUID();
        UUID osaProjectId = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO project.project_item (id, tenant_id, name, description, status, priority, due_date, created_at, updated_at, version, created_by, metadata, owner_id, key)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """,
            marsProjectId, tenant, "Mars Mission 2026", "Establish first permanent colony on Mars. Full mission lifecycle from launch to landing.",
            "RUNNING", "HIGH", LocalDate.now().plusYears(2), tsNow, tsNow, 0, "system-bootstrap",
            "{\"priority_sector\": \"aerospace\", \"classification\": \"TOP_SECRET\"}", lead.getId(), "MARS"
        );

        jdbcTemplate.update("""
            INSERT INTO project.project_settings (project_id, tenant_id, default_view, updated_at, created_at, created_by, version, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """, marsProjectId, tenant, "BOARD", tsNow, tsNow, "system-bootstrap", 0, "{}");

        jdbcTemplate.update("""
            INSERT INTO project.project_item (id, tenant_id, name, description, status, priority, due_date, created_at, updated_at, version, created_by, metadata, owner_id, key)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
            """,
            osaProjectId, tenant, "Orbital Station Alpha", "Design and deploy a modular orbital research station.",
            "PLANNING", "MEDIUM", LocalDate.now().plusYears(3), tsNow, tsNow, 0, "system-bootstrap",
            "{\"priority_sector\": \"orbital_ops\"}", lead.getId(), "OSA"
        );

        jdbcTemplate.update("""
            INSERT INTO project.project_settings (project_id, tenant_id, default_view, updated_at, created_at, created_by, version, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """, osaProjectId, tenant, "LIST", tsNow, tsNow, "system-bootstrap", 0, "{}");

        // Project Members
        seedProjectMember(tenant, marsProjectId, lead.getId(), "OWNER");
        seedProjectMember(tenant, marsProjectId, dev1.getId(), "MEMBER");
        seedProjectMember(tenant, marsProjectId, dev2.getId(), "MEMBER");
        seedProjectMember(tenant, marsProjectId, qa.getId(), "MEMBER");
        seedProjectMember(tenant, marsProjectId, viewer.getId(), "VIEWER");
        seedProjectMember(tenant, osaProjectId, lead.getId(), "OWNER");
        seedProjectMember(tenant, osaProjectId, dev2.getId(), "MEMBER");

        // ============================================================
        // PHASE 6: SPRINTS (Planning Service)
        // ============================================================
        log.info("🏃 Seeding Sprints...");

        UUID sprint1Id = UUID.randomUUID();
        UUID sprint2Id = UUID.randomUUID();
        UUID sprint3Id = UUID.randomUUID();

        seedSprint(sprint1Id, tenant, "Sprint 1: Liftoff", "ACTIVE", now, Instant.now().plusSeconds(86400 * 14), tsNow, "{\"sprint_goal\": \"Validate orbital trajectory and engine readiness\"}");
        seedSprint(sprint2Id, tenant, "Sprint 2: Orbital Insertion", "PLANNED", Instant.now().plusSeconds(86400 * 15), Instant.now().plusSeconds(86400 * 28), tsNow, "{\"sprint_goal\": \"Achieve stable orbit and deploy comms array\"}");
        seedSprint(sprint3Id, tenant, "Sprint 3: Deep Space", "PLANNED", Instant.now().plusSeconds(86400 * 29), Instant.now().plusSeconds(86400 * 42), tsNow, "{\"sprint_goal\": \"Long-range navigation and life support stress tests\"}");

        // ============================================================
        // PHASE 7: TASKS (8 tasks across sprints)
        // ============================================================
        log.info("📋 Seeding Tasks...");

        UUID t1 = seedTask(tenant, sprint1Id, "Finalize Engine Diagnostics", "Perform 10x redundancy check on Raptor engines.", "IN_PROGRESS", "CRITICAL", dev1.getId(), lead.getId(), 8, "MARS-1", tsNow);
        UUID t2 = seedTask(tenant, sprint1Id, "Navigation System Calibration", "Calibrate star-tracker and inertial nav systems for deep space.", "TODO", "HIGH", dev2.getId(), lead.getId(), 5, "MARS-2", tsNow);
        UUID t3 = seedTask(tenant, sprint1Id, "Life Support Testing", "Run full-duration life support tests in simulated Mars atmosphere.", "IN_PROGRESS", "HIGH", qa.getId(), lead.getId(), 13, "MARS-3", tsNow);
        UUID t4 = seedTask(tenant, sprint1Id, "Communication Array Setup", "Deploy and test high-gain antenna for Earth-Mars relay.", "DONE", "MEDIUM", dev1.getId(), lead.getId(), 3, "MARS-4", tsNow);
        UUID t5 = seedTask(tenant, sprint2Id, "Thermal Shield Analysis", "Model re-entry thermal profiles for Martian atmosphere.", "TODO", "HIGH", dev2.getId(), lead.getId(), 8, "MARS-5", tsNow);
        UUID t6 = seedTask(tenant, sprint2Id, "Fuel Cell Efficiency Report", "Document fuel cell performance under sustained load.", "TODO", "MEDIUM", dev1.getId(), lead.getId(), 5, "MARS-6", tsNow);
        UUID t7 = seedTask(tenant, sprint3Id, "Crew Rotation Protocol", "Design shift schedules for 6-month deep space transit.", "TODO", "LOW", qa.getId(), lead.getId(), 3, "MARS-7", tsNow);
        UUID t8 = seedTask(tenant, null, "Research: Radiation Shielding", "Investigate novel materials for cosmic radiation protection.", "BACKLOG", "MEDIUM", null, lead.getId(), 13, "MARS-8", tsNow);

        // ============================================================
        // PHASE 8: LABELS
        // ============================================================
        log.info("🏷️ Seeding Labels...");

        UUID bugLabel = seedLabel(tenant, "Bug", "#EF4444", "Defects and issues");
        UUID featureLabel = seedLabel(tenant, "Feature", "#3B82F6", "New functionality");
        UUID urgentLabel = seedLabel(tenant, "Urgent", "#F97316", "Time-sensitive items");
        UUID researchLabel = seedLabel(tenant, "Research", "#8B5CF6", "Investigation and exploration");
        UUID blockedLabel = seedLabel(tenant, "Blocked", "#DC2626", "Waiting on external dependency");

        // Assign labels to tasks
        seedTaskLabel(t1, urgentLabel);
        seedTaskLabel(t3, featureLabel);
        seedTaskLabel(t4, featureLabel);
        seedTaskLabel(t5, researchLabel);
        seedTaskLabel(t8, researchLabel);
        seedTaskLabel(t8, blockedLabel);

        // ============================================================
        // PHASE 9: TASK COMMENTS
        // ============================================================
        log.info("💬 Seeding Comments...");

        UUID c1 = seedComment(tenant, t1, null, lead.getId(), "Engine test results from batch 7 look nominal. Proceeding with final certification run.", tsNow);
        seedComment(tenant, t1, c1, dev1.getId(), "Roger. Running batch 8 now. ETA 4 hours for full telemetry analysis.", tsNow);
        seedComment(tenant, t3, null, qa.getId(), "Life support module 3 shows 0.02% O2 variance. Within tolerance but flagging for review.", tsNow);
        seedComment(tenant, t4, null, dev1.getId(), "Array deployed and verified. Signal strength nominal at 47dBm. Marking as DONE.", tsNow);

        // ============================================================
        // PHASE 10: TASK ASSIGNEES & WATCHERS
        // ============================================================
        seedTaskAssignee(t1, dev1.getId());
        seedTaskAssignee(t1, dev2.getId()); // co-assigned
        seedTaskAssignee(t3, qa.getId());
        seedTaskAssignee(t5, dev2.getId());

        seedTaskWatcher(t1, lead.getId());
        seedTaskWatcher(t1, qa.getId());
        seedTaskWatcher(t3, lead.getId());
        seedTaskWatcher(t5, lead.getId());

        // ============================================================
        // PHASE 11: MONGODB — PLANNING EVENTS
        // ============================================================
        log.info("🍃 Bootstrapping MongoDB event store...");

        seedMongoEvent(t1, "TASK_CREATED", dev1.getId(), Map.of("title", "Finalize Engine Diagnostics", "priority", "CRITICAL"), now);
        seedMongoEvent(t2, "TASK_CREATED", dev2.getId(), Map.of("title", "Navigation System Calibration", "priority", "HIGH"), now);
        seedMongoEvent(t4, "TASK_STATUS_CHANGED", dev1.getId(), Map.of("from", "IN_PROGRESS", "to", "DONE"), now.plusSeconds(3600));
        seedMongoEvent(t1, "COMMENT_ADDED", lead.getId(), Map.of("body", "Engine test results from batch 7 look nominal."), now.plusSeconds(7200));

        log.info("✅ V1 Beta seeding complete. {} users, {} orgs, {} projects, {} sprints, {} tasks.",
                6, 2, 2, 3, 8);
    }

    // ── Helper Methods ──────────────────────────────────────────────────

    private Organization createOrg(String name, String slug, String tenantId, String tier, Map<String, Object> metadata) {
        Organization org = new Organization();
        org.setName(name);
        org.setSlug(slug);
        org.setTenantId(tenantId);
        org.setTier(tier);
        org.setCreatedBy("system-bootstrap");
        org.setMetadata(metadata);
        return organizationRepository.save(org);
    }

    private User createUser(String email, String fullName, String password, String tenantId, String globalRole) {
        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setTenantId(tenantId);
        user.setGlobalRole(globalRole);
        user.setCreatedBy("system-bootstrap");
        user.setMetadata(Map.of("onboarded", true));
        return userRepository.save(user);
    }

    private void createMembership(Organization org, User user, String tenantId, MembershipRole role) {
        Membership m = new Membership();
        m.setOrganization(org);
        m.setUser(user);
        m.setTenantId(tenantId);
        m.setRole(role);
        m.setStatus("ACTIVE");
        m.setCreatedBy("system-bootstrap");
        membershipRepository.save(m);
    }

    private UUID seedRole(String tenant, String code, String name, String desc, boolean isSystem) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO identity.roles (id, tenant_id, code, name, description, is_system, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """, id, tenant, code, name, desc, isSystem, "system-bootstrap");
        return id;
    }

    private void seedRolePerm(UUID roleId, UUID permId) {
        jdbcTemplate.update("INSERT INTO identity.role_permissions (role_id, permission_id, granted_by) VALUES (?, ?, ?)",
                roleId, permId, "system-bootstrap");
    }

    private void seedUserRole(UUID userId, UUID roleId, String tenant) {
        jdbcTemplate.update("INSERT INTO identity.user_roles (user_id, role_id, tenant_id, assigned_by) VALUES (?, ?, ?, ?)",
                userId, roleId, tenant, "system-bootstrap");
    }

    private UUID seedUserGroup(String tenant, String name, String desc) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO identity.user_groups (id, tenant_id, name, description, created_by)
            VALUES (?, ?, ?, ?, ?)
            """, id, tenant, name, desc, "system-bootstrap");
        return id;
    }

    private void seedGroupMember(UUID groupId, UUID userId) {
        jdbcTemplate.update("INSERT INTO identity.user_group_members (group_id, user_id, joined_by) VALUES (?, ?, ?)",
                groupId, userId, "system-bootstrap");
    }

    private void seedProjectMember(String tenant, UUID projectId, UUID userId, String role) {
        jdbcTemplate.update("""
            INSERT INTO project.project_member (id, tenant_id, project_id, user_id, role, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """, UUID.randomUUID(), tenant, projectId, userId, role, "system-bootstrap");
    }

    private void seedSprint(UUID id, String tenant, String name, String status, Instant start, Instant end, Timestamp ts, String meta) {
        jdbcTemplate.update("""
            INSERT INTO planning.planning_sprint (id, tenant_id, name, status, start_date, end_date, created_at, updated_at, version, created_by, metadata)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            """, id, tenant, name, status, Timestamp.from(start), Timestamp.from(end), ts, ts, 0, "system-bootstrap", meta);
    }

    private UUID seedTask(String tenant, UUID sprintId, String title, String desc, String status, String priority,
                          UUID assigneeId, UUID reporterId, int storyPoints, String taskKey, Timestamp ts) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO planning.planning_task (id, tenant_id, sprint_id, title, description, status, priority, created_at, updated_at, version, created_by, metadata, assignee_id, reporter_id, story_points, task_key)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
            """, id, tenant, sprintId, title, desc, status, priority, ts, ts, 0, "system-bootstrap", "{}", assigneeId, reporterId, storyPoints, taskKey);
        return id;
    }

    private UUID seedLabel(String tenant, String name, String color, String desc) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO planning.planning_label (id, tenant_id, name, color, description, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """, id, tenant, name, color, desc, "system-bootstrap");
        return id;
    }

    private void seedTaskLabel(UUID taskId, UUID labelId) {
        jdbcTemplate.update("INSERT INTO planning.planning_task_label (task_id, label_id, added_by) VALUES (?, ?, ?)",
                taskId, labelId, "system-bootstrap");
    }

    private UUID seedComment(String tenant, UUID taskId, UUID parentId, UUID authorId, String body, Timestamp ts) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
            INSERT INTO planning.planning_task_comment (id, tenant_id, task_id, parent_id, author_id, body, created_at, updated_at, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, id, tenant, taskId, parentId, authorId, body, ts, ts, "system-bootstrap");
        return id;
    }

    private void seedTaskAssignee(UUID taskId, UUID userId) {
        jdbcTemplate.update("INSERT INTO planning.planning_task_assignee (task_id, user_id, assigned_by) VALUES (?, ?, ?)",
                taskId, userId, "system-bootstrap");
    }

    private void seedTaskWatcher(UUID taskId, UUID userId) {
        jdbcTemplate.update("INSERT INTO planning.planning_task_watcher (task_id, user_id) VALUES (?, ?)", taskId, userId);
    }

    private void seedMongoEvent(UUID taskId, String eventType, UUID userId, Map<String, Object> data, Instant timestamp) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("streamId", taskId.toString());
        doc.put("eventType", eventType);
        doc.put("metadata", Map.of("userId", userId.toString()));
        doc.put("data", data);
        doc.put("timestamp", timestamp);
        mongoTemplate.save(doc, "planning_events");
    }
}
