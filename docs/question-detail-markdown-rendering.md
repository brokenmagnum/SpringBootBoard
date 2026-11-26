# 질문 상세 화면의 Markdown 렌더링

질문과 답변 본문을 Markdown에서 HTML로 변환하는 과정에서 발생한 Thymeleaf 오류를 조사하고, 변환을 Java 코드에서 수행하는 방식으로 해결한 내용을 정리했습니다. 현재 코드는 질문 상세 화면에 HTML을 전달할 때 질문 본문과 각 답변 본문을 미리 변환합니다.

## 오류 증상과 원인

2026년 11월 26일 14시 28분경(KST), 질문 상세 페이지 요청에서 `TemplateInputException`이 발생했습니다. 당시 로그는 `question_detail.html` 8행 36열을 가리켰습니다. 그 위치의 표현식은 `${@commonUtil.markdown(question.content)}`였으며, `th:utext`가 이 표현식을 평가해 질문 본문을 출력하려고 했습니다. 답변 본문의 당시 45행도 같은 방식으로 `@commonUtil.markdown(answer.content)`를 호출했습니다.

`TemplateInputException`은 템플릿을 처리하는 과정에서 발생한 예외를 감쌀 수 있습니다. 처음 받은 로그 한 줄에는 내부 예외가 없었기 때문에 `commonUtil` 빈 등록, CommonMark 클래스 로딩, `null` 본문의 가능성을 검토했습니다. 이후 터미널에서 실행 클래스패스와 문제의 표현식을 확인해 원인을 좁혔습니다.

Thymeleaf 3.1.7은 `th:utext` 표현식을 제한된 모드에서 평가합니다. 이 모드에서는 `@commonUtil`과 같은 Spring 빈 참조가 차단됩니다. `CommonUtil`이 Spring에 등록되어 있어도 표현식을 평가하는 단계에서 예외가 발생하므로 `markdown()` 호출까지 도달하지 못합니다. 예를 들어 `**안녕하세요**`를 HTML로 변환하기 전에 `@commonUtil` 참조에서 처리가 멈춥니다.

## 터미널 재현 결과

오류를 기록한 서버 프로세스와 IDE 터미널의 과거 출력에는 접근할 수 없었습니다. 대신 오류 시각 직전에 생성된 VS Code Java 실행 인자 파일에서 CommonMark 0.21.0이 클래스패스에 포함된 것을 확인했습니다. 해당 클래스패스로 `CommonUtil` 빈을 등록하고, 질문과 답변의 본문 표현식을 각각 메모리에서 렌더링했습니다.

질문 본문에서 최초 로그와 동일한 8행 36열 오류가 재현됐고, 답변 본문에서도 당시 위치인 45행 36열에서 같은 오류가 발생했습니다. 재현 과정에서 확인한 내부 예외는 다음과 같습니다.

```text
TemplateProcessingException: Instantiation of new objects and access to static classes or parameters is forbidden in this context
```

같은 환경에서 Java가 변환한 HTML을 모델 변수로 전달하자 질문 한 건과 서로 다른 ID의 답변 두 건이 각각 `<strong>` 태그로 렌더링됐습니다. 이 검증은 본문 템플릿 조각에 대해 수행했으며, 서버를 시작하거나 전체 HTTP 요청을 실행하지 않았습니다.

## 현재 코드의 처리 흐름

### 1. Java에서 본문을 HTML로 변환

`CommonUtil.markdown()`은 CommonMark의 `Parser`로 Markdown을 파싱하고 `HtmlRenderer`로 HTML을 생성합니다. `renderAnswerHtmlById()`는 답변 목록을 순회하면서 답변 ID와 변환된 HTML을 `Map<Integer, String>`에 저장합니다. 예를 들어 ID가 `42`인 답변의 HTML은 키 `42`에 저장됩니다. [CommonUtil.java](../board/src/main/java/com/board/CommonUtil.java) · [build.gradle](../board/build.gradle)

### 2. 상세 화면을 반환하는 두 경로에서 모델에 담기

두 컨트롤러는 `CommonUtil`을 주입받아 `question_detail`을 반환하기 전에 아래 모델 속성을 준비합니다.

```java
model.addAttribute("question", question);
model.addAttribute("questionHtml", commonUtil.markdown(question.getContent()));
model.addAttribute("answerHtmlById", commonUtil.renderAnswerHtmlById(question.getAnswerList()));
```

| 요청 | 추가 위치와 역할 |
| --- | --- |
| `GET /question/detail/{id}` | `QuestionController.detail()`에서 질문을 조회한 뒤 본문 HTML을 모델에 담습니다. 일반적인 질문 상세 화면 진입 경로입니다. [QuestionController.java](../board/src/main/java/com/board/question/QuestionController.java) |
| `POST /answer/create/{id}` | `AnswerController.createAnswer()`의 `bindingResult.hasErrors()` 분기에서 같은 모델 속성을 담습니다. 로그인한 사용자가 빈 답변을 제출하는 등 검증에 실패하면 기존 본문과 검증 메시지를 같은 화면에 표시합니다. 답변 등록에 성공하면 질문 상세 URL로 리다이렉트합니다. [AnswerController.java](../board/src/main/java/com/board/answer/AnswerController.java) |

### 3. Thymeleaf에서 변환된 HTML을 출력

질문 본문은 모델의 `questionHtml`을 출력합니다. 답변 본문은 `question.answerList` 반복문에서 현재 답변의 `answer.id`를 키로 사용해 해당 HTML을 꺼냅니다. `th:utext`는 전달된 문자열의 HTML 태그를 화면에 렌더링합니다. [question_detail.html](../board/src/main/resources/templates/question_detail.html)

```html
<!-- 질문 본문 -->
<div class="card-text" th:utext="${questionHtml}"></div>

<!-- 답변 반복문 안의 본문 -->
<div class="card-text" th:utext="${answerHtmlById[answer.id]}"></div>
```

## 화면에서 확인하는 방법

1. 질문 본문과 답변 두 건에 서로 다른 `**굵은 글씨**` 예시를 입력합니다. 질문 상세 화면에서 각 본문의 글씨가 굵게 표시되고, 답변 내용이 해당 답변 카드에 표시되는지 확인합니다.
2. 로그인 상태에서 답변 입력란을 비워 두고 등록합니다. 같은 화면에 검증 메시지가 표시되고 질문과 기존 답변의 Markdown 서식이 유지되는지 확인합니다.

## 개정이력

- `2026-11-26 14:50:16` KST — 대화에서 확인한 오류 원인과 재현 결과, 현재 구현의 처리 흐름을 문서화했습니다.
