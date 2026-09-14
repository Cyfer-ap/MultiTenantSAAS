CREATE TABLE workflow_definitions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    definition_version INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_workflow_definitions PRIMARY KEY (id),
    CONSTRAINT uk_workflow_definition_name UNIQUE (tenant_id, normalized_name),
    CONSTRAINT uk_workflow_definition_scope_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_workflow_definition_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_workflow_definition_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_workflow_definition_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED')),
    CONSTRAINT ck_workflow_definition_version
        CHECK (definition_version >= 1)
);

CREATE INDEX idx_workflow_definitions_tenant_status_name
    ON workflow_definitions (tenant_id, status, name);

CREATE TABLE workflow_nodes (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    node_type VARCHAR(20) NOT NULL,
    operation_type VARCHAR(80) NOT NULL,
    configuration_json VARCHAR(4000),
    position_x INTEGER NOT NULL,
    position_y INTEGER NOT NULL,

    CONSTRAINT pk_workflow_nodes PRIMARY KEY (id),
    CONSTRAINT uk_workflow_node_key UNIQUE (tenant_id, workflow_id, node_key),
    CONSTRAINT fk_workflow_node_definition
        FOREIGN KEY (tenant_id, workflow_id)
            REFERENCES workflow_definitions (tenant_id, id)
            ON DELETE CASCADE,
    CONSTRAINT ck_workflow_node_type
        CHECK (node_type IN ('TRIGGER', 'CONDITION', 'ACTION')),
    CONSTRAINT ck_workflow_node_position_x
        CHECK (position_x BETWEEN -100000 AND 100000),
    CONSTRAINT ck_workflow_node_position_y
        CHECK (position_y BETWEEN -100000 AND 100000)
);

CREATE INDEX idx_workflow_nodes_definition
    ON workflow_nodes (tenant_id, workflow_id, node_key);

CREATE TABLE workflow_edges (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    source_node_key VARCHAR(64) NOT NULL,
    target_node_key VARCHAR(64) NOT NULL,
    branch_type VARCHAR(20) NOT NULL,

    CONSTRAINT pk_workflow_edges PRIMARY KEY (id),
    CONSTRAINT uk_workflow_edge_branch
        UNIQUE (tenant_id, workflow_id, source_node_key, branch_type),
    CONSTRAINT fk_workflow_edge_definition
        FOREIGN KEY (tenant_id, workflow_id)
            REFERENCES workflow_definitions (tenant_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_workflow_edge_source
        FOREIGN KEY (tenant_id, workflow_id, source_node_key)
            REFERENCES workflow_nodes (tenant_id, workflow_id, node_key)
            ON DELETE CASCADE,
    CONSTRAINT fk_workflow_edge_target
        FOREIGN KEY (tenant_id, workflow_id, target_node_key)
            REFERENCES workflow_nodes (tenant_id, workflow_id, node_key)
            ON DELETE CASCADE,
    CONSTRAINT ck_workflow_edge_branch
        CHECK (branch_type IN ('DEFAULT', 'TRUE', 'FALSE')),
    CONSTRAINT ck_workflow_edge_not_self
        CHECK (source_node_key <> target_node_key)
);

CREATE INDEX idx_workflow_edges_definition
    ON workflow_edges (tenant_id, workflow_id, source_node_key);

CREATE TABLE workflow_executions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    workflow_version INTEGER NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    trigger_operation VARCHAR(80) NOT NULL,
    source_entity_type VARCHAR(50),
    source_entity_id UUID,
    status VARCHAR(20) NOT NULL,
    explanation VARCHAR(2000),
    error_message VARCHAR(2000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,

    CONSTRAINT pk_workflow_executions PRIMARY KEY (id),
    CONSTRAINT uk_workflow_execution_event UNIQUE (tenant_id, workflow_id, event_key),
    CONSTRAINT fk_workflow_execution_definition
        FOREIGN KEY (tenant_id, workflow_id)
            REFERENCES workflow_definitions (tenant_id, id),
    CONSTRAINT ck_workflow_execution_version
        CHECK (workflow_version >= 1),
    CONSTRAINT ck_workflow_execution_status
        CHECK (status IN ('RUNNING', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX idx_workflow_executions_history
    ON workflow_executions (tenant_id, workflow_id, started_at);

CREATE INDEX idx_workflow_executions_tenant_started
    ON workflow_executions (tenant_id, started_at DESC);
