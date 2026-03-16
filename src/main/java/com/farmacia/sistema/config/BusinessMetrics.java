package com.farmacia.sistema.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Métricas de negocio para Prometheus/Grafana.
 * Registra contadores de ventas, errores y tiempos de respuesta clave.
 */
@Component
public class BusinessMetrics {

    private final Counter ventasCounter;
    private final Counter ventasAnuladasCounter;
    private final Counter productosCreados;
    private final Counter erroresCounter;
    private final Counter loginExitosos;
    private final Counter loginFallidos;
    private final Timer ventaTimer;

    public BusinessMetrics(MeterRegistry registry) {
        ventasCounter = Counter.builder("nexoerp.ventas.total")
                .description("Total de ventas realizadas")
                .register(registry);
        ventasAnuladasCounter = Counter.builder("nexoerp.ventas.anuladas")
                .description("Total de ventas anuladas")
                .register(registry);
        productosCreados = Counter.builder("nexoerp.productos.creados")
                .description("Total de productos creados")
                .register(registry);
        erroresCounter = Counter.builder("nexoerp.errores.total")
                .description("Total de errores del sistema")
                .register(registry);
        loginExitosos = Counter.builder("nexoerp.auth.login.exitosos")
                .description("Logins exitosos")
                .register(registry);
        loginFallidos = Counter.builder("nexoerp.auth.login.fallidos")
                .description("Logins fallidos")
                .register(registry);
        ventaTimer = Timer.builder("nexoerp.ventas.duracion")
                .description("Tiempo de procesamiento de ventas")
                .register(registry);
    }

    public void registrarVenta() { ventasCounter.increment(); }
    public void registrarVentaAnulada() { ventasAnuladasCounter.increment(); }
    public void registrarProductoCreado() { productosCreados.increment(); }
    public void registrarError() { erroresCounter.increment(); }
    public void registrarLoginExitoso() { loginExitosos.increment(); }
    public void registrarLoginFallido() { loginFallidos.increment(); }
    public Timer getVentaTimer() { return ventaTimer; }
}
