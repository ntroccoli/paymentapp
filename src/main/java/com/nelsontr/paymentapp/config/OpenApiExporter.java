package com.nelsontr.paymentapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Component
public class OpenApiExporter {
    private static final Logger log = LoggerFactory.getLogger(OpenApiExporter.class);

    private final Environment env;

    @Value("${openapi.export.enabled:false}")
    private boolean exportEnabled;

    @Value("${openapi.export.once:false}")
    private boolean exportOnce;

    @Value("${openapi.export.output-file:openapi.yaml}")
    private String outputFile;

    public OpenApiExporter(Environment env) {
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!exportEnabled) {
            return;
        }
        try {
            String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
            String apiDocsPath = env.getProperty("springdoc.api-docs.path", "/v3/api-docs");
            String url = "http://localhost:" + port + (apiDocsPath.endsWith(".yaml") ? apiDocsPath : apiDocsPath + ".yaml");

            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.ACCEPT, "application/x-yaml, application/yaml, text/yaml, */*" );
            ResponseEntity<String> resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.error("Failed to fetch OpenAPI YAML from {}. Status: {}", url, resp.getStatusCode());
                return;
            }

            Path outPath = Path.of(System.getProperty("user.dir")).resolve(StringUtils.hasText(outputFile) ? outputFile : "openapi.yaml");
            Files.writeString(outPath, resp.getBody(), StandardCharsets.UTF_8);
            log.info("OpenAPI spec exported to {}", outPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("Error exporting OpenAPI spec", e);
        } finally {
            if (exportOnce) {
                // Give logs a moment to flush
                try { Thread.sleep(Duration.ofMillis(300).toMillis()); } catch (InterruptedException ignored) {}
                System.exit(0);
            }
        }
    }
}

