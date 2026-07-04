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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests unitarios del aspecto de auditoria: confirma que registra la accion enriquecida tras
 * una ejecucion satisfactoria (con actor del contexto y snapshot en {@code details}), que es
 * fail-open ante un fallo del registro, y que no audita cuando el metodo de negocio falla.
 */
@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    private static final String ACTION = "APPROVE_REQUEST";
    private static final String ENTITY_TYPE = "Request";
    private static final String DETAILS_JSON = "{\"actorLogin\":\"admin\"}";

    @Mock
    private AuditRecorder auditRecorder;

    @Mock
    private AuditContextResolver contextResolver;

    @Mock
    private AuditDetailsSerializer detailsSerializer;

    @Mock
    private ProceedingJoinPoint joinPoint;

    /** Entidad de resultado con {@code getId()} para verificar la derivacion de {@code entityId}. */
    private record SampleResult(Long getId) {
    }

    @Test
    void should_insert_audit_entry_when_auditable_use_case_succeeds() throws Throwable {
        // Given
        AuditAspect aspect = new AuditAspect(auditRecorder, contextResolver, detailsSerializer);
        Auditable auditable = auditable(ACTION, ENTITY_TYPE);
        SampleResult result = new SampleResult(15L);
        AuditContext context = new AuditContext(7L, "admin", "10.0.0.5", "JUnit");
        given(joinPoint.proceed()).willReturn(result);
        given(contextResolver.resolve()).willReturn(context);
        given(detailsSerializer.serialize(context, result)).willReturn(DETAILS_JSON);

        // When
        Object returned = aspect.audit(joinPoint, auditable);

        // Then
        assertThat(returned).isEqualTo(result);
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        then(auditRecorder).should().record(captor.capture());
        AuditEntry entry = captor.getValue();
        assertThat(entry.actorEmployeeId()).isEqualTo(7L);
        assertThat(entry.action()).isEqualTo(ACTION);
        assertThat(entry.entityType()).isEqualTo(ENTITY_TYPE);
        assertThat(entry.entityId()).isEqualTo(15L);
        assertThat(entry.details()).isEqualTo(DETAILS_JSON);
    }

    @Test
    void should_insert_audit_entry_with_null_actor_when_system_action() throws Throwable {
        // Given: sin actor autenticado (contexto de sistema)
        AuditAspect aspect = new AuditAspect(auditRecorder, contextResolver, detailsSerializer);
        Auditable auditable = auditable(ACTION, ENTITY_TYPE);
        given(joinPoint.proceed()).willReturn("ok");
        given(contextResolver.resolve()).willReturn(AuditContext.system());

        // When
        aspect.audit(joinPoint, auditable);

        // Then
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        then(auditRecorder).should().record(captor.capture());
        assertThat(captor.getValue().actorEmployeeId()).isNull();
        assertThat(captor.getValue().entityId()).isNull();
    }

    @Test
    void should_return_business_result_when_audit_recorder_fails() throws Throwable {
        // Given: la operacion de negocio se aplica, pero la auditoria falla (fail-open)
        AuditAspect aspect = new AuditAspect(auditRecorder, contextResolver, detailsSerializer);
        Auditable auditable = auditable(ACTION, ENTITY_TYPE);
        given(joinPoint.proceed()).willReturn("ok");
        given(contextResolver.resolve()).willReturn(AuditContext.system());
        willThrow(new IllegalStateException("audit down"))
                .given(auditRecorder).record(org.mockito.ArgumentMatchers.any());

        // When
        Object result = aspect.audit(joinPoint, auditable);

        // Then: se devuelve el resultado de negocio y el fallo de auditoria no propaga
        assertThat(result).isEqualTo("ok");
        then(auditRecorder).should().record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void should_not_record_audit_entry_when_method_throws() throws Throwable {
        // Given
        AuditAspect aspect = new AuditAspect(auditRecorder, contextResolver, detailsSerializer);
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
