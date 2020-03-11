package com.xxl.job.executor.service.jobhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * 跨平台Http任务
 *
 * @author xuxueli 2018-09-16 03:48:34
 */
@JobHandler(value = "httpJobHandler")
@Component
public class HttpJobHandler extends IJobHandler {

    @Override
    public ReturnT<String> execute(String param) throws Exception {

        // request
        HttpURLConnection connection = null;
        BufferedReader bufferedReader = null;
        try {
            // connection
            URL realUrl = new URL(param);
            connection = (HttpURLConnection) realUrl.openConnection();

            // connection setting
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(5 * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            // do connection
            connection.connect();

            //Map<String, List<String>> map = connection.getHeaderFields();

            // valid StatusCode
            int statusCode = connection.getResponseCode();
            if (statusCode != 200) {
                throw new RuntimeException("Http Request StatusCode(" + statusCode + ") Invalid.");
            }

            // result
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                result.append(line);
            }
            String responseMsg = result.toString();

            XxlJobLogger.log(responseMsg);

            // 反序列化响应结果
            Map<String, Object> resultMap = new ObjectMapper().readValue(responseMsg, Map.class);
            resultMap = readFromEpinn(resultMap);
            int code = Integer.parseInt(resultMap.get("code").toString());
            if (code == ReturnT.SUCCESS_CODE) {
                return ReturnT.SUCCESS;
            }
            String msg = String.valueOf(resultMap.get("msg"));
            return new ReturnT<>(ReturnT.FAIL_CODE, msg);
        } catch (Exception e) {
            XxlJobLogger.log(e);
            return FAIL;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                XxlJobLogger.log(e2);
            }
        }

    }

    /**
     * 对来自Epinn的响应结果进行格式转换
     *
     * @param sourceMap
     * @return
     */
    private Map<String, Object> readFromEpinn(Map<String, Object> sourceMap) {
        if (sourceMap.containsKey("ok") == false) {
            return sourceMap;
        }
        Map<String, Object> result = new HashMap<>();
        boolean isOk = Boolean.parseBoolean(sourceMap.get("ok").toString());
        if (isOk) {
            result.put("code", ReturnT.SUCCESS_CODE);
        } else {
            String msg = String.valueOf(sourceMap.get("message"));

            result.put("code", ReturnT.FAIL_CODE);
            result.put("msg", msg);
        }
        return result;
    }
}
