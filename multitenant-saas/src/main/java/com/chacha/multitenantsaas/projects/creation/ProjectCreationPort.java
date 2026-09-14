package com.chacha.multitenantsaas.projects.creation;

public interface ProjectCreationPort {

    ProjectCreationResult createProject(ProjectCreationCommand command);
}
