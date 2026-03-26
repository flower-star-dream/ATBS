package top.flowerstardream.atbs.auth.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import top.flowerstardream.atbs.auth.ao.req.*;
import top.flowerstardream.atbs.auth.ao.res.WxLoginTokenRES;
import top.flowerstardream.atbs.auth.biz.client.UserClient;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserMapper;
import top.flowerstardream.atbs.auth.biz.mapper.AuthUserSocialMapper;
import top.flowerstardream.atbs.auth.biz.service.IAuthRoleService;
import top.flowerstardream.atbs.auth.biz.service.IAuthUserService;
import top.flowerstardream.atbs.auth.biz.service.IUserSocialService;
import top.flowerstardream.atbs.auth.bo.eo.AuthUserEO;
import top.flowerstardream.atbs.auth.bo.eo.RoleEO;
import top.flowerstardream.atbs.auth.bo.eo.UserSocialEO;
import top.flowerstardream.base.properties.JwtProperties;
import top.flowerstardream.base.properties.WeChatProperties;
import top.flowerstardream.base.result.PageResult;
import top.flowerstardream.base.result.Result;
import top.flowerstardream.base.service.Impl.BaseServiceImpl;
import top.flowerstardream.base.state.BaseStatus;
import top.flowerstardream.base.utils.HttpClientUtil;
import top.flowerstardream.base.utils.JwtUtil;
import top.flowerstardream.base.utils.WechatDecryptUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static top.flowerstardream.atbs.auth.common.AuthConstant.DEFAULT_USER_ROLE;
import static top.flowerstardream.atbs.auth.common.AuthExceptionEnum.*;
import static top.flowerstardream.atbs.auth.common.WxConstant.WX_PLACEHOLDER_PASSWORD;
import static top.flowerstardream.atbs.tools.constants.CommonConstant.WX_USER_PREFIX;
import static top.flowerstardream.base.exception.BaseExceptionEnum.*;
import static top.flowerstardream.base.exception.ExceptionEnum.*;

/**
 * @Author: 花海
 * @Date: 2026/03/18/23:57
 * @Description: 用户服务实现
 */
@Service
@Slf4j
public class AuthUserServiceImpl extends BaseServiceImpl<AuthUserMapper, AuthUserEO> implements IAuthUserService {

    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

    @Resource
    private UserClient userClient;

    @Resource
    private AuthUserMapper userMapper;

    @Resource
    private AuthUserSocialMapper userSocialMapper;

    @Resource
    private WeChatProperties weChatProperties;

    @Resource
    private BCryptPasswordEncoder passwordEncoder;

    @Resource
    private IUserSocialService userSocialService;

    @Resource
    private IAuthRoleService authRoleService;

    @Resource
    @Lazy
    private IAuthUserService self;

    /**
     * 微信登录
     *
     * @param loginREQ 微信登录请求
     * @return 登录响应
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public WxLoginTokenRES wechatLogin(WechatLoginREQ loginREQ) {
        // 1. 调用微信接口服务，获取当前微信用户的openid
        Map<String, String> res = getWeChatRES(loginREQ.getCode());
        String openid = res.get("openid");
        String sessionKey = res.get("session_key");
        String unionid = res.get("unionid");
        JSONObject json = new JSONObject();
        json.put("sessionKey", sessionKey);
        // 判断openid是否为空，为空则表示登录失败，抛出业务异常
        if (openid == null) {
            throw OPENID_CANNOT_BE_EMPTY.toException();
        }

        // 2. 根据openid查询用户信息，判断用户是否为新用户
        UserSocialEO social = userSocialService.getUserSocialByOpenId(openid);
        AuthUserEO user;
        if (social == null) {
            // 3. 获取当前微信用户的手机号
            String phoneNumber = WechatDecryptUtil.getPhoneNumber(loginREQ.getEncryptedData(), sessionKey, loginREQ.getIv());
            // 如果是新用户则创建用户信息，自动完成注册，否则直接返回用户信息
            LambdaQueryWrapper<AuthUserEO> userWrapper = new LambdaQueryWrapper<>();
            userWrapper.eq(AuthUserEO::getPhone, phoneNumber);
            user = userMapper.selectOne(userWrapper);

            if (user == null) {
                user = AuthUserEO.builder()
                        .username(WX_USER_PREFIX + UUID.randomUUID())
                        .password(WX_PLACEHOLDER_PASSWORD)
                        .phone(phoneNumber)
                        .status(BaseStatus.ENABLE)
                        .build();
                self.save(user);
                authRoleService.setRolesByUserId(user.getId(), Collections.singletonList(DEFAULT_USER_ROLE));
            }
            social = UserSocialEO.builder()
                    .userId(user.getId())
                    .platform("wechat_mini")
                    .openid(openid)
                    .unionid(unionid)
                    .extraData(json.toJSONString())
                    .platformNickname(loginREQ.getUserInfo().getNickname())
                    .platformAvatar(loginREQ.getUserInfo().getAvatarUrl())
                    .build();
            userSocialMapper.insert(social);
            UserSynchronizeREQ userSynchronizeREQ = UserSynchronizeREQ.builder()
                .id(user.getId())
                .openid(social.getOpenid())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(BaseStatus.ENABLE)
                .build();
            userClient.wxRegister(userSynchronizeREQ);
        } else {
            user = self.getById(social.getUserId());
        }

        // 5. 返回登录响应
        return WxLoginTokenRES.builder()
                .id(user.getId())
                .openid(social.getOpenid())
                .username(user.getUsername())
                .build();
    }

    /**
     * 更新当前用户信息
     *
     * @param userInfoREQ
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void updateInfo(UserInfoREQ userInfoREQ) {
        AuthUserEO authUserEO = new AuthUserEO();
        BeanUtils.copyProperties(userInfoREQ, authUserEO);
        if (!self.updateById(authUserEO)) {
            log.error("更新当前登录用户信息失败：{}", authUserEO);
            throw MODIFICATION_FAILED.toException();
        }
        toSynchronization(authUserEO);
    }

    /**
     * 获取统一用户列表
     *
     * @param userPageQueryREQ
     * @return
     */
    @Override
    public PageResult<AuthUserEO> list(UserPageQueryREQ userPageQueryREQ) {
        return userMapper.autoPage(userPageQueryREQ, true);
    }

