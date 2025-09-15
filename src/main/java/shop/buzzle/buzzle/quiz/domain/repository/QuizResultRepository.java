package shop.buzzle.buzzle.quiz.domain.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.quiz.domain.QuizResult;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    @Query("SELECT qr FROM QuizResult qr WHERE qr.member = :member AND qr.isCorrect = false ORDER BY qr.createdAt DESC")
    List<QuizResult> findIncorrectAnswersByMember(@Param("member") Member member);

    @Query("SELECT qr FROM QuizResult qr WHERE qr.member = :member ORDER BY qr.createdAt DESC")
    List<QuizResult> findAllByMemberOrderByCreatedAtDesc(@Param("member") Member member);

    @Query("SELECT qr FROM QuizResult qr WHERE qr.member = :member AND qr.isCorrect = :isCorrect ORDER BY qr.createdAt DESC")
    List<QuizResult> findByMemberAndIsCorrectOrderByCreatedAtDesc(@Param("member") Member member, @Param("isCorrect") Boolean isCorrect);
}