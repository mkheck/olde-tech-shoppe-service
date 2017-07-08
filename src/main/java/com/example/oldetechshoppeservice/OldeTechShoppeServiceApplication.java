package com.example.oldetechshoppeservice;

import lombok.*;
import lombok.extern.java.Log;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Date;

@Log
@SpringBootApplication
public class OldeTechShoppeServiceApplication {
    @Bean
    CommandLineRunner demoData(OldTechRepository repository) {
        return strings -> {
            Flux.just("iPad 3", "Nexus 7", "iPod Touch", "iPod Video", "Kindle reader", "Commodore 64", "Commodore VIC-20")
                    .map(desc -> new Item(desc))
                    .flatMap(repository::save)
                    .subscribe(item -> log.info("Old tech item saved: " + item));
        };
    }

    @Bean
    RouterFunction<?> routes(ItemHandler handler) {
        return RouterFunctions.route(RequestPredicates.GET("/items"), handler::all)
                .andRoute(RequestPredicates.GET("/items/{id}"), handler::byId)
                .andRoute(RequestPredicates.GET("/items/{id}/watches"), handler::watches)
                .andRoute(RequestPredicates.GET("/items/{id}/watchesbyid"), handler::watchesbyid);
    }

    public static void main(String[] args) {
        SpringApplication.run(OldeTechShoppeServiceApplication.class, args);
    }
}

//@RestController
//@RequestMapping("/items")
//class ItemController {
//    private final OldTechService service;
//
//    ItemController(OldTechService service) {
//        this.service = service;
//    }
//
//    @GetMapping
//    public Flux<Item> all() {
//        return service.findAll();
//    }
//
//    @GetMapping("/{id}")
//    public Mono<Item> byId(@PathVariable String id) {
//        return service.findOne(id);
//    }
//
//    @GetMapping(value = "/{id}/watches", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<Watch> watches(@PathVariable String id) {
//        //return service.getWatchesForItem(id);
//        return service.findOne(id)
//                .flatMapMany(service::getWatchesForItem);
//    }
//}

@Component
class ItemHandler {
    private final OldTechService ots;

    ItemHandler(OldTechService ots) {
        this.ots = ots;
    }

    public Mono<ServerResponse> all(ServerRequest request) {
        return ServerResponse.ok().body(ots.findAll(), Item.class);
    }

    public Mono<ServerResponse> byId(ServerRequest request) {
        return ServerResponse.ok()
                .body(ots.findOne(request.pathVariable("id")), Item.class);
    }

    public Mono<ServerResponse> watches(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(ots.findOne(request.pathVariable("id"))
                        .flatMapMany(ots::getWatchesForItem), Watch.class);
    }

    public Mono<ServerResponse> watchesbyid(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(ots.getWatchesById(request.pathVariable("id")), Watch.class);
    }
}

@Service
class OldTechService {
    private final OldTechRepository repository;

    OldTechService(OldTechRepository repository) {
        this.repository = repository;
    }

    public Flux<Item> findAll() {
        return repository.findAll();
    }

    public Mono<Item> findOne(String id) {
        return repository.findById(id);
    }

    public Flux<Watch> getWatchesForItem(Item item) {
        return Flux.<Watch>generate(sink -> sink.next(new Watch(item, new Date())))
                .delayElements(Duration.ofSeconds(1));
    }

    public Flux<Watch> getWatchesById(String id) {
        return repository.findById(id)
                .flatMapMany(item -> Flux.<Watch>generate(sink -> sink.next(new Watch(item, new Date()))))
                .delayElements(Duration.ofSeconds(1));
    }
}

interface OldTechRepository extends ReactiveMongoRepository<Item, String> {}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
class Item {
    @Id
    private String id;
    @NonNull
    private String description;
}

@Data
@AllArgsConstructor
class Watch {
    private Item item;
    private Date date;
}