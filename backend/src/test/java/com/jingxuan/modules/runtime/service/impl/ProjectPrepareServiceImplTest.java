package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.entity.WorkAttachment;
import com.jingxuan.mapper.WorkAttachmentMapper;
import com.jingxuan.modules.runtime.dto.ProjectPrepareResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProjectPrepareServiceImpl")
class ProjectPrepareServiceImplTest {

    @Mock
    private WorkAttachmentMapper workAttachmentMapper;

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("should unzip and scan a valid spring boot project package")
    void shouldPrepareValidProjectPackage() throws Exception {
        Path uploadRoot = tempDir.resolve("uploads");
        Path attachmentDir = uploadRoot.resolve("2026/06/02");
        Files.createDirectories(attachmentDir);
        Path zipPath = attachmentDir.resolve("demo.zip");
        createDemoZip(zipPath);

        WorkAttachment attachment = new WorkAttachment();
        attachment.setWorkId(12L);
        attachment.setFileName("demo.zip");
        attachment.setFileType("zip");
        attachment.setFileUrl("/uploads/2026/06/02/demo.zip");

        when(workAttachmentMapper.selectList(any())).thenReturn(java.util.List.of(attachment));

        ProjectPrepareServiceImpl service = new ProjectPrepareServiceImpl(
                workAttachmentMapper,
                new SimpleAiProjectManifestServiceImpl()
        );
        ReflectionTestUtils.setField(service, "uploadDir", uploadRoot.toString());

        ProjectPrepareResult result = service.prepareProject(12L);

        assertTrue(result.isValid());
        assertNotNull(result.getProjectPath());
        assertNotNull(result.getManifestPath());
        assertTrue(Files.exists(Path.of(result.getManifestPath())));
        assertNotNull(result.getScanResult());
        assertEquals("backend", result.getScanResult().getBackendPath());
        assertEquals("frontend", result.getScanResult().getFrontendPath());
        assertEquals("sql/init.sql", result.getScanResult().getInitSqlPath());
        String manifestContent = Files.readString(Path.of(result.getManifestPath()), StandardCharsets.UTF_8);
        assertTrue(manifestContent.contains("project:"));
        assertTrue(manifestContent.contains("backend:"));
        assertTrue(manifestContent.contains("frontend:"));
    }

    private void createDemoZip(Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            putEntry(zos, "demo/backend/pom.xml", """
                    <project>
                      <groupId>com.example</groupId>
                      <artifactId>demo-runtime</artifactId>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                      </dependencies>
                    </project>
                    """);
            putEntry(zos, "demo/frontend/package.json", """
                    {
                      "name": "demo-frontend",
                      "scripts": {
                        "dev": "vite",
                        "build": "vite build"
                      },
                      "dependencies": {
                        "vue": "^3.5.0"
                      }
                    }
                    """);
            putEntry(zos, "demo/sql/init.sql", """
                    CREATE TABLE demo_user (
                      id BIGINT PRIMARY KEY,
                      name VARCHAR(50)
                    );
                    """);
        }
    }

    private void putEntry(ZipOutputStream zos, String name, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
}
