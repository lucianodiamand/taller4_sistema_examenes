package com.exam_system.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.exam_system.exam.domain.AttemptQuestion;
import com.exam_system.exam.domain.AttemptStatus;
import com.exam_system.exam.domain.Exam;
import com.exam_system.exam.domain.ExamAttempt;
import com.exam_system.exam.domain.ExamCall;
import com.exam_system.exam.domain.Question;
import com.exam_system.exam.domain.QuestionType;
import com.exam_system.exam.repository.ExamAttemptRepository;
import com.exam_system.exam.repository.ExamCallRepository;
import com.exam_system.exam.repository.ExamRepository;
import com.exam_system.exam.repository.QuestionRepository;
import com.exam_system.user.domain.Permission;
import com.exam_system.user.domain.Role;
import com.exam_system.user.domain.User;
import com.exam_system.user.repository.PermissionRepository;
import com.exam_system.user.repository.RoleRepository;
import com.exam_system.user.repository.UserRepository;

@Configuration
public class BootstrapData {

    private static final Logger logger = LoggerFactory.getLogger(BootstrapData.class);
    private static final String DEMO_EXAM_TITLE = "[DEMO] Simulacro de Programacion IV";
    private static final LocalDateTime DEMO_OPEN_CALL_START = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDateTime DEMO_OPEN_CALL_END = LocalDateTime.of(2099, 12, 31, 23, 59);
    private static final LocalDateTime DEMO_HISTORY_CALL_START = LocalDateTime.of(2024, 2, 1, 8, 0);
    private static final LocalDateTime DEMO_HISTORY_CALL_END = LocalDateTime.of(2024, 2, 15, 18, 0);
    private static final LocalDateTime DEMO_HISTORY_ATTEMPT_STARTED_AT = LocalDateTime.of(2024, 2, 10, 9, 0);
    private static final LocalDateTime DEMO_HISTORY_ATTEMPT_SUBMITTED_AT = LocalDateTime.of(2024, 2, 10, 9, 40);

    @Bean
    public ApplicationRunner adminSeeder(BootstrapProperties bootstrapProperties,
                                         UserRepository userRepository,
                                         RoleRepository roleRepository,
                                         PermissionRepository permissionRepository,
                                         ExamRepository examRepository,
                                         QuestionRepository questionRepository,
                                         ExamCallRepository examCallRepository,
                                         ExamAttemptRepository examAttemptRepository,
                                         PasswordEncoder passwordEncoder) {
        return args -> {
            seedRolesAndPermissions(roleRepository, permissionRepository);
            seedLocalTestUsers(bootstrapProperties, userRepository, roleRepository, passwordEncoder);
            seedStudentDemoData(bootstrapProperties,
                    userRepository,
                    examRepository,
                    questionRepository,
                    examCallRepository,
                    examAttemptRepository);

            if (!bootstrapProperties.isSeedAdmin()) {
                return;
            }

            if (userRepository.existsByUsername(bootstrapProperties.getAdminUsername())) {
                return;
            }

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

            User admin = new User();
            admin.setName(bootstrapProperties.getAdminName());
            admin.setUsername(bootstrapProperties.getAdminUsername());
            admin.setPassword(passwordEncoder.encode(bootstrapProperties.getAdminPassword()));
            admin.setRole(adminRole);
            userRepository.save(admin);

            logger.info("Seeded default admin user {}", bootstrapProperties.getAdminUsername());
        };
    }

    private void seedLocalTestUsers(BootstrapProperties bootstrapProperties,
                                    UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    PasswordEncoder passwordEncoder) {
        if (!bootstrapProperties.isSeedLocalTestUsers()) {
            return;
        }

        Role professorRole = roleRepository.findByName("PROFESSOR")
                .orElseThrow(() -> new IllegalStateException("PROFESSOR role not found"));
        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        seedUserIfMissing(
                userRepository,
                passwordEncoder,
                professorRole,
                bootstrapProperties.getProfessorName(),
                bootstrapProperties.getProfessorUsername(),
                bootstrapProperties.getProfessorPassword()
        );
        seedUserIfMissing(
                userRepository,
                passwordEncoder,
                studentRole,
                bootstrapProperties.getStudentName(),
                bootstrapProperties.getStudentUsername(),
                bootstrapProperties.getStudentPassword()
        );
    }

    private void seedUserIfMissing(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   Role role,
                                   String name,
                                   String username,
                                   String password) {
        if (userRepository.existsByUsername(username)) {
            return;
        }

        User user = new User();
        user.setName(name);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        userRepository.save(user);
        logger.info("Seeded local test user {} ({})", username, role.getName());
    }

