/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.log.util

import org.apache.http.HeaderElementIterator
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.CredentialsProvider
import org.apache.http.message.BasicHeaderElementIterator
import org.apache.http.protocol.HTTP
import org.apache.http.protocol.HttpContext
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.slf4j.LoggerFactory
import javax.net.ssl.SSLContext

object ESConfigUtils {

    fun getClientBuilder(
        httpHost: HttpHost,
        tcpKeepAliveSeconds: Long,
        sslContext: SSLContext?,
        credentialsProvider: CredentialsProvider?
    ): RestClientBuilder {
        // 初始化 RestClient 配置
        val builder = RestClient.builder(httpHost)

        // HTTP连接设置
        return builder.setHttpClientConfigCallback { httpClientBuilder ->
            if (sslContext != null) httpClientBuilder.setSSLContext(sslContext)
            if (credentialsProvider != null) httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            httpClientBuilder.setKeepAliveStrategy { response: HttpResponse, context: HttpContext? ->
                try {
                    val headers: HeaderElementIterator = BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE))
                    while (headers.hasNext()) {
                        val headerElement = headers.nextElement()
                        val param = headerElement.name
                        val value = headerElement.value
                        if (value != null && param.equals("timeout", ignoreCase = true)) {
                            return@setKeepAliveStrategy value.toLong() * 1000
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Fetch cluster KeepAliveStrategy error, context: $context", e)
                }
                // 默认30秒刷新超时
                tcpKeepAliveSeconds * 1000
            }
            httpClientBuilder
        }.setRequestConfigCallback {
            // 默认3秒连接超时
            requestConfigBuilder -> requestConfigBuilder.setSocketTimeout(3000)
        }
    }

    private val logger = LoggerFactory.getLogger(ESConfigUtils::class.java)
}
