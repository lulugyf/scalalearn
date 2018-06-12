package pm;

public class ServiceDescriptor {
    private String serviceId; // like "web", "jabber" etc.
    private int localPort;
    private String remoteHost;
    private int remotePort;

    public ServiceDescriptor(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceId() {
        return serviceId;
    }

    void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public int getLocalPort() {
        return localPort;
    }

    void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getRemoteHost() {
        return remoteHost;
    }

    void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public int getRemotePort() {
        return remotePort;
    }

    void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }
}
