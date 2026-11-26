package com.board;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import com.board.answer.Answer;

@Component
public class CommonUtil {
    public String markdown(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        return renderer.render(document);
    }

    public Map<Integer, String> renderAnswerHtmlById(List<Answer> answers) {
    Map<Integer, String> answerHtmlById = new HashMap<>();
    for (Answer answer : answers) {
        answerHtmlById.put(answer.getId(), markdown(answer.getContent()));
    }
    return answerHtmlById;
}
}

