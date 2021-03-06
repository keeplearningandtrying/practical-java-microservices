package microbucks.customerservice.runner;

import lombok.extern.slf4j.Slf4j;
import microbucks.customerservice.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
@Slf4j
public class CoffeeRunner implements ApplicationRunner {
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Order order = createOrder();
        order = payOrder(order);
        takeCoffee(order);
    }

    private Order payOrder(Order order) {
        Order payOrder = Order.builder().price(order.getPrice()).state(1).build();
        restTemplate.put("http://order-service/orders/{id}", payOrder, order.getId());
        order = restTemplate.getForObject("http://order-service/orders/{id}", Order.class, order.getId());
        log.info("Order after payment: {}", order);
        return order;
    }

    private Order createOrder() {
        Order newOrder = Order.builder().customer("digitalsonic").content("cappuccino").build();
        Order response = restTemplate.postForObject("http://order-service/orders/", newOrder, Order.class);
        log.info("Create Order: {}", response);
        return response;
    }

    private void takeCoffee(Order order) throws InterruptedException {
        boolean taken = false;
        URI uri = UriComponentsBuilder.fromUriString("http://order-service/orders/{id}")
                .buildAndExpand(order.getId()).toUri();
        RequestEntity<Order> entity = RequestEntity.put(uri).body(Order.builder().state(4).build());
        while (!taken) {
            ResponseEntity<Order> responseEntity = restTemplate.exchange("http://order-service/orders/{id}",
                    HttpMethod.PUT, entity, Order.class, order.getId());
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.info("The coffee[{}] has been taken.", responseEntity.getBody());
                break;
            }
            log.warn("The coffee is not ready, status code is {}.", responseEntity.getStatusCodeValue());
            Thread.sleep(3000);
        }
    }
}
