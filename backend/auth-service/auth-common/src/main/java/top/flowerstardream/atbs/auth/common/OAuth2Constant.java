package top.flowerstardream.atbs.auth.common;

import java.util.List;

/**
 * @Author: 花海
 * @Date: 2026/03/18/02:05
 * @Description: OAuth2常量类
 */
public class OAuth2Constant {
    public static final String RESPONSE_TYPE = "response_type";
    public static final String GRANT_TYPE = "grant_type";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String CODE = "code";
    public static final String STATE = "state";
    public static final String SCOPE = "scope";
    public static final String USER_ID = "user_id";
    public static final String CODE_VERIFIER = "code_verifier";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
    public static final String PASSWORD_GRANT_TYPE = "password";
    public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    public static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
    public static final String WECHAT_MINI_GRANT_TYPE = "wechat_mini";
    public static final String CODE_RES_TYPE = "code";
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";
    public static final String CLIENT_SECRET_POST = "client_secret_post";
    public static final String OPENID_SCOPE = "openid";
    public static final String PROFILE_SCOPE = "profile";
    public static final String READ_SCOPE = "read";
    public static final String WRITE_SCOPE = "write";
    public static final String ADMIN_SCOPE = "admin";
    public static final String INTERNAL_SCOPE = "internal";
    public static final String TIMESTAMP_SCOPE = "timestamp";
    public static final String NONCE_STR_SCOPE = "nonce_str";
    public static final String INVITE_CODE_SCOPE = "invite_code";
    public static final String SCENE_SCOPE = "scene";
    public static final String OFFLINE_ACCESS_SCOPE = "offline_access";
    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";
    public static final String CODE_CHALLENGE = "code_challenge";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final Integer AUTH_CODE_TTL_MINUTES = 5;

    public static final String AUTHORIZATION_ENDPOINT = "/oauth/authorize";
    public static final String TOKEN_ENDPOINT = "/oauth/token";
    public static final String REVOKE_TOKEN_ENDPOINT = "/oauth/revoke";
    public static final String INTROSPECTION_ENDPOINT = "/oauth/introspection";
    public static final String USER_INFO_ENDPOINT = "/userinfo";
    public static final String JWKS_ENDPOINT = "/.well-known/jwks.json";
    public static final List<String> GRANT_TYPES_SUPPORTED = List.of(AUTHORIZATION_CODE_GRANT_TYPE, PASSWORD_GRANT_TYPE, CLIENT_CREDENTIALS_GRANT_TYPE, REFRESH_TOKEN_GRANT_TYPE, WECHAT_MINI_GRANT_TYPE);
    public static final List<String> RESPONSE_TYPES_SUPPORTED = List.of(CODE_RES_TYPE);
    public static final List<String> TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED = List.of(CLIENT_SECRET_BASIC, CLIENT_SECRET_POST);
    public static final List<String> SCOPES_SUPPORTED = List.of(OPENID_SCOPE, PROFILE_SCOPE, READ_SCOPE,
                                                                WRITE_SCOPE, ADMIN_SCOPE, INTERNAL_SCOPE,
                                                                OFFLINE_ACCESS_SCOPE, TIMESTAMP_SCOPE, NONCE_STR_SCOPE,
                                                                INVITE_CODE_SCOPE, SCENE_SCOPE);
    public static final List<String> CODE_CHALLENGE_METHODS_SUPPORTED = List.of(CODE_CHALLENGE_METHOD_S256);


}

