package io.metersphere.system.controller;

import com.jayway.jsonpath.JsonPath;
import io.metersphere.sdk.constants.SessionConstants;
import io.metersphere.sdk.util.JSON;
import io.metersphere.system.dto.UserCreateInfo;
import io.metersphere.system.dto.UserRoleOption;
import io.metersphere.system.dto.request.UserBatchProcessRequest;
import io.metersphere.system.dto.request.UserChangeEnableRequest;
import io.metersphere.system.utils.UserTestUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(scripts = {"/dml/init_user_controller_none_permission_test.sql"},
        config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class UserControllerNonePermissionTests {
    @Resource
    private MockMvc mockMvc;
    protected static String sessionId;
    protected static String csrfToken;
    private static final String NONE_ROLE_USERNAME = "tianyang.member@163.com";
    private static final String NONE_ROLE_PASSWORD = "tianyang.member@163.com";

    private static final ResultMatcher CHECK_RESULT_MATHER = status().isForbidden();

    /**
     * 用户创建接口判断无权限用户是否可以访问
     * 接口参数校验优先级大于操作用户权限校验，所以参数要保证合法化
     */
    @Test
    @Order(0)
    public void testGetGlobalSystemUserRoleSuccess() throws Exception {
        UserCreateInfo paramUserInfo = new UserCreateInfo() {{
            setId("testId");
            setName("tianyang.no.permission.email");
            setEmail("tianyang.no.permission.email@126.com");
        }};
        List<UserRoleOption> paramRoleList = new ArrayList<>() {{
            this.add(
                    new UserRoleOption() {{
                        this.setId("member");
                        this.setName("member");
                    }});
        }};
        this.requestGet(String.format(UserTestUtils.URL_USER_GET, NONE_ROLE_USERNAME), CHECK_RESULT_MATHER);
        //校验权限：系统全局用户组获取
        this.requestGet(UserTestUtils.URL_GET_GLOBAL_SYSTEM, CHECK_RESULT_MATHER);
        //校验权限：用户创建
        this.requestPost(UserTestUtils.URL_USER_CREATE,
                UserTestUtils.getUserCreateDTO(
                        paramRoleList,
                        new ArrayList<>() {{
                            add(paramUserInfo);
                        }}
                ),
                CHECK_RESULT_MATHER);
        //校验权限：分页查询用户列表
        this.requestPost(UserTestUtils.URL_USER_PAGE, UserTestUtils.getDefaultPageRequest(), CHECK_RESULT_MATHER);
        //校验权限：修改用户
        this.requestPost(UserTestUtils.URL_USER_UPDATE,
                UserTestUtils.getUserUpdateDTO(paramUserInfo, paramRoleList), CHECK_RESULT_MATHER);
        //校验权限：启用/禁用用户
        UserChangeEnableRequest userChangeEnableRequest = new UserChangeEnableRequest();
        userChangeEnableRequest.setEnable(false);
        userChangeEnableRequest.setUserIdList(new ArrayList<>() {{
            this.add("testId");
        }});
        this.requestPost(UserTestUtils.URL_USER_UPDATE_ENABLE, userChangeEnableRequest, CHECK_RESULT_MATHER);

        //用户导入
        //导入正常文件
        String filePath = this.getClass().getClassLoader().getResource("file/user_import_success.xlsx").getPath();
        MockMultipartFile file = new MockMultipartFile("file", "userImport.xlsx", MediaType.APPLICATION_OCTET_STREAM_VALUE, UserTestUtils.getFileBytes(filePath));
        this.requestFile(UserTestUtils.URL_USER_IMPORT, file, CHECK_RESULT_MATHER);
        //用户删除
        UserBatchProcessRequest request = new UserBatchProcessRequest();
        request.setUserIdList(new ArrayList<>() {{
            this.add("testId");
        }});
        this.requestPost(UserTestUtils.URL_USER_DELETE, request, CHECK_RESULT_MATHER);
    }

    @BeforeEach
    public void login() throws Exception {
        if (StringUtils.isAnyBlank(sessionId, csrfToken)) {
            MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/login")
                            .content("{\"username\":\"" + NONE_ROLE_USERNAME + "\",\"password\":\"" + NONE_ROLE_PASSWORD + "\"}")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();
            sessionId = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.data.sessionId");
            csrfToken = JsonPath.read(mvcResult.getResponse().getContentAsString(), "$.data.csrfToken");
        }
    }

    private void requestPost(String url, Object param, ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header(SessionConstants.HEADER_TOKEN, sessionId)
                        .header(SessionConstants.CSRF_TOKEN, csrfToken)
                        .content(JSON.toJSONString(param))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(resultMatcher)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private void requestFile(String url, MockMultipartFile file, ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart(url)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .header(SessionConstants.HEADER_TOKEN, sessionId)
                        .header(SessionConstants.CSRF_TOKEN, csrfToken))
                .andExpect(resultMatcher)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private void requestGet(String url, ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .header(SessionConstants.HEADER_TOKEN, sessionId)
                        .header(SessionConstants.CSRF_TOKEN, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(resultMatcher)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}