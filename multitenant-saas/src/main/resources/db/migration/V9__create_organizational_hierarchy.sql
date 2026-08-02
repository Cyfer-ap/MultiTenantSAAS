CREATE TABLE organizational_units (
                                      id UUID NOT NULL,
                                      tenant_id UUID NOT NULL,
                                      parent_unit_id UUID,

                                      name VARCHAR(150) NOT NULL,
                                      code VARCHAR(100),
                                      type VARCHAR(30) NOT NULL,
                                      status VARCHAR(30) NOT NULL,

                                      created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                      updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                      CONSTRAINT pk_organizational_units
                                          PRIMARY KEY (id),

                                      CONSTRAINT uk_org_unit_tenant_code
                                          UNIQUE (tenant_id, code),

                                      CONSTRAINT fk_org_unit_tenant
                                          FOREIGN KEY (tenant_id)
                                              REFERENCES tenants (id),

                                      CONSTRAINT fk_org_unit_parent
                                          FOREIGN KEY (parent_unit_id)
                                              REFERENCES organizational_units (id),

                                      CONSTRAINT ck_org_unit_not_self_parent
                                          CHECK (
                                              parent_unit_id IS NULL
                                                  OR parent_unit_id <> id
                                              )
);

CREATE INDEX idx_org_unit_tenant
    ON organizational_units (tenant_id);

CREATE INDEX idx_org_unit_tenant_parent
    ON organizational_units (
                             tenant_id,
                             parent_unit_id
        );

CREATE INDEX idx_org_unit_tenant_status
    ON organizational_units (
                             tenant_id,
                             status
        );

CREATE INDEX idx_org_unit_tenant_type
    ON organizational_units (
                             tenant_id,
                             type
        );


CREATE TABLE organizational_unit_closure (
                                             tenant_id UUID NOT NULL,
                                             ancestor_unit_id UUID NOT NULL,
                                             descendant_unit_id UUID NOT NULL,
                                             depth INTEGER NOT NULL,

                                             CONSTRAINT pk_organizational_unit_closure
                                                 PRIMARY KEY (
                                                              tenant_id,
                                                              ancestor_unit_id,
                                                              descendant_unit_id
                                                     ),

                                             CONSTRAINT fk_org_closure_tenant
                                                 FOREIGN KEY (tenant_id)
                                                     REFERENCES tenants (id),

                                             CONSTRAINT fk_org_closure_ancestor
                                                 FOREIGN KEY (ancestor_unit_id)
                                                     REFERENCES organizational_units (id),

                                             CONSTRAINT fk_org_closure_descendant
                                                 FOREIGN KEY (descendant_unit_id)
                                                     REFERENCES organizational_units (id),

                                             CONSTRAINT ck_org_closure_depth
                                                 CHECK (depth >= 0),

                                             CONSTRAINT ck_org_closure_self_depth
                                                 CHECK (
                                                     (
                                                         ancestor_unit_id = descendant_unit_id
                                                             AND depth = 0
                                                         )
                                                         OR
                                                     (
                                                         ancestor_unit_id <> descendant_unit_id
                                                             AND depth > 0
                                                         )
                                                     )
);

CREATE INDEX idx_org_closure_ancestor
    ON organizational_unit_closure (
                                    tenant_id,
                                    ancestor_unit_id,
                                    depth
        );

CREATE INDEX idx_org_closure_descendant
    ON organizational_unit_closure (
                                    tenant_id,
                                    descendant_unit_id,
                                    depth
        );