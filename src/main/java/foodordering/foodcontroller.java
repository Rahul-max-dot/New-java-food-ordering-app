
package foodordering;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api")
class FoodController {

    record Food(long id, String name, int price) {}
    record OrderItem(long foodId, int quantity) {}
    record OrderRequest(List<OrderItem> items) {}
    record OrderResponse(
        long orderId, List<Food> items, int total, String status
    ) {}

    private final List<Food> menu = List.of(
        
        new Food(1, "Pizza", 1500),
        new Food(2, "Burger", 100),
        new Food(3, "Dosa", 60),
        new Food(4, "idli", 60),
        new Food(5, "puri", 75),
        new Food(6, "polao", 60)
    );

    private final Map<Long, OrderResponse> orders =
        new ConcurrentHashMap<>();

    private final AtomicLong orderCounter = new AtomicLong(1000);

    @GetMapping("/foods")
    public List<Food> getFoods() {
        return menu;
    }

    @PostMapping("/orders")
    public OrderResponse placeOrder(
            @RequestBody OrderRequest request) {

        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<Food> orderedFoods = new ArrayList<>();
        int total = 0;

        for (OrderItem item : request.items()) {
            if (item.quantity() < 1 || item.quantity() > 100) {
                throw new IllegalArgumentException("Invalid quantity");
            }

            Food food = menu.stream()
                .filter(f -> f.id() == item.foodId())
                .findFirst()
                .orElseThrow(() ->
                    new IllegalArgumentException("Food not found"));

            for (int i = 0; i < item.quantity(); i++) {
                orderedFoods.add(food);
                total += food.price();
            }
        }

        long id = orderCounter.incrementAndGet();

        OrderResponse response = new OrderResponse(
            id, orderedFoods, total, "CONFIRMED"
        );

        orders.put(id, response);
        return response;
    }

    @GetMapping("/orders/{id}")
    public OrderResponse getOrder(@PathVariable long id) {
        OrderResponse order = orders.get(id);

        if (order == null) {
            throw new NoSuchElementException("Order not found");
        }

        return order;
    }
}
