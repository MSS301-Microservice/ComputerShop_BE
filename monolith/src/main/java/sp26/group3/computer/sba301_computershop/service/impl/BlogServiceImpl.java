package sp26.group3.computer.sba301_computershop.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sp26.group3.computer.sba301_computershop.client.UserServiceClient;
import sp26.group3.computer.sba301_computershop.dto.request.BlogCreationRequest;
import sp26.group3.computer.sba301_computershop.dto.request.BlogUpdateRequest;
import sp26.group3.computer.sba301_computershop.dto.response.BlogResponse;
import sp26.group3.computer.sba301_computershop.dto.response.PagedResponse;
import sp26.group3.computer.sba301_computershop.dto.response.UserSummaryResponse;
import sp26.group3.computer.sba301_computershop.entity.Blog;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.mapper.BlogMapper;
import sp26.group3.computer.sba301_computershop.repository.BlogRepository;
import sp26.group3.computer.sba301_computershop.service.BlogService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BlogServiceImpl implements BlogService {

    BlogRepository blogRepository;
    BlogMapper blogMapper;
    UserServiceClient userServiceClient;

    private BlogResponse toResponseWithUserName(Blog blog) {
        BlogResponse response = blogMapper.toBlogResponse(blog);
        UserSummaryResponse user = userServiceClient.getUser(blog.getUserId());
        response.setUserName(user != null ? user.getUsername() : null);
        return response;
    }

    @Override
    public BlogResponse createBlog(BlogCreationRequest request) {
        log.info("Creating blog with title: {}", request.getTitle());

        // Check if user exists (via user-service, không còn bảng users cục bộ)
        UserSummaryResponse user = userServiceClient.getUser(request.getUserId());
        if (user == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        Blog blog = blogMapper.toBlog(request);
        blog.setUserId(request.getUserId());
        blog.setPublishedAt(LocalDateTime.now());

        Blog savedBlog = blogRepository.save(blog);
        log.info("Blog created successfully with id: {}", savedBlog.getBlogId());

        return toResponseWithUserName(savedBlog);
    }

    @Override
    public BlogResponse updateBlog(int blogId, BlogUpdateRequest request) {
        log.info("Updating blog with id: {}", blogId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blogMapper.updateBlog(blog, request);

        Blog updatedBlog = blogRepository.save(blog);
        log.info("Blog updated successfully with id: {}", blogId);

        return toResponseWithUserName(updatedBlog);
    }

    @Override
    public BlogResponse getBlogById(int blogId) {
        log.info("Getting blog with id: {}", blogId);

        Blog blog = blogRepository.findById(blogId)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        return toResponseWithUserName(blog);
    }

    @Override
    public List<BlogResponse> getAllBlogs() {
        log.info("Getting all blogs");

        return blogRepository.findAll()
                .stream()
                .map(this::toResponseWithUserName)
                .toList();
    }

    @Override
    public PagedResponse<BlogResponse> getAllBlogsPaged(Pageable pageable) {
        Page<BlogResponse> page = blogRepository.findAll(pageable)
                .map(this::toResponseWithUserName);
        return PagedResponse.<BlogResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public List<BlogResponse> getBlogsByUserId(int userId) {
        log.info("Getting blogs for user id: {}", userId);

        if (userServiceClient.getUser(userId) == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }

        return blogRepository.findByUserId(userId)
                .stream()
                .map(this::toResponseWithUserName)
                .toList();
    }

    @Override
    public PagedResponse<BlogResponse> getBlogsByUserIdPaged(int userId, Pageable pageable) {
        if (userServiceClient.getUser(userId) == null) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        Page<BlogResponse> page = blogRepository.findByUserId(userId, pageable)
                .map(this::toResponseWithUserName);
        return PagedResponse.<BlogResponse>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public void deleteBlog(int blogId) {
        log.warn("Deleting blog with id: {}", blogId);

        if (!blogRepository.existsById(blogId)) {
            throw new AppException(ErrorCode.BLOG_NOT_FOUND);
        }

        blogRepository.deleteById(blogId);
        log.info("Blog deleted successfully with id: {}", blogId);
    }
}
