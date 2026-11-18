package pl.tracer.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import pl.tracer.client.EventSender.Header;

final class Payloads {
    private Payloads() {}

    static List<Header> headers(Map<String, List<String>> h, List<String> mask) {
        List<Header> out = new ArrayList<>();
        h.forEach((name, values) -> {
            boolean masked = mask.stream().anyMatch(m -> m.equalsIgnoreCase(name));
            for (String v : values) out.add(new Header(name, masked ? "***" : v));
        });
        return out;
    }

    static String text(String contentType, byte[] b, int max) {
        if (b == null || b.length == 0) return null;
        String ct = contentType == null ? "" : contentType.toLowerCase();
        boolean textual = ct.isEmpty() || ct.startsWith("text/") || ct.contains("json") || ct.contains("xml")
                || ct.contains("x-www-form-urlencoded");
        if (!textual) return "<binary " + b.length + " bytes>";
        int n = Math.min(b.length, max);
        String s = new String(b, 0, n, StandardCharsets.UTF_8);
        return b.length > n ? s + "…[truncated, total " + b.length + " bytes]" : s;
    }
}
