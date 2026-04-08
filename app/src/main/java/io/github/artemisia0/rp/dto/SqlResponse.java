package io.github.artemisia0.rp.dto;

public class SqlResponse {

    private final boolean success;
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    public SqlResponse(boolean success, int exitCode, String stdout, String stderr) {
        this.success = success;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }
}
