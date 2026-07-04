package com.aleatica.parking.audit;

import static org.mockito.BDDMockito.then;

import com.aleatica.parking.audit.application.RetentionPurgeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test unitario del job de purga: delega en el servicio de purga sin logica propia (el
 * disparo temporal se ejerce invocando el metodo directamente, S2925).
 */
@ExtendWith(MockitoExtension.class)
class RetentionPurgeJobTest {

    @Mock
    private RetentionPurgeService purgeService;

    @Test
    void should_invoke_purge_service_when_daily_job_runs() {
        // Given
        RetentionPurgeJob job = new RetentionPurgeJob(purgeService);

        // When
        job.runDailyPurge();

        // Then
        then(purgeService).should().purge();
    }
}
