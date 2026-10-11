package com.frank.jevving.domain.service;

import com.frank.jevving.domain.valueobjects.Answers;
import com.frank.jevving.domain.valueobjects.Questions;
import io.smallrye.mutiny.Uni;

public interface RequestAssessor {

    Uni<Answers> assess(String request, Questions questions);
}
