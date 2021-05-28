package com.itmd.elasticsearch.pojo;


import com.itmd.pojo.BookClassification;
import com.itmd.pojo.BookPress;
import com.itmd.vo.PageResult;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SearchResult extends PageResult<Books> {

    private List<BookClassification> classification;
    private List<BookPress> press;
    public SearchResult(Long total, Long totalPage, List<Books>items, List<BookClassification>classification, List<BookPress>press){
        super(total,totalPage,items);
        this.press = press;
        this.classification = classification;
    }

}
