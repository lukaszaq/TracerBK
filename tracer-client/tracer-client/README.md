# tracer-client
1. `mvn install`, potem w mikroserwisie dodaj zależność `pl.tracer:tracer-client:0.1.0`.
2. application.yml mikroserwisu:

    spring.application.name: orders
    tracer:
      collector-url: http://tracer:8080
      hosts:                      # host[:port] wywoływanego serwisu -> nazwa na diagramie
        "billing:8080": billing
        "localhost:8082": billing

3. Klientów twórz z wstrzykniętego buildera (nie `new RestTemplate()` / `RestClient.create()`):
   `RestClient.Builder builder` albo `RestTemplateBuilder builder`.
4. Opcjonalnie logback: `%X{trackingUuid}` w patternie.
5. Raportowane są oba kierunki: wychodzące (interceptor, side=CLIENT) i przychodzące (filtr, side=SERVER).
   Backend skleja je po spanId w jedną strzałkę. Gdy wołany serwis też ma tracer-client, `tracer.hosts` jest zbędne.
   Pomijane ścieżki: `tracer.exclude-paths` (domyślnie /actuator/**, /error).
