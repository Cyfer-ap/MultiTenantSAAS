package com.chacha.multitenantsaas.forms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "form_fields",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_form_field_key",
                    columnNames = {"tenant_id", "project_id", "form_id", "field_key"}),
            @UniqueConstraint(
                    name = "uk_form_field_position",
                    columnNames = {"tenant_id", "project_id", "form_id", "position_index"})
        })
public class FormField {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private FormDefinition definition;

    @Column(name = "field_key", nullable = false, length = 64)
    private String fieldKey;

    @Column(nullable = false, length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    private FormFieldType fieldType;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "options_json", length = 8000)
    private String optionsJson;

    @Column(name = "position_index", nullable = false)
    private int positionIndex;

    protected FormField() {}

    FormField(
            FormDefinition definition,
            String fieldKey,
            String label,
            FormFieldType fieldType,
            boolean required,
            String optionsJson,
            int positionIndex) {
        this.definition = definition;
        this.tenantId = definition.getTenantId();
        this.projectId = definition.getProjectId();
        this.fieldKey = fieldKey;
        this.label = label;
        this.fieldType = fieldType;
        this.required = required;
        this.optionsJson = optionsJson;
        this.positionIndex = positionIndex;
    }

    public UUID getId() {
        return id;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getLabel() {
        return label;
    }

    public FormFieldType getFieldType() {
        return fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public int getPositionIndex() {
        return positionIndex;
    }
}
