package info.sup.proj.backend.repositories;

import info.sup.proj.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u ORDER BY u.elo DESC")
    Page<User> findAllByOrderByEloDesc(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.country = :country ORDER BY u.elo DESC")
    Page<User> findByCountryOrderByEloDesc(@Param("country") String country, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE :username")
    List<User> findByUsernameContainingIgnoreCase(@Param("username") String username);
}