package com.frank.jevving.infrastructure.jev;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JevQuestion.Noul.class, name = "noul"),
        @JsonSubTypes.Type(value = JevQuestion.Choice.class, name = "choice"),
        @JsonSubTypes.Type(value = JevQuestion.Score.class, name = "score")
})
public sealed interface JevQuestion {

    String instructions();

    /** Yes/no question. Optional criteria keys: {@code true}, {@code false}. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Noul(String instructions, Map<String, String> criteria) implements JevQuestion {
        public Noul(String instructions) {
            this(instructions, null);
        }
    }

    /** Pick one option. Option id → description (description may be null, max 255 options). */
    record Choice(String instructions, Map<String, String> criteria) implements JevQuestion {
    }

    /** Rate on an ordered scale (2 to 10 levels). */
    record Score(String instructions, List<String> criteria) implements JevQuestion {
    }
}