    /**
     * 新增用户账号
     *
     * @param authUserREQ
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void add(AuthUserREQ authUserREQ) {
        validateAuthUserIsNotEmpty(authUserREQ.getUsername(), authUserREQ.getPhone(), authUserREQ.getEmail());
        AuthUserEO authUserEO = new AuthUserEO();
        BeanUtils.copyProperties(authUserREQ, authUserEO);
        if (authUserREQ.getId() != null) {
            authUserEO.setId(null);
        }
        authUserEO.setPassword(passwordEncoder.encode(authUserEO.getPassword()));
        authRoleService.setRolesByUserId(authUserEO.getId(), Collections.singletonList(DEFAULT_USER_ROLE));
        if (!self.save(authUserEO)) {
            log.error("新增用户账号失败：{}", authUserEO);
            throw INSERTION_FAILED.toException();
        }
        toSynchronization(authUserEO);
    }

    /**
     * 修改用户账号
     *
     * @param authUserREQ
     */
    @Override
    @GlobalTransactional(rollbackFor = Exception.class)
    public void update(AuthUserREQ authUserREQ) {
        validateAuthUserIsEmpty(authUserREQ.getUsername(), authUserREQ.getPhone(), authUserREQ.getEmail());
        log.info("更新当前登录用户id：{}", authUserREQ.getId());
        AuthUserEO authUserEO = new AuthUserEO();
        BeanUtils.copyProperties(authUserREQ, authUserEO);
        if (authUserREQ.getPassword() != null) {
            authUserEO.setPassword(passwordEncoder.encode(authUserREQ.getPassword()));
        }
        log.info("更新当前登录用户信息：{}", authUserEO);
        if (!self.updateById(authUserEO)) {
            log.error("更新用户账号失败：{}", authUserEO);
            throw MODIFICATION_FAILED.toException();
        }
        toSynchronization(authUserEO);
    }

    /**
     * 重置密码
     *
     * @param resetPwdREQ
     */
    @Override
    public void resetPassword(ResetPwdREQ resetPwdREQ) {
        if (resetPwdREQ == null) {
            throw THE_QUERY_PARAMETER_CANNOT_BE_EMPTY.toException();
        }
        AuthUserEO authUserEO = getById(resetPwdREQ.getId());
        if (authUserEO == null) {
            throw USER_NOT_EXIST.toException();
        }
        if (!passwordEncoder.matches(resetPwdREQ.getOldPwd(), authUserEO.getPassword())) {
            throw THE_OLD_PASSWORD_IS_INCORRECT.toException();
        }
        if (Objects.equals(resetPwdREQ.getOldPwd(), resetPwdREQ.getNewPwd())) {
            throw THE_OLD_PASSWORD_CANNOT_EQUALS_NEW_ONE.toException();
        }
        if (!Objects.equals(resetPwdREQ.getNewPwd(), resetPwdREQ.getConfirmPwd())){
            throw THE_NEW_PASSWORD_IS_INCONSISTENT_WITH_THE_CONFIRMED_PASSWORD.toException();
        }
        AuthUserEO newAuthUser = AuthUserEO.builder()
                .id(resetPwdREQ.getId())
                .password(passwordEncoder.encode(resetPwdREQ.getNewPwd()))
                .build();
        boolean update = updateById(newAuthUser);
        if (!update) {
            log.error("更新密码失败：{}", newAuthUser);
            throw MODIFICATION_FAILED.toException();
        }
    }

