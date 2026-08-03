package com.ea.common.constant;

/**
 * 通用常量
 */
public final class Constants {

    private Constants() {
    }

    public static final String UTF8 = "UTF-8";

    /** Token 请求头 */
    public static final String TOKEN_HEADER = "Authorization";

    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 登录用户 Redis Key 前缀 */
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    /** 验证码 Redis Key 前缀 */
    public static final String CAPTCHA_CODE_KEY = "captcha:code:";

    /** 默认页码 */
    public static final int DEFAULT_PAGE_NUM = 1;

    /** 默认每页条数 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 排序字段：升序 */
    public static final String ASC = "asc";

    /** 排序字段：降序 */
    public static final String DESC = "desc";
}
