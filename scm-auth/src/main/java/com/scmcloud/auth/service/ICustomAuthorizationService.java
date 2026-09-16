package com.scmcloud.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.Set;

/**
 *
 *
 * @author Deng
 * createData 2025/11/13 10:46
 * @version 1.0
 */
public interface ICustomAuthorizationService {

    /**
     * 鍒涘缓 OAuth2 鎺堟潈
     *
     * @param client 宸叉敞鍐岀殑瀹㈡埛锟?
     * @param principal 璁よ瘉涓讳綋
     * @param authorizedScopes 宸叉巿鏉冪殑浣滅敤鍩熼泦锟?
     * @return OAuth2 鎺堟潈瀵硅薄
     */
    OAuth2Authorization createAuthorization(RegisteredClient client,
                                            Authentication principal,
                                            Set<String> authorizedScopes);
}