    /**
     * 同步用户账号
     * @param userSynchronizeREQ
     */
    @Override
    public Long synchronize(UserSynchronizeREQ userSynchronizeREQ) {
        log.info("同步用户账号: {}", userSynchronizeREQ);
        AuthUserEO userEO = self.getById(userSynchronizeREQ.getId());
        if (userEO == null) {
            AuthUserEO authUserEO = new AuthUserEO();
            BeanUtils.copyProperties(userSynchronizeREQ, authUserEO);
            authUserEO.setId(null);
            authUserEO.setPassword(passwordEncoder.encode(authUserEO.getPassword()));
            authUserEO.setStatus(BaseStatus.ENABLE);
            if (!self.save(authUserEO)) {
                log.error("同步新增用户账号失败：{}", authUserEO);
                throw INSERTION_FAILED.toException();
            }
            if (StrUtil.isNotBlank(userSynchronizeREQ.getPermissionLevel())) {
                authRoleService.setRolesByUserId(authUserEO.getId(), Collections.singletonList(userSynchronizeREQ.getPermissionLevel()));
            } else {
                authRoleService.setRolesByUserId(authUserEO.getId(), Collections.singletonList(DEFAULT_USER_ROLE));
            }
            return authUserEO.getId();
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getUsername())){
            userEO.setUsername(userSynchronizeREQ.getUsername());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getPhone())) {
            userEO.setPhone(userSynchronizeREQ.getPhone());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getEmail())) {
            userEO.setEmail(userSynchronizeREQ.getEmail());
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getPassword()) && userEO.getPassword().equals(WX_PLACEHOLDER_PASSWORD)){
            userEO.setPassword(passwordEncoder.encode(userSynchronizeREQ.getPassword()));
        }
        if (StrUtil.isNotBlank(userSynchronizeREQ.getPermissionLevel())) {
            authRoleService.setRolesByUserId(userEO.getId(), Collections.singletonList(userSynchronizeREQ.getPermissionLevel()));
        }
        if (!self.updateById(userEO)) {
            log.error("同步用户账号失败：{}", userEO);
            throw MODIFICATION_FAILED.toException();
        }
        return userEO.getId();
    }

    /**
     * 批量获取用户名称
     *
     * @param userIds
     * @return
     */
    @Override
    public Result<Map<Long, String>> batchGetNames(List<Long> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            List<AuthUserEO> records = self.listByIds(userIds);
            if (records != null && !records.isEmpty()) {
                return Result.successResult(records.stream().collect(Collectors.toMap(AuthUserEO::getId, AuthUserEO::getUsername)));
            }
        }
        return Result.successResult(Map.of());
    }

    /**
     * 通过用户名或手机号或邮箱查询用户账号
     * @param username
     * @return
     */
    private AuthUserEO getAuthUser(String username, String phone, String email) {
        AuthUserEO authUserEO = null;
        if(StringUtils.isNotBlank(username)){
            authUserEO = getOne(Wrappers.<AuthUserEO>lambdaQuery()
                    .eq(AuthUserEO::getUsername, username)
                    .or()
                    .eq(AuthUserEO::getPhone, phone)
                    .or()
                    .eq(AuthUserEO::getEmail, email)
            );
        }
        return authUserEO;
    }

    /**
     * 验证用户账号是否为空
     * @param username
     * @return
     */
    private AuthUserEO validateAuthUserIsEmpty(String username, String phone, String email) {
        AuthUserEO authUserEO = getAuthUser(username, phone, email);
        if (authUserEO != null) {
            return authUserEO;
        }
        throw USER_NOT_EXIST.toException();
    }

    /**
     * 验证用户账号是否非空
     * @param username
     * @param phone
     * @return
     */
    private void validateAuthUserIsNotEmpty(String username, String phone, String email) {
        AuthUserEO authUserEO = getAuthUser(username, phone, email);
        if (authUserEO == null) {
            return; // 用户不存在
        }
        throw USER_ALREADY_EXISTS.toException();
    }

    /**
     * 调取微信接口服务，获取微信用户的openid
     * @param code
     * @return
     */
    private Map<String, String> getWeChatRES(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        Map<String, String> res = new HashMap<>();
        res.put("openid", jsonObject.getString("openid"));
        res.put("session_key", jsonObject.getString("session_key"));
        res.put("unionid", jsonObject.getString("unionid"));
        return res;
    }

    private void toSynchronization(AuthUserEO authUserEO){
        List<RoleEO> rolesByUserId = authRoleService.getRolesByUserId(authUserEO.getId());
        String permissionLevel = rolesByUserId != null && !rolesByUserId.isEmpty() ? rolesByUserId.get(0).getRoleCode() : DEFAULT_USER_ROLE;
        UserSynchronizeREQ userSynchronizeREQ = UserSynchronizeREQ.builder()
                .id(authUserEO.getId())
                .username(authUserEO.getUsername())
                .phone(authUserEO.getPhone())
                .email(authUserEO.getEmail())
                .status(authUserEO.getStatus() == BaseStatus.DISABLE ? BaseStatus.DISABLE : null)
                .permissionLevel(permissionLevel)
                .build();
        userClient.synchronizationUserInfo(userSynchronizeREQ);
    }
}
