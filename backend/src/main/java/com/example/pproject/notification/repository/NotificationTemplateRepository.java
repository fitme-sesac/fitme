package com.example.pproject.notification.repository;

import com.example.pproject.Constant.NotificationChannel;
import com.example.pproject.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateCode(String templateCode);

    Optional<NotificationTemplate> findByTemplateCodeAndChannel(String templateCode, NotificationChannel channel);

    boolean existsByTemplateCode(String templateCode);
}
