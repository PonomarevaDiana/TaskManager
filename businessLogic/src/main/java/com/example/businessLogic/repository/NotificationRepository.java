package com.example.businessLogic.repository;

import com.example.businessLogic.entity.Notification;
import com.example.businessLogic.entity.NotificationType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Поиск уведомлений по владельцу (id)
    List<Notification> findByOwnerIdOrderByDateDesc(String ownerId);

    // Поиск непрочитанных уведомлений
    List<Notification> findByOwnerIdAndIsReadFalseOrderByDateDesc(String ownerId);

    // Поиск уведомлений по типу
    List<Notification> findByOwnerIdAndTypeOrderByDateDesc(String ownerId, NotificationType type);

    // Поиск уведомлений по типу и отправителю
    List<Notification> findByOwnerIdAndTypeAndSenderIdOrderByDateDesc(String owner, String type, String senderId);

    // Поиск уведомлений по типу без учета отправителя
    List<Notification> findByOwnerIdAndType(String owner, NotificationType type);

    // Поиск уведомлений по типу и отправителю
    List<Notification> findByOwnerIdAndTypeAndSenderId(String owner, NotificationType type, String senderId);

    // Подсчет непрочитанных уведомлений
    long countByOwnerIdAndIsReadFalse(String ownerId);

    // Подсчет непрочитанных уведомлений по типу
    long countByOwnerIdAndTypeAndIsReadFalse(String owner, String type);

    // Отметить уведомление как прочитанное
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.ownerId = :ownerId")
    void markAsRead(@Param("id") Long id, @Param("ownerId") String ownerId);

    // Отметить все уведомления как прочитанные
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.ownerId = :ownerId")
    void markAllAsRead(@Param("owner") String ownerId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.ownerId = :ownerId AND n.id = :id")
    void deleteByOwnerIdAndId(@Param("ownerId") String ownerId, @Param("id") Long id);

    // Отметить уведомления определенного типа как прочитанные
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.ownerId = :owner AND n.type = :type")
    void markAllAsReadByType(@Param("owner") String owner, @Param("type") String type);

    // Удаление уведомлений по владельцу
    @Modifying
    @Transactional
    void deleteAllByOwnerId(String ownerId);

    // Удаление уведомлений по типу
    void deleteByOwnerIdAndType(String owner, String type);

    // Удаление уведомлений по типу и отправителю
    void deleteByOwnerIdAndTypeAndSenderId(String owner, String type, String senderId);

    // Удаление конкретного уведомления по ID и владельцу
    void deleteByIdAndOwnerId(Long id, String owner);

    // Удаление всех прочитанных уведомлений
    void deleteByOwnerIdAndIsReadTrue(String owner);

    // Удаление всех непрочитанных уведомлений
    void deleteByOwnerIdAndIsReadFalse(String owner);

    // Удаление старых уведомлений
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.ownerId = :ownerId AND n.date < :date")
    void deleteOldNotifications(@Param("owner") String ownerId, @Param("date") LocalDateTime date);

    // Удаление уведомлений старше определенной даты по типу
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.ownerId = :owner AND n.type = :type AND n.date < :date")
    void deleteOldNotificationsByType(@Param("owner") String owner, @Param("type") String type, @Param("date") String date);

    // Проверка существования уведомления
    boolean existsByOwnerIdAndTypeAndSenderIdAndIsReadFalse(String owner, String type, String senderId);

    // Поиск уведомлений по нескольким типам
    @Query("SELECT n FROM Notification n WHERE n.ownerId = :owner AND n.type IN :types ORDER BY n.date DESC")
    List<Notification> findByOwnerAndTypeIn(@Param("owner") String owner, @Param("types") List<String> types);

    // Поиск уведомлений с пагинацией
    @Query("SELECT n FROM Notification n WHERE n.ownerId = :owner ORDER BY n.date DESC")
    List<Notification> findLatestNotifications(@Param("owner") String owner, org.springframework.data.domain.Pageable pageable);

    // Поиск непрочитанных уведомлений с пагинацией
    @Query("SELECT n FROM Notification n WHERE n.ownerId = :owner AND n.isRead = false ORDER BY n.date DESC")
    List<Notification> findLatestUnreadNotifications(@Param("owner") String owner, org.springframework.data.domain.Pageable pageable);

    // Обновление содержимого уведомления
    @Modifying
    @Query("UPDATE Notification n SET n.title = :title, n.message = :message WHERE n.id = :id AND n.ownerId = :owner")
    void updateNotificationContent(@Param("id") Long id, @Param("owner") String owner, @Param("title") String title, @Param("message") String message);

    // Поиск уведомлений по отправителю
    List<Notification> findBySenderIdOrderByDateDesc(String senderId);

    // Поиск уведомлений по владельцу и отправителю
    List<Notification> findByOwnerIdAndSenderIdOrderByDateDesc(String owner, String senderId);

    // Удаление всех уведомлений от определенного отправителя
    void deleteBySenderId(String senderId);

    // Удаление уведомлений по владельцу и отправителю
    void deleteByOwnerIdAndSenderId(String owner, String senderId);

    // Получение количества уведомлений по типу
    long countByOwnerIdAndType(String owner, String type);

    // Проверка наличия непрочитанных уведомлений
    boolean existsByOwnerIdAndIsReadFalse(String owner);

    // Поиск уведомлений по ключевым словам в заголовке или сообщении
    @Query("SELECT n FROM Notification n WHERE n.ownerId = :owner AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(n.message) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY n.date DESC")
    List<Notification> searchByKeyword(@Param("owner") String owner, @Param("keyword") String keyword);

    // Получение последнего уведомления
    Notification findFirstByOwnerIdOrderByDateDesc(String owner);

    // Получение последнего уведомления по типу
    Notification findFirstByOwnerIdAndTypeOrderByDateDesc(String owner, String type);

    @Query("select n.id from Notification n WHERE n.invitationId = :invitationId")
    Long findIdByInvitationId(Long invitationId);
}