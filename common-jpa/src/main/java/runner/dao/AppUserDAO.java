package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import runner.entity.AppUser;

public interface AppUserDAO extends JpaRepository<AppUser, Long> {
    AppUser findAppUsersByTelegramUserId(Long id);
}
