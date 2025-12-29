package com.ebay.behavior.gds.mdm.signal.service.userAnswer;

import com.ebay.behavior.gds.mdm.common.model.JavaType;
import com.ebay.behavior.gds.mdm.common.model.SurfaceType;

import org.junit.jupiter.api.Test;

import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.templateQuestion;
import static com.ebay.behavior.gds.mdm.signal.testUtil.TestModelUtils.unstagedEvent;
import static com.ebay.behavior.gds.mdm.commonTestUtil.TestUtils.getRandomLong;
import static org.assertj.core.api.Assertions.assertThat;

class UserAnswerPropertySetterTest {

    private final UserAnswerSetter setter = new UserAnswerPropertySetter();

    @Test
    void apply_string() {
        var answer = "answer";
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.STRING)
                .setAnswerPropertyName("name")
                .setAnswer(answer)
                .setIsList(false)
                .setAnswerPropertySetterClass(UserAnswerPropertySetter.class.getSimpleName());
        var event = unstagedEvent();

        setter.apply(question, event);

        assertThat(event.getName()).isEqualTo(answer);
    }

    @Test
    void apply_enum() {
        var answer = SurfaceType.RAPTOR_IO;
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.STRING)
                .setAnswerPropertyName("surfaceType")
                .setAnswer(answer.name())
                .setIsList(false)
                .setAnswerPropertySetterClass(UserAnswerPropertySetter.class.getSimpleName());
        var event = unstagedEvent().toBuilder().surfaceType(SurfaceType.IOS).build();

        setter.apply(question, event);

        assertThat(event.getSurfaceType()).isEqualTo(answer);
    }

    @Test
    void apply_long() {
        var answer = getRandomLong();
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("eventSourceId")
                .setAnswer(String.valueOf(answer))
                .setIsList(false)
                .setAnswerPropertySetterClass(UserAnswerPropertySetter.class.getSimpleName());
        var event = unstagedEvent();

        setter.apply(question, event);

        assertThat(event.getEventSourceId()).isEqualTo(answer);
    }

    @Test
    void apply_set() {
        var answer = "1, 1, 2, 3, 2, 3";
        var question = templateQuestion()
                .setAnswerJavaType(JavaType.LONG)
                .setAnswerPropertyName("pageIds")
                .setAnswer(answer)
                .setIsList(true)
                .setAnswerPropertySetterClass(UserAnswerPropertySetter.class.getSimpleName());
        var event = unstagedEvent();

        setter.apply(question, event);

        assertThat(event.getPageIds()).containsOnly(1L, 2L, 3L);
    }
}