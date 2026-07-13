package sp26.group3.computer.gateway.filter;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/**
 * Gắn correlation-id vào mỗi request đi qua Gateway để log tương quan
 * xuyên suốt Gateway -> service/monolith. Đây là bước chuẩn bị cho
 * centralized logging (ELK/Loki) ở giai đoạn triển khai sau.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdGlobalFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        final String finalCorrelationId = correlationId;

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(CORRELATION_ID_HEADER, finalCorrelationId)
                .build();
        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
        mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, finalCorrelationId);

        long startedAt = System.currentTimeMillis();
        log.info("[{}] --> {} {}", finalCorrelationId, exchange.getRequest().getMethod(), exchange.getRequest().getPath());

        return chain.filter(mutatedExchange)
                .doFinally(signal -> {
                    long durationMs = System.currentTimeMillis() - startedAt;
                    log.info("[{}] <-- {} {} ({} ms, status={})",
                            finalCorrelationId,
                            exchange.getRequest().getMethod(),
                            exchange.getRequest().getPath(),
                            durationMs,
                            mutatedExchange.getResponse().getStatusCode());
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
