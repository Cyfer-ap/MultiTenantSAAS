package com.chacha.multitenantsaas.whiteboards;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class WhiteboardDocumentValidatorTest {

    private final WhiteboardDocumentValidator validator = new WhiteboardDocumentValidator();

    @Test
    void acceptsBoundedVisualGraphIncludingCycles() {
        var nodes =
                List.of(node("a", WhiteboardNodeType.STICKY), node("b", WhiteboardNodeType.TEXT));
        var edges = List.of(edge("a", "b"), edge("b", "a"));

        assertThatCode(() -> validator.validate(nodes, edges)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateNodeKeysAndUnknownConnectorEndpoints() {
        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        List.of(
                                                node("same", WhiteboardNodeType.STICKY),
                                                node("same", WhiteboardNodeType.TEXT)),
                                        List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate whiteboard node key");

        assertThatThrownBy(
                        () ->
                                validator.validate(
                                        List.of(node("a", WhiteboardNodeType.STICKY)),
                                        List.of(edge("a", "missing"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existing nodes");
    }

    @Test
    void rejectsSelfAndDuplicateConnectors() {
        var nodes =
                List.of(node("a", WhiteboardNodeType.STICKY), node("b", WhiteboardNodeType.STICKY));

        assertThatThrownBy(() -> validator.validate(nodes, List.of(edge("a", "a"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot target itself");

        assertThatThrownBy(() -> validator.validate(nodes, List.of(edge("a", "b"), edge("a", "b"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Duplicate whiteboard connector");
    }

    private WhiteboardDtos.NodeRequest node(String key, WhiteboardNodeType type) {
        return new WhiteboardDtos.NodeRequest(key, type, "content", 0, 0, 200, 120, 0);
    }

    private WhiteboardDtos.EdgeRequest edge(String source, String target) {
        return new WhiteboardDtos.EdgeRequest(source, target, null);
    }
}
