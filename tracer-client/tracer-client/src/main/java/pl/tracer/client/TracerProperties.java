package pl.tracer.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tracer")
public class TracerProperties {
    private boolean enabled = true;
    /** Nazwa tego serwisu na diagramie; domyślnie spring.application.name. */
    private String serviceName;
    private String collectorUrl = "http://localhost:8080";
    private int maxPayloadBytes = 16384;
    private List<String> maskHeaders = List.of("authorization", "cookie", "set-cookie", "proxy-authorization");
    /** Mapowanie host[:port] wywoływanego serwisu -> nazwa na diagramie. */
    /** Ścieżki (ant) pomijane przez filtr po stronie serwera. */
    private List<String> excludePaths = List.of("/actuator/**", "/error");
    private Map<String, String> hosts = new HashMap<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getCollectorUrl() { return collectorUrl; }
    public void setCollectorUrl(String collectorUrl) { this.collectorUrl = collectorUrl; }
    public int getMaxPayloadBytes() { return maxPayloadBytes; }
    public void setMaxPayloadBytes(int maxPayloadBytes) { this.maxPayloadBytes = maxPayloadBytes; }
    public List<String> getMaskHeaders() { return maskHeaders; }
    public void setMaskHeaders(List<String> maskHeaders) { this.maskHeaders = maskHeaders; }
    public List<String> getExcludePaths() { return excludePaths; }
    public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
    public Map<String, String> getHosts() { return hosts; }
    public void setHosts(Map<String, String> hosts) { this.hosts = hosts; }
}
