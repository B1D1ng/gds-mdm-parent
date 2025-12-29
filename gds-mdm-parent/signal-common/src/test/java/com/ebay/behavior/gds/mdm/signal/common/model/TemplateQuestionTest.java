package com.ebay.behavior.gds.mdm.signal.common.model;

import com.ebay.behavior.gds.mdm.common.model.JavaType;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateQuestionTest {

    @Test
    void getAnswerObject_singleValue() {
        var question = new TemplateQuestion();
        question.setAnswer("35");
        question.setIsList(false);
        question.setAnswerJavaType(JavaType.INTEGER);

        Object result = question.getAnswerObject();
        assertThat(result).isInstanceOf(Integer.class).isEqualTo(35);
    }

    @Test
    void getAnswerObject_setValue() {
        var question = new TemplateQuestion();
        question.setAnswer("    32, 33 , 34  ");
        question.setIsList(true);
        question.setAnswerJavaType(JavaType.LONG);

        Object result = question.getAnswerObject();
        assertThat(result).isInstanceOf(Set.class);
        var resultSet = (Set<Long>) result;
        assertThat(resultSet).hasSize(3).contains(32L, 33L, 34L);
    }
}