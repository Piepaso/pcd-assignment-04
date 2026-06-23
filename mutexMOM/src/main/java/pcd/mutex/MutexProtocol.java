package pcd.mutex;

public class MutexProtocol {

    public static final String HOST = "localhost";
    public static final String COORDINATOR_QUEUE = "mutex.requests";
    public static final String DEFAULT_EXCHANGE = "";

    public static final String REQUEST = "REQUEST";
    public static final String RELEASE = "RELEASE";
    public static final String GRANT   = "GRANT";
}
