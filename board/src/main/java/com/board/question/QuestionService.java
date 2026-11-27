package com.board.question;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.board.DataNotFoundException;
import com.board.answer.Answer;
import com.board.user.SiteUser;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class QuestionService {

    private final QuestionRepository questionRepository;

    private final QuestionAttachmentRepository attachmentRepository;

    private static final int MAX_ATTACHMENTS = 3;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "txt");

    public List<Question> getList() {
        return this.questionRepository.findAll();
    }

    public Question getQuestion(Integer id) {
        Optional<Question> question = this.questionRepository.findById(id);
        if (question.isPresent()) {
            return question.get();
        } else {
            throw new DataNotFoundException("question not found");
        }
    }

    // public void create(String subject, String content, SiteUser user) {
    // Question q = new Question();
    // q.setSubject(subject);
    // q.setContent(content);
    // q.setCreateDate(LocalDateTime.now());
    // q.setAuthor(user);
    // this.questionRepository.save(q);
    // }

    @Transactional
    public void create(String subject, String content, SiteUser user) {
        create(subject, content, user, List.of());
    }

    @Transactional
    public void create(String subject, String content, SiteUser user, List<MultipartFile> files) {
        List<MultipartFile> uploads = getSelectedFiles(files);
        checkAttachmentCount(uploads.size());

        Question question = new Question();
        question.setSubject(subject);
        question.setContent(content);
        question.setCreateDate(LocalDateTime.now());
        question.setAuthor(user);
        addAttachments(question, uploads);
        this.questionRepository.save(question);
    }

    public Page<Question> getList(int page, String kw) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        Specification<Question> spec = search(kw);
        return this.questionRepository.findAll(spec, pageable);
    }

    // public void modify(Question question, String subject, String content) {
    // question.setSubject(subject);
    // question.setContent(content);
    // question.setModifyDate(LocalDateTime.now());
    // this.questionRepository.save(question);
    // }

    // public void delete(Question question) {
    // this.questionRepository.delete(question);
    // }

    @Transactional
    public void modify(Integer id, QuestionForm form, String username) {
        Question question = getQuestionForUpdate(id, username);
        List<QuestionAttachment> attachments = question.getAttachments();
        Set<Integer> removalIds = new HashSet<>(form.getRemoveAttachmentIds());
        Set<Integer> existingIds = attachments.stream()
                .map(QuestionAttachment::getId)
                .collect(Collectors.toSet());

        if (!existingIds.containsAll(removalIds)) {
            throw new AttachmentValidationException("첨부 목록이 변경됐습니다. 목록을 확인하고 다시 저장해 주세요.");
        }

        List<MultipartFile> uploads = getSelectedFiles(form.getFiles());
        checkAttachmentCount(attachments.size() - removalIds.size() + uploads.size());

        attachments.removeIf(attachment -> removalIds.contains(attachment.getId()));
        addAttachments(question, uploads);
        question.setSubject(form.getSubject());
        question.setContent(form.getContent());
        question.setModifyDate(LocalDateTime.now());
    }

    @Transactional
    public void delete(Integer id, String username) {
        Question question = getQuestionForUpdate(id, username);
        this.questionRepository.delete(question);
    }

    public Question getEditableQuestion(Integer id, String username) {
        Question question = getQuestion(id);
        checkAuthor(question, username);
        return question;
    }

    public List<QuestionAttachmentRepository.Summary> getAttachments(Integer questionId) {
        return this.attachmentRepository.findByQuestion_IdOrderByIdAsc(questionId);
    }

    @Transactional(readOnly = true)
    public QuestionAttachment getAttachment(Integer questionId, Integer attachmentId) {
        return this.attachmentRepository.findByQuestion_IdAndId(questionId, attachmentId)
                .orElseThrow(() -> new DataNotFoundException("attachment not found"));
    }

    private Question getQuestionForUpdate(Integer id, String username) {
        Question question = this.questionRepository.findForUpdate(id)
                .orElseThrow(() -> new DataNotFoundException("question not found"));
        checkAuthor(question, username);
        return question;
    }

    private void checkAuthor(Question question, String username) {
        if (question.getAuthor() == null || !question.getAuthor().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "질문 작성자만 변경할 수 있습니다.");
        }
    }

    private List<MultipartFile> getSelectedFiles(List<MultipartFile> files) {
        return files.stream()
                .filter(file -> !file.isEmpty() || StringUtils.hasText(file.getOriginalFilename()))
                .toList();
    }

    private void checkAttachmentCount(int count) {
        if (count > MAX_ATTACHMENTS) {
            throw new AttachmentValidationException("질문당 파일은 최대 3개까지 첨부할 수 있습니다.");
        }
    }

    private void addAttachments(Question question, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new AttachmentValidationException("내용이 비어 있는 파일은 첨부할 수 없습니다.");
            }

            String originalFilename = file.getOriginalFilename();
            String filename = originalFilename == null
                    ? "" : StringUtils.getFilename(originalFilename.replace('\\', '/')).strip();
            if (filename.isBlank() || filename.length() > 200
                    || filename.codePoints().anyMatch(Character::isISOControl)) {
                throw new AttachmentValidationException("파일 이름을 확인해 주세요. 최대 200자까지 사용할 수 있습니다.");
            }

            int dot = filename.lastIndexOf('.');
            String extension = dot > 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new AttachmentValidationException("PDF, PNG, JPG, JPEG, TXT 파일만 첨부할 수 있습니다.");
            }

            try {
                question.addAttachment(filename, file.getBytes());
            } catch (IOException e) {
                throw new UncheckedIOException("첨부파일을 읽을 수 없습니다.", e);
            }
        }
    }

    public static class AttachmentValidationException extends RuntimeException {
        AttachmentValidationException(String message) {
            super(message);
        }
    }

    // 추천
    public void vote(Question question, SiteUser siteUser) {
        question.getVoter().add(siteUser);
        this.questionRepository.save(question);
    }

    private Specification<Question> search(String kw) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true); // 중복을 제거
                Join<Question, SiteUser> u1 = q.join("author", JoinType.LEFT);
                Join<Question, Answer> a = q.join("answerList", JoinType.LEFT);
                Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
                return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), // 제목
                        cb.like(q.get("content"), "%" + kw + "%"), // 내용
                        cb.like(u1.get("username"), "%" + kw + "%"), // 질문 작성자
                        cb.like(a.get("content"), "%" + kw + "%"), // 답변 내용
                        cb.like(u2.get("username"), "%" + kw + "%")); // 답변 작성자
            }
        };
    }
}
