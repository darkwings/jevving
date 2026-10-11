package com.frank.jevving.infrastructure.jev;

import java.util.Map;

public record JevRequest(String state, String model, Map<String, JevQuestion> questions) {
}
