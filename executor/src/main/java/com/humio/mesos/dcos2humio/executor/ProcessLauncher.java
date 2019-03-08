package com.humio.mesos.dcos2humio.executor;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

public class ProcessLauncher {
    private boolean enabled;
    private final String[] commandAndArgs;
    private final File runtimeDir;
    private final File pidFile;
    private final File outputLog;
    private final File errorLog;
    private Process process;

    public ProcessLauncher(File runtimeDir, boolean enabled, String... commandAndArgs) {
        this.enabled = enabled;
        this.runtimeDir = runtimeDir;
        pidFile = new File(runtimeDir, "pid");
        outputLog = new File(runtimeDir, "stdout");
        errorLog = new File(runtimeDir, "stderr");
        this.commandAndArgs = commandAndArgs;
    }

    public boolean isHealthy() {
        //More checks
        return this.isRunning();
    }

    protected void preflightChecks() {
        if (!runtimeDir.exists() && runtimeDir.mkdir()) {
            System.out.println("Created runtime directory " + runtimeDir.getAbsolutePath());
        }

        if (pidFile.exists()) {
            try {
                final String orphanePid = FileUtils.readFileToString(pidFile, "UTF-8");

                final int killOrphaneExitCode = new ProcessBuilder("kill", orphanePid).start().waitFor();
                if (killOrphaneExitCode == 0) {
                    System.out.println("Killed orphane child");
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                //todo: fatal
                throw new RuntimeException("Failed to kill orphaned child.", e);
            }
        }
    }

    public void start() {
        preflightChecks();
        try {
            process = new ProcessBuilder()
                    .command(commandAndArgs)
                    .redirectOutput(outputLog)
                    .redirectError(errorLog)
                    .start();
            FileUtils.write(pidFile, String.valueOf(getPid(process)), "UTF-8");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        process.destroy();
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNotRunning() {
        return !isRunning();
    }

    private int getPid(Process process) {
        if (process.isAlive()) {
            try {
                //TODO: This is just one too hacky for comfort
                //TODO: Java 9 to the rescue. Upgrade. https://www.javaworld.com/article/3176874/java-language/java-9s-other-new-enhancements-part-3.html
                final Field pid = this.getClass().getClassLoader().loadClass("java.lang.UNIXProcess").getDeclaredField("pid");
                pid.setAccessible(true);
                return pid.getInt(process);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        throw new RuntimeException("Failed to get pid of process");
    }

    public boolean hasStopped() {
        return process != null && !process.isAlive();
    }

    public boolean hasFinished() {
        return hasStopped() && process.exitValue() == 0;
    }

    public boolean hasFailed() {
        return hasStopped() && process.exitValue() > 0;
    }

    public int exitValue() {
        return process.exitValue();
    }
}
