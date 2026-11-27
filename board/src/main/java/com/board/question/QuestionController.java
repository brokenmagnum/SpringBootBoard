package com.board.question;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.board.CommonUtil;
import com.board.answer.AnswerForm;
import com.board.user.SiteUser;
import com.board.user.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequestMapping("/question")
@RequiredArgsConstructor
@Controller
public class QuestionController {

    private final QuestionService questionService;
    private final UserService userService;
    private final CommonUtil commonUtil;

    @GetMapping("/list")
    public String list(Model model, @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "kw", defaultValue = "") String kw) {
        Page<Question> paging = this.questionService.getList(page, kw);
        model.addAttribute("paging", paging);
        model.addAttribute("kw", kw);
        return "question_list";
    }

    @GetMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Integer id, AnswerForm answerForm) {
        Question question = this.questionService.getQuestion(id);
        model.addAttribute("question", question);
        model.addAttribute("questionHtml", commonUtil.markdown(question.getContent()));
        model.addAttribute("answerHtmlById", commonUtil.renderAnswerHtmlById(question.getAnswerList()));
        model.addAttribute("attachments", this.questionService.getAttachments(id));
        return "question_detail";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/create")
    public String questionCreate(QuestionForm questionForm) {
        return "question_form";
    }

    // @PreAuthorize("isAuthenticated()")
    // @PostMapping("/create")
    // public String questionCreate(@Valid QuestionForm questionForm, BindingResult
    // bindingResult, Principal principal) {
    // if (bindingResult.hasErrors()) {
    // return "question_form";
    // }
    // SiteUser siteUser = this.userService.getUser(principal.getName());
    // this.questionService.create(questionForm.getSubject(),
    // questionForm.getContent(), siteUser);
    // return "redirect:/question/list";
    // }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    public String questionCreate(@Valid QuestionForm questionForm, BindingResult bindingResult, Principal principal) {
        if (bindingResult.hasErrors()) {
            return "question_form";
        }

        SiteUser siteUser = this.userService.getUser(principal.getName());
        try {
            this.questionService.create(questionForm.getSubject(), questionForm.getContent(),
                    siteUser, questionForm.getFiles());
        } catch (QuestionService.AttachmentValidationException e) {
            bindingResult.reject("attachment.invalid", e.getMessage());
            return "question_form";
        }
        return "redirect:/question/list";
    }

    // // questionModify 메서드를 추가했다.
    // // 만약 현재 로그인한 사용자와 질문의 작성자가 동일하지 않을 경우에는 '수정 권한이 없습니다.'라는 오류가 발생하도록
    // // ResponseStatusException을 발생시킨다. 만약 동일하다면 questionForm에 기존 질문의 제목과 내용을 세팅하고
    // // question_form.html을 반환한다.
    // @PreAuthorize("isAuthenticated()")
    // @GetMapping("/modify/{id}")
    // public String questionModify(QuestionForm questionForm, @PathVariable("id")
    // Integer id, Principal principal) {
    // Question question = this.questionService.getQuestion(id);
    // if (!question.getAuthor().getUsername().equals(principal.getName())) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
    // }
    // questionForm.setSubject(question.getSubject());
    // questionForm.setContent(question.getContent());
    // return "question_form";
    // }

    // // 수정
    // @PreAuthorize("isAuthenticated()")
    // @PostMapping("/modify/{id}")
    // public String questionModify(@Valid QuestionForm questionForm, BindingResult
    // bindingResult,
    // Principal principal, @PathVariable("id") Integer id) {
    // if (bindingResult.hasErrors()) {
    // return "question_form";
    // }
    // Question question = this.questionService.getQuestion(id);
    // if (!question.getAuthor().getUsername().equals(principal.getName())) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
    // }
    // this.questionService.modify(question, questionForm.getSubject(),
    // questionForm.getContent());
    // return String.format("redirect:/question/detail/%s", id);
    // }

    // // 삭제
    // @PreAuthorize("isAuthenticated()")
    // @GetMapping("/delete/{id}")
    // public String questionDelete(Principal principal, @PathVariable("id") Integer
    // id) {
    // Question question = this.questionService.getQuestion(id);
    // if (!question.getAuthor().getUsername().equals(principal.getName())) {
    // throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
    // }
    // this.questionService.delete(question);
    // return "redirect:/";
    // }

    // 작성자인지 확인한 뒤 기존 제목, 본문, 첨부 목록을 수정 화면에 전달합니다.
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String questionModify(Model model, QuestionForm questionForm, @PathVariable("id") Integer id,
            Principal principal) {
        Question question = this.questionService.getEditableQuestion(id, principal.getName());
        questionForm.setSubject(question.getSubject());
        questionForm.setContent(question.getContent());
        addModifyAttributes(model, id);
        return "question_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String questionModify(Model model, @Valid QuestionForm questionForm, BindingResult bindingResult,
            Principal principal, @PathVariable("id") Integer id) {
        if (bindingResult.hasErrors()) {
            this.questionService.getEditableQuestion(id, principal.getName());
            addModifyAttributes(model, id);
            return "question_form";
        }

        try {
            this.questionService.modify(id, questionForm, principal.getName());
        } catch (QuestionService.AttachmentValidationException e) {
            bindingResult.reject("attachment.invalid", e.getMessage());
            addModifyAttributes(model, id);
            return "question_form";
        }
        return "redirect:/question/detail/" + id;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/delete/{id}")
    public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
        this.questionService.delete(id, principal.getName());
        return "redirect:/";
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(@PathVariable("id") Integer id,
            @PathVariable("attachmentId") Integer attachmentId) {
        QuestionAttachment attachment = this.questionService.getAttachment(id, attachmentId);
        String disposition = ContentDisposition.attachment()
                .filename(attachment.getOriginalFilename(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(attachment.getContent());
    }

    private void addModifyAttributes(Model model, Integer id) {
        model.addAttribute("questionId", id);
        model.addAttribute("attachments", this.questionService.getAttachments(id));
    }

    // 추천
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String questionVote(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.questionService.vote(question, siteUser);
        return String.format("redirect:/question/detail/%s", id);
    }
}
