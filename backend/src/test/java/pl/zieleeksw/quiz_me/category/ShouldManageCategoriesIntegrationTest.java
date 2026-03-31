package pl.zieleeksw.quiz_me.category;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import pl.zieleeksw.quiz_me.BaseIntegration;
import pl.zieleeksw.quiz_me.auth.AuthenticationApi;
import pl.zieleeksw.quiz_me.course.TestCourseDto;
import pl.zieleeksw.quiz_me.course.TestCreateCourseRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static pl.zieleeksw.quiz_me.utils.HttpValidationUtils.*;

class ShouldManageCategoriesIntegrationTest extends BaseIntegration {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationApi authenticationApi;

    @Test
    void shouldCreateFetchRenameAndArchiveCategoryAsOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Boot Associate",
                        "A focused course for architecture, persistence, and testing drills."
                )
        );

        final ResultActions createResult = mockMvc.perform(post("/courses/{courseId}/categories", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestCreateCategoryRequest("Web"))));

        final TestCategoryDto createdCategory = readResponse(createResult, TestCategoryDto.class);

        itShouldReturnCreatedStatus(createResult);
        assertThat(createdCategory.id()).isNotNull();
        assertThat(createdCategory.courseId()).isEqualTo(course.id());
        assertThat(createdCategory.name()).isEqualTo("Web");
        assertThat(createdCategory.active()).isTrue();

        final ResultActions updateResult = mockMvc.perform(put("/courses/{courseId}/categories/{categoryId}", course.id(), createdCategory.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestUpdateCategoryRequest("Spring Web"))));

        final TestCategoryDto updatedCategory = readResponse(updateResult, TestCategoryDto.class);

        itShouldReturnOkStatus(updateResult);
        assertThat(updatedCategory.id()).isEqualTo(createdCategory.id());
        assertThat(updatedCategory.name()).isEqualTo("Spring Web");
        assertThat(updatedCategory.updatedAt()).isAfterOrEqualTo(createdCategory.updatedAt());

        final ResultActions fetchResult = mockMvc.perform(get("/courses/{courseId}/categories", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestCategoryDto> categories = readListResponse(fetchResult);

        itShouldReturnOkStatus(fetchResult);
        assertThat(categories)
                .extracting(TestCategoryDto::name)
                .containsExactly("Spring Web");

        final ResultActions deleteResult = mockMvc.perform(delete("/courses/{courseId}/categories/{categoryId}", course.id(), createdCategory.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        itShouldReturnNoContentStatus(deleteResult);
        itShouldHaveEmptyResponseBody(deleteResult);

        final ResultActions fetchAfterDeleteResult = mockMvc.perform(get("/courses/{courseId}/categories", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(ownerAuthentication.accessToken().value()))
                .contentType(MediaType.APPLICATION_JSON));

        final List<TestCategoryDto> categoriesAfterDelete = readListResponse(fetchAfterDeleteResult);

        itShouldReturnOkStatus(fetchAfterDeleteResult);
        assertThat(categoriesAfterDelete).isEmpty();
    }

    @Test
    void shouldReturnForbiddenWhenCreatingCategoryAsRegularNonOwner() throws Exception {
        final var ownerAuthentication = authenticationApi.registerAndLogin();
        final TestCourseDto course = createCourse(
                ownerAuthentication.accessToken().value(),
                new TestCreateCourseRequest(
                        "Spring Security Associate",
                        "A focused course for filters, JWTs, authorization rules, and access control."
                )
        );
        final String anotherUserAccessToken = authenticationApi.registerAndLogin().accessToken().value();

        final ResultActions result = mockMvc.perform(post("/courses/{courseId}/categories", course.id())
                .header(AUTHORIZATION_HEADER, bearerToken(anotherUserAccessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TestCreateCategoryRequest("Security"))));

        itShouldReturnForbiddenStatus(result);
        itShouldHaveEmptyResponseBody(result);
    }

    private TestCourseDto createCourse(
            final String accessToken,
            final TestCreateCourseRequest request
    ) throws Exception {
        final MvcResult result = mockMvc.perform(post("/courses")
                        .header(AUTHORIZATION_HEADER, bearerToken(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), TestCourseDto.class);
    }

    private List<TestCategoryDto> readListResponse(
            final ResultActions result
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }

    private <T> T readResponse(
            final ResultActions result,
            final Class<T> responseClass
    ) throws Exception {
        final MvcResult mvcResult = result.andReturn();
        final String content = mvcResult.getResponse().getContentAsString();
        return objectMapper.readValue(content, responseClass);
    }

    private String bearerToken(final String accessToken) {
        return BEARER_PREFIX + accessToken;
    }
}
