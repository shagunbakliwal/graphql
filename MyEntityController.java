import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entities")
public class MyEntityController {

    private final MyEntityService service;

    public MyEntityController(MyEntityService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public List<MyEntity> search(@RequestParam String name,
                                 @RequestParam String status,
                                 @RequestParam Map<String, String> jsonFilters) {
        return service.searchEntities(name, status, jsonFilters);
    }
}
