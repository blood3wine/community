package com.nowcoder.community.util;

import net.minidev.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

public class CommunityUtil {
    //生成随机字符串 【激活码 【上传文件的随机名字
    public static String generateUUID(){
        //不想要- 替换掉
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    //MD5加密 它只能加密不能解密 每次加密同一个东西 都是同一个值
    // 提交注册密码时是明文 存的时候加密
    //hello -> abc123def456
    //hello + 3e4a8 -> abc123def456abc【3e4a8是随机的 破解难度++
    // key是hello + 3e4a8【3e4a8是salt字段
    public static String md5(String key){
        if(StringUtils.isBlank(key)){
            return null;
        }
        //加密成16进制字符串
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    //?????
    public static String getJSONString(int code, String msg, Map<String, Object> map){
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        if(map != null){
            for(String key : map.keySet()){
                json.put(key, map.get(key));
            }
        }
        return json.toJSONString();
    }

    public static String getJSONString(int code, String msg){
        return getJSONString(code, msg, null);
    }

    public static String getJSONString(int code){
        return getJSONString(code, null, null);
    }
}
