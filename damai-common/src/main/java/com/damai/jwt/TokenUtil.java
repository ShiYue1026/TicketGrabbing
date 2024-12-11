package com.damai.jwt;

import com.alibaba.fastjson.JSONObject;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class TokenUtil {
    private static final SignatureAlgorithm SIGNATURE_ALGORITHM = SignatureAlgorithm.HS256;

    /**
     * 用户登录成功后生成Jwt
     * 使用Hs256算法  私匙使用用户密码
     *
     * @param id        标识
     * @param info      登录成功的user对象
     * @param ttlMillis jwt过期时间
     * @param tokenSecret 私钥
     * @return
     */
    public static String createToken(String id, String info, long ttlMillis, String tokenSecret) {
        long nowMillis = System.currentTimeMillis();

        JwtBuilder builder = Jwts.builder()
                                 .setId(id)
                                 .setIssuedAt(new Date(nowMillis))
                                 .setSubject(info)
                                 .signWith(SIGNATURE_ALGORITHM, tokenSecret);

        // 设置jwt令牌过期时间
        if(ttlMillis >= 0){
            builder.setExpiration(new Date(nowMillis + ttlMillis));
        }
        return builder.compact();
    }

    /**
     * Token的解密
     *
     * @param token 加密后的token
     * @param tokenSecret 私钥
     * @return
     */
    public static String parseToken(String token, String tokenSecret) {
        return Jwts.parser()
                   .setSigningKey(tokenSecret)
                   .parseClaimsJws(token)
                   .getBody()
                   .getSubject();
    }

    public static void main(String[] args) {
        String tokenSecret = "AFDBHDV";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", "value1");
        jsonObject.put("key2", "value2");

        String token1 = TokenUtil.createToken("1", jsonObject.toJSONString(), 3600 * 1000, tokenSecret);
        System.out.println("token:" + token1);

        String token2 = token1;
        String subject = TokenUtil.parseToken(token2, tokenSecret);
        System.out.println("解析token后的值:" + subject);
    }
}
