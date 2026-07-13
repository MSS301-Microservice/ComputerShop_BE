package sp26.group3.computer.sba301_computershop.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import sp26.group3.computer.sba301_computershop.dto.request.BlogCreationRequest;
import sp26.group3.computer.sba301_computershop.dto.request.BlogUpdateRequest;
import sp26.group3.computer.sba301_computershop.dto.response.BlogResponse;
import sp26.group3.computer.sba301_computershop.entity.Blog;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(target = "blogId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    Blog toBlog(BlogCreationRequest request);

    // userName không map được ở đây nữa vì Blog chỉ còn userId thô (User thuộc
    // user-service, DB khác) — service layer tự enrich userName qua UserServiceClient
    // sau khi gọi hàm này.
    @Mapping(target = "userName", ignore = true)
    BlogResponse toBlogResponse(Blog blog);

    @Mapping(target = "blogId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    void updateBlog(@MappingTarget Blog blog, BlogUpdateRequest request);
}