# Study
## Environment
+ DB: H2
+ Java 25
+ SpringBoot 4.2
+ thymeleaf
+ JPA

## IDE
+ Eclipse
+ STS
+ IntelliJ
+ **VSCode** <-

## Spring Initializr
https://start.spring.io/

## PackageManager
+ Maven
+ **Gradle** <-

## VSCode
### 확장
+ Spring Boot DashBoard: Microsoft
+ Spring Initializr Java Support: Microsoft
+ Spring Boot Extension Pack: VMWare
+ Spring Boot Tools: VMWare
+ Debugger for Java: Microsoft
+ Extension Pack for Java: Microsoft
+ Gradle for Java: Microsoft
+ Language Support for Java(TM) by Red Hat: Red Hat
+ Maven for Java: Microsoft
+ Project Manager for Java: Microsoft
+ Test Runner for Java: Microsoft

## Flow
Browser -> Controller -> Service -> Entity -> DB

## Directory
### src/main/resources
+ src/main/resources 디렉터리는 자바 파일을 제외한 HTML, CSS, 자바스크립트, 환경 파일 등을 저장하는 공간입니다.

#### templates
+ templates에는 템플릿 파일을 저장합니다. 
+ 템플릿 파일은 자바 코드를 삽입할 수 있는 HTML 형식의 파일로, 스프링 부트에서 생성한 자바 객체를 HTML 형태로 출력할 수 있습니다.

#### static
+프로젝트의 스타일시트(css 파일), 자바스크립트(js 파일) 그리고 이미지 파일(jpg 파일, png 파일 등) 등을 저장합니다.

#### application.properties
+ application.properties 파일은 프로젝트의 환경을 설정합니. 프로젝트의 환경 변수, 데이터베이스 등의 설정을 이 파일에 저장합니다.

### src/test/java
+ 프로젝트에서 작성한 파일을 테스트하는 코드를 저장하는 공간입니다.

## build.gradle
+ 그레이들(Gradle)이 사용하는 환경 파일입니다.
+ 그레이들은 그루비(Groovy)를 기반으로 한 빌드 도구로 build.gradle 파일에는 프로젝트에 필요한 플러그인과 라이브러리를 설치하기 위한 내용을 작성합니다.

## EntryPoint
``` java
// @SpringBootApplication 애너테이션을 통해 스프링 부트 애플리케이션을 시작할 수 있습니다.
@SpringBootApplication
public class BoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardApplication.class, args);
	}

}
```

