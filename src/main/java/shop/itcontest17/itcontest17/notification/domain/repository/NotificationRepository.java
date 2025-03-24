package shop.itcontest17.itcontest17.notification.domain.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long>{

    List<Notification> findAllByReceiver(Member receiver);
}
