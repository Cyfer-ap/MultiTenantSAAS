package com.chacha.multitenantsaas.tasks.automation;

public interface TaskAutomationMutationPort {

    TaskAutomationSnapshot mutate(TaskAutomationMutationCommand command);
}
