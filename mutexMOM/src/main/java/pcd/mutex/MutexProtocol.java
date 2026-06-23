package pcd.mutex;

public final class MutexProtocol {

    private MutexProtocol() { }

    public static final String HOST = "localhost";
    public static final String COORDINATOR_QUEUE = "mutex.requests";

    public static final String CMD_REQUEST = "REQUEST";
    public static final String CMD_RELEASE = "RELEASE";
    public static final String CMD_GRANT   = "GRANT";
}
