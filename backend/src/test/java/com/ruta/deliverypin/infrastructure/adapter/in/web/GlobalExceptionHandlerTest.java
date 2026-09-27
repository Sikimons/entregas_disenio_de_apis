package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.port.out.TokenProviderPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica, extremo a extremo por HTTP, que GlobalExceptionHandler traduce cada excepcion
 * de dominio (y las tecnicas relevantes) al codigo y formato correctos, usando
 * ThrowingTestController como unico controlador de esta rebanada. No depende de
 * SecurityConfig: aqui solo importa el mapeo de excepciones, no la autorizacion.
 */
@WebMvcTest(controllers = ThrowingTestController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // JwtAuthenticationFilter (Filter + @Component) es detectado igual por @WebMvcTest aunque
    // no se pruebe la seguridad aqui (addFilters = false); solo hace falta que el bean se
    // pueda construir para que arranque el contexto.
    @MockBean
    private TokenProviderPort tokenProviderPort;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void invalidCredentials_returns401() throws Exception {
        // Contrato unico de error (ErrorResponse, Tanda 2): "path" y "timestamp" presentes
        // en cualquier error, no solo en el de validacion.
        mockMvc.perform(get("/test/throw/invalid-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contrasena incorrectos."))
                .andExpect(jsonPath("$.path").value("/test/throw/invalid-credentials"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void tooManyLoginAttempts_returns429() throws Exception {
        mockMvc.perform(get("/test/throw/too-many-login-attempts"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void tooManyConfirmAttempts_returns429() throws Exception {
        mockMvc.perform(get("/test/throw/too-many-confirm-attempts"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void deliveryAlreadyConfirmed_returns409() throws Exception {
        mockMvc.perform(get("/test/throw/already-confirmed"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La entrega de esta factura ya fue confirmada."));
    }

    @Test
    void invalidPin_returns422() throws Exception {
        mockMvc.perform(get("/test/throw/invalid-pin"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("El PIN ingresado no es correcto."));
    }

    @Test
    void deliveryRejectedGeneric_returns400() throws Exception {
        mockMvc.perform(get("/test/throw/delivery-rejected"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La factura no existe."));
    }

    @Test
    void dataIntegrityViolation_returns409WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/throw/integrity-violation"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La operacion no se pudo completar porque el registro esta referenciado por otros datos."));
    }

    @Test
    void duplicateKey_returns409WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/throw/duplicate-key"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un registro con ese valor unico."));
    }

    @Test
    void genericDataAccessFailure_returns503() throws Exception {
        mockMvc.perform(get("/test/throw/data-access"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void erpUnavailable_returns503() throws Exception {
        mockMvc.perform(get("/test/throw/erp-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Servicio de facturacion temporalmente no disponible. Intenta nuevamente en unos segundos."));
    }

    @Test
    void circuitBreakerOpen_returns503() throws Exception {
        mockMvc.perform(get("/test/throw/circuit-open"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void simulatedFailure_returns503() throws Exception {
        mockMvc.perform(get("/test/throw/simulated-failure"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void driverAlreadyExists_returns409() throws Exception {
        mockMvc.perform(get("/test/throw/driver-exists"))
                .andExpect(status().isConflict());
    }

    @Test
    void selfAccountModification_returns409() throws Exception {
        mockMvc.perform(get("/test/throw/self-modification"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No puedes desactivar o eliminar tu propia cuenta."));
    }

    @Test
    void driverNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test/throw/driver-not-found/7"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontro el usuario con id 7."));
    }

    @Test
    void illegalArgument_returns400() throws Exception {
        mockMvc.perform(get("/test/throw/illegal-argument"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedException_returns500WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/throw/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno. Intenta nuevamente."));
    }

    @Test
    void validationFailure_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/throw/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Uno o mas campos no son validos."))
                .andExpect(jsonPath("$.errors.name").exists());
    }
}
