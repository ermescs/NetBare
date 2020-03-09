package com.github.megatronking.netbare.http;

import androidx.annotation.NonNull;

import com.github.megatronking.netbare.gateway.Request;
import com.github.megatronking.netbare.gateway.Response;
import com.github.megatronking.netbare.gateway.VirtualGateway;
import com.github.megatronking.netbare.gateway.VirtualGatewayFactory;
import com.github.megatronking.netbare.net.Session;
import com.github.megatronking.netbare.ssl.JKS;

import java.util.ArrayList;
import java.util.List;

public class HttpSwitchableVirtualGatewayFactory implements VirtualGatewayFactory {

    private List<HttpInterceptorFactory> mFactories;
    private JKS mJKS;
    public static Boolean enableSSL = true;

    /**
     * Constructs a {@link HttpSwitchableVirtualGatewayFactory} instance with {@link JKS} and a collection of
     * {@link HttpInterceptorFactory}.
     *
     * @param factories a collection of {@link HttpInterceptorFactory}.
     * @return A instance of {@link HttpSwitchableVirtualGatewayFactory}.
     */
    public HttpSwitchableVirtualGatewayFactory(JKS jks,
                                               @NonNull List<HttpInterceptorFactory> factories) {
        this.mJKS = jks;
        this.mFactories = factories;
    }

    @Override
    public VirtualGateway create(Session session, Request request, Response response) {
        if (enableSSL)
            return new HttpVirtualGateway(session, request, response, mJKS, new ArrayList<>(mFactories));
        else
            return new HttpVirtualGateway(session, request, response, null, new ArrayList<>(mFactories));
    }

    /**
     * Create a {@link HttpSwitchableVirtualGatewayFactory} instance with {@link JKS} and a collection of
     * {@link HttpInterceptorFactory}.
     *
     * @param factories a collection of {@link HttpInterceptorFactory}.
     * @return A instance of {@link HttpSwitchableVirtualGatewayFactory}.
     */
    public static VirtualGatewayFactory create(JKS authority,
                                               @NonNull List<HttpInterceptorFactory> factories) {
        return new HttpSwitchableVirtualGatewayFactory(authority, factories);
    }

}