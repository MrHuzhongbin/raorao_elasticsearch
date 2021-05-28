package com.itmd.elasticsearch;

import com.itmd.elasticsearch.client.BookServiceClient;
import com.itmd.elasticsearch.pojo.Books;
import com.itmd.elasticsearch.repository.BookRepository;
import com.itmd.elasticsearch.service.SearchService;
import com.itmd.vo.BookLw;
import com.itmd.vo.PageResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RaoraoElasticsearchApplicationTests {
    @Autowired
    private ElasticsearchTemplate template;
    @Autowired
    private BookServiceClient bookServiceClient;
    @Autowired
    private SearchService searchService;
    @Autowired
    private BookRepository bookRepository;
    @Test
    public void contextLoads() {
        template.createIndex(Books.class);
        template.putMapping(Books.class);
    }

    @Test
    public void BuildData(){
        //查询spu数据
        int page = 1;
        int row = 100;
        int size = 0;
        do {
            PageResult<BookLw> bookLwPageResult = bookServiceClient.queryBookInfoByPage(page, row, null, true);
            List<BookLw> items = bookLwPageResult.getItems();
            if(CollectionUtils.isEmpty(items)){
                System.out.println("执行了break");
                break;
            }
            //构建goods
            List<Books> collect = items.stream().map(searchService::BuildBooks).collect(Collectors.toList());

            //存入索引库
            bookRepository.saveAll(collect);
            page++;
            size = collect.size();
        }while(size == 20);
    }
}
