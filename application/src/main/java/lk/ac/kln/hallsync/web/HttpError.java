package lk.ac.kln.hallsync.web;

public final class HttpError extends RuntimeException {
    private final int status;
    public HttpError(int status,String message) { super(message); this.status=status; }
    public int status() { return status; }
}
