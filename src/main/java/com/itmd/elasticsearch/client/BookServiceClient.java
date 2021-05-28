package com.itmd.elasticsearch.client;

import com.itmd.api.BookServiceApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("book-service")
public interface BookServiceClient extends BookServiceApi {
}