## 애너테이션
+ @Getter: Lombok Getter입니다. Getter 메서드를 자동 생성합니다.
+ @Setter: Lombok Setter입니다. Setter 메서드를 자동 생성합니다.
+ @SpringBootApplication: EntryPoint
+ @Controller: 클래스에 @Controller 애너테이션을 적용하면 클래스는 스프링 부트의 컨트롤러가 됩니다.
+ @GetMapping("/"):  메서드의 @GetMapping 애너테이션은 요청된 URL(/)과의 매핑을 담당합니다. 브라우저가 URL을 요청하면 스프링 부트는 요청 페이지와 매핑되는 메서드를 찾아 실행합니다.
+ @ResponseBody: @ResponseBody 애너테이션은 URL 요청에 대한 응답으로 문자열을 리턴하라는 의미로 쓰입니다.
+ @Entity: 엔티티는 데이터베이스 테이블과 매핑되는 자바 클래스를 말합니다. @Entity 애너테이션을 적용해야 스프링 부트가 클래스를 엔티티로 인식한다.
+ @Id: 적용한 Property를 PrimaryKey로 설정합니다.
+ @GeneratedValue: 적용한 Property의 값을 자동으로 1씩 증가시킵니다. strategy = GenerationType.IDENTITY는 고유한 번호를 생성하는 방법을 지정하는 부분으로, GenerationType.IDENTITY는 해당 Property만 별도로 번호가 차례대로 늘어나도록 할 때 사용합니다. strategy 옵션을 생략한다면 @GeneratedValue 애너테이션이 지정된 모든 Property에 번호를 차례로 생성하므로 순서가 일정한 고유 번호를 가질 수 없게 됩니다. 이러한 이유로 보통 strategy = GenerationType.IDENTITY를 많이 사용합니다.
+ @Column: 엔티티의 Property는 테이블의 열 이름과 일치하는데 열의 세부 설정을 위해 @Column 애너테이션을 사용합니다.  length는 열의 길이를 설정할 때 사용하고, columnDefinition은 열 데이터의 유형이나 성격을 정의할 때 사용합니다. columnDefinition = "TEXT"는 말 그대로 '텍스트'를 열 데이터로 넣을 수 있음을 의미하고, 글자 수를 제한할 수 없는 경우에 사용합니다.
+ @Transient: 엔티티의 Property는 @Column 애너테이션을 사용하지 않더라도 테이블의 열로 인식합니다. 테이블의 열로 인식하고 싶지 않다면 @Transient 애너테이션을 사용합니다. @Transient 애너테이션은 엔티티의 Property를 테이블의 열로 만들지 않고 클래스의 Property 기능으로만 사용하고자 할 때 씁니다.
+ @ManyToOne: 답변은 Many(많은 것)가 되고 질문은 One(하나)이 됩니다. 즉, @ManyToOne 애너테이션을 사용하면 N:1 관계를 나타낼 수 있습니다. 이렇게 @ManyToOne 애너테이션을 설정하면 Answer(답변) 엔티티의 question 속성과 Question(질문) 엔티티가 서로 연결됩니다(실제 데이터베이스에서는 외래키(foreign key) 관계가 생성됩니다.).
+ @OneToMany:  답변과 질문이 N:1 관계라면 질문과 답변은 1:N 관계라고 할 수 있습니다. 이런 경우에는 @ManyToOne이 아닌 @OneToMany 애너테이션을 사용합니다. 질문 하나에 답변은 여러 개이므로 Question 엔티티에 추가할 Answer Property는 List 형태로 구성해야 한다.
  + @OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE) 
  + mappedBy는 참조 엔티티의 Property 이름을 정의한다. 즉, Answer 엔티티에서 Question 엔티티를 참조한 Property인 question을 mappedBy에 전달해야 한다.
  + CascadeType.REMOVE란 무엇일까?: 게시판 서비스에서는 질문 하나에 답변이 여러 개 작성될 수 있습니다. 그런데 보통 게시판 서비스에서는 질문을 삭제하면 그에 달린 답변들도 함께 삭제됩니다. SBB도 질문을 삭제하면 그에 달린 답변들도 모두 삭제되도록 cascade = CascadeType.REMOVE를 사용했습니다.
+ @SpringBootTest: @SpringBootTest 애너테이션은 SbbApplicationTests 클래스가 스프링 부트의 테스트 클래스임을 의미한다. 
+ @Autowired: 스프링의 '의존성 주입(DI)'이라는 기능을 사용하여 객체를 주입합니다.
  + 객체를 주입하는 방식에는 @Autowired 애너테이션을 사용하는 것 외에 Setter 메서드 또는 생성자를 사용하는 방식이 있습니다. 순환 참조 문제와 같은 이유로 개발 시 @Autowired보다는 생성자를 통한 객체 주입 방식을 권장합니다. 하지만 테스트 코드의 경우 JUnit이 생성자를 통한 객체 주입을 지원하지 않으므로 테스트 코드 작성 시에만 @Autowired를 사용하고 실제 코드 작성 시에는 생성자를 통한 객체 주입 방식을 권장합니다.
+ @Test: 테스트 메서드임을 나타냅니다.

## Gradle


## Service
서비스란 무엇일까? 서비스(service)는 간단히 말해 스프링에서 데이터 처리를 위해 작성하는 클래스이다. 우리는 그동안 서비스 없이도 웹 프로그램을 동작시키는 데 문제가 없었다. 그런데 굳이 서비스를 사용해야 할까? 서비스가 필요한 이유를 좀 더 자세히 알아보자.

