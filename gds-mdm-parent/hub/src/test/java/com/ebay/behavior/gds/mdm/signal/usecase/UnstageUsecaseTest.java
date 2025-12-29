package com.ebay.behavior.gds.mdm.signal.usecase;

import com.ebay.behavior.gds.mdm.common.exception.DataNotFoundException;
import com.ebay.behavior.gds.mdm.signal.common.model.SignalTemplate;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstageRequest;
import com.ebay.behavior.gds.mdm.signal.common.model.UnstagedField;
import com.ebay.behavior.gds.mdm.signal.service.SignalTemplateService;
import com.ebay.behavior.gds.mdm.signal.service.TemplateQuestionService;

import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.ebay.behavior.gds.mdm.common.model.CompletionStatus.DRAFT;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.signalTemplate;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomSmallString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstageUsecaseTest {

    private final long planId = 1L;

    @Mock
    private SignalTemplateService signalService;

    @Mock
    private TemplateQuestionService questionService;

    @Spy
    @InjectMocks
    private UnstageUsecase usecase;

    private UnstageRequest params;
    private long signalTemplateId;
    private SignalTemplate signalTemplate;
    private final String name = getRandomSmallString();
    private final String desc = getRandomSmallString();

    @BeforeEach
    void setUp() {
        signalTemplate = signalTemplate().withId(getRandomLong());
        signalTemplateId = signalTemplate.getId();
        params = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);

        reset(signalService, questionService, usecase);
    }

    @Test
    void copySignalFromTemplate_signalTemplateNotFound() {
        when(signalService.getByIdWithAssociations(signalTemplateId)).thenThrow(new DataNotFoundException("signalTemplate"));

        assertThatThrownBy(() -> usecase.copySignalFromTemplate(params))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessageContaining("signalTemplate");
    }

    @Test
    void copySignalFromTemplate_signalTemplateNotCompleted() {
        signalTemplate.setCompletionStatus(DRAFT);
        when(signalService.getByIdWithAssociations(signalTemplateId)).thenReturn(signalTemplate);

        assertThatThrownBy(() -> usecase.copySignalFromTemplate(params))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be COMPLETED");
    }

    @Test
    void validateUserAnswers_withNullAnsweredQuestions() {
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, null);
        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.of());

        var enrichedQuestions = usecase.validateUserAnswers(signalService, request);

        assertThat(enrichedQuestions).isEmpty();
    }

    @Test
    void validateUserAnswers_withAnsweredQuestionsLessThanPersistentQuestions_error() {
        var answeredQuestion = templateQuestion().setQuestion("question1").setAnswer("answer1").withId(1L);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(answeredQuestion));
        var persistedQuestions = List.of(
                templateQuestion().setQuestion("question1").setAnswerPropertyName("prop1").withId(1L),
                templateQuestion().setQuestion("question2").setAnswerPropertyName("prop2").withId(2L));

        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.copyOf(persistedQuestions));
        when(questionService.getByIdWithAssociations(1L)).thenReturn(persistedQuestions.get(0));
        when(questionService.getByIdWithAssociations(2L)).thenReturn(persistedQuestions.get(1));

        assertThatThrownBy(() -> usecase.validateUserAnswers(signalService, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Answered user questions do not match persisted questions");

        verify(questionService, times(1)).getByIdWithAssociations(1L);
    }

    @Test
    void validateUserAnswers_withAnsweredQuestionsMoreThanPersistentQuestions_error() {
        var answeredQuestion1 = templateQuestion().setQuestion("question1").setAnswer("answer1").withId(1L);
        var answeredQuestion2 = templateQuestion().setQuestion("question2").setAnswer("answer2").withId(2L);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(answeredQuestion1, answeredQuestion2));
        var persistedQuestion = templateQuestion().setQuestion("question1").setAnswerPropertyName("prop1").withId(1L);

        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.of(persistedQuestion));
        when(questionService.getByIdWithAssociations(1L)).thenReturn(persistedQuestion);

        assertThatThrownBy(() -> usecase.validateUserAnswers(signalService, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Answered user questions do not match persisted questions");

        verify(questionService, times(1)).getByIdWithAssociations(1L);
    }

    @Test
    void validateUserAnswers_withUnansweredQuestion_error() {
        var answeredQuestion = templateQuestion().setQuestion("question").setAnswer("   ").withId(1L);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(answeredQuestion));
        var persistedQuestion = templateQuestion().setQuestion("question").setAnswerPropertyName("prop").withId(1L);

        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.of(persistedQuestion));
        when(questionService.getByIdWithAssociations(1L)).thenReturn(persistedQuestion);

        assertThatThrownBy(() -> usecase.validateUserAnswers(signalService, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User answers cannot be blank");

        verify(questionService, times(1)).getByIdWithAssociations(1L);
    }

    @Test
    void validateUserAnswers_withoutQuestions_emptyList() {
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of());
        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.of());

        val enrichedQuestions = usecase.validateUserAnswers(signalService, request);

        assertThat(enrichedQuestions).isEmpty();
    }

    @Test
    void validateUserAnswers_withAnsweredQuestionsButDifferentOrder() {
        var answeredQuestion1 = templateQuestion().setQuestion("question1").setAnswer("answer1").withId(1L);
        var answeredQuestion2 = templateQuestion().setQuestion("question2").setAnswer("answer2").withId(2L);
        var answeredQuestion3 = templateQuestion().setQuestion("question3").setAnswer("answer3").withId(3L);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(answeredQuestion1, answeredQuestion2, answeredQuestion3));
        var persistedQuestions = List.of(
                templateQuestion().setQuestion("question3").setAnswerPropertyName("prop3").withId(3L),
                templateQuestion().setQuestion("question2").setAnswerPropertyName("prop2").withId(2L),
                templateQuestion().setQuestion("question1").setAnswerPropertyName("prop1").withId(1L));

        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.copyOf(persistedQuestions));
        when(questionService.getByIdWithAssociations(1L)).thenReturn(persistedQuestions.get(0));
        when(questionService.getByIdWithAssociations(2L)).thenReturn(persistedQuestions.get(1));
        when(questionService.getByIdWithAssociations(3L)).thenReturn(persistedQuestions.get(2));

        val enrichedQuestions = usecase.validateUserAnswers(signalService, request);

        assertThat(enrichedQuestions).hasSize(3);
        verify(questionService, times(1)).getByIdWithAssociations(1L);
        verify(questionService, times(1)).getByIdWithAssociations(2L);
        verify(questionService, times(1)).getByIdWithAssociations(3L);
    }

    @Test
    void validateUserAnswers_withAnsweredQuestionsButTemplateIdDoesNotMatch_error() {
        var answeredQuestion = templateQuestion().setQuestion("question").setAnswer("answer").withId(1L);
        var request = UnstageRequest.ofTemplate(planId, signalTemplateId, name, desc, Set.of(answeredQuestion));
        var persistedQuestion = templateQuestion().setQuestion("question").setAnswerPropertyName("prop").withId(2L);

        when(signalService.getQuestions(signalTemplateId)).thenReturn(Set.of(persistedQuestion));
        when(questionService.getByIdWithAssociations(2L)).thenReturn(persistedQuestion);

        assertThatThrownBy(() -> usecase.validateUserAnswers(signalService, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Answered user questions do not match persisted questions");
    }


    @Test
    void extractTag() {
        var field = UnstagedField.builder().name("tag").expression("event.eventPayload.timestamp - viewedTs").build();
        String tag = usecase.extractTag(field);
        assertThat(tag).isEqualTo("timestamp");

        field.setExpression("event.getEventPayload().getTimestamp()");
        tag = usecase.extractTag(field);
        assertThat(tag).isEqualTo("timestamp");

        field.setExpression("event.getEventPayload().getEventProperties().get(\"duration\")");
        tag = usecase.extractTag(field);
        assertThat(tag).isEqualTo("duration");

        field.setExpression("event.eventPayload.eventProperties.duration");
        tag = usecase.extractTag(field);
        assertThat(tag).isEqualTo("duration");

        field.setExpression("event.eventPayload.timestamp");
        tag = usecase.extractTag(field);
        assertThat(tag).isEqualTo("timestamp");

        field.setExpression("no_such_patters");
        tag = usecase.extractTag(field);
        assertThat(tag).isEqualTo(field.getName());
    }
}