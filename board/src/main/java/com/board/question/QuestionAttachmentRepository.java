package com.board.question;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionAttachmentRepository extends JpaRepository<QuestionAttachment, Integer> {
    List<Summary> findByQuestion_IdOrderByIdAsc(Integer questionId);

    Optional<QuestionAttachment> findByQuestion_IdAndId(Integer questionId, Integer attachmentId);

    interface Summary {
        Integer getId();

        String getOriginalFilename();
    }
}