### 복잡한 코드를 모듈화할 수 있다
예를 들어 A라는 컨트롤러가 어떤 기능을 수행하기 위해 C라는 리포지터리의 메서드 a, b, c를 순서대로 실행해야 한다고 가정해 보자. 그리고 B라는 컨트롤러도 A 컨트롤러와 동일한 기능을 수행해야 한다면 A, B 컨트롤러가 C 리포지터리의 메서드 a, b, c를 호출해 사용하는 중복된 코드를 가지게 된다. 이런 경우 C 리포지터리의 a, b, c 메서드를 호출하는 기능을 서비스로 만들고 컨트롤러에서 이 서비스를 호출하여 사용할 수 있다. 즉, 서비스를 사용하면 이와 같은 모듈화가 가능하다.

### 엔티티 객체를 DTO 객체로 변환할 수 있다.
우리가 앞에서 작성한 Question, Answer 클래스는 모두 엔티티 클래스이다. 엔티티 클래스는 데이터베이스와 직접 맞닿아 있는 클래스이므로 컨트롤러 또는 타임리프와 같은 템플릿 엔진에 전달해 사용하는 것은 좋지 않다. 왜냐하면 엔티티 객체에는 민감한 데이터가 포함될 수 있는데, 타임리프에서 엔티티 객체를 직접 사용하면 민감한 데이터가 노출될 위험이 있기 때문이다.

이러한 이유로 Question, Answer 같은 엔티티 클래스는 컨트롤러에서 사용하지 않도록 설계하는 것이 좋다. 그래서 Question, Answer를 대신해 사용할 DTO (Data Transfer Object) 클래스가 필요하다. 그리고 Question, Answer 등의 엔티티 객체를 DTO 객체로 변환하는 작업도 필요하다. 그러면 엔티티 객체를 DTO 객체로 변환하는 일은 어디서 처리해야 할까? 이때도 서비스가 필요하다. 서비스는 컨트롤러와 리포지터리의 중간에서 엔티티 객체와 DTO 객체를 서로 변환하여 양방향에 전달하는 역할을 한다.

> 이 책은 간결한 설명을 위해 별도의 DTO를 만들지 않고 엔티티 객체를 컨트롤러와 타임리프에서 그대로 사용할 것이다. 하지만 실제 프로그램을 개발할 때는 엔티티 클래스를 대신할 DTO 클래스를 만들어 사용하기를 권장한다.

## JPA
### application.properties
```
spring.jpa.hibernate.ddl-auto=update
```

+ none: 엔티티가 변경되더라도 데이터베이스를 변경하지 않습니다.
+ update: 엔티티의 변경된 부분만 데이터베이스에 적용합니다.
+ validate: 엔티티와 테이블 간에 차이점이 있는지 검사만 합니다.
+ create: 스프링 부트 서버를 시작할 때 테이블을 모두 삭제한 후 다시 생성합니다.
+ create-drop: create와 동일하지만 스프링 부트 서버를 종료할 때에도 테이블을 모두 삭제합니다.

### 엔티티의 속성 이름과 테이블의 열 이름의 차이를 알아보자
Question 엔티티에서 작성 일시에 해당하는 createDate 속성의 이름은 데이터베이스의 테이블에서는 create_date라는 열 이름으로 설정됩니다. 즉, createDate처럼 카멜 케이스(camel case) 형식의 이름은 create_date처럼 모두 소문자로 변경되고 단어가 언더바(_)로 구분되어 데이터베이스 테이블의 열 이름이 됩니다.

### 엔티티를 만들 때 Setter 메서드는 사용하지 않는다
일반적으로 엔티티를 만들 때에는 Setter 메서드를 사용하지 않기를 권장합니다. 왜냐하면 엔티티는 데이터베이스와 바로 연결되므로 데이터를 자유롭게 변경할 수 있는 Setter 메서드를 허용하는 것이 안전하지 않다고 판단하기 때문입니다. 그렇다면 Setter 메서드 없이 어떻게 엔티티에 값을 저장할 수 있을까요?
엔티티는 생성자에 의해서만 엔티티의 값을 저장할 수 있게 하고, 데이터를 변경해야 할 경우에는 메서드를 추가로 작성하면 됩니다. 다만, 이 책은 복잡도를 낮추고 원활한 설명을 위해 엔티티에 Setter 메서드를 추가하여 진행합니다.

### Methods
+ findAll
+ findById
+ findBySubject
+ findBySubjectAndContent
+ findBySubjectLike
+ delete
+ save

## Thymeleaf