    private void seedStudentDemoData(BootstrapProperties bootstrapProperties,
                                     UserRepository userRepository,
                                     ExamRepository examRepository,
                                     QuestionRepository questionRepository,
                                     ExamCallRepository examCallRepository,
                                     ExamAttemptRepository examAttemptRepository) {
        if (!bootstrapProperties.isSeedLocalTestUsers()) {
            return;
        }

        User professor = userRepository.findByUsername(bootstrapProperties.getProfessorUsername()).orElse(null);
        User student = userRepository.findByUsername(bootstrapProperties.getStudentUsername()).orElse(null);
        if (professor == null || student == null) {
            logger.warn("Skipped demo exam seed: professor or student test user missing");
            return;
        }

        Exam demoExam = findOrCreateDemoExam(examRepository, professor);
        List<Question> demoQuestions = findOrCreateDemoQuestions(questionRepository, demoExam);

        findOrCreateExamCall(
                examCallRepository,
                demoExam,
                DEMO_OPEN_CALL_START,
                DEMO_OPEN_CALL_END,
                30,
                0
        );

        ExamCall historyCall = findOrCreateExamCall(
                examCallRepository,
                demoExam,
                DEMO_HISTORY_CALL_START,
                DEMO_HISTORY_CALL_END,
                30,
                1
        );

        if (examAttemptRepository.findByExamCallIdAndStudentId(historyCall.getId(), student.getId()).isPresent()) {
            return;
        }

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamCall(historyCall);
        attempt.setStudent(student);
        attempt.setStartedAt(DEMO_HISTORY_ATTEMPT_STARTED_AT);
        attempt.setSubmittedAt(DEMO_HISTORY_ATTEMPT_SUBMITTED_AT);
        attempt.setStatus(AttemptStatus.GRADED);

        BigDecimal finalScore = BigDecimal.ZERO;
        for (int index = 0; index < demoQuestions.size(); index++) {
            Question question = demoQuestions.get(index);

            AttemptQuestion attemptQuestion = new AttemptQuestion();
            attemptQuestion.setAttempt(attempt);
            attemptQuestion.setQuestion(question);
            attemptQuestion.setQuestionOrder(index + 1);

            String answerText;
            switch (index) {
                case 0:
                    answerText = "Autenticacion confirma quien sos con credenciales o token; autorizacion define que acciones permite tu rol.";
                    break;
                case 1:
                    answerText = "Verdadero. JWT se envia en Authorization con esquema Bearer.";
                    break;
                default:
                    answerText = "Validar token y rol en cada endpoint, y aplicar minimo privilegio por permisos especificos.";
                    break;
            }
            attemptQuestion.setAnswerText(answerText);

            Long points = question.getPoints() == null ? 0L : Long.valueOf(question.getPoints());
            BigDecimal awardedScore = index == demoQuestions.size() - 1
                    ? BigDecimal.valueOf(Math.max(points - 1L, 0L))
                    : BigDecimal.valueOf(points);
            attemptQuestion.setAwardedScore(awardedScore);
            attemptQuestion.setReviewComment(index == 0
                    ? "Definicion clara y correcta"
                    : index == 1
                    ? "Correcto"
                    : "Bien encaminado, faltaron ejemplos concretos de implementacion");

            finalScore = finalScore.add(awardedScore);
            attempt.getQuestions().add(attemptQuestion);
        }

        attempt.setFinalScore(finalScore);
        examAttemptRepository.save(attempt);
        logger.info("Seeded demo graded attempt for student {}", student.getUsername());
    }

    private Exam findOrCreateDemoExam(ExamRepository examRepository, User professor) {
        return examRepository.findByProfessorId(professor.getId()).stream()
                .filter(exam -> DEMO_EXAM_TITLE.equals(exam.getTitle()))
                .findFirst()
                .orElseGet(() -> {
                    Exam exam = new Exam();
                    exam.setTitle(DEMO_EXAM_TITLE);
                    exam.setDescription("Examen demo para flujo E2E de estudiante");
                    exam.setDurationMinutes(60);
                    exam.setProfessor(professor);
                    return examRepository.save(exam);
                });
    }

