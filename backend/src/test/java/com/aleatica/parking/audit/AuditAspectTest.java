package com.aleatica.parking.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios del aspecto de auditoria: confirma que registra la accion tras
 * una ejecucion satisfactoria y que no audita cuando el metodo de negocio falla.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    private static final String ACTION = "APPROVE_REQUEST";
    private static final String ENTITY_TYPE = "Request";

    @Mock
    private AuditRecorder auditRecorder;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Test
    void should_record_audit_entry_when_method_succeeds() throws Throwable {
        // Given
        AuditAspect aspect = new AuditAspect(auditRecorder);
        Auditable auditable = auditable(ACTION, ENTITY_TYPE);
        given(joinPoint.proceed()).willReturn("ok");

        // When
        Object result = aspect.audit(joinPoint, auditable);

        // Then
        assertThat(result).isEqualTo("ok");
        then(auditRecorder).should().record(ACTION, ENTITY_TYPE, null);
    }

    @Test
    void should_return_business_result_when_audit_recorder_fails() throws Throwable {
        // Given: la operacion de negocio se aplica, pero la auditoria falla (fail-open)
        AuditAspect aspect = new AuditAspect(auditRecorder);
        Auditable auditable = auditable(ACTION, ENTITY_TYPE);
        given(joinPoint.proceed()).willReturn("ok");
        willThrow(new IllegalStateException("audit down"))
                .given(auditRecorder).record(ACTION, ENTITY_TYPE, null);

        // When
        Object result = aspect.audit(joinPoint, auditable);

        // Then: se devuelve el resultado de negocio y el fallo de auditoria no propaga
        assertThat(result).isEqualTo("ok");
        then(auditRecorder).should().record(ACTION, ENTITY_TYPE, null);
    }

    @Test
    void should_not_record_audit_entry_when_method_throws() throws Throwable {
        // Given
        AuditAspect aspect = new AuditAspect(auditRecorder);
        Auditable auditable = mock(Auditable.class);
        given(joinPoint.proceed()).willThrow(new IllegalStateException("boom"));

        // When / Then
        assertThatThrownBy(() -> aspect.audit(joinPoint, auditable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        then(auditRecorder).shouldHaveNoInteractions();
    }

    private static Auditable auditable(String action, String entityType) {
        Auditable auditable = mock(Auditable.class);
        given(auditable.action()).willReturn(action);
        given(auditable.entityType()).willReturn(entityType);
        return auditable;
    }
}
