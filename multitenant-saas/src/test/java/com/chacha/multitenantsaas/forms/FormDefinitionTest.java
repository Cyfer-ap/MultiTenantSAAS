package com.chacha.multitenantsaas.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chacha.multitenantsaas.entity.ProjectTaskPriority;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FormDefinitionTest {

    @Test
    void rejectsPauseBeforeActivationAsClientValidationError() {
        FormDefinition definition = definition();

        assertThatThrownBy(definition::pause)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only active forms can be paused");
    }

    @Test
    void supportsActivateThenPauseLifecycle() {
        FormDefinition definition = definition();

        definition.activate();
        assertThat(definition.getStatus()).isEqualTo(FormStatus.ACTIVE);

        definition.pause();
        assertThat(definition.getStatus()).isEqualTo(FormStatus.PAUSED);
    }

    private FormDefinition definition() {
        return new FormDefinition(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Incident intake",
                "incident intake",
                null,
                "title",
                null,
                null,
                ProjectTaskPriority.MEDIUM,
                null);
    }
}
