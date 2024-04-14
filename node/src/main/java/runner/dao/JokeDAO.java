package runner.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import runner.entity.Joke;

import java.util.Optional;

public interface JokeDAO extends JpaRepository<Joke, Long> {
    Optional<Joke> findById(Long id);
};
