package shop.buzzle.buzzle.notification.domain.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>{

    List<Notification> findAllByReceiver(Member receiver);
}
