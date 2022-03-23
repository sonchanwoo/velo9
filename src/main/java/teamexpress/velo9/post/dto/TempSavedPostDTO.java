package teamexpress.velo9.post.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import teamexpress.velo9.post.domain.Post;

import java.time.LocalDateTime;


public class TempSavedPostDTO {

    private String title;
    private String introduce;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd-HH-mm-ss", timezone = "Asia/Seoul")
    private LocalDateTime createdDate;


    public TempSavedPostDTO(Post post) {
        title = post.getTitle();
        introduce = post.getIntroduce();
        createdDate = post.getCreatedDate();
    }
}
