import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class MyEntityService {

    private final MyEntityRepository repository;
    private final ObjectMapper objectMapper;

    public MyEntityService(MyEntityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<MyEntity> searchEntities(String name, String status, Map<String, String> jsonFilters) {
        try {
            String jsonFilter = objectMapper.writeValueAsString(jsonFilters);
            return repository.findByNameStatusAndJson(name, status, jsonFilter);
        } catch (Exception e) {
            throw new RuntimeException("Error converting JSON filter", e);
        }
    }
}
