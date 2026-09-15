CREATE TABLE whiteboards (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    normalized_name VARCHAR(100) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_whiteboards PRIMARY KEY (id),
    CONSTRAINT uk_whiteboard_name UNIQUE (tenant_id, project_id, normalized_name),
    CONSTRAINT uk_whiteboard_scope_id UNIQUE (tenant_id, project_id, id),
    CONSTRAINT fk_whiteboard_project
        FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id),
    CONSTRAINT fk_whiteboard_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_whiteboard_version CHECK (version >= 0)
);

CREATE INDEX idx_whiteboards_project_name
    ON whiteboards (tenant_id, project_id, name);

CREATE TABLE whiteboard_nodes (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    board_id UUID NOT NULL,
    node_key VARCHAR(64) NOT NULL,
    node_type VARCHAR(20) NOT NULL,
    content VARCHAR(4000),
    linked_task_id UUID,
    position_x INTEGER NOT NULL,
    position_y INTEGER NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    z_index INTEGER NOT NULL,

    CONSTRAINT pk_whiteboard_nodes PRIMARY KEY (id),
    CONSTRAINT uk_whiteboard_node_key UNIQUE (tenant_id, project_id, board_id, node_key),
    CONSTRAINT fk_whiteboard_node_board
        FOREIGN KEY (tenant_id, project_id, board_id)
            REFERENCES whiteboards (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_whiteboard_node_linked_task
        FOREIGN KEY (tenant_id, project_id, linked_task_id)
            REFERENCES project_tasks (tenant_id, project_id, id),
    CONSTRAINT ck_whiteboard_node_type
        CHECK (node_type IN ('STICKY', 'TEXT', 'SHAPE')),
    CONSTRAINT ck_whiteboard_node_position_x CHECK (position_x BETWEEN -100000 AND 100000),
    CONSTRAINT ck_whiteboard_node_position_y CHECK (position_y BETWEEN -100000 AND 100000),
    CONSTRAINT ck_whiteboard_node_width CHECK (width BETWEEN 40 AND 4000),
    CONSTRAINT ck_whiteboard_node_height CHECK (height BETWEEN 40 AND 4000),
    CONSTRAINT ck_whiteboard_node_z_index CHECK (z_index BETWEEN -10000 AND 10000)
);

CREATE INDEX idx_whiteboard_nodes_board
    ON whiteboard_nodes (tenant_id, project_id, board_id, z_index, node_key);

CREATE TABLE whiteboard_edges (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    board_id UUID NOT NULL,
    source_node_key VARCHAR(64) NOT NULL,
    target_node_key VARCHAR(64) NOT NULL,
    label VARCHAR(500),

    CONSTRAINT pk_whiteboard_edges PRIMARY KEY (id),
    CONSTRAINT uk_whiteboard_edge UNIQUE (
        tenant_id, project_id, board_id, source_node_key, target_node_key
    ),
    CONSTRAINT fk_whiteboard_edge_board
        FOREIGN KEY (tenant_id, project_id, board_id)
            REFERENCES whiteboards (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT fk_whiteboard_edge_source
        FOREIGN KEY (tenant_id, project_id, board_id, source_node_key)
            REFERENCES whiteboard_nodes (tenant_id, project_id, board_id, node_key)
            ON DELETE CASCADE,
    CONSTRAINT fk_whiteboard_edge_target
        FOREIGN KEY (tenant_id, project_id, board_id, target_node_key)
            REFERENCES whiteboard_nodes (tenant_id, project_id, board_id, node_key)
            ON DELETE CASCADE,
    CONSTRAINT ck_whiteboard_edge_not_self CHECK (source_node_key <> target_node_key)
);

CREATE INDEX idx_whiteboard_edges_board
    ON whiteboard_edges (tenant_id, project_id, board_id, source_node_key);
