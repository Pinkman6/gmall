package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\Learning\\IDEAWorkspace\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\Learning\\IDEAWorkspace\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "132&*_=#jk");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDI2NDcwMDN9.lWzmzOZ9q5rruekv3SHZz0rZxBxc1USYad4WPIITV8h6AERGzGOCsUvaxJuAUWrF0gD0nmb90zZWDQNrhdT0bN8O06yT960D9RieW6WOME17cFIDSR-8SRQa-XInpO6LEWm6SW9y3uWsaUNADfGEzOYKt8wYKlyyyYVNIp1yuwj6944VAhsfUtzDVO1cWGhdS4SAQ17H8D69urf7d3UKJ-E5DAOddtLaIgCXxuH3WqFRnqaF3penYbadhAO6z_52A_UNrUUMAIuuT2K_4W0yAs54PNMvZerkAnpW21BbZ2P7WKFpAJxMI8c2g61bb0y4_Up5fbNEy-hALcfvCf8iJA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}