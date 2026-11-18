# Tracer
Backend: `cd backend && mvn spring-boot:run` (Elasticsearch :9200, MSSQL baza `tracer`, patrz application.yml).
Frontend: `ng new tracer-ui --minimal --style=css`, skopiuj `frontend/src/app/*` i `proxy.conf.json` do projektu,
potem `ng serve --proxy-config proxy.conf.json`.

Przykład zdarzeń (POST /api/events, tablica):
[
 {"trackingUuid":"abc","timestamp":"2026-11-12T10:00:00.000Z","type":"REST_REQUEST","from":"gateway","to":"orders",
  "method":"POST","uri":"/orders","headers":[{"name":"Content-Type","value":"application/json"}],"payload":"{\"id\":1}"},
 {"trackingUuid":"abc","timestamp":"2026-11-12T10:00:00.050Z","type":"KAFKA_PRODUCE","from":"orders","to":"topic:order-created",
  "topic":"order-created","messageKey":"1","payload":"{\"id\":1}"},
 {"trackingUuid":"abc","timestamp":"2026-11-12T10:00:00.080Z","type":"KAFKA_CONSUME","from":"topic:order-created","to":"billing",
  "topic":"order-created","messageKey":"1","payload":"{\"id\":1}"},
 {"trackingUuid":"abc","timestamp":"2026-11-12T10:00:00.120Z","type":"REST_RESPONSE","from":"orders","to":"gateway","status":201}
]
