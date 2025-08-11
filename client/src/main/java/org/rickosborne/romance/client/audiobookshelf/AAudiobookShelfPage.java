package org.rickosborne.romance.client.audiobookshelf;

import lombok.Data;

@Data
public abstract class AAudiobookShelfPage {
    String filter;
    String include;
    Integer limit;
    Boolean minified;
    Integer offset;
    Integer page;
    String sortBy;
    Boolean sortDesc;
    Integer total;
}
