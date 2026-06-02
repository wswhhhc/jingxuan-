package com.jingxuan.modules.runtime.service.impl;

import com.jingxuan.modules.runtime.dto.ProcessStartResult;
import com.jingxuan.modules.runtime.dto.RuntimeStartContext;
import com.jingxuan.modules.runtime.service.RuntimeAdapter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Component
public class WindowsProcessRuntimeAdapter implements RuntimeAdapter {

    @Override
    public ProcessStartResult startBackend(RuntimeStartContext context) {
        Path backendDir = Paths.get(context.getProjectPath(), context.getManifest().getBackend().getPath());
        ProcessStartResult buildResult = runCommandAndWait(
                context.getManifest().getBackend().getBuildCommand().getWindows(),
                backendDir,
                context.getBackendEnv(),
                180
        );
        if (!buildResult.isStarted()) {
            return buildResult;
        }

        String javaCommand = resolveJavaCommand();
        Path jarPath = resolveJarPath(context);
        return startJavaProcess(javaCommand, jarPath, backendDir, context.getBackendEnv());
    }

    @Override
    public ProcessStartResult startFrontend(RuntimeStartContext context) {
        Path frontendDir = Paths.get(context.getProjectPath(), context.getManifest().getFrontend().getPath());
        ProcessStartResult installResult = runCommandAndWait(
                context.getManifest().getFrontend().getInstallCommand().getWindows(),
                frontendDir,
                context.getFrontendEnv(),
                180
        );
        if (!installResult.isStarted()) {
            return installResult;
        }

        String command = context.getManifest().getFrontend().getStartCommand().getWindows()
                .replace("${FRONTEND_PORT}", String.valueOf(context.getFrontendPort()))
                .replace("${VITE_API_BASE_URL}", "http://127.0.0.1:" + context.getBackendPort());
        return startProcess(command, frontendDir, context.getFrontendEnv());
    }

    @Override
    public boolean isAlive(Long pid) {
        return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
    }

    @Override
    public void stopProcess(Long pid) {
        ProcessHandle.of(pid).ifPresent(handle -> {
            handle.descendants().forEach(ProcessHandle::destroyForcibly);
            handle.destroyForcibly();
        });
    }

    private ProcessStartResult startProcess(String command, Path workingDirectory, Map<String, String> environment) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command);
            processBuilder.directory(workingDirectory.toFile());
            processBuilder.redirectErrorStream(true);
            if (environment != null && !environment.isEmpty()) {
                processBuilder.environment().putAll(environment);
            }
            Process process = processBuilder.start();
            return ProcessStartResult.builder()
                    .pid(process.pid())
                    .command(command)
                    .workingDirectory(workingDirectory.toString())
                    .started(process.isAlive())
                    .exitCode(process.isAlive() ? null : process.exitValue())
                    .errorMessage(process.isAlive() ? null : "Process exited immediately")
                    .build();
        } catch (IOException e) {
            return ProcessStartResult.builder()
                    .command(command)
                    .workingDirectory(workingDirectory.toString())
                    .started(false)
                    .exitCode(-1)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private ProcessStartResult startJavaProcess(String javaCommand, Path jarPath, Path workingDirectory, Map<String, String> environment) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(javaCommand, "-jar", jarPath.toString());
            processBuilder.directory(workingDirectory.toFile());
            processBuilder.redirectErrorStream(true);
            if (environment != null && !environment.isEmpty()) {
                processBuilder.environment().putAll(environment);
            }
            Process process = processBuilder.start();
            return ProcessStartResult.builder()
                    .pid(process.pid())
                    .command(javaCommand + " -jar " + jarPath)
                    .workingDirectory(workingDirectory.toString())
                    .started(process.isAlive())
                    .exitCode(process.isAlive() ? null : process.exitValue())
                    .errorMessage(process.isAlive() ? null : "Java process exited immediately")
                    .build();
        } catch (IOException e) {
            return ProcessStartResult.builder()
                    .command(javaCommand + " -jar " + jarPath)
                    .workingDirectory(workingDirectory.toString())
                    .started(false)
                    .exitCode(-1)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private ProcessStartResult runCommandAndWait(String command, Path workingDirectory, Map<String, String> environment, long timeoutSeconds) {
        if (command == null || command.isBlank()) {
            return ProcessStartResult.builder()
                    .started(true)
                    .command(command)
                    .workingDirectory(workingDirectory.toString())
                    .build();
        }
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", command);
            processBuilder.directory(workingDirectory.toFile());
            processBuilder.redirectErrorStream(true);
            if (environment != null && !environment.isEmpty()) {
                processBuilder.environment().putAll(environment);
            }
            Process process = processBuilder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ProcessStartResult.builder()
                        .command(command)
                        .workingDirectory(workingDirectory.toString())
                        .started(false)
                        .exitCode(-1)
                        .errorMessage("Command timed out")
                        .build();
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return ProcessStartResult.builder()
                        .command(command)
                        .workingDirectory(workingDirectory.toString())
                        .started(false)
                        .exitCode(exitCode)
                        .errorMessage(readProcessOutput(process))
                        .build();
            }
            return ProcessStartResult.builder()
                    .command(command)
                    .workingDirectory(workingDirectory.toString())
                    .started(true)
                    .exitCode(exitCode)
                    .build();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return ProcessStartResult.builder()
                    .command(command)
                    .workingDirectory(workingDirectory.toString())
                    .started(false)
                    .exitCode(-1)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private Path resolveJarPath(RuntimeStartContext context) {
        Path backendRoot = Paths.get(context.getProjectPath(), context.getManifest().getBackend().getPath());
        String artifactPath = context.getManifest().getBackend().getArtifactPath();
        if (artifactPath.contains("*")) {
            Path artifactDir = backendRoot.resolve(artifactPath.substring(0, artifactPath.indexOf('*'))).normalize();
            try (Stream<Path> stream = Files.walk(artifactDir.getParent() == null ? backendRoot : artifactDir.getParent())) {
                Optional<Path> jar = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .sorted(Comparator.comparing(Path::toString))
                        .findFirst();
                return jar.orElse(backendRoot.resolve("target/app.jar"));
            } catch (IOException e) {
                return backendRoot.resolve("target/app.jar");
            }
        }
        return backendRoot.resolve(artifactPath).normalize();
    }

    private String resolveJavaCommand() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null && !javaHome.isBlank()) {
            Path javaPath = Paths.get(javaHome, "bin", "java.exe");
            if (Files.exists(javaPath)) {
                return javaPath.toString();
            }
        }

        String javaHomeProperty = System.getProperty("java.home");
        if (javaHomeProperty != null && !javaHomeProperty.isBlank()) {
            Path javaPath = Paths.get(javaHomeProperty, "bin", "java.exe");
            if (Files.exists(javaPath)) {
                return javaPath.toString();
            }
        }

        return "java";
    }

    private String readProcessOutput(Process process) throws IOException {
        try (InputStream inputStream = process.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            if (bytes.length == 0) {
                return "Command failed without output";
            }
            String output = new String(bytes);
            List<String> lines = output.lines().limit(20).toList();
            return String.join(System.lineSeparator(), lines);
        }
    }
}
