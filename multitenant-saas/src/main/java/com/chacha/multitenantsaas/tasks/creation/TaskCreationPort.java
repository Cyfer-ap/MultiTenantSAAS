package com.chacha.multitenantsaas.tasks.creation;

public interface TaskCreationPort {

    TaskCreationResult createTask(TaskCreationCommand command);
}
