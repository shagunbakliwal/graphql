import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MyEntityRepository extends JpaRepository<MyEntity, Long> {

    @Query(value = "SELECT * FROM my_table e WHERE e.name = :name AND e.status = :status " +
            "AND e.data @> CAST(:jsonFilter AS jsonb)", nativeQuery = true)
    List<MyEntity> findByNameStatusAndJson(@Param("name") String name, 
                                           @Param("status") String status, 
                                           @Param("jsonFilter") String jsonFilter);
}
