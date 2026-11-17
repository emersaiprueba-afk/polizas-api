package com.santiagozarate.polizas.api;

import com.santiagozarate.polizas.entity.Poliza;
import com.santiagozarate.polizas.entity.Riesgo;
import com.santiagozarate.polizas.integration.CoreGateway;
import com.santiagozarate.polizas.repository.PolizaRepository;
import com.santiagozarate.polizas.security.ApiKeyFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración: levantan Spring completo (filtro, controladores,
 * servicio, JPA con H2) y llaman los endpoints como lo haría un cliente.
 * El CORE se reemplaza por un mock para verificar cuándo se le avisa.
 */
@SpringBootTest
@ActiveProfiles("test")
class PolizaApiIntegrationTest {

    private static final String API_KEY = "123456";

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojDePrueba() {
            return Clock.fixed(LocalDate.of(2026, 11, 17).atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    ZoneId.systemDefault());
        }
    }

    @Autowired
    private WebApplicationContext contexto;

    @Autowired
    private ApiKeyFilter apiKeyFilter;

    @Autowired
    private PolizaRepository polizaRepository;

    @MockitoBean
    private CoreGateway coreGateway;

    private MockMvc mvc;
    private Long individualId;
    private Long colectivaId;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).addFilters(apiKeyFilter).build();
        polizaRepository.deleteAll();
        individualId = polizaRepository.save(Poliza.individual("Laura", LocalDate.of(2026, 1, 1), 12,
                new BigDecimal("1500000"), new Riesgo("Cra 7 # 72-10", "Laura", "Pedro"))).getId();
        Poliza colectiva = Poliza.colectiva("Inmobiliaria", LocalDate.of(2026, 3, 1), 12, new BigDecimal("2000000"));
        colectiva.agregarRiesgo(new Riesgo("Cl 100 # 15-20", "Carlos", "Ana"));
        colectivaId = polizaRepository.save(colectiva).getId();
    }

    @Test
    void sinApiKeyResponde401() throws Exception {
        mvc.perform(get("/polizas")).andExpect(status().isUnauthorized());
    }

    @Test
    void listaConFiltroPorTipo() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "COLECTIVA").header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("COLECTIVA"));
    }

    @Test
    void filtroConValorInvalidoResponde400EnFormatoProblemDetails() throws Exception {
        mvc.perform(get("/polizas").param("tipo", "XYZ").header("x-api-key", API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void jsonMalFormadoResponde400EnFormatoProblemDetails() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{ esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void renovarDosVecesResponde422LaSegundaYAvisaAlCoreUnaSolaVez() throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", individualId).header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RENOVADA"));
        mvc.perform(post("/polizas/{id}/renovar", individualId).header("x-api-key", API_KEY))
                .andExpect(status().is(422));

        verify(coreGateway, times(1)).notificarActualizacion(individualId);
    }

    @Test
    void agregarRiesgoAColectivaResponde201YAvisaAlCore() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"Cl 50 # 10-20\",\"arrendatario\":\"Juan\",\"arrendador\":\"Sofia\"}"))
                .andExpect(status().isCreated());

        verify(coreGateway).notificarActualizacion(colectivaId);
    }

    @Test
    void agregarRiesgoAIndividualResponde422YNoAvisaAlCore() throws Exception {
        mvc.perform(post("/polizas/{id}/riesgos", individualId).header("x-api-key", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"direccionInmueble\":\"X\",\"arrendatario\":\"Y\",\"arrendador\":\"Z\"}"))
                .andExpect(status().is(422));

        verify(coreGateway, never()).notificarActualizacion(any());
    }

    @Test
    void cancelarPolizaCancelaSusRiesgos() throws Exception {
        mvc.perform(post("/polizas/{id}/cancelar", colectivaId).header("x-api-key", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        mvc.perform(get("/polizas/{id}/riesgos", colectivaId).header("x-api-key", API_KEY))
                .andExpect(jsonPath("$[0].estado").value("CANCELADO"));
    }

    @Test
    void polizaInexistenteResponde404() throws Exception {
        mvc.perform(post("/polizas/{id}/renovar", 9999).header("x-api-key", API_KEY))
                .andExpect(status().isNotFound());
    }
}
