package com.itmd.elasticsearch.Controller;

import com.itmd.elasticsearch.pojo.Books;
import com.itmd.elasticsearch.pojo.SearchRequest;
import com.itmd.elasticsearch.pojo.SearchResult;
import com.itmd.elasticsearch.service.SearchService;

import com.itmd.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 条件过滤查询分页
     * @param request
     * @return
     */
    @PostMapping("search/page")
    public ResponseEntity<PageResult<Books>> search(@RequestBody SearchRequest request){
        return ResponseEntity.ok(searchService.search(request));
    }

    /**
     * 根据条件查询所有相关提示
     * @param key
     * @return
     */
    @PostMapping("search/key/Str")
    public ResponseEntity<List<String>>searchStr(@RequestParam("StrKey")String key){
        return ResponseEntity.ok(searchService.searchStr(key));
    }
}
