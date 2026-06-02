package com.jingxuan.modules.runtime.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.exception.BusinessException;
import com.jingxuan.mapper.WorkAttachmentMapper;
import com.jingxuan.modules.runtime.dto.ManifestValidationResult;
import com.jingxuan.modules.runtime.dto.ProjectPrepareResult;
import com.jingxuan.modules.runtime.dto.ProjectScanResult;
import com.jingxuan.modules.runtime.dto.RuntimeManifestDTO;
import com.jingxuan.modules.runtime.service.AiProjectManifestService;
import com.jingxuan.modules.runtime.service.ProjectPrepareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ProjectPrepareServiceImpl implements ProjectPrepareService {

    private static final Pattern ARTIFACT_ID_PATTERN = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private static final Pattern GROUP_ID_PATTERN = Pattern.compile("<groupId>([^<]+)</groupId>");

    private final WorkAttachmentMapper workAttachmentMapper;
    private final AiProjectManifestService aiProjectManifestService;

    @Value("${jingxuan.upload.path:./uploads}")
    private String uploadDir;

    @Override
    public ProjectPrepareResult prepareProject(Long workId) {
        try {
            WorkAttachment attachment = findProjectPackage(workId);
            if (attachment == null) {
                return invalid("No project package attachment found", List.of("projectPackage"), List.of());
            }

            if (!isZipAttachment(attachment)) {
                return invalid("Only .zip project packages are supported in the current version", List.of("projectPackage.zip"), List.of());
            }

            Path zipPath = resolveAttachmentPath(attachment);
            if (!Files.exists(zipPath)) {
                return invalid("Project package file does not exist on disk", List.of(), List.of(zipPath.toString()));
            }

            Path projectRoot = unzipProjectPackage(zipPath, workId);
            ProjectScanResult scanResult = scanProject(projectRoot, workId);
            RuntimeManifestDTO manifest = aiProjectManifestService.generateManifest(scanResult);
            ManifestValidationResult validation = validateManifest(manifest, projectRoot);
            String manifestPath = saveManifest(manifest, projectRoot);

            return ProjectPrepareResult.builder()
                    .valid(validation.isValid())
                    .message(validation.getMessage())
                    .projectPath(projectRoot.toString())
                    .manifestPath(manifestPath)
                    .missingFields(validation.getMissingFields())
                    .missingFiles(validation.getMissingFiles())
                    .scanResult(scanResult)
                    .manifest(manifest)
                    .build();
        } catch (IOException e) {
            throw new BusinessException("Failed to prepare runtime project: " + e.getMessage());
        }
    }

    @Override
    public RuntimeManifestDTO loadManifest(String manifestPath) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(manifestPath), StandardCharsets.UTF_8);
            return RuntimeManifestDTO.builder()
                    .version(parseInteger(lines, "version:"))
                    .project(com.jingxuan.modules.runtime.dto.ProjectInfoDTO.builder()
                            .name(parseValue(lines, "  name:"))
                            .type(parseValue(lines, "  type:"))
                            .description(parseValue(lines, "  description:"))
                            .build())
                    .backend(com.jingxuan.modules.runtime.dto.BackendRuntimeDTO.builder()
                            .framework(parseSectionValue(lines, "backend:", "  framework:"))
                            .path(parseSectionValue(lines, "backend:", "  path:"))
                            .buildCommand(com.jingxuan.modules.runtime.dto.CommandPairDTO.builder()
                                    .windows(parseNestedSectionValue(lines, "backend:", "  buildCommand:", "    windows:"))
                                    .linux(parseNestedSectionValue(lines, "backend:", "  buildCommand:", "    linux:"))
                                    .build())
                            .artifactPath(parseSectionValue(lines, "backend:", "  artifactPath:"))
                            .startCommand(com.jingxuan.modules.runtime.dto.CommandPairDTO.builder()
                                    .windows(parseNestedSectionValue(lines, "backend:", "  startCommand:", "    windows:"))
                                    .linux(parseNestedSectionValue(lines, "backend:", "  startCommand:", "    linux:"))
                                    .build())
                            .healthPath(parseSectionValue(lines, "backend:", "  healthPath:"))
                            .portEnv(parseSectionValue(lines, "backend:", "  portEnv:"))
                            .build())
                    .frontend(com.jingxuan.modules.runtime.dto.FrontendRuntimeDTO.builder()
                            .path(parseSectionValue(lines, "frontend:", "  path:"))
                            .installCommand(com.jingxuan.modules.runtime.dto.CommandPairDTO.builder()
                                    .windows(parseNestedSectionValue(lines, "frontend:", "  installCommand:", "    windows:"))
                                    .linux(parseNestedSectionValue(lines, "frontend:", "  installCommand:", "    linux:"))
                                    .build())
                            .startCommand(com.jingxuan.modules.runtime.dto.CommandPairDTO.builder()
                                    .windows(parseNestedSectionValue(lines, "frontend:", "  startCommand:", "    windows:"))
                                    .linux(parseNestedSectionValue(lines, "frontend:", "  startCommand:", "    linux:"))
                                    .build())
                            .portEnv(parseSectionValue(lines, "frontend:", "  portEnv:"))
                            .apiBaseUrlEnv(parseSectionValue(lines, "frontend:", "  apiBaseUrlEnv:"))
                            .build())
                    .database(com.jingxuan.modules.runtime.dto.DatabaseRuntimeDTO.builder()
                            .mysql(com.jingxuan.modules.runtime.dto.MysqlRuntimeDTO.builder()
                                    .enabled(Boolean.parseBoolean(parseNestedSectionValue(lines, "database:", "  mysql:", "    enabled:")))
                                    .initSqlPath(parseNestedSectionValue(lines, "database:", "  mysql:", "    initSqlPath:"))
                                    .schemaNamePattern(parseNestedSectionValue(lines, "database:", "  mysql:", "    schemaNamePattern:"))
                                    .usernameEnv(parseNestedSectionValue(lines, "database:", "  mysql:", "    usernameEnv:"))
                                    .passwordEnv(parseNestedSectionValue(lines, "database:", "  mysql:", "    passwordEnv:"))
                                    .jdbcUrlEnv(parseNestedSectionValue(lines, "database:", "  mysql:", "    jdbcUrlEnv:"))
                                    .build())
                            .build())
                    .cache(com.jingxuan.modules.runtime.dto.CacheRuntimeDTO.builder()
                            .redis(com.jingxuan.modules.runtime.dto.RedisRuntimeDTO.builder()
                                    .enabled(Boolean.parseBoolean(parseNestedSectionValue(lines, "cache:", "  redis:", "    enabled:")))
                                    .hostEnv(parseNestedSectionValue(lines, "cache:", "  redis:", "    hostEnv:"))
                                    .portEnv(parseNestedSectionValue(lines, "cache:", "  redis:", "    portEnv:"))
                                    .databaseEnv(parseNestedSectionValue(lines, "cache:", "  redis:", "    databaseEnv:"))
                                    .build())
                            .build())
                    .runtime(com.jingxuan.modules.runtime.dto.RuntimePolicyDTO.builder()
                            .previewPath(parseSectionValue(lines, "runtime:", "  previewPath:"))
                            .idleTimeoutMinutes(parseSectionInteger(lines, "runtime:", "  idleTimeoutMinutes:"))
                            .requiredFiles(parseListAfter(lines, "  requiredFiles:"))
                            .build())
                    .build();
        } catch (IOException e) {
            throw new BusinessException("Failed to load manifest: " + e.getMessage());
        }
    }

    private WorkAttachment findProjectPackage(Long workId) {
        List<WorkAttachment> attachments = workAttachmentMapper.selectList(
                Wrappers.<WorkAttachment>lambdaQuery()
                        .eq(WorkAttachment::getWorkId, workId)
                        .orderByAsc(WorkAttachment::getCreateTime)
        );
        return attachments.stream()
                .filter(this::isZipAttachment)
                .findFirst()
                .orElse(null);
    }

    private boolean isZipAttachment(WorkAttachment attachment) {
        if (attachment == null) {
            return false;
        }
        String fileType = attachment.getFileType();
        String fileName = attachment.getFileName();
        return "zip".equalsIgnoreCase(fileType)
                || (fileName != null && fileName.toLowerCase().endsWith(".zip"));
    }

    private Path resolveAttachmentPath(WorkAttachment attachment) {
        String relativeUrl = attachment.getFileUrl().replace("/uploads/", "");
        return Paths.get(uploadDir).resolve(relativeUrl).normalize();
    }

    private Path unzipProjectPackage(Path zipPath, Long workId) throws IOException {
        Path targetRoot = Paths.get(uploadDir, "projects", String.valueOf(workId)).toAbsolutePath().normalize();
        recreateDirectory(targetRoot);

        try (InputStream inputStream = Files.newInputStream(zipPath);
             ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = targetRoot.resolve(entry.getName()).toAbsolutePath().normalize();
                if (!target.startsWith(targetRoot)) {
                    throw new BusinessException("Invalid zip entry path: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zipInputStream, target);
                }
            }
        }

        return normalizeProjectRoot(targetRoot);
    }

    private void recreateDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> stream = Files.walk(directory)) {
                List<Path> paths = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                for (Path path : paths) {
                    Files.deleteIfExists(path);
                }
            }
        }
        Files.createDirectories(directory);
    }

    private Path normalizeProjectRoot(Path extractedRoot) throws IOException {
        try (Stream<Path> stream = Files.list(extractedRoot)) {
            List<Path> children = stream.collect(Collectors.toList());
            if (children.size() == 1 && Files.isDirectory(children.get(0))) {
                return children.get(0);
            }
        }
        return extractedRoot;
    }

    private ProjectScanResult scanProject(Path projectRoot, Long workId) throws IOException {
        List<Path> allPaths;
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            allPaths = stream.filter(Files::isRegularFile).collect(Collectors.toList());
        }

        Map<String, String> npmScripts = new HashMap<>();
        Path pomPath = choosePath(projectRoot, allPaths, "backend/pom.xml", "pom.xml");
        Path packageJsonPath = choosePath(projectRoot, allPaths, "frontend/package.json", "package.json");
        Path initSqlPath = chooseSqlPath(projectRoot, allPaths);

        if (packageJsonPath != null) {
            npmScripts.putAll(extractScripts(packageJsonPath));
        }

        String backendPath = pomPath == null ? null : relativizeParent(projectRoot, pomPath);
        String frontendPath = packageJsonPath == null ? null : relativizeParent(projectRoot, packageJsonPath);
        String pomContent = pomPath == null ? "" : Files.readString(pomPath, StandardCharsets.UTF_8);
        String packageJsonContent = packageJsonPath == null ? "" : Files.readString(packageJsonPath, StandardCharsets.UTF_8);

        return ProjectScanResult.builder()
                .workId(workId)
                .projectRoot(projectRoot.toString())
                .backendPath(backendPath)
                .springBootProject(pomPath != null && pomContent.contains("spring-boot"))
                .pomPath(relativize(projectRoot, pomPath))
                .artifactId(firstMatch(pomContent, ARTIFACT_ID_PATTERN))
                .groupId(firstMatch(pomContent, GROUP_ID_PATTERN))
                .mainClass(null)
                .backendConfigFiles(findMatching(projectRoot, allPaths, "application.yml", "application.yaml", "application.properties"))
                .frontendPath(frontendPath)
                .packageJsonPath(relativize(projectRoot, packageJsonPath))
                .frontendFramework(detectFrontendFramework(packageJsonContent))
                .npmScripts(npmScripts)
                .sqlFiles(findAllSqlFiles(projectRoot, allPaths))
                .initSqlPath(relativize(projectRoot, initSqlPath))
                .shellScripts(findByExtension(projectRoot, allPaths, ".sh"))
                .batchScripts(findByExtension(projectRoot, allPaths, ".bat", ".cmd"))
                .allFiles(allPaths.stream().map(path -> relativize(projectRoot, path)).collect(Collectors.toList()))
                .build();
    }

    private ManifestValidationResult validateManifest(RuntimeManifestDTO manifest, Path projectRoot) {
        List<String> missingFields = new ArrayList<>();
        List<String> missingFiles = new ArrayList<>();

        if (manifest == null) {
            missingFields.add("manifest");
            return ManifestValidationResult.builder()
                    .valid(false)
                    .message("Runtime manifest could not be generated")
                    .missingFields(missingFields)
                    .missingFiles(missingFiles)
                    .build();
        }

        if (manifest.getProject() == null || isBlank(manifest.getProject().getName())) {
            missingFields.add("project.name");
        }
        if (manifest.getProject() == null || isBlank(manifest.getProject().getType())) {
            missingFields.add("project.type");
        }
        if (manifest.getBackend() == null || isBlank(manifest.getBackend().getPath())) {
            missingFields.add("backend.path");
        }
        if (manifest.getBackend() == null || manifest.getBackend().getBuildCommand() == null
                || isBlank(manifest.getBackend().getBuildCommand().getWindows())) {
            missingFields.add("backend.buildCommand.windows");
        }
        if (manifest.getBackend() == null || manifest.getBackend().getStartCommand() == null
                || isBlank(manifest.getBackend().getStartCommand().getWindows())) {
            missingFields.add("backend.startCommand.windows");
        }
        if (manifest.getBackend() == null || isBlank(manifest.getBackend().getArtifactPath())) {
            missingFields.add("backend.artifactPath");
        }
        if (manifest.getFrontend() == null || isBlank(manifest.getFrontend().getPath())) {
            missingFields.add("frontend.path");
        }
        if (manifest.getFrontend() == null || manifest.getFrontend().getInstallCommand() == null
                || isBlank(manifest.getFrontend().getInstallCommand().getWindows())) {
            missingFields.add("frontend.installCommand.windows");
        }
        if (manifest.getFrontend() == null || manifest.getFrontend().getStartCommand() == null
                || isBlank(manifest.getFrontend().getStartCommand().getWindows())) {
            missingFields.add("frontend.startCommand.windows");
        }
        if (manifest.getDatabase() == null || manifest.getDatabase().getMysql() == null
                || manifest.getDatabase().getMysql().getEnabled() == null) {
            missingFields.add("database.mysql.enabled");
        }

        String backendPath = manifest.getBackend() == null ? null : manifest.getBackend().getPath();
        String frontendPath = manifest.getFrontend() == null ? null : manifest.getFrontend().getPath();
        checkFile(projectRoot, backendPath, missingFiles);
        checkFile(projectRoot, frontendPath, missingFiles);
        checkFile(projectRoot, joinPath(backendPath, "pom.xml"), missingFiles);
        checkFile(projectRoot, joinPath(frontendPath, "package.json"), missingFiles);

        if (manifest.getDatabase() != null && manifest.getDatabase().getMysql() != null
                && Boolean.TRUE.equals(manifest.getDatabase().getMysql().getEnabled())) {
            String initSqlPath = manifest.getDatabase().getMysql().getInitSqlPath();
            if (isBlank(initSqlPath)) {
                missingFields.add("database.mysql.initSqlPath");
            } else {
                checkFile(projectRoot, initSqlPath, missingFiles);
            }
        }

        boolean valid = missingFields.isEmpty() && missingFiles.isEmpty();
        return ManifestValidationResult.builder()
                .valid(valid)
                .message(valid ? "Project runtime preparation completed" : "Project runtime description is incomplete")
                .missingFields(missingFields)
                .missingFiles(missingFiles)
                .build();
    }

    private String saveManifest(RuntimeManifestDTO manifest, Path projectRoot) throws IOException {
        Path manifestPath = projectRoot.resolve("jingxuan-demo.yml");
        Files.writeString(
                manifestPath,
                renderManifest(manifest),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        return manifestPath.toString();
    }

    private String renderManifest(RuntimeManifestDTO manifest) {
        StringBuilder builder = new StringBuilder();
        builder.append("version: ").append(manifest.getVersion()).append('\n');
        builder.append('\n');
        builder.append("project:\n");
        builder.append("  name: ").append(safe(manifest.getProject().getName())).append('\n');
        builder.append("  type: ").append(safe(manifest.getProject().getType())).append('\n');
        builder.append("  description: ").append(safe(manifest.getProject().getDescription())).append('\n');
        builder.append('\n');
        builder.append("backend:\n");
        builder.append("  framework: ").append(safe(manifest.getBackend().getFramework())).append('\n');
        builder.append("  path: ").append(safe(manifest.getBackend().getPath())).append('\n');
        builder.append("  buildCommand:\n");
        builder.append("    windows: ").append(safe(manifest.getBackend().getBuildCommand().getWindows())).append('\n');
        builder.append("    linux: ").append(safe(manifest.getBackend().getBuildCommand().getLinux())).append('\n');
        builder.append("  artifactPath: ").append(safe(manifest.getBackend().getArtifactPath())).append('\n');
        builder.append("  startCommand:\n");
        builder.append("    windows: ").append(safe(manifest.getBackend().getStartCommand().getWindows())).append('\n');
        builder.append("    linux: ").append(safe(manifest.getBackend().getStartCommand().getLinux())).append('\n');
        builder.append("  healthPath: ").append(safe(manifest.getBackend().getHealthPath())).append('\n');
        builder.append("  portEnv: ").append(safe(manifest.getBackend().getPortEnv())).append('\n');
        builder.append('\n');
        builder.append("frontend:\n");
        builder.append("  path: ").append(safe(manifest.getFrontend().getPath())).append('\n');
        builder.append("  installCommand:\n");
        builder.append("    windows: ").append(safe(manifest.getFrontend().getInstallCommand().getWindows())).append('\n');
        builder.append("    linux: ").append(safe(manifest.getFrontend().getInstallCommand().getLinux())).append('\n');
        builder.append("  startCommand:\n");
        builder.append("    windows: ").append(safe(manifest.getFrontend().getStartCommand().getWindows())).append('\n');
        builder.append("    linux: ").append(safe(manifest.getFrontend().getStartCommand().getLinux())).append('\n');
        builder.append("  portEnv: ").append(safe(manifest.getFrontend().getPortEnv())).append('\n');
        builder.append("  apiBaseUrlEnv: ").append(safe(manifest.getFrontend().getApiBaseUrlEnv())).append('\n');
        builder.append('\n');
        builder.append("database:\n");
        builder.append("  mysql:\n");
        builder.append("    enabled: ").append(Boolean.TRUE.equals(manifest.getDatabase().getMysql().getEnabled())).append('\n');
        builder.append("    initSqlPath: ").append(safe(manifest.getDatabase().getMysql().getInitSqlPath())).append('\n');
        builder.append("    schemaNamePattern: ").append(safe(manifest.getDatabase().getMysql().getSchemaNamePattern())).append('\n');
        builder.append("    usernameEnv: ").append(safe(manifest.getDatabase().getMysql().getUsernameEnv())).append('\n');
        builder.append("    passwordEnv: ").append(safe(manifest.getDatabase().getMysql().getPasswordEnv())).append('\n');
        builder.append("    jdbcUrlEnv: ").append(safe(manifest.getDatabase().getMysql().getJdbcUrlEnv())).append('\n');
        builder.append('\n');
        builder.append("cache:\n");
        builder.append("  redis:\n");
        builder.append("    enabled: ").append(Boolean.TRUE.equals(manifest.getCache().getRedis().getEnabled())).append('\n');
        builder.append("    hostEnv: ").append(safe(manifest.getCache().getRedis().getHostEnv())).append('\n');
        builder.append("    portEnv: ").append(safe(manifest.getCache().getRedis().getPortEnv())).append('\n');
        builder.append("    databaseEnv: ").append(safe(manifest.getCache().getRedis().getDatabaseEnv())).append('\n');
        builder.append('\n');
        builder.append("runtime:\n");
        builder.append("  previewPath: ").append(safe(manifest.getRuntime().getPreviewPath())).append('\n');
        builder.append("  idleTimeoutMinutes: ").append(manifest.getRuntime().getIdleTimeoutMinutes()).append('\n');
        builder.append("  requiredFiles:\n");
        for (String requiredFile : manifest.getRuntime().getRequiredFiles()) {
            builder.append("    - ").append(safe(requiredFile)).append('\n');
        }
        return builder.toString();
    }

    private ProjectPrepareResult invalid(String message, List<String> missingFields, List<String> missingFiles) {
        return ProjectPrepareResult.builder()
                .valid(false)
                .message(message)
                .missingFields(missingFields)
                .missingFiles(missingFiles)
                .build();
    }

    private Path choosePath(Path root, List<Path> files, String preferred, String fallbackFileName) {
        Path preferredPath = root.resolve(preferred).normalize();
        if (Files.exists(preferredPath)) {
            return preferredPath;
        }
        return files.stream()
                .filter(path -> fallbackFileName.equals(path.getFileName().toString()))
                .findFirst()
                .orElse(null);
    }

    private Path chooseSqlPath(Path root, List<Path> files) {
        Path preferredPath = root.resolve("sql/init.sql").normalize();
        if (Files.exists(preferredPath)) {
            return preferredPath;
        }
        return files.stream()
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".sql"))
                .findFirst()
                .orElse(null);
    }

    private Map<String, String> extractScripts(Path packageJsonPath) throws IOException {
        String content = Files.readString(packageJsonPath, StandardCharsets.UTF_8);
        Map<String, String> scripts = new HashMap<>();
        String[] candidates = {"dev", "start", "build"};
        for (String candidate : candidates) {
            Pattern pattern = Pattern.compile("\"" + candidate + "\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                scripts.put(candidate, matcher.group(1));
            }
        }
        return scripts;
    }

    private String detectFrontendFramework(String packageJsonContent) {
        if (packageJsonContent.contains("\"vue\"")) {
            return "vue";
        }
        if (packageJsonContent.contains("\"react\"")) {
            return "react";
        }
        return "unknown";
    }

    private String firstMatch(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<String> findMatching(Path root, List<Path> files, String... fileNames) {
        List<String> names = List.of(fileNames);
        return files.stream()
                .filter(path -> names.contains(path.getFileName().toString()))
                .map(path -> relativize(root, path))
                .collect(Collectors.toList());
    }

    private List<String> findAllSqlFiles(Path root, List<Path> files) {
        return files.stream()
                .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".sql"))
                .map(path -> relativize(root, path))
                .collect(Collectors.toList());
    }

    private List<String> findByExtension(Path root, List<Path> files, String... extensions) {
        List<String> extList = List.of(extensions);
        return files.stream()
                .filter(path -> extList.stream().anyMatch(ext -> path.getFileName().toString().toLowerCase().endsWith(ext)))
                .map(path -> relativize(root, path))
                .collect(Collectors.toList());
    }

    private void checkFile(Path root, String relativePath, List<String> missingFiles) {
        if (isBlank(relativePath)) {
            return;
        }
        if (!Files.exists(root.resolve(relativePath).normalize())) {
            missingFiles.add(relativePath);
        }
    }

    private String relativize(Path root, Path path) {
        if (path == null) {
            return null;
        }
        return root.relativize(path).toString().replace('\\', '/');
    }

    private String relativizeParent(Path root, Path path) {
        if (path == null || path.getParent() == null) {
            return null;
        }
        return relativize(root, path.getParent());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String parseValue(List<String> lines, String prefix) {
        return lines.stream()
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .findFirst()
                .orElse(null);
    }

    private Integer parseInteger(List<String> lines, String prefix) {
        String value = parseValue(lines, prefix);
        return value == null || value.isBlank() ? null : Integer.parseInt(value);
    }

    private String parseSectionValue(List<String> lines, String sectionAnchor, String prefix) {
        int sectionIndex = indexOf(lines, sectionAnchor);
        if (sectionIndex < 0) {
            return null;
        }
        for (int i = sectionIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("  ")) {
                break;
            }
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private String parseNestedSectionValue(List<String> lines, String sectionAnchor, String nestedAnchor, String prefix) {
        int sectionIndex = indexOf(lines, sectionAnchor);
        if (sectionIndex < 0) {
            return null;
        }
        int nestedIndex = -1;
        for (int i = sectionIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("  ")) {
                break;
            }
            if (line.startsWith(nestedAnchor)) {
                nestedIndex = i;
                break;
            }
        }
        if (nestedIndex < 0) {
            return null;
        }
        for (int i = nestedIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("    ")) {
                break;
            }
            if (line.startsWith(prefix)) {
                return line.substring(prefix.length()).trim();
            }
        }
        return null;
    }

    private Integer parseSectionInteger(List<String> lines, String sectionAnchor, String prefix) {
        String value = parseSectionValue(lines, sectionAnchor, prefix);
        return value == null || value.isBlank() ? null : Integer.parseInt(value);
    }

    private List<String> parseListAfter(List<String> lines, String prefix) {
        int startIndex = indexOf(lines, prefix);
        if (startIndex < 0) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("    - ")) {
                break;
            }
            values.add(line.substring("    - ".length()).trim());
        }
        return values;
    }

    private int indexOf(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private String joinPath(String parent, String child) {
        if (isBlank(parent)) {
            return child;
        }
        return parent.replace('\\', '/') + "/" + child;
    }
}