    private List<Question> findOrCreateDemoQuestions(QuestionRepository questionRepository, Exam exam) {
        List<Question> existing = questionRepository.findByExamIdOrderByIdAsc(exam.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        Question first = new Question();
        first.setExam(exam);
        first.setStatement("Explica diferencia entre autenticacion y autorizacion");
        first.setType(QuestionType.OPEN);
        first.setPoints(4);

        Question second = new Question();
        second.setExam(exam);
        second.setStatement("JWT se envia en header Authorization Bearer: verdadero o falso");
        second.setType(QuestionType.TRUE_FALSE);
        second.setPoints(3);

        Question third = new Question();
        third.setExam(exam);
        third.setStatement("Menciona dos buenas practicas para proteger endpoints REST");
        third.setType(QuestionType.OPEN);
        third.setPoints(3);

        questionRepository.saveAll(List.of(first, second, third));
        return questionRepository.findByExamIdOrderByIdAsc(exam.getId());
    }

    private ExamCall findOrCreateExamCall(ExamCallRepository examCallRepository,
                                          Exam exam,
                                          LocalDateTime startDate,
                                          LocalDateTime endDate,
                                          Integer totalCapacity,
                                          Integer currentEnrollment) {
        return examCallRepository.findAll().stream()
                .filter(call -> call.getExam() != null
                        && exam.getId().equals(call.getExam().getId())
                        && startDate.equals(call.getStartDate())
                        && endDate.equals(call.getEndDate()))
                .findFirst()
                .orElseGet(() -> {
                    ExamCall examCall = new ExamCall();
                    examCall.setExam(exam);
                    examCall.setStartDate(startDate);
                    examCall.setEndDate(endDate);
                    examCall.setTotalCapacity(totalCapacity);
                    examCall.setCurrentEnrollment(currentEnrollment);
                    return examCallRepository.save(examCall);
                });
    }

    private void seedRolesAndPermissions(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        Role admin = ensureRole(roleRepository, "ADMIN", "System administrator");
        Role professor = ensureRole(roleRepository, "PROFESSOR", "Professor");
        Role student = ensureRole(roleRepository, "STUDENT", "Student");

        Map<String, String> permissionCatalog = new LinkedHashMap<>();
        permissionCatalog.put("permissions.manage", "Manage role permissions");
        permissionCatalog.put("users.read.any", "Read any user profile");
        permissionCatalog.put("users.read.self", "Read own profile");
        permissionCatalog.put("users.update.self", "Update own profile");
        permissionCatalog.put("users.create.any", "Create users as admin");
        permissionCatalog.put("users.update.any", "Update users as admin");
        permissionCatalog.put("users.delete.any", "Delete users as admin");
        permissionCatalog.put("users.create.professor", "Create professor users");
        permissionCatalog.put("exams.create", "Create exams");
        permissionCatalog.put("exams.solve", "Solve exams");
        permissionCatalog.put("exams.grade", "Grade exams");
        permissionCatalog.put("exam.validations.read.self", "Read own validation comments");
        permissionCatalog.put("exam.results.read.self", "Read own exam results");

        for (Map.Entry<String, String> entry : permissionCatalog.entrySet()) {
            permissionRepository.findByCode(entry.getKey()).orElseGet(() -> {
                Permission permission = new Permission();
                permission.setCode(entry.getKey());
                permission.setDescription(entry.getValue());
                return permissionRepository.save(permission);
            });
        }

        seedRolePermissionsIfMissing(permissionRepository, roleRepository, admin, List.of(
                "permissions.manage",
                "users.read.any",
                "users.read.self",
                "users.update.self",
                "users.create.any",
                "users.update.any",
                "users.delete.any",
                "users.create.professor",
                "exams.create",
                "exams.solve",
                "exams.grade"
        ));

        seedRolePermissionsIfMissing(permissionRepository, roleRepository, professor, List.of(
                "users.read.self",
                "users.update.self",
                "exams.create",
                "exams.grade"
        ));

        seedRolePermissionsIfMissing(permissionRepository, roleRepository, student, List.of(
                "users.read.self",
                "users.update.self",
                "exams.solve",
                "exam.validations.read.self",
                "exam.results.read.self"
        ));
    }

    private Role ensureRole(RoleRepository roleRepository, String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            return roleRepository.save(role);
        });
    }

    private void seedRolePermissionsIfMissing(PermissionRepository permissionRepository,
                                              RoleRepository roleRepository,
                                              Role role,
                                              List<String> permissionCodes) {
        if (!role.getPermissions().isEmpty()) {
            return;
        }

        Set<Permission> permissions = Set.copyOf(permissionRepository.findByCodeIn(permissionCodes));
        role.getPermissions().addAll(permissions);
        roleRepository.save(role);
    }
}
