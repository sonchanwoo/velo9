package teamexpress.velo9.post.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import teamexpress.velo9.member.domain.LookRepository;
import teamexpress.velo9.member.domain.Love;
import teamexpress.velo9.member.domain.LoveRepository;
import teamexpress.velo9.member.domain.Member;
import teamexpress.velo9.member.domain.MemberRepository;
import teamexpress.velo9.post.api.PostThumbnailFileUploader;
import teamexpress.velo9.post.domain.Post;
import teamexpress.velo9.post.domain.PostRepository;
import teamexpress.velo9.post.domain.PostStatus;
import teamexpress.velo9.post.domain.PostTag;
import teamexpress.velo9.post.domain.PostTagRepository;
import teamexpress.velo9.post.domain.PostThumbnail;
import teamexpress.velo9.post.domain.Series;
import teamexpress.velo9.post.domain.SeriesRepository;
import teamexpress.velo9.post.domain.Tag;
import teamexpress.velo9.post.domain.TagRepository;
import teamexpress.velo9.post.domain.TemporaryPost;
import teamexpress.velo9.post.domain.TemporaryPostRepository;
import teamexpress.velo9.post.dto.LookPostDTO;
import teamexpress.velo9.post.dto.LoveDTO;
import teamexpress.velo9.post.dto.LovePostDTO;
import teamexpress.velo9.post.dto.PostLoadDTO;
import teamexpress.velo9.post.dto.PostMainDTO;
import teamexpress.velo9.post.dto.PostReadDTO;
import teamexpress.velo9.post.dto.PostWriteDTO;
import teamexpress.velo9.post.dto.ReadDTO;
import teamexpress.velo9.post.dto.SearchCondition;
import teamexpress.velo9.post.dto.SeriesDTO;
import teamexpress.velo9.post.dto.SeriesPostSummaryDTO;
import teamexpress.velo9.post.dto.SeriesReadDTO;
import teamexpress.velo9.post.dto.TempSavedPostDTO;
import teamexpress.velo9.post.dto.TemporaryPostWriteDTO;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

	private static final int MAX_TEMPORARY_COUNT = 20;

	private final PostRepository postRepository;
	private final SeriesRepository seriesRepository;
	private final MemberRepository memberRepository;
	private final LoveRepository loveRepository;
	private final LookRepository lookRepository;
	private final TemporaryPostRepository temporaryPostRepository;
	private final TagRepository tagRepository;
	private final PostTagRepository postTagRepository;

	@Transactional
	public Long write(PostWriteDTO postWriteDTO, Long memberId) {
		if (isNewPost(postWriteDTO)) {
			return writeNew(postWriteDTO, memberId);
		}
		return edit(postWriteDTO, memberId);
	}

	private boolean isNewPost(PostWriteDTO postWriteDTO) {
		return postWriteDTO.getPostId() == null;
	}

	private Long writeNew(PostWriteDTO postWriteDTO, Long memberId) {
		PostThumbnail postThumbnail = getPostThumbnail(postWriteDTO.getThumbnailFileName());
		Series series = getSeries(postWriteDTO.getSeriesId());
		Member member = getMember(memberId);

		Post post = postRepository.save(postWriteDTO.toPost(member, series, postThumbnail));
		updateTags(post, postWriteDTO.getTags());
		return post.getId();
	}

	private Long edit(PostWriteDTO postWriteDTO, Long memberId) {
		PostThumbnail postThumbnailToSave = getPostThumbnail(postWriteDTO.getThumbnailFileName());
		Series series = getSeries(postWriteDTO.getSeriesId());

		Post post = postRepository.findByIdWithJoin(postWriteDTO.getPostId()).orElseThrow();
		checkSameWriter(post, memberId);

		postWriteDTO.makeIntroduce();
		post.edit(postWriteDTO.getTitle(),
			postWriteDTO.getIntroduce(),
			postWriteDTO.getContent(),
			postWriteDTO.getAccess(),
			series,
			updateThumbnail(post.getPostThumbnail(), postThumbnailToSave));

		updateTags(post, postWriteDTO.getTags());

		return post.getId();
	}

	private PostThumbnail getPostThumbnail(String fileName) {
		return fileName != null ? PostThumbnailFileUploader.divideFileName(fileName).toPostThumbnail() : null;
	}

	private Series getSeries(Long seriesId) {
		return seriesId != null ? seriesRepository.findById(seriesId).orElseThrow(IllegalStateException::new) : null;
	}

	private Member getMember(Long memberId) {
		if (memberId == null) {
			throw new NullPointerException("member id MUST NOT BE NULL!!!");
		}

		return memberRepository.findById(memberId)
			.orElseThrow(() -> new NullPointerException("no member"));
	}

	private void updateTags(Post post, List<String> tagNames) {
		cleanTagsIfNecessary(post);

		if (isEmpty(tagNames)) {
			return;
		}

		tagNames = removeDuplication(tagNames);

		List<Tag> existingTags = addTagsUnknown(tagNames);

		putPostTags(post, existingTags, tagNames);		
	}

	private List<Tag> addTagsUnknown(List<String> tagNames){
		List<Tag> existingTags = tagRepository.findAll();

		existingTags.addAll(
			tagNames.stream(
				).filter(name -> existingTags.stream().map(Tag::getName).noneMatch(name::equals)
				).map(name -> tagRepository.save(Tag.builder().name(name).build()))
				.collect(Collectors.toList())
		);

		return existingTags;
	}

	private void putPostTags(Post post, List<Tag> existingTags, List<String> tagNames){
		tagNames.stream(
		).map(name ->
			existingTags.stream().filter(tag -> tag.getName().equals(name)).findFirst().orElseThrow()
		).forEach(tag -> postTagRepository.save(
			PostTag.builder()
				.tag(tag)
				.post(post)
				.build()
		));
	}

	private void cleanTagsIfNecessary(Post post) {
		if (post.getId() != null) {
			postTagRepository.deleteAllByPost(post);
		}
	}

	private void checkSameWriter(Post post, Long memberId) {
		if (!post.getMember().getId().equals(memberId)) {
			throw new IllegalStateException("잘못된 접근입니다.");
		}
	}

	private PostThumbnail updateThumbnail(PostThumbnail savedPostThumbnail, PostThumbnail postThumbnailToSave) {
		return savedPostThumbnail != null && postThumbnailToSave != null ?
			savedPostThumbnail.edit(postThumbnailToSave.getUuid(),
				postThumbnailToSave.getPath(),
				postThumbnailToSave.getName()
			) : postThumbnailToSave;
	}

	private List<String> removeDuplication(List<String> tags) {
		return tags.stream().distinct().collect(Collectors.toList());
	}

	private boolean isEmpty(List<String> tags) {
		return tags == null || tags.isEmpty();
	}

	@Transactional
	public Long writeTemporary(TemporaryPostWriteDTO temporaryPostWriteDTO, Long memberId) {
		if (temporaryPostWriteDTO.getPostId() != null) {
			return writeAlternativeTemporary(temporaryPostWriteDTO, memberId);
		}

		return writeNewTemporary(temporaryPostWriteDTO, memberId);
	}

	@Transactional
	public void remove(Long id) {
		lookRepository.deleteByPostId(id);
		loveRepository.deleteByPostId(id);
		postRepository.deleteById(id);
	}

	public PostLoadDTO findPostById(Long id) {
		Post post = postRepository.findById(id).orElseThrow(
			() -> new IllegalStateException("존재하지 않는 포스트입니다."));
		List<PostTag> postTags = postTagRepository.findByPost(post);
		return new PostLoadDTO(post, postTags);
	}

	public Slice<SeriesDTO> findSeries(String nickname, Pageable pageable) {
		Slice<Series> seriesList = seriesRepository.findPostBySeriesName(nickname, pageable);
		return seriesList.map(SeriesDTO::new);
	}

	public Slice<PostReadDTO> findMainPost(String nickname, String tagName, Pageable pageable) {
		Slice<Post> posts = postRepository.findPost(nickname, tagName, pageable);
		return posts.map(PostReadDTO::new);
	}

	@Transactional
	public void loveOrNot(LoveDTO loveDTO, Long memberId) {
		Member member = memberRepository.findById(memberId).orElseThrow();
		Post post = postRepository.findById(loveDTO.getPostId()).orElseThrow();

		toggleLove(member, post);
		postRepository.updateLoveCount(post, loveRepository.countByPost(post));
	}

	@Transactional
	public void look(Long postId, Long memberId) {
		makeLook(memberId, postId);
		postRepository.updateViewCount(postId);
	}

	public Page<PostMainDTO> searchMain(SearchCondition searchCondition, Pageable pageable) {
		return postRepository.search(searchCondition, pageable).map(PostMainDTO::new);
	}

	public List<TempSavedPostDTO> findTempPosts(Long id) {
		List<Post> findPosts = postRepository.getTempSavedPost(id, PostStatus.TEMPORARY);

		return findPosts.stream()
			.map(TempSavedPostDTO::new)
			.collect(Collectors.toList());
	}

	private Long writeAlternativeTemporary(TemporaryPostWriteDTO temporaryPostWriteDTO, Long memberId) {
		Post post = postRepository.findById(temporaryPostWriteDTO.getPostId()).orElseThrow();

		if (post.getStatus().equals(PostStatus.TEMPORARY)) {
			return writeNewTemporary(temporaryPostWriteDTO, memberId);
		}

		if (post.getTemporaryPost() != null) {
			temporaryPostWriteDTO.setAlternativeId(post.getTemporaryPost().getId());
		}

		TemporaryPost temporaryPost = temporaryPostWriteDTO.toTemporaryPost();
		temporaryPostRepository.save(temporaryPost);
		postRepository.updateTempPost(post.getId(), temporaryPost);

		return post.getId();
	}

	private Long writeNewTemporary(TemporaryPostWriteDTO temporaryPostWriteDTO, Long memberId) {
		checkCountTemp(memberId);
		Member member = getMember(memberId);
		return postRepository.save(
			temporaryPostWriteDTO.toPost(
				member, postRepository.getCreatedDate(temporaryPostWriteDTO.getPostId()))).getId();
	}

	private void toggleLove(Member member, Post post) {
		loveRepository.findByPostAndMember(post, member).ifPresentOrElse(
			loveRepository::delete,
			() -> loveRepository.save(
				Love.builder()
					.post(post)
					.member(member)
					.build()
			)
		);
	}

	private void makeLook(Long memberId, Long postId) {
		if (lookRepository.findByPostAndMember(postId, memberId).isEmpty()) {
			lookRepository.saveLook(memberId, postId);
		}
	}

	private void checkCountTemp(Long memberId) {
		if (postRepository.countByMemberAndStatus(memberRepository.findById(memberId).orElseThrow(), PostStatus.TEMPORARY) >= MAX_TEMPORARY_COUNT) {
			throw new IllegalStateException("임시저장은 " + MAX_TEMPORARY_COUNT + "개까지만 가능");
		}
	}

	public Slice<LovePostDTO> findLovePosts(Long memberId, PageRequest page) {
		Slice<Post> lovePosts = postRepository.findByJoinLove(memberId, page);
		return lovePosts.map(LovePostDTO::new);
	}

	public Slice<LookPostDTO> findReadPost(Long memberId, PageRequest page) {
		Slice<Post> lookPosts = postRepository.findByJoinLook(memberId, page);
		return lookPosts.map(LookPostDTO::new);
	}

	public ReadDTO findPostDetails(Long postId, String nickname) {
		Post findPost = postRepository.findPostMemberById(postId).orElseThrow();
		checkOwner(findPost, nickname);
		List<Post> pagePost = postRepository.findPrevNextPost(findPost);
		List<PostTag> postTags = postTagRepository.findByPost(findPost);
		return new ReadDTO(findPost, pagePost, postTags);
	}

	public Slice<SeriesPostSummaryDTO> findPostsInSeries(String nickname, String seriesName, PageRequest page) {
		Slice<Post> seriesPosts = postRepository.findByJoinSeries(nickname, seriesName, page);
		return seriesPosts.map(SeriesPostSummaryDTO::new);
	}

	public List<SeriesReadDTO> findAllSeries(String nickname) {
		return seriesRepository.findUsedSeries(nickname)
			.stream().map(SeriesReadDTO::new).collect(Collectors.toList());
	}

	private void checkOwner(Post findPost, String nickname) {
		if (!findPost.getMember().getNickname().equals(nickname)) {
			throw new IllegalStateException("비정상적인 접근입니다.");
		}
	}
}
