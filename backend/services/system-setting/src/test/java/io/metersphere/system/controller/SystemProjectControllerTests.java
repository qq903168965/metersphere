package io.metersphere.system.controller;

import base.BaseTest;
import io.metersphere.project.domain.Project;
import io.metersphere.project.domain.ProjectExample;
import io.metersphere.project.mapper.ProjectMapper;
import io.metersphere.sdk.constants.InternalUserRole;
import io.metersphere.sdk.constants.SessionConstants;
import io.metersphere.sdk.controller.handler.ResultHolder;
import io.metersphere.sdk.dto.AddProjectRequest;
import io.metersphere.sdk.dto.ProjectDTO;
import io.metersphere.sdk.dto.UpdateProjectRequest;
import io.metersphere.sdk.log.constants.OperationLogType;
import io.metersphere.sdk.util.JSON;
import io.metersphere.sdk.util.Pager;
import io.metersphere.system.domain.UserRoleRelation;
import io.metersphere.system.domain.UserRoleRelationExample;
import io.metersphere.system.dto.UserExtend;
import io.metersphere.system.mapper.UserRoleRelationMapper;
import io.metersphere.system.request.ProjectAddMemberRequest;
import io.metersphere.system.request.ProjectMemberRequest;
import io.metersphere.system.request.ProjectRequest;
import io.metersphere.utils.JsonUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SystemProjectControllerTests extends BaseTest {

    @Resource
    private MockMvc mockMvc;

    private final static String prefix = "/system/project";
    private final static String addProject = prefix + "/add";
    private final static String updateProject = prefix + "/update";
    private final static String deleteProject = prefix + "/delete/";
    private final static String revokeProject = prefix + "/revoke/";
    private final static String getProject = prefix + "/get/";
    private final static String getProjectList = prefix + "/page";
    private final static String getProjectMemberList = prefix + "/member-list";
    private final static String addProjectMember = prefix + "/add-member";
    private final static String removeProjectMember = prefix + "/remove-member/";
    private static final ResultMatcher BAD_REQUEST_MATCHER = status().isBadRequest();
    private static final ResultMatcher ERROR_REQUEST_MATCHER = status().is5xxServerError();

    private static String projectId;

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private UserRoleRelationMapper userRoleRelationMapper;

    private void requestPost(String url, Object param, ResultMatcher resultMatcher) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header(SessionConstants.HEADER_TOKEN, sessionId)
                        .header(SessionConstants.CSRF_TOKEN, csrfToken)
                        .content(JSON.toJSONString(param))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(resultMatcher).andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private MvcResult responsePost(String url, Object param) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .header(SessionConstants.HEADER_TOKEN, sessionId)
                        .header(SessionConstants.CSRF_TOKEN, csrfToken)
                        .content(JSON.toJSONString(param))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();
    }

    private MvcResult responseGet(String url) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(url)
                        .header(SessionConstants.HEADER_TOKEN, sessionId)
                        .header(SessionConstants.CSRF_TOKEN, csrfToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andDo(print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andReturn();
    }

    public static <T> T parseObjectFromMvcResult(MvcResult mvcResult, Class<T> parseClass) {
        try {
            String returnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
            ResultHolder resultHolder = JsonUtils.parseObject(returnData, ResultHolder.class);
            //返回请求正常
            Assertions.assertNotNull(resultHolder);
            return JSON.parseObject(JSON.toJSONString(resultHolder.getData()), parseClass);
        } catch (Exception ignore) {
        }
        return null;
    }
    public static void compareProjectDTO(Project currentProject, Project result) {
        Assertions.assertNotNull(currentProject);
        Assertions.assertNotNull(result);
        //判断ID是否一样
        Assertions.assertTrue(StringUtils.equals(currentProject.getId(), result.getId()));
        //判断名称是否一样
        Assertions.assertTrue(StringUtils.equals(currentProject.getName(), result.getName()));
        //判断组织是否一样
        Assertions.assertTrue(StringUtils.equals(currentProject.getOrganizationId(), result.getOrganizationId()));
        //判断描述是否一样
        Assertions.assertTrue(StringUtils.equals(currentProject.getDescription(), result.getDescription()));
        //判断是否启用
        Assertions.assertTrue(currentProject.getEnable() == result.getEnable());

    }

    public AddProjectRequest generatorAdd(String organizationId, String name, String description, boolean enable, List<String> userIds) {
        AddProjectRequest addProjectDTO = new AddProjectRequest();
        addProjectDTO.setOrganizationId(organizationId);
        addProjectDTO.setName(name);
        addProjectDTO.setDescription(description);
        addProjectDTO.setEnable(enable);
        addProjectDTO.setUserIds(userIds);
        return addProjectDTO;
    }
    public UpdateProjectRequest generatorUpdate(String organizationId,
                                                String projectId,
                                                String name,
                                                String description,
                                                boolean enable,
                                                List<String> userIds) {
        UpdateProjectRequest updateProjectDTO = new UpdateProjectRequest();
        updateProjectDTO.setOrganizationId(organizationId);
        updateProjectDTO.setId(projectId);
        updateProjectDTO.setName(name);
        updateProjectDTO.setDescription(description);
        updateProjectDTO.setEnable(enable);
        updateProjectDTO.setUserIds(userIds);
        return updateProjectDTO;
    }
    @Test
    @Order(1)
    /**
     * 测试添加项目成功的情况
     */
    @Sql(scripts = {"/dml/init_project.sql"},
            config = @SqlConfig(encoding = "utf-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
            executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    public void testAddProjectSuccess() throws Exception {
        AddProjectRequest project = this.generatorAdd("organizationId","name", "description", true, List.of("admin"));
        MvcResult mvcResult = this.responsePost(addProject, project);
        Project result = this.parseObjectFromMvcResult(mvcResult, Project.class);
        ProjectExample projectExample = new ProjectExample();
        projectExample.createCriteria().andOrganizationIdEqualTo(project.getOrganizationId()).andNameEqualTo(project.getName());
        List<Project> projects = projectMapper.selectByExample(projectExample);
        projectId = result.getId();
        // 校验日志
        checkLog(projectId, OperationLogType.ADD);

        this.compareProjectDTO(projects.get(0), result);
        UserRoleRelationExample userRoleRelationExample = new UserRoleRelationExample();
        userRoleRelationExample.createCriteria().andSourceIdEqualTo(projectId).andRoleIdEqualTo(InternalUserRole.PROJECT_ADMIN.getValue());
        List<UserRoleRelation> userRoleRelations = userRoleRelationMapper.selectByExample(userRoleRelationExample);
        Assertions.assertEquals(userRoleRelations.stream().map(UserRoleRelation::getUserId).collect(Collectors.toList()).containsAll(List.of("admin")), true);
        userRoleRelationExample.createCriteria().andSourceIdEqualTo(projectId).andRoleIdEqualTo(InternalUserRole.PROJECT_MEMBER.getValue());
         userRoleRelations = userRoleRelationMapper.selectByExample(userRoleRelationExample);
        Assertions.assertEquals(userRoleRelations.stream().map(UserRoleRelation::getUserId).collect(Collectors.toList()).containsAll(List.of("admin")), true);
    }

    @Test
    @Order(2)
    /**
     * 测试添加项目失败的用例
     */
    public void testAddProjectError() throws Exception {
        //项目名称存在 500
        AddProjectRequest project = this.generatorAdd("organizationId","name", "description", true, List.of("admin"));
        this.requestPost(addProject, project, ERROR_REQUEST_MATCHER);
        //参数组织Id为空
        project = this.generatorAdd(null, null, null, true, List.of("admin"));
        this.requestPost(addProject, project, BAD_REQUEST_MATCHER);
        //项目名称为空
        project = this.generatorAdd("organizationId", null, null, true, List.of("admin"));
        this.requestPost(addProject, project, BAD_REQUEST_MATCHER);
        //项目成员为空
        project = this.generatorAdd("organizationId", "name", null, true, new ArrayList<>());
        this.requestPost(addProject, project, BAD_REQUEST_MATCHER);
        //项目成员在系统中不存在
        project = this.generatorAdd("organizationId", "name", null, true, List.of("admin", "admin1", "admin2"));
        this.requestPost(addProject, project, ERROR_REQUEST_MATCHER);

    }
    @Test
    @Order(3)
    public void testGetProject() throws Exception {
        MvcResult mvcResult = this.responseGet(getProject + projectId);
        Project project = this.parseObjectFromMvcResult(mvcResult, Project.class);
        Assertions.assertTrue(StringUtils.equals(project.getId(), projectId));
    }
    @Test
    @Order(4)
    public void testGetProjectError() throws Exception {
        //项目不存在
        MvcResult mvcResult = this.responseGet(getProject + "111111");
        String returnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JsonUtils.parseObject(returnData, ResultHolder.class);
        Assertions.assertNull(resultHolder.getData());
    }

    @Test
    @Order(5)
    public void testGetProjectList() throws Exception {
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.setCurrent(1);
        projectRequest.setPageSize(10);
        MvcResult mvcResult = this.responsePost(getProjectList, projectRequest);
        Pager<?> returnPager = parseObjectFromMvcResult(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), projectRequest.getCurrent());
        //返回的数据量不超过规定要返回的数据量相同
        Assertions.assertTrue(((List<ProjectDTO>) returnPager.getList()).size() <= projectRequest.getPageSize());
        projectRequest.setSort(new HashMap<>() {{
            put("createTime", "desc");
        }});
        mvcResult = this.responsePost(getProjectList, projectRequest);
        returnPager = parseObjectFromMvcResult(mvcResult, Pager.class);
        //第一个数据的createTime是最大的
        List<ProjectDTO> projectDTOS = JSON.parseArray(JSON.toJSONString(returnPager.getList()), ProjectDTO.class);
        long firstCreateTime = projectDTOS.get(0).getCreateTime();
        for (ProjectDTO projectDTO : projectDTOS) {
            Assertions.assertFalse(projectDTO.getCreateTime() > firstCreateTime);
        }
    }

    @Test
    @Order(6)
    public void testPageError() throws Exception {
        //当前页码不大于0
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.setPageSize(5);
        this.requestPost(getProjectList, projectRequest, BAD_REQUEST_MATCHER);
        //当前页数不大于5
        projectRequest = new ProjectRequest();
        projectRequest.setCurrent(1);
        this.requestPost(getProjectList, projectRequest, BAD_REQUEST_MATCHER);
        //当前页数大于100
        projectRequest = new ProjectRequest();
        projectRequest.setCurrent(1);
        projectRequest.setPageSize(101);
        this.requestPost(getProjectList, projectRequest, BAD_REQUEST_MATCHER);
        //排序字段不合法
        projectRequest = new ProjectRequest();
        projectRequest.setCurrent(1);
        projectRequest.setPageSize(5);
        projectRequest.setSort(new HashMap<>() {{
            put("SELECT * FROM user", "asc");
        }});
        this.requestPost(getProjectList, projectRequest, BAD_REQUEST_MATCHER);
    }

    @Test
    @Order(7)
    public void testUpdateProject() throws Exception {
        UpdateProjectRequest project = this.generatorUpdate("organizationId", "projectId1","TestName", "Edit name", true, List.of("admin", "admin1"));
        MvcResult mvcResult = this.responsePost(updateProject, project);
        Project result = this.parseObjectFromMvcResult(mvcResult, Project.class);
        Project currentProject = projectMapper.selectByPrimaryKey(project.getId());
        this.compareProjectDTO(currentProject, result);
        UserRoleRelationExample userRoleRelationExample = new UserRoleRelationExample();
        userRoleRelationExample.createCriteria().andSourceIdEqualTo("projectId1").andRoleIdEqualTo(InternalUserRole.PROJECT_ADMIN.getValue());
        List<UserRoleRelation> userRoleRelations = userRoleRelationMapper.selectByExample(userRoleRelationExample);
        Assertions.assertEquals(userRoleRelations.stream().map(UserRoleRelation::getUserId).collect(Collectors.toList()).containsAll(List.of("admin", "admin1")), true);
        userRoleRelationExample.createCriteria().andSourceIdEqualTo("projectId").andRoleIdEqualTo(InternalUserRole.PROJECT_MEMBER.getValue());
        userRoleRelations = userRoleRelationMapper.selectByExample(userRoleRelationExample);
        Assertions.assertEquals(userRoleRelations.stream().map(UserRoleRelation::getUserId).collect(Collectors.toList()).containsAll(List.of("admin", "admin1")), true);
    }

    @Test
    @Order(8)
    public void testUpdateProjectError() throws Exception {
        //项目名称存在 500
        UpdateProjectRequest project = this.generatorUpdate("organizationId", "projectId","TestName", "description", true, List.of("admin"));
        this.requestPost(updateProject, project, ERROR_REQUEST_MATCHER);
        //参数组织Id为空
        project = this.generatorUpdate(null, "projectId",null, null, true , List.of("admin"));
        this.requestPost(updateProject, project, BAD_REQUEST_MATCHER);
        //项目Id为空
        project = this.generatorUpdate("organizationId", null,null, null, true, List.of("admin"));
        this.requestPost(updateProject, project, BAD_REQUEST_MATCHER);
        //项目名称为空
        project = this.generatorUpdate("organizationId", "projectId",null, null, true, List.of("admin"));
        this.requestPost(updateProject, project, BAD_REQUEST_MATCHER);
        //用户id为空
        project = this.generatorUpdate("organizationId", "projectId","TestName", null, true, null);
        this.requestPost(updateProject, project, BAD_REQUEST_MATCHER);
        //项目不存在
        project = this.generatorUpdate("organizationId", "1111","123", null, true, List.of("admin"));
        MvcResult mvcResult = this.responsePost(updateProject, project);
        String returnData = mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        ResultHolder resultHolder = JsonUtils.parseObject(returnData, ResultHolder.class);
        Assertions.assertNull(resultHolder.getData());

    }

    @Test
    @Order(9)
    public void testDeleteProject() throws Exception {
        String id = "projectId";
        MvcResult mvcResult = this.responseGet(deleteProject + id);
        int count = parseObjectFromMvcResult(mvcResult, Integer.class);
        Project currentProject = projectMapper.selectByPrimaryKey(id);
        Assertions.assertEquals(currentProject.getDeleted(), true);
        Assertions.assertTrue(currentProject.getId().equals(id));
        Assertions.assertTrue(count == 1);
    }

    @Test
    @Order(10)
    public void testDeleteProjectError() throws Exception {
        String id = "1111";
        MvcResult mvcResult = this.responseGet(deleteProject + id);
        int count = parseObjectFromMvcResult(mvcResult, Integer.class);
        Assertions.assertTrue(count == 0);
    }

    @Test
    @Order(11)
    public void revokeSuccess() throws Exception {
        String id = "projectId";
        MvcResult mvcResult = this.responseGet(revokeProject + id);
        int count = parseObjectFromMvcResult(mvcResult, Integer.class);
        Project currentProject = projectMapper.selectByPrimaryKey(id);
        Assertions.assertEquals(currentProject.getDeleted(), false);
        Assertions.assertTrue(currentProject.getId().equals(id));
        Assertions.assertTrue(count == 1);
    }

    @Test
    @Order(12)
    public void testRevokeProjectError() throws Exception {
        String id = "1111";
        MvcResult mvcResult = this.responseGet(revokeProject + id);
        int count = parseObjectFromMvcResult(mvcResult, Integer.class);
        Assertions.assertTrue(count == 0);
    }

    @Test
    @Order(13)
    public void testAddProjectMember() throws Exception{
        ProjectAddMemberRequest projectAddMemberRequest = new ProjectAddMemberRequest();
        projectAddMemberRequest.setProjectId("projectId");
        List<String> userIds = List.of("admin1", "admin2");
        projectAddMemberRequest.setUserIds(userIds);
        this.requestPost(addProjectMember, projectAddMemberRequest, status().isOk());
        UserRoleRelationExample userRoleRelationExample = new UserRoleRelationExample();
        userRoleRelationExample.createCriteria().andSourceIdEqualTo("projectId").andRoleIdEqualTo(InternalUserRole.PROJECT_MEMBER.getValue());
        List<UserRoleRelation> userRoleRelations = userRoleRelationMapper.selectByExample(userRoleRelationExample);
        Assertions.assertEquals(userRoleRelations.stream().map(UserRoleRelation::getUserId).collect(Collectors.toList()).containsAll(userIds), true);
        Assertions.assertTrue(userRoleRelations.stream().map(UserRoleRelation::getUserId).collect(Collectors.toList()).containsAll(userIds));
    }

    @Test
    @Order(14)
    public void testAddProjectMemberError() throws Exception{
        //项目Id为空
        ProjectAddMemberRequest projectAddMemberRequest = new ProjectAddMemberRequest();
        projectAddMemberRequest.setProjectId(null);
        this.requestPost(addProjectMember, projectAddMemberRequest, BAD_REQUEST_MATCHER);
        //用户Id为空
        projectAddMemberRequest = new ProjectAddMemberRequest();
        projectAddMemberRequest.setProjectId("projectId");
        this.requestPost(addProjectMember, projectAddMemberRequest, BAD_REQUEST_MATCHER);
        //用户Id不存在
        projectAddMemberRequest = new ProjectAddMemberRequest();
        projectAddMemberRequest.setProjectId("projectId");
        projectAddMemberRequest.setUserIds(List.of("admin3"));
        this.requestPost(addProjectMember, projectAddMemberRequest, ERROR_REQUEST_MATCHER);
    }

    @Test
    @Order(15)
    public void testGetMemberSuccess() throws Exception {
        ProjectMemberRequest memberRequest = new ProjectMemberRequest();
        memberRequest.setCurrent(1);
        memberRequest.setPageSize(5);
        memberRequest.setProjectId("projectId");
        MvcResult mvcResult = this.responsePost(getProjectMemberList, memberRequest);
        Pager<?> returnPager = parseObjectFromMvcResult(mvcResult, Pager.class);
        //返回值不为空
        Assertions.assertNotNull(returnPager);
        //返回值的页码和当前页码相同
        Assertions.assertEquals(returnPager.getCurrent(), memberRequest.getCurrent());
        //返回的数据量不超过规定要返回的数据量相同
        Assertions.assertTrue(((List<UserExtend>) returnPager.getList()).size() <= memberRequest.getPageSize());
        memberRequest.setSort(new HashMap<>() {{
            put("createTime", "desc");
        }});
        mvcResult = this.responsePost(getProjectMemberList, memberRequest);
        returnPager = parseObjectFromMvcResult(mvcResult, Pager.class);
        //第一个数据的createTime是最大的
        List<UserExtend> userExtends = JSON.parseArray(JSON.toJSONString(returnPager.getList()), UserExtend.class);
        long firstCreateTime = userExtends.get(0).getCreateTime();
        for (UserExtend userExtend : userExtends) {
            Assertions.assertFalse(userExtend.getCreateTime() > firstCreateTime);
        }
    }

    @Test
    @Order(16)
    public void testGetMemberError() throws Exception {
        //当前页码不大于0
        ProjectMemberRequest memberRequest = new ProjectMemberRequest();
        memberRequest.setPageSize(5);
        this.requestPost(getProjectMemberList, memberRequest, BAD_REQUEST_MATCHER);
        //当前页数不大于5
        memberRequest = new ProjectMemberRequest();
        memberRequest.setCurrent(1);
        this.requestPost(getProjectMemberList, memberRequest, BAD_REQUEST_MATCHER);
        //当前页数大于100
        memberRequest = new ProjectMemberRequest();
        memberRequest.setCurrent(1);
        memberRequest.setPageSize(101);
        this.requestPost(getProjectMemberList, memberRequest, BAD_REQUEST_MATCHER);
        //项目Id为空
        memberRequest = new ProjectMemberRequest();
        memberRequest.setCurrent(1);
        memberRequest.setPageSize(5);
        this.requestPost(getProjectMemberList, memberRequest, BAD_REQUEST_MATCHER);
        //排序字段不合法
        memberRequest = new ProjectMemberRequest();
        memberRequest.setCurrent(1);
        memberRequest.setPageSize(5);
        memberRequest.setProjectId("projectId1");
        memberRequest.setSort(new HashMap<>() {{
            put("SELECT * FROM user", "asc");
        }});
        this.requestPost(getProjectMemberList, memberRequest, BAD_REQUEST_MATCHER);
    }

    @Test
    @Order(17)
    public void testRemoveProjectMember() throws Exception{
        String projectId = "projectId1";
        String userId = "admin1";
        MvcResult mvcResult = this.responseGet(removeProjectMember + projectId + "/" + userId);
        int count = parseObjectFromMvcResult(mvcResult, Integer.class);
        Assertions.assertTrue(count == 1);
    }

    @Test
    @Order(18)
    public void testRemoveProjectMemberError() throws Exception{
        String projectId = "projectId1";
        String userId = "admin1";
        MvcResult mvcResult = this.responseGet(removeProjectMember + projectId + "/" + userId);
        int count = parseObjectFromMvcResult(mvcResult, Integer.class);
        Assertions.assertTrue(count == 0);
    }

}