package com.huamanga.tourism.health;

import com.huamanga.tourism.health.dto.HealthResponse;
import com.huamanga.tourism.health.dto.HealthStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico de estado del sistema.
 *
 * <p>La ruta completa es {@code /api/v1/health}: el prefijo {@code /api/v1}
 * lo aporta el context-path del servidor (RNF-28).</p>
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Salud del sistema", description = "Estado operativo del API y de su infraestructura")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    @Operation(
            summary = "Estado del sistema",
            description = """
                    Comprueba en vivo la conectividad con PostgreSQL y Redis.
                    Devuelve 200 si todo esta operativo y 503 si algun componente
                    falla, para que un monitor externo (UptimeRobot) lo detecte.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Todos los componentes operativos"),
            @ApiResponse(responseCode = "503", description = "Al menos un componente no responde")
    })
    public ResponseEntity<HealthResponse> health() {
        HealthResponse response = healthService.check();
        HttpStatus httpStatus = response.status() == HealthStatus.UP
                ? HttpStatus.OK
                : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(response);
    }
}
