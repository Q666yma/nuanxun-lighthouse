package com.maka.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.maka.pojo.Admin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class JwtUtil {


    public static final long TTL = 30*60*100;
    public static final String SECRET = System.getenv("JWT_SECRET");

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    public static String createToken(String username, long ttl, String secret){
        Algorithm singer = Algorithm.HMAC256(secret);
        String token = JWT.create()
                .withClaim("username",username)
                .withExpiresAt(new Date(System.currentTimeMillis() + ttl))
                .sign(singer);
        logger.info("token: {}",token);
        return  token;
    }

    public static Boolean verifyToken(String token,String userName,String secret){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm)
                .withClaim("username",userName)
                .build();
        verifier.verify(token);
        return true;
    }

    public static String getUserNameByToken(String token){
        DecodedJWT decode = JWT.decode(token);
        return decode.getClaim("username").asString();
    }



}
