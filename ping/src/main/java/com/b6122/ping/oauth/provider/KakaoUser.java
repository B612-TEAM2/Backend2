package com.b6122.ping.oauth.provider;

import java.util.Map;

public class KakaoUser implements OAuthUser {

    private Map<String, Object> attribute;

    public KakaoUser(Map<String, Object> attribute) {
        this.attribute = attribute;
    }

    @Override
    public String getProviderId() {
        return (String)attribute.get("providerId");
    }

    @Override
    public String getProvider() {
        return (String)attribute.get("provider");
    }

    @Override
    public String getName() {
        return (String)attribute.get("username");
    }

}
