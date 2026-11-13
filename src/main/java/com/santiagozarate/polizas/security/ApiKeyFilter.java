package com.santiagozarate.polizas.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Seguridad mínima pedida en la prueba: toda petición debe traer el
 * header x-api-key con el valor configurado. Si no, responde 401.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "x-api-key";

    private final byte[] apiKeyEsperada;

    public ApiKeyFilter(@Value("${seguridad.api-key}") String apiKey) {
        this.apiKeyEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recibida = request.getHeader(HEADER);
        if (recibida == null || !sonIguales(recibida)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":401,\"detail\":\"Falta el header x-api-key o es inválido\"}");
            return; // corta la cadena: la petición no llega al controlador
        }
        chain.doFilter(request, response);
    }

    /** Comparación en tiempo constante para no filtrar pistas por tiempos de respuesta. */
    private boolean sonIguales(String recibida) {
        return MessageDigest.isEqual(apiKeyEsperada, recibida.getBytes(StandardCharsets.UTF_8));
    }
}
