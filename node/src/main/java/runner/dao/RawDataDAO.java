package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import runner.entity.RawData;

public interface RawDataDAO extends JpaRepository<RawData, Long> {
}
